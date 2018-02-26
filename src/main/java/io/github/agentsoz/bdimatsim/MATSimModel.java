package io.github.agentsoz.bdimatsim;

import java.util.*;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.util.Disruption;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import io.github.agentsoz.util.Location;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
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
	//private static final Logger log = Logger..getLogger(MATSimModel.class) ;
	public static final String MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR = "--matsim-output-directory";
	private final EvacConfig evacConfig;
	private final FireWriter fireWriter;
	private final DisruptionWriter disruptionWriter;
	private final Config config;
	private boolean configLoaded = false ;

	public enum EvacRoutingMode {carFreespeed, carGlobalInformation, emergencyVehicle}

	private final Scenario scenario ;

	/**
	 * A helper class essentially provided by the framework, used here.  The only direct connection to matsim are the event
	 * monitors, which need to be registered, via the events monitor registry, as a matsim events handler.
	 */
	private final PAAgentManager agentManager ;

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

	public MATSimModel(String matSimFile, String matsimOutputDirectory) {
		// not the most elegant way of doing this ...
		// yy maybe just pass the whole string from above and take apart ourselves?
		this(
		matsimOutputDirectory==null ?
				new String[]{matSimFile} :
				new String[]{ matSimFile, MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, matsimOutputDirectory }
		);
	}

	public MATSimModel( String[] args) {
//		((ch.qos.logback.classic.Logger)log).setLevel(Level.DEBUG);

		config = ConfigUtils.loadConfig( args[0] ) ;

		Utils.parseAdditionalArguments(args, config);

		config.network().setTimeVariantNetwork(true);

		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);

		config.qsim().setStartTime( 1.00 );
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

	public final void init(List<String> bdiAgentIDs) {
		if ( !scenarioLoaded ) {
			loadAndPrepareScenario() ;
		}

		// yy this could now be done in upstream code.  But since upstream code is user code, maybe we don't want it in there?  kai, nov'17
		for(String agentId: bdiAgentIDs) {
			agentManager.createAndAddBDIAgent(agentId);

			// default action:
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					ActionList.DRIVETO, new DRIVETODefaultActionHandler(this) );
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
		// some blackboardy thing that sits between ABM and BDI
		server.subscribe(this, PerceptList.FIRE_DATA);
		server.subscribe(this, PerceptList.DISRUPTION);
	}

	@Override public boolean dataUpdate(double time, String dataType, Object data) {
		if ( time+1 < getTime() || time-1 > getTime() ) {
			log.error( "given time in dataUpdate is {}, simulation time is {}.  " +
							   "Don't know what that means.  Will use simulation time.",
					time, getTime() );
		}
		double now = getTime() ;

		switch( dataType ) {
			case PerceptList.FIRE_DATA:
				return processFireData(data, now, penaltyFactorsOfLinks, scenario,
						penaltyFactorsOfLinksForEmergencyVehicles, fireWriter);
			case PerceptList.DISRUPTION:
				return processDisruptionData(data, now, scenario, disruptionWriter);
			default:
				throw new RuntimeException("not implemented") ;
		}
	}

	private boolean processDisruptionData( Object data, double now, Scenario scenario, DisruptionWriter disruptionWriter ) {
		log.info("receiving disruption data at time={}", (now/3600) ); ;

		log.info( new Gson().toJson(data) ) ;

		Map<Double,Disruption> timeMapOfDisrupions = (Map<Double,Disruption>)data;

		for (Disruption dd : timeMapOfDisrupions.values()) {

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
					addNetworkChangeEvent(speedInMpS, link, startTime);
					disruptionWriter.write(startTime, link.getId(), link.getCoord(), speedInMpS);
				}
				{
					double startTime = convertTimeToSeconds(dd.getEndHHMM());
					addNetworkChangeEvent(prevSpeed, link, startTime);
					disruptionWriter.write(startTime, link.getId(), link.getCoord(), prevSpeed);
				}
			}
		}
		return true ;
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
	
	private double convertTimeToSeconds(String startHHMM) {
		int hours = Integer.parseInt(startHHMM.substring(0, 2));
		int minutes = Integer.parseInt(startHHMM.substring(2, 4));
		double startTime = hours * 3600 + minutes * 60;
		log.info("orig={}, hours={}, min={}, sTime={}", startHHMM, hours, minutes, startTime);
		return startTime;
	}
	
	private static boolean processFireData(Object data, double now, Map<Id<Link>, Double> penaltyFactorsOfLinks,
										   Scenario scenario, Map<Id<Link>, Double> penaltyFactorsOfLinksForEmergencyVehicles,
										   FireWriter fireWriter) {
		// Is this called in every time step, or just every 5 min or so?  kai, dec'17

		// Normally only one polygon per time step.  Might want to test for this, and get rid of multi-polygon code
		// below.  On other hand, probably does not matter much.  kai, dec'17

		log.warn("receiving fire data at time={}", now/3600. ) ;

		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, scenario.getConfig().global().getCoordinateSystem());

		final String json = new Gson().toJson(data);
		log.debug(json);

		//			GeoJSONReader reader = new GeoJSONReader();
		//			Geometry geometry = reader.read(json);
		// unfortunately does not work since the incoming data is not typed accordingly. kai, dec'17

		Map<Double, Double[][]> map = (Map<Double, Double[][]>) data;
		// the map key is time; we just take the superset of all polygons
		Geometry fire = null ;
		for ( Double[][] pairs : map.values() ) {
			List<Coord> coords = new ArrayList<>() ;
			for (Double[] pair : pairs) {
				coords.add(transform.transform(new Coord(pair[0], pair[1])));
			}
			Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
			if ( fire==null ) {
				fire = polygon ;
			} else {
				fire = fire.union(polygon);
			}
		}

//		https://stackoverflow.com/questions/38404095/how-to-calculate-the-distance-in-meters-between-a-geographic-point-and-a-given-p
		{
			// FIXME: Using a hardwired 10km buffer for vehicles; move to config
			final double bufferWidth = 10000.;
			Geometry buffer = fire.buffer(bufferWidth);
			penaltyFactorsOfLinks.clear();
//		Utils.penaltyMethod1(fire, buffer, penaltyFactorsOfLinks, scenario );
			Utils.penaltyMethod2(fire, buffer, bufferWidth, penaltyFactorsOfLinks, scenario);
			// I think that penaltyMethod2 looks nicer than method1.  kai, dec'17
			// yy could make this settable, but for the time being this pedestrian approach
			// seems sufficient.  kai, jan'18
		}
		{
			// FIXME: Using a hardwired 1km buffer for emergency services; move to config
			final double bufferWidth = 1000. ;
			Geometry buffer = fire.buffer(bufferWidth);
			penaltyFactorsOfLinksForEmergencyVehicles.clear();
			Utils.penaltyMethod2(fire, buffer, bufferWidth, penaltyFactorsOfLinksForEmergencyVehicles, scenario);
		}

		fireWriter.write( now, fire);
		return true;
	}

	public final double getTime() {
		return this.qSim.getSimTimer().getTimeOfDay() ;
	}

	@Override public Object queryPercept(String agentID, String perceptID, Object args) {
		log.debug("received query from agent {} for percept {} with args {}; not implemented yet!", agentID, perceptID, args);
		switch(perceptID) {
			case PerceptList.REQUEST_LOCATION:
				if (args == null || !(args instanceof String)) {
					throw new RuntimeException("Query percept '"+perceptID+"' expecting String argument, but found: " + args);
				}
				Link link = scenario.getNetwork().getLinks().get( Id.createLinkId( (String)args ));
				if(link == null) {
					log.warn(perceptID + " argument '"+args+"' is not a valid link id");
					break;
				}
				Location[] coords = {
						new Location(link.getFromNode().getId().toString(), link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()),
						new Location(link.getToNode().getId().toString(), link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())
				};
				return coords;
//			case abc :
//				LeastCostPathCalculator.Path result = this.replanner.editRoutes(EvacRoutingMode.carFreespeed).getPathCalculator().calcLeastCostPath(
//						fromNode, toNode, starttime, person, vehicle
//				);;
//				result.travelTime ;
//				result.travelCost ;
			default:
				throw new RuntimeException("Unknown query percept '"+perceptID+"' received from agent "+agentID+" with args " + args);
		}
		return null;
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

}
