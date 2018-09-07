package io.github.agentsoz.bdimatsim;

import java.util.*;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.util.Disruption;
import io.github.agentsoz.util.EmergencyMessage;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import io.github.agentsoz.util.Location;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlayPauseSimulationControl;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.nonmatsim.PAAgentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author QingyuChen, KaiNagel, Dhi Singh
 */
public final class MATSimModel implements ABMServerInterface, QueryPerceptInterface, DataClient {
	private static final Logger log = LoggerFactory.getLogger(MATSimModel.class);
	public static final String MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR = "--matsim-output-directory";

	public static final String eGlobalStartHhMm = "startHHMM";
	private static final String eConfigFile = "configXml";
	private static final String eOutputDir = "outputDir";
	private static final String eMaxDistanceForFireVisual = "maxDistanceForFireVisual";
	private static final String eMaxDistanceForSmokeVisual = "maxDistanceForSmokeVisual";
	private static final String eFireAvoidanceBufferForVehicles = "fireAvoidanceBufferForVehicles";
	private static final String eFireAvoidanceBufferForEmergencyVehicles = "fireAvoidanceBufferForEmergencyVehicles";
	private static final String eCongestionEvaluationInterval = "congestionEvaluationInterval";
	private static final String eCongestionToleranceThreshold = "congestionToleranceThreshold";
	private static final String eCongestionReactionProbability = "congestionReactionProbability";

	// Defaults
	private String optConfigFile = null;
	private String optOutputDir = null;
	private double optMaxDistanceForFireVisual = 1000;
	private double optMaxDistanceForSmokeVisual = 3000;
	private double optFireAvoidanceBufferForVehicles = 10000;
	private double optFireAvoidanceBufferForEmergencyVehicles = 1000;
	private double optStartTimeInSeconds = 1.0;
	private double optCongestionEvaluationInterval = 180;
	private double optCongestionToleranceThreshold = 0.25;
	private double optCongestionReactionProbability = 0.10;


	private EvacConfig evacConfig = null;
	private FireWriter fireWriter = null;
	private EmberWriter emberWriter = null;
	private DisruptionWriter disruptionWriter = null;
	private Config config = null;
	private boolean configLoaded = false;
    private Object sequenceLock;

    public enum EvacRoutingMode {carFreespeed, carGlobalInformation, emergencyVehicle}

	private Scenario scenario = null;

	/**
	 * A helper class essentially provided by the framework, used here.  The only direct connection to matsim are the event
	 * monitors, which need to be registered, via the events monitor registry, as a matsim events handler.
	 */
	private PAAgentManager agentManager = null;

	/**
	 * This is in fact a MATSim class that provides a view onto the QSim.
	 */
	private final MobsimDataProvider mobsimDataProvider = new MobsimDataProvider() ;

	private QSim qSim;

	/**
	 * can be null (so non-final is ok)
	 */
	private List<EventHandler> eventHandlers;

	private PlayPauseSimulationControl playPause;
	private final EventsMonitorRegistry eventsMonitors  = new EventsMonitorRegistry() ;
	private Thread matsimThread;
	@Inject private Replanner replanner;

	private boolean scenarioLoaded = false ;

	private final Map<Id<Link>,Double> penaltyFactorsOfLinks = new HashMap<>() ;
	private final Map<Id<Link>,Double> penaltyFactorsOfLinksForEmergencyVehicles = new HashMap<>() ;

	private DataServer dataServer;
	private final Map<String, DataClient> dataListeners = createDataListeners();
	private io.github.agentsoz.bdiabm.v2.AgentDataContainer adc = new io.github.agentsoz.bdiabm.v2.AgentDataContainer();

	public MATSimModel(Map<String, String> opts, DataServer dataServer) {
		this(opts.get(eConfigFile), opts.get(eOutputDir), opts.get(eGlobalStartHhMm));
		registerDataServer(dataServer);
		if (opts == null) {
			return;
		}
		for (String opt : opts.keySet()) {
			log.info("Found option: {}={}", opt, opts.get(opt));
			switch(opt) {
				case eGlobalStartHhMm:
					optStartTimeInSeconds = convertTimeToSeconds(opts.get(opt).replace(":", ""));
					break;
				case eConfigFile:
					optConfigFile = opts.get(opt);
					break;
				case eOutputDir:
					optOutputDir = opts.get(opt);
					break;
				case eMaxDistanceForFireVisual:
					optMaxDistanceForFireVisual = Double.parseDouble(opts.get(opt));
					break;
				case eMaxDistanceForSmokeVisual:
					optMaxDistanceForSmokeVisual = Double.parseDouble(opts.get(opt));
					break;
				case eFireAvoidanceBufferForVehicles:
					optFireAvoidanceBufferForVehicles = Double.parseDouble(opts.get(opt));
					break;
				case eFireAvoidanceBufferForEmergencyVehicles:
					optFireAvoidanceBufferForVehicles = Double.parseDouble(opts.get(opt));
					break;
				case eCongestionEvaluationInterval:
					optCongestionEvaluationInterval= Double.parseDouble(opts.get(opt));
					break;
				case eCongestionToleranceThreshold:
					optCongestionToleranceThreshold= Double.parseDouble(opts.get(opt));
					break;
				case eCongestionReactionProbability:
					optCongestionReactionProbability= Double.parseDouble(opts.get(opt));
					break;
				default:
					log.warn("Ignoring option: " + opt + "=" + opts.get(opt));
			}
		}
	}

	public MATSimModel(String matSimFile, String matsimOutputDirectory, String startHHMM) {
		// not the most elegant way of doing this ...
		// yy maybe just pass the whole string from above and take apart ourselves?
		this(
		matsimOutputDirectory==null ?
				new String[]{matSimFile} :
				new String[]{ matSimFile, MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, matsimOutputDirectory, eGlobalStartHhMm, startHHMM }
		);
	}

	public MATSimModel( String[] args) {
//		((ch.qos.logback.classic.Logger)log).setLevel(Level.DEBUG);

		config = ConfigUtils.loadConfig( args[0] ) ;

		Utils.parseAdditionalArguments(args, config);

		config.network().setTimeVariantNetwork(true);

		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);

		config.qsim().setSimStarttimeInterpretation( StarttimeInterpretation.onlyUseStarttime );

		config.controler().setWritePlansInterval(1);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.planCalcScore().setWriteExperiencedPlans(true);

		evacConfig = ConfigUtils.addOrGetModule(config,EvacConfig.class) ;
		Gbl.assertNotNull(evacConfig);

		// we have to declare those routingModes where we want to use the network router:
		{
			Collection<String> modes = config.plansCalcRoute().getNetworkModes();
			Set<String> newModes = new TreeSet<>( modes ) ;
			for ( EvacRoutingMode mode : EvacRoutingMode.values() ) {
				newModes.add( mode.name() ) ;
			}
			config.plansCalcRoute().setNetworkModes( newModes );
		}

		// the router also needs scoring parameters:
		for ( EvacRoutingMode mode : EvacRoutingMode.values() ) {
			ModeParams params = new ModeParams(mode.name());
			config.planCalcScore().addModeParams(params);
		}

//		config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		//		ConfigUtils.setVspDefaults(config);

		// ---

		scenario = ScenarioUtils.createScenario(config);
		// (this is _created_ already here so that the scenario pointer can be final)

		// ---

		this.agentManager = new PAAgentManager(eventsMonitors) ;

		this.fireWriter = new FireWriter( config ) ;
		this.emberWriter = new EmberWriter( config ) ;
		this.disruptionWriter = new DisruptionWriter( config ) ;

	}

	public Config loadAndPrepareConfig() {
		// currently already done in constructor.  But I want to have this more
		// expressive than model.getConfig().  kai, nov'17
		configLoaded = true ;
		return config ;
	}

	public Scenario loadAndPrepareScenario() {
		if ( !configLoaded ) {
			loadAndPrepareConfig() ;
		}

		ScenarioUtils.loadScenario(scenario) ;

		// make sure links don't have speed infinity (results in problems with the router; yy instead repair input files?):
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			final double veryLargeSpeed = 9999999999.;
			if ( link.getFreespeed() > veryLargeSpeed ) {
				link.setFreespeed(veryLargeSpeed);
			}
			if ( evacConfig.getSetup()== EvacConfig.Setup.tertiaryRoadsCorrection ) {
				// yyyy correction for much too high speed value on tertiary roads. kai, dec'17
				if (link.getFreespeed() == 27.77777777777778 && link.getCapacity() == 600.) {
					link.setFreespeed(50. / 3.6);
				}
			}
		}

		scenarioLoaded=true ;
		return scenario ;
	}

	public EvacConfig initialiseEvacConfig(Config config) {
		EvacConfig evacConfig = ConfigUtils.addOrGetModule(config, EvacConfig.class);
		evacConfig.setSetup(EvacConfig.Setup.standard);
		evacConfig.setCongestionEvaluationInterval(getOptCongestionEvaluationInterval());
		evacConfig.setCongestionToleranceThreshold(getOptCongestionToleranceThreshold());
		evacConfig.setCongestionReactionProbability(getOptCongestionReactionProbability());
		return evacConfig;
	}

	public final void init(List<String> bdiAgentIDs) {
		if ( !scenarioLoaded ) {
			loadAndPrepareScenario() ;
		}

		// yy this could now be done in upstream code.  But since upstream code is user code, maybe we don't want it in there?  kai, nov'17
		for(String agentId: bdiAgentIDs) {
			agentManager.createAndAddBDIAgent(agentId);

			// default action:
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					ActionList.DRIVETO, new DRIVETODefaultActionHandlerV2(this) );
		}
		{
			ActivityParams params = new ActivityParams("driveTo");
			params.setScoringThisActivityAtAll(false);
			scenario.getConfig().planCalcScore().addActivityParams(params);
		}
		// ---

		final Controler controller = new Controler( scenario );

		controller.getEvents().addHandler(eventsMonitors);

		// Register any supplied event handlers
		if (eventHandlers != null) {
			for (EventHandler handler : eventHandlers) {
				controller.getEvents().addHandler(handler);
			}
		}

		controller.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				setupEmergencyVehicleRouting();
				setupCarGlobalInformationRouting();
				setupCarFreespeedRouting();

				install( new EvacQSimModule(MATSimModel.this, controller.getEvents()) ) ;

				this.addMobsimListenerBinding().toInstance((MobsimInitializedListener) e -> {
					// memorize the qSim:
					qSim = (QSim) e.getQueueSimulation() ;

					// start the playPause functionality
					playPause = new PlayPauseSimulationControl( qSim ) ;
					playPause.pause();

					//						initialiseVisualisedAgents() ;
				}) ;

				// congestion detection in {@link EvacAgentTracker}

				this.addMobsimListenerBinding().toInstance( mobsimDataProvider );
			}

			private void setupCarFreespeedRouting() {
				String routingMode = EvacRoutingMode.carFreespeed.name() ;

				addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car,routingMode)) ;

				// freespeed travel time:
				addTravelTimeBinding(routingMode).to(FreeSpeedTravelTime.class);

				// travel disutility includes the fire penalty. If no data arrives, it makes no difference.
				TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyFactorsOfLinks);
				addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
			}

			private void setupCarGlobalInformationRouting() {
				final String routingMode = EvacRoutingMode.carGlobalInformation.name();

				addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode)) ;

				// congested travel time:
				bind(WithinDayTravelTime.class).in(Singleton.class);
				addEventHandlerBinding().to(WithinDayTravelTime.class);
				addMobsimListenerBinding().to(WithinDayTravelTime.class);
				addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;

				// travel disutility includes the fire penalty. If no data arrives, it makes no difference.
				TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyFactorsOfLinks);
				// (yyyyyy the now following cases are just there because of the different tests.  Solve by config,
				addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
			}

			private void setupEmergencyVehicleRouting() {
				final String routingMode = EvacRoutingMode.emergencyVehicle.name();

				addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode)) ;

				// congested travel time:
				bind(WithinDayTravelTime.class).in(Singleton.class);
				addEventHandlerBinding().to(WithinDayTravelTime.class);
				addMobsimListenerBinding().to(WithinDayTravelTime.class);
				addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;

				// travel disutility includes the fire penalty:
				TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyFactorsOfLinksForEmergencyVehicles);
				// yyyyyy This uses the same disutility as the evacuees.  May not be what we want.
				// But what do we want?  kai, dec'17/jan'18

//					TravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
				addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
			}
		}) ;

		// wrap the controller into a thread and start it:
		this.matsimThread = new Thread( controller ) ;
		matsimThread.start();

		// wait until the thread has initialized before returning:
		while( this.playPause==null ) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public final Replanner getReplanner() {
		return this.replanner ;
	}

	@Override public final void takeControl(AgentDataContainer agentDataContainer){
		runUntil( (int)(playPause.getLocalTime() + 1), agentDataContainer ) ;
	}

	public final void runUntil( long newTime , AgentDataContainer agentDataContainer ) {
			log.trace("Received {} ", agentManager.getAgentDataContainer());
			agentManager.updateActions(); // handle incoming BDI actions
			playPause.doStep( (int) (newTime) );
			agentManager.transferActionsPerceptsToDataContainer(); // send back BDI actions/percepts/status'
	}

	public final void runUntilV2( long newTime , io.github.agentsoz.bdiabm.v2.AgentDataContainer inAdc) {
		log.trace("Received {} ", inAdc);
		//agentManager.updateActions(); // handle incoming BDI actions
		agentManager.updateActions(inAdc, adc);
		playPause.doStep( (int) (newTime) );
		//agentManager.transferActionsPerceptsToDataContainer(); // send back BDI actions/percepts/status'
	}

	public void setAgentDataContainer(io.github.agentsoz.bdiabm.v2.AgentDataContainer adc) {
		this.adc = adc;
	}



	public final boolean isFinished() {
		return playPause.isFinished() ;
	}

	public void finish() {
		playPause.play();
		while( matsimThread.isAlive() ) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if ( fireWriter!=null ) {
			fireWriter.close();
		}
		if ( emberWriter !=null ) {
			emberWriter.close();
		}
		if ( disruptionWriter!=null ) {
			disruptionWriter.finish(this.getTime());
		}
	}

	public final Scenario getScenario() {
		// needed in the BDIActionHandlers!
		return this.scenario ;
	}

	public void setEventHandlers(List<EventHandler> eventHandlers) {
		this.eventHandlers = eventHandlers;
	}

	private final void setFreeSpeedExample(){
		// example how to set the freespeed of some link to zero:
		final double now = this.qSim.getSimTimer().getTimeOfDay();
		if ( now == 0.*3600. + 6.*60. ) {
			NetworkChangeEvent event = new NetworkChangeEvent( now ) ;
			event.setFreespeedChange(new NetworkChangeEvent.ChangeValue( NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS,  0. ));
			event.addLink( scenario.getNetwork().getLinks().get( Id.createLinkId( 51825 )));
			NetworkUtils.addNetworkChangeEvent( scenario.getNetwork(),event);

			for ( MobsimAgent agent : this.getMobsimDataProvider().getAgents().values() ) {
				if ( !(agent instanceof MATSimStubAgent) ) {
					this.getReplanner().reRouteCurrentLeg(agent, now);
				}
			}
		}
	}

	public final void registerDataServer( DataServer server ) {
		this.dataServer = server;
		server.subscribe(this, PerceptList.TAKE_CONTROL_ABM);
		server.subscribe(this, PerceptList.FIRE_DATA);
		server.subscribe(this, PerceptList.EMBERS_DATA);
		server.subscribe(this, PerceptList.DISRUPTION);
		server.subscribe(this, PerceptList.EMERGENCY_MESSAGE);
	}

	@Override public void receiveData(double time, String dataType, Object data) {
		if ( time+1 < getTime() || time-1 > getTime() ) {
			log.error( "given time in receiveData is {}, simulation time is {}.  " +
							   "Don't know what that means.  Will use given time.",
					time, getTime() );
		}
		double now = time; //getTime() ;

		switch( dataType ) {
			case PerceptList.TAKE_CONTROL_ABM:
			case PerceptList.FIRE_DATA:
			case PerceptList.EMBERS_DATA:
			case PerceptList.DISRUPTION:
			case PerceptList.EMERGENCY_MESSAGE:
				dataListeners.get(dataType).receiveData(now, dataType, data);
				break;
			default:
				throw new RuntimeException("Unknown data type received: " + dataType) ;
		}
	}

	/**
	 * Creates a listener for each type of message we expect from the DataServer
	 * @return
	 */
	private Map<String, DataClient> createDataListeners() {
		Map<String, DataClient> listeners = new  HashMap<>();

		listeners.put(PerceptList.TAKE_CONTROL_ABM, (DataClient<io.github.agentsoz.bdiabm.v2.AgentDataContainer>) (time, dataType, data) -> {
			synchronized (sequenceLock) {
				adc.clear();
				runUntilV2((long) time, data);
				synchronized (this.getAgentManager().getAgentDataContainerV2()) {
					copy(this.getAgentManager().getAgentDataContainerV2(), adc);
					this.getAgentManager().getAgentDataContainerV2().clear();
				}
				dataServer.publish(PerceptList.AGENT_DATA_CONTAINER_FROM_ABM, adc);
			}
		});

		listeners.put(PerceptList.FIRE_DATA, (DataClient<Map<Double, Double[][]>>) (time, dataType, data) -> {
			processFireData(data, time, penaltyFactorsOfLinks, scenario,
					penaltyFactorsOfLinksForEmergencyVehicles, fireWriter);
		});

		listeners.put(PerceptList.EMBERS_DATA, (DataClient<Map<Double, Double[][]>>) (time, dataType, data) -> {
			processEmbersData(data, time, scenario, emberWriter);
		});

		listeners.put(PerceptList.DISRUPTION, (DataClient<Map<Double,Disruption>>) (time, dataType, data) -> {
			processDisruptionData(data, time, scenario, disruptionWriter);
		});

		listeners.put(PerceptList.EMERGENCY_MESSAGE, (DataClient<Map<Double,EmergencyMessage>>) (time, dataType, data) -> {
			processEmergencyMessageData(data, time, scenario);
		});

		return listeners;
	}

	private void copy(io.github.agentsoz.bdiabm.v2.AgentDataContainer from, io.github.agentsoz.bdiabm.v2.AgentDataContainer to) {
		if (from != null) {
			Iterator<String> it = from.getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				// Copy percepts
				Map<String, PerceptContent> percepts = from.getAllPerceptsCopy(agentId);
				for (String perceptId : percepts.keySet()) {
					PerceptContent content = percepts.get(perceptId);
					to.putPercept(agentId, perceptId, content);
				}
				// Copy actions
				Map<String, ActionContent> actions = from.getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent content = actions.get(actionId);
					to.putAction(agentId, actionId, content);
				}
			}
		}
	}

	private void processEmbersData(Map<Double, Double[][]> data, double now, Scenario scenario, EmberWriter emberWriter) {
		log.info("receiving embers data at time={}", now);
		log.info( "{}{}", new Gson().toJson(data).substring(0,Math.min(new Gson().toJson(data).length(),200)),
				"... use DEBUG to see full coordinates list") ;
		log.debug( "{}", new Gson().toJson(data)) ;
		Geometry embers = getGeometry(data, scenario);
		if (embers == null) {
			return;
		}
		Geometry embersBuffer = embers.buffer(optMaxDistanceForSmokeVisual);
		List<Id<Person>> personsMatched = getPersonsWithin(scenario, embersBuffer);
		log.info("Embers/smoke seen by {} persons: {} ", personsMatched.size(), Arrays.toString(personsMatched.toArray()));
		// package the messages up to send to the BDI side
		for (Id<Person> personId : personsMatched) {
			PAAgent agent = this.getAgentManager().getAgent(personId.toString());
			if (agent != null) { // only do this if this is a BDI-like agent
				PerceptContent pc = new PerceptContent(PerceptList.FIELD_OF_VIEW, PerceptList.SIGHTED_EMBERS);
				getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.FIELD_OF_VIEW, pc);
			}
		}
		emberWriter.write( now, embers);
	}

	private void processDisruptionData( Map<Double,Disruption> data, double now, Scenario scenario, DisruptionWriter disruptionWriter ) {
		log.info("receiving disruption data at time={}", now); ;
		log.info( "{}", new Gson().toJson(data) ) ;

		Map<Double,Disruption> timeMapOfDisruptions = data;

		for (Disruption dd : timeMapOfDisruptions.values()) {

			double speedInMpS = 0;
			switch (dd.getEffectiveSpeedUnit()) {
				case "kmph":
				case "KMPH":
					speedInMpS = dd.getEffectiveSpeed() / 3.6;
					break;
				default:
					throw new RuntimeException("unimplemented speed unit");
			}

			String linkIds = Arrays.toString(dd.getImpactLinks())
					.replaceAll("\\[", "")
					.replaceAll("\\]", "")
					.replaceAll(",", " ");
			List<Link> links = NetworkUtils.getLinks(scenario.getNetwork(),NetworkUtils.getLinkIds(linkIds));
			for (Link link : links) {
				double prevSpeed = link.getFreespeed(now);
				log.info("Updating freespeed on link {} from {} to {} due to disruption",
						link.getId(), prevSpeed, speedInMpS);
				{
					double startTime = convertTimeToSeconds(dd.getStartHHMM());
					if (startTime < now) {
						startTime = now;
					}
					addNetworkChangeEvent(speedInMpS, link, startTime);
					disruptionWriter.write(startTime, link.getId(), link.getCoord(), speedInMpS);
				}
				{
					double startTime = convertTimeToSeconds(dd.getEndHHMM());
					if (startTime < now) {
						startTime = now;
					}
					addNetworkChangeEvent(prevSpeed, link, startTime);
					disruptionWriter.write(startTime, link.getId(), link.getCoord(), prevSpeed);
				}
			}
		}
	}

	private void processEmergencyMessageData(Map<Double,EmergencyMessage> data, double now, Scenario scenario) {
		log.info("receiving emergency message data at time={}", now);
		log.info( "{}{}", new Gson().toJson(data).substring(0,Math.min(new Gson().toJson(data).length(),200)),
				"... use DEBUG to see full coordinates list") ;
		log.debug( "{}", new Gson().toJson(data)) ;


		Map<Double,EmergencyMessage> timeMapOfEmergencyMessages = data;

		// FIXME: Assumes incoming is WSG84 format. See https://github.com/agentsoz/bdi-abm-integration/issues/34
		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, scenario.getConfig().global().getCoordinateSystem());

		// The key is time, value is a message
		for (EmergencyMessage msg : timeMapOfEmergencyMessages.values()) {
			// For each zone in this message
			List<Id<Person>> personsInZones = new ArrayList<>();

			for (String zoneId : msg.getBroadcastZones().keySet()) {
				Double[][] pairs = msg.getBroadcastZones().get(zoneId);
				List<Coord> coords = new ArrayList<>() ;
				for (Double[] pair : pairs) {
					coords.add(transform.transform(new Coord(pair[0], pair[1])));
				}
				// Create a polygon for this zone
				Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
				// And find everyone inside it
				List<Id<Person>> personsMatched = getPersonsWithin(scenario, polygon);
				personsInZones.addAll(personsMatched);
				log.info("Zone {} has persons: {} ", zoneId, Arrays.toString(personsMatched.toArray()));
			}
			log.info("Message " + msg.getType() + " will be sent to total " + personsInZones.size() + " persons in zones " + msg.getBroadcastZones().keySet());
			// package the messages up to send to the BDI side
			for (Id<Person> personId : personsInZones) {
				PAAgent agent = this.getAgentManager().getAgent(personId.toString());
				if (agent != null) { // only do this if this is a BDI-like agent
					PerceptContent pc = new PerceptContent(PerceptList.EMERGENCY_MESSAGE, msg.getType() + "," + msg.getContent());
					getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.EMERGENCY_MESSAGE, pc);
				}
			}

		}
	}

	private List<Id<Person>> getPersonsWithin(Scenario scenario, Geometry shape) {
		List<Id<Person>> personsWithin = new ArrayList<>();
		for(Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
			MobsimAgent agent = this.getMobsimDataProvider().getAgent(personId);
			if (agent == null) {
				log.error("MobsimAgent {} not found, should never happen!!", personId);
				continue;
			}
            final Id<Link> linkId = agent.getCurrentLinkId();
            final Link link = scenario.getNetwork().getLinks().get(linkId);
            Point fromPoint = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
            if (shape.contains(fromPoint)) { // coming from polygon area
                // this agent is in (or has potentially just exited) the messaging area
                personsWithin.add(personId);
            }
        }
		return personsWithin;
	}

	private void addNetworkChangeEvent(double speedInMpS, Link link, double startTime) {
		NetworkChangeEvent changeEvent = new NetworkChangeEvent( startTime ) ;
		changeEvent.setFreespeedChange(new NetworkChangeEvent.ChangeValue(
				NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS,  speedInMpS
		) ) ;
		changeEvent.addLink( link ) ;

		// (1) add to mobsim:
		this.qSim.addNetworkChangeEvent(changeEvent);

		// (2) add to replanner:
		this.replanner.addNetworkChangeEvent(changeEvent);
		// yyyy wanted to delay this until some agent has actually encountered it.  kai, feb'18

	}
	
	public static double convertTimeToSeconds(String startHHMM) {
		int hours = Integer.parseInt(startHHMM.substring(0, 2));
		int minutes = Integer.parseInt(startHHMM.substring(2, 4));
		double startTime = hours * 3600 + minutes * 60;
		log.debug("orig={}, hours={}, min={}, sTime={}", startHHMM, hours, minutes, startTime);
		return startTime;
	}
	
	private void processFireData(Map<Double, Double[][]> data, double now, Map<Id<Link>, Double> penaltyFactorsOfLinks,
										   Scenario scenario, Map<Id<Link>, Double> penaltyFactorsOfLinksForEmergencyVehicles,
										   FireWriter fireWriter) {

		log.info("receiving fire data at time={}", now);
		log.info( "{}{}", new Gson().toJson(data).substring(0,Math.min(new Gson().toJson(data).length(),200)),
				"... use DEBUG to see full coordinates list") ;
		log.debug( "{}", new Gson().toJson(data)) ;

		Geometry fire = getGeometry(data, scenario);

		{
			Geometry buffer = fire.buffer(optMaxDistanceForFireVisual);
			List<Id<Person>> personsMatched = getPersonsWithin(scenario, buffer);
			log.info("Fire seen by {} persons: {} ", personsMatched.size(), Arrays.toString(personsMatched.toArray()));
			// package the messages up to send to the BDI side
			for (Id<Person> personId : personsMatched) {
				PAAgent agent = this.getAgentManager().getAgent(personId.toString());
				if (agent != null) { // only do this if this is a BDI-like agent
					PerceptContent pc = new PerceptContent(PerceptList.FIELD_OF_VIEW, PerceptList.SIGHTED_FIRE);
					getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.FIELD_OF_VIEW, pc);
				}
			}
		}
//		https://stackoverflow.com/questions/38404095/how-to-calculate-the-distance-in-meters-between-a-geographic-point-and-a-given-p
		{
			final double bufferWidth = optFireAvoidanceBufferForVehicles;
			Geometry buffer = fire.buffer(bufferWidth);
			penaltyFactorsOfLinks.clear();
//		Utils.penaltyMethod1(fire, buffer, penaltyFactorsOfLinks, scenario );
			Utils.penaltyMethod2(fire, buffer, bufferWidth, penaltyFactorsOfLinks, scenario);
			// I think that penaltyMethod2 looks nicer than method1.  kai, dec'17
			// yy could make this settable, but for the time being this pedestrian approach
			// seems sufficient.  kai, jan'18
		}
		{
			final double bufferWidth = optFireAvoidanceBufferForEmergencyVehicles;
			Geometry buffer = fire.buffer(bufferWidth);
			penaltyFactorsOfLinksForEmergencyVehicles.clear();
			Utils.penaltyMethod2(fire, buffer, bufferWidth, penaltyFactorsOfLinksForEmergencyVehicles, scenario);
		}
		fireWriter.write( now, fire);
	}

	private Geometry getGeometry(Map<Double, Double[][]> data, Scenario scenario) {
		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, scenario.getConfig().global().getCoordinateSystem());

		Map<Double, Double[][]> map = data;
		// the map key is time; we just take the superset of all polygons
		Geometry shape = null ;
		for ( Double[][] pairs : map.values() ) {
			List<Coord> coords = new ArrayList<>() ;
			for (Double[] pair : pairs) {
				coords.add(transform.transform(new Coord(pair[0], pair[1])));
			}
			Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
			if ( shape==null ) {
				shape = polygon ;
			} else {
				shape = shape.union(polygon);
			}
		}
		return shape;
	}

	public final double getTime() {
		return this.qSim.getSimTimer().getTimeOfDay() ;
	}

	@Override public Object queryPercept(String agentID, String perceptID, Object args) {
		log.debug("received query from agent {} for percept {} with args {}", agentID, perceptID, args);
		switch(perceptID) {
			case PerceptList.REQUEST_LOCATION:
				final Link link = scenario.getNetwork().getLinks().get( this.getMobsimAgentFromIdString(agentID).getCurrentLinkId() );
				Location[] coords = {
						new Location(link.getFromNode().getId().toString(), link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()),
						new Location(link.getToNode().getId().toString(), link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())
				};
				return coords;
			case PerceptList.REQUEST_DRIVING_DISTANCE_TO :
				if (args == null || !(args instanceof double[])) {
					throw new RuntimeException("Query percept '"+perceptID+"' expecting double[] coordinates argument, but found: " + args);
				}
				double[] dest = (double[]) args;
				Coord coord = new Coord( dest[0], dest[1] ) ;
				final Link destLink = NetworkUtils.getNearestLink(getScenario().getNetwork(), coord );
				Gbl.assertNotNull(destLink);
				final Link currentLink = scenario.getNetwork().getLinks().get( this.getMobsimAgentFromIdString(agentID).getCurrentLinkId() );
				final double now = getTime();
				//final Person person = scenario.getPopulation().getPersons().get(agentID);
				double res = 0.0;
				synchronized (this.replanner) {
					LeastCostPathCalculator.Path result = this.replanner.editRoutes(EvacRoutingMode.carFreespeed).getPathCalculator().calcLeastCostPath(
							currentLink.getFromNode(), destLink.getFromNode(), now, null, null
					);
					res = RouteUtils.calcDistance(result);
				}
				return res;
			default:
				throw new RuntimeException("Unknown query percept '"+perceptID+"' received from agent "+agentID+" with args " + args);
		}
	}

	public PAAgentManager getAgentManager() {
		return agentManager;
	}

	public MobsimDataProvider getMobsimDataProvider() {
		return mobsimDataProvider;
	}

	public MobsimAgent getMobsimAgentFromIdString( String idString ) {
		return this.getMobsimDataProvider().getAgent( Id.createPersonId(idString) ) ;
	}

	public EventsManager getEvents() {
		return this.qSim.getEventsManager() ;
	}

	public Config getConfig() {
		return config;
	}

	public double getOptCongestionEvaluationInterval() {
		return optCongestionEvaluationInterval;
	}

	public double getOptCongestionToleranceThreshold() {
		return optCongestionToleranceThreshold;
	}

	public double getOptCongestionReactionProbability() {
		return optCongestionReactionProbability;
	}

    public void useSequenceLock(Object sequenceLock) {
	    this.sequenceLock = sequenceLock;
    }

}
