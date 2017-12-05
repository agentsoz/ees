package io.github.agentsoz.bdimatsim;

import java.util.*;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import io.github.agentsoz.dataInterface.DataClient;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlayPauseSimulationControl;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author QingyuChen, KaiNagel, Dhi Singh
 */
public final class MATSimModel implements ABMServerInterface, DataClient {
	private static final Logger logger = LoggerFactory.getLogger("");
	public static final String MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR = "--matsim-output-directory";
	private static final String FIRE_DATA_MSG = "fire_data";
	private Config config;
	
	public static enum EvacRoutingMode {carFreespeed, carGlobalInformation}

	private final Scenario scenario ;

	/**
	 * some blackboardy thing that sits between ABM and BDI. can be null (so non-final is ok)
	 */
	private DataServer dataServer;

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

		config = ConfigUtils.loadConfig( args[0] ) ;
		parseAdditionalArguments(args, config);
		config.network().setTimeVariantNetwork(true);
		
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);


		config.qsim().setStartTime( 1.00 );
		config.qsim().setSimStarttimeInterpretation( StarttimeInterpretation.onlyUseStarttime );

		config.controler().setWritePlansInterval(1);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		
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
		
		// ---

		this.agentManager = new PAAgentManager(eventsMonitors) ;

	}
	
	public Scenario loadAndPrepareScenario() {

		ScenarioUtils.loadScenario(scenario) ;
		
		// make sure links don't have speed infinity (results in problems with the router; yy instead repair input files?):
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			final double veryLargeSpeed = 9999999999.;
			if ( link.getFreespeed() > veryLargeSpeed ) {
				link.setFreespeed(veryLargeSpeed);
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
		for(String agentId: bdiAgentIDs) 
		{
			agentManager.createAndAddBDIAgent(agentId);

			// default action:
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					MATSimActionList.DRIVETO, new DRIVETODefaultActionHandler(this) );
		}
		
		ActivityParams params = new ActivityParams("driveTo") ;
		params.setScoringThisActivityAtAll(false);
		scenario.getConfig().planCalcScore().addActivityParams(params);

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
				{
					final String routingMode = EvacRoutingMode.carGlobalInformation.name();

					bind(TravelTimeCollector.class).in(Singleton.class);
					addEventHandlerBinding().to(TravelTimeCollector.class);
					addMobsimListenerBinding().to(TravelTimeCollector.class);
					addTravelTimeBinding(routingMode).to(TravelTimeCollector.class) ;

					addRoutingModuleBinding(routingMode).toProvider(
							new NetworkRoutingProvider(TransportMode.car, routingMode)
					) ;

//					final RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory
//							= new RandomizingTimeDistanceTravelDisutilityFactory(routingMode, getConfig().planCalcScore());
//					disutilityFactory.setSigma(0.) ;
					OnlyTimeDependentTravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();;
					addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
				}
				{
					String routingMode = EvacRoutingMode.carFreespeed.name() ;
					
					addRoutingModuleBinding(routingMode).toProvider(
							new NetworkRoutingProvider(TransportMode.car,routingMode)
					) ;
					
					addTravelTimeBinding(routingMode).to(FreeSpeedTravelTime.class);
					
//					final RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory
//							= new RandomizingTimeDistanceTravelDisutilityFactory(routingMode, getConfig().planCalcScore());
//					disutilityFactory.setSigma(0.) ;
					OnlyTimeDependentTravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();;
					addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
				}
		

				
				install( new EvacQSimModule(MATSimModel.this, controller.getEvents()) ) ;

				this.addMobsimListenerBinding().toInstance( new MobsimInitializedListener() {
					@Override public void notifyMobsimInitialized(MobsimInitializedEvent e) {

						qSim = (QSim) e.getQueueSimulation() ;

						playPause = new PlayPauseSimulationControl( qSim ) ;
						playPause.pause(); 

						//						initialiseVisualisedAgents() ;
					}
				}) ;
				
				// some infrastructure that makes results available: 
				//this.addMobsimListenerBinding().toInstance( new MobsimAfterSimStepListener() {
				//	@Override public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
				//		publishDataToExternalListeners();
				//	}
				//} ) ;

				this.addMobsimListenerBinding().toInstance( mobsimDataProvider );
			}
		}) ;

		// twrap the controller into a thread and start it:
		this.matsimThread = new Thread( controller ) ;
		matsimThread.start();

		while( this.playPause==null ) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return ;

	}

	public final Replanner getReplanner() {
//		return qSim.getChildInjector().getInstance( Replanner.class ) ;
		// this _should_ now be a singleton by injection. kai, nov'17
		return this.replanner ;
	}

	@Override
	public final void takeControl(AgentDataContainer agentDataContainer){
		runUntil( (int)(playPause.getLocalTime() + 1), agentDataContainer ) ;
	}
	public final void runUntil( long newTime , AgentDataContainer agentDataContainer ) {
			logger.trace("Received {}", agentManager.getAgentDataContainer());
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

	}

	public final Scenario getScenario() {
		// needed in the BDIActionHandlers!
		return this.scenario ;
	}

	public void setEventHandlers(List<EventHandler> eventHandlers) {
		this.eventHandlers = eventHandlers;
	}

	private static void parseAdditionalArguments(String[] args, Config config) {
		for (int i = 1; i < args.length; i++) {
			switch (args[i]) {
			case MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR:
				if (i + 1 < args.length) {
					i++;
					logger.info("setting matsim output directory to " + args[i] );
					config.controler().setOutputDirectory( args[i] );
				}
				break;
			default:
				throw new RuntimeException("unknown config option") ;
			}
		}
	}

//	private final void setFreeSpeedExample(){
//		// example how to set the freespeed of some link to zero:
//		final double now = this.qSim.getSimTimer().getTimeOfDay();
//		if ( now == 0.*3600. + 6.*60. ) {
//			NetworkChangeEvent event = new NetworkChangeEvent( now ) ;
//			event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0. ));
//			event.addLink( scenario.getNetwork().getLinks().get( Id.createLinkId( 51825 )));
//			NetworkUtils.addNetworkChangeEvent( scenario.getNetwork(),event);
//
//			for ( MobsimAgent agent : this.getMobsimDataProvider().getAgents().values() ) {
//				if ( !(agent instanceof MATSimStubAgent) ) {
//					this.getReplanner().reRouteCurrentLeg(agent, now);
//				}
//			}
//		}
//	}

	public final void registerDataServer( DataServer server ) {
		dataServer = server;
		dataServer.subscribe(this, FIRE_DATA_MSG);
	}

	@Override
	public boolean dataUpdate(double time, String dataType, Object data) {
		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, config.global().getCoordinateSystem());
		
		
		if (FIRE_DATA_MSG.equals(dataType)) {
			final String json = new Gson().toJson(data);
			logger.error("Received '{}', must do something with it\n{}",
					dataType, json);
			logger.error("class =" + data.getClass() ) ;

//			GeoJSONReader reader = new GeoJSONReader();
//			Geometry geometry = reader.read(json);
			// unfortunately does not work since the incoming data is not typed accordingly. kai, dec'17

			Map<Double, Double[][]> map = (Map<Double, Double[][]>) data;
			List<Polygon> polygons = new ArrayList<>() ;
			// the map key is time; we just take the superset of all polygons
			for ( Double[][] pairs : map.values() ) {
				List<Coord> coords = new ArrayList<>() ;
				for ( int ii=0 ; ii<pairs.length ; ii++ ) {
					Coord coordOrig = new Coord( pairs[ii][0], pairs[ii][1]) ;
					Coord coordTransformed = transform.transform(coordOrig) ;
					coords.add( coordTransformed ) ;
				}
				Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
				polygons.add(polygon) ;
			}

			for ( Node node : scenario.getNetwork().getNodes().values() ) {
				Point point = GeometryUtils.createGeotoolsPoint( node.getCoord() ) ;
				for ( Polygon polygon : polygons ) {
					if (polygon.contains(point)) {
						logger.warn("node {} is IN fire area", node.getId());
					} else {
//						logger.warn("node {} is OUTSIDE fire area", node.getId());
					}
				}
			}
//			Map map = (Map) data;
//			for ( Iterator it = map.entrySet().iterator() ; it.hasNext() ; ) {
//				Map.Entry entry = (Map.Entry) it.next();
//				logger.error("key={}, value={}", entry.getKey(), entry.getValue());
//			}
			return true;
		}
		return false;
		
	}


	public final double getTime() {
		return this.qSim.getSimTimer().getTimeOfDay() ;
	}

	@Override
	public Object queryPercept(String agentID, String perceptID) {
		// TODO Auto-generated method stub
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
