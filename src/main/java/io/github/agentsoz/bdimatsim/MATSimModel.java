package io.github.agentsoz.bdimatsim;

import java.io.PrintStream;
import java.util.*;

import ch.qos.logback.classic.Level;
import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.util.evac.ActionList;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
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
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
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
public final class MATSimModel implements ABMServerInterface, DataClient {
	private static final Logger log = LoggerFactory.getLogger(MATSimModel.class);
	//private static final Logger log = Logger..getLogger(MATSimModel.class) ;
	public static final String MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR = "--matsim-output-directory";
	static final String FIRE_DATA_MSG = "fire_data";
	private final EvacConfig evacConfig;
	private PrintStream fireWriter;
	private Config config;
	private boolean configLoaded = false ;
	
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
	
	private Map<Id<Link>,Double> penaltyOfLink = new HashMap<>() ;
	
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
		parseAdditionalArguments(args, config);
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
		
		// ---

		this.agentManager = new PAAgentManager(eventsMonitors) ;
		
	}
	
	public Config loadAndPrepareConfig() {
		// currently already done in constructor.  But I want to make this more
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
		for(String agentId: bdiAgentIDs) 
		{
			agentManager.createAndAddBDIAgent(agentId);

			// default action:
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					ActionList.DRIVETO, new DRIVETODefaultActionHandler(this) );
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
					
					addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode)) ;

					// congested travel time:
					bind(WithinDayTravelTime.class).in(Singleton.class);
					addEventHandlerBinding().to(WithinDayTravelTime.class);
					addMobsimListenerBinding().to(WithinDayTravelTime.class);
					addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;
					
					// travel disutility includes the fire penalty:
					TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyOfLink);
					switch( evacConfig.getSetup() ) {
						// use switch so we note missing cases. kai, dec'17
						case standard:
						case blockage:
						case tertiaryRoadsCorrection:
							break;
						case withoutFireArea:
						case withBlockageButWithoutFire:
							disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
							break;
						default:
							Gbl.fail();
					}
					addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
				}
				{
					String routingMode = EvacRoutingMode.carFreespeed.name() ;
					
					addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car,routingMode)) ;
					
					// freespeed travel time:
					addTravelTimeBinding(routingMode).to(FreeSpeedTravelTime.class);
					
					// travel disutility includes the fire penalty:
					TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyOfLink);
					switch( evacConfig.getSetup() ) {
						// use switch so we note missing cases. kai, dec'17
						case standard:
						case blockage:
						case tertiaryRoadsCorrection:
							break;
						case withoutFireArea:
						case withBlockageButWithoutFire:
							disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
							break;
						default:
							Gbl.fail();
					}

					addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
				}
		

				
				install( new EvacQSimModule(MATSimModel.this, controller.getEvents()) ) ;

				this.addMobsimListenerBinding().toInstance((MobsimInitializedListener) e -> {

					qSim = (QSim) e.getQueueSimulation() ;

					playPause = new PlayPauseSimulationControl( qSim ) ;
					playPause.pause();

					//						initialiseVisualisedAgents() ;
				}) ;
				
				// early prototype for congestion detection:
//				this.addMobsimListenerBinding().toInstance(new MobsimAfterSimStepListener() {
//					@Override
//					public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
//						for ( MobsimAgent agent : ((QSim)e.getQueueSimulation()).getAgents().values() ) {
//							if ( agent instanceof EvacAgent ) {
//								double delay = e.getSimulationTime() - ((EvacAgent) agent).getExpectedLinkLeaveTime();
//								if ( delay > 60 ) {
//									Id<Vehicle> vehicleId = ((EvacAgent) agent).getVehicle().getId() ;
//									getEvents().processEvent( new AgentInCongestionEvent(e.getSimulationTime(), vehicleId, agent.getId(), agent.getCurrentLinkId() ));
//								}
//							}
//						}
//					}
//				}) ;
				/** --> now done in {@link EvacAgentTracker} */
				
				// some infrastructure that makes results available: 
				//this.addMobsimListenerBinding().toInstance( new MobsimAfterSimStepListener() {
				//	@Override public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
				//		publishDataToExternalListeners();
				//	}
				//} ) ;

				this.addMobsimListenerBinding().toInstance( mobsimDataProvider );
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

	@Override
	public final void takeControl(AgentDataContainer agentDataContainer){
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
					log.info("setting matsim output directory to " + args[i] );
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
		if ( time+1 < getTime() || time-1 > getTime() ) {
			log.error( "given time in dataUpdate is {}, simulation time is {}.  " +
							   "Don't know what that means.  Will use simulation time.",
					time, getTime() );
		}
		double now = getTime() ;
		
		log.warn("receiving fire data at time={}", now/3600. ) ;
		
		switch( evacConfig.getSetup() ) {
			// use switch so we note missing cases.  kai, dec'17
			case standard:
			case blockage:
			case tertiaryRoadsCorrection:
				break;
			case withoutFireArea:
			case withBlockageButWithoutFire:
				return false ;
			default:
				throw new RuntimeException("not implemented") ;
		}
		
		// Is this called in every time step, or just every 5 min or so?  kai, dec'17 --> Normally only one polygon
		// per time step.  Might want to test for this, and get rid of multi-polygon code below.  On other hand,
		// probably does not matter much.  kai, dec'17
		if (! FIRE_DATA_MSG.equals(dataType)) {
			return false;
		}
		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, config.global().getCoordinateSystem());
		
		final String json = new Gson().toJson(data);
		log.debug(json);
		
		//			GeoJSONReader reader = new GeoJSONReader();
		//			Geometry geometry = reader.read(json);
		// unfortunately does not work since the incoming data is not typed accordingly. kai, dec'17
		
		Map<Double, Double[][]> map = (Map<Double, Double[][]>) data;
//		List<Polygon> polygons = new ArrayList<>() ;
//		List<Geometry> buffers = new ArrayList<>() ;
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
//			polygons.add(polygon) ;
//			buffers.add( polygon.buffer(1000.) );
		}
		
		Geometry buffer = fire.buffer(10000.);
		
		penaltyOfLink.clear();
		
//		https://stackoverflow.com/questions/38404095/how-to-calculate-the-distance-in-meters-between-a-geographic-point-and-a-given-p
		
		// risk increasing are essentially forbidden:
		final double buffer2fire = 1000. ;
		final double out2buffer = 500 ;
		final double out2fire = out2buffer + buffer2fire ;
		final double buffer2bufferGettingCloser = 500. ;

		// risk reducing is ok, we just want to keep them short:
		final double fire2buffer = 20. ;
		final double fire2out = 10. ;
		final double buffer2out = 5. ;
		final double buffer2bufferGettingAway = 5. ;

		// in fire essentially forbidden:
		final double fire2fire = 1000. ;
		

		
		
		for ( Node node : scenario.getNetwork().getNodes().values() ) {
			Point point = GeometryUtils.createGeotoolsPoint(node.getCoord());
			if (fire.contains(point)) {
				log.debug("node {} is IN fire area " + node.getId());
				// outlinks:
				for (Link link : node.getOutLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());
					if (fire.contains(point2)) {
						penaltyOfLink.put(link.getId(), fire2fire ); // fire --> fire
					} else if (buffer.contains(point2)) {
						penaltyOfLink.put(link.getId(), fire2buffer ); // fire --> buffer
					} else {
						penaltyOfLink.put(link.getId(), fire2out ); // fire --> out
					}
				}
				// in links
				for (Link link : node.getInLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());
					if (fire.contains(point2)) {
						penaltyOfLink.put(link.getId(), fire2fire ); // fire <-- fire
					} else if (buffer.contains(point2)) {
						penaltyOfLink.put(link.getId(), buffer2fire ); // fire <-- buffer
					} else {
						penaltyOfLink.put(link.getId(), out2fire ); // fire <-- out
					}
				}
			} else if ( buffer.contains(point) ) {
				log.debug("node {} is IN buffer", node.getId());
				// outlinks:
				for (Link link : node.getOutLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());
					if (fire.contains(point2)) {
						penaltyOfLink.put(link.getId(), buffer2fire ); // buffer --> fire
					} else if (buffer.contains(point2)) {// buffer --> buffer
						final double distanceFromNode = point.distance(fire) ;
						final double distanceToNode = point2.distance(fire) ;
						if ( distanceToNode > distanceFromNode ) {
							penaltyOfLink.put(link.getId(), buffer2bufferGettingAway);
						} else {
							penaltyOfLink.put(link.getId(), buffer2bufferGettingCloser);
						}
					} else {
						penaltyOfLink.put(link.getId(), buffer2out ); // buffer --> out
					}
				}
				// in links
				for (Link link : node.getInLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
					if (fire.contains(point2)) {
						penaltyOfLink.put(link.getId(), fire2buffer); // buffer <-- fire
					} else if (buffer.contains(point2)) { // buffer <-- buffer
						final double distanceToNode = point.distance(fire) ;
						final double distanceFromNode = point2.distance(fire) ;
						if ( distanceToNode > distanceFromNode ) {
							penaltyOfLink.put(link.getId(), buffer2bufferGettingAway);
						} else {
							penaltyOfLink.put(link.getId(), buffer2bufferGettingCloser);
						}
					} else {
						penaltyOfLink.put(link.getId(), out2buffer); // buffer <-- out
					}
				}
			}
		}
		// yyyy above is start, but one will need the full "potential" approach from Gregor. Otherwise, you cut a link
		// in two and get a different answer.  Which should not be. kai, dec'17
		
		if ( fireWriter==null ) {
			final String filename = config.controler().getOutputDirectory() + "/output_fireCoords.txt.gz";
			log.warn("writing fire data to " + filename);
			fireWriter = IOUtils.getPrintStream(filename);
			fireWriter.println("time\tx\ty") ;
		}
		// can't do this earlier since the output directories are not yet prepared by the controler.  kai, dec'17
		
		Envelope env = new Envelope();
//		for(Geometry g : polygons){
			env.expandToInclude(fire.getEnvelopeInternal());
//		}
		double gridsize = 100. ;
		boolean atLeastOnePixel = false ;
		for (double yy = env.getMinY(); yy <= env.getMaxY(); yy += gridsize) {
			for (double xx = env.getMinX(); xx <= env.getMaxX(); xx += gridsize) {
				if (fire.contains(new GeometryFactory().createPoint(new Coordinate(xx, yy)))) {
					final String str = now + "\t" + xx + "\t" + yy;
//					log.debug(str);
					fireWriter.println(str);
					atLeastOnePixel = true;
				}
			}
		}
		if (!atLeastOnePixel) {
			final String str = now + "\t" + fire.getCentroid().getX() + "\t" + fire.getCentroid().getY() ;
			fireWriter.println(str);
		}
		return true;
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
