package io.github.agentsoz.bdimatsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlayPauseSimulationControl;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.network.NetworkUtils;
import org.matsim.withinday.mobsim.MobsimDataProvider;
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
import io.github.agentsoz.nonmatsim.SimpleMessage;

/**
 * @author QingyuChen, KaiNagel, Dhi Singh
 */
public final class MATSimModel implements ABMServerInterface {
	private static final Logger logger = LoggerFactory.getLogger("");
	public static final String MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR = "--matsim-output-directory";

	private final Scenario scenario ;

	/**
	 * some blackboardy thing that sits between ABM and BDI. can be null (so non-final is ok)
	 */
	private DataServer dataServer;

	/**
	 * some message buffer for the data server
	 */
	private final List<SimpleMessage> agentsUpdateMessages = new ArrayList<>(); 

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

	public MATSimModel( String[] args) {

		Config config = ConfigUtils.loadConfig( args[0] ) ;
		parseAdditionalArguments(args, config);
		config.network().setTimeVariantNetwork(true);

		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);


		config.qsim().setStartTime( 1.00 );
		config.qsim().setSimStarttimeInterpretation( StarttimeInterpretation.onlyUseStarttime );
		//		config.qsim().setEndTime( 8.*3600 + 1800. );

		config.controler().setWritePlansInterval(1);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		//		ConfigUtils.setVspDefaults(config);

		// ---

		this.agentManager = new PAAgentManager(eventsMonitors) ;

		// ---

		scenario = ScenarioUtils.loadScenario(config) ;

	}

	public final void init(List<String> bdiAgentIDs) {

		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			final double veryLargeSpeed = 9999999999.;
			if ( link.getFreespeed() > veryLargeSpeed ) {
				link.setFreespeed(veryLargeSpeed);
			}
		}

		// yy this could now be done in upstream code.  But since upstream code is user code, maybe we don't want it in there?  kai, nov'17
		for(String agentId: bdiAgentIDs) {
			agentManager.createAndAddBDIAgent(agentId);

			// default action:
			agentManager.getAgent(agentId).getActionHandler().registerBDIAction(
					MATSimActionList.DRIVETO, new DRIVETODefaultActionHandler(this) );
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
				
				this.addTravelTimeBinding(TransportMode.car).to( FreeSpeedTravelTime.class ) ;

				install( new EvacQSimModule(MATSimModel.this) ) ;

				this.addMobsimListenerBinding().toInstance( new MobsimInitializedListener() {
					@Override public void notifyMobsimInitialized(MobsimInitializedEvent e) {

						qSim = (QSim) e.getQueueSimulation() ;

						playPause = new PlayPauseSimulationControl( qSim ) ;
						playPause.pause(); 

						// add stub agent to keep simulation alive.  yyyy find nicer way to do this.
						Id<Link> dummyLinkId = qSim.getNetsimNetwork().getNetsimLinks().keySet().iterator().next() ;
						MobsimVehicle dummyVeh = null ;
						qSim.insertAgentIntoMobsim(new MATSimStubAgent(dummyLinkId,Id.createPersonId("StubAgent"),dummyVeh));

						//						initialiseVisualisedAgents() ;
					}
				}) ;
				
				// some infrastructure that makes results available: 
				this.addMobsimListenerBinding().toInstance( new MobsimAfterSimStepListener() {
					@Override public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
						publishDataToExternalListeners();
					}
				} ) ;

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
		return qSim.getChildInjector().getInstance( Replanner.class ) ;
		// this _should_ now be a singleton by injection. kai, nov'17
	}

	@Override
	public final void takeControl(AgentDataContainer agentDataContainer){
		runUntil( (int)(playPause.getLocalTime() + 1), agentDataContainer ) ;
	}
	public final void runUntil( long newTime , AgentDataContainer agentDataContainer ) {
		synchronized (agentDataContainer) {
			logger.trace("Received {}", agentManager.getAgentDataContainer());
			agentManager.updateActions();
			playPause.doStep( (int) (newTime) );
		}
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
	public final synchronized void addDateServerEvent(String type,SimpleMessage newEvent){
		//This does mean that the visualiser should be registered very early or events may be thrown away
		if(dataServer != null) agentsUpdateMessages.add(newEvent);
	}

	public final Scenario getScenario() {
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

	private final void publishDataToExternalListeners() {
		synchronized(this) {
			if (dataServer != null) {
				dataServer.publish( "matsim_agent_updates", agentsUpdateMessages.toArray(new SimpleMessage[agentsUpdateMessages.size()]) );
			}
			agentsUpdateMessages.clear();
		}
	}

	private final void setFreeSpeedExample(){
		// example how to set the freespeed of some link to zero:
		final double now = this.qSim.getSimTimer().getTimeOfDay();
		if ( now == 6.*3600. + 10.*60. ) {
			NetworkChangeEvent event = new NetworkChangeEvent( now ) ;
			event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0. ));
			event.addLink( scenario.getNetwork().getLinks().get( Id.createLinkId( 6876 )));
			NetworkUtils.addNetworkChangeEvent( scenario.getNetwork(),event);

			for ( MobsimAgent agent : this.getMobsimDataProvider().getAgents().values() ) {
				if ( !(agent instanceof MATSimStubAgent) ) {
					this.getReplanner().reRouteCurrentLeg(agent, now);
				}
			}
		}
	}

	public final void registerDataServer( DataServer server ) {
		dataServer = server;
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

	private void initialiseVisualisedAgents(){
		Map<Id<Link>,? extends Link> links = this.getScenario().getNetwork().getLinks();
		for(MobsimAgent agent: this.getMobsimDataProvider().getAgents().values()){
			SimpleMessage m = new SimpleMessage();
			m.name = "initAgent";
			//m.params = new Object[]{agent.getId().toString(),links.get(agent.getCurrentLinkId()).getFromNode().getId().toString(),(((MATSimReplannableAgent)agent).taxi == true?"T":"N")};
			m.params = new Object[]{//agent.getId().toString(),links.get(agent.getCurrentLinkId()).getFromNode().getId().toString(),"T"};
					agent.getId().toString(),
					links.get(agent.getCurrentLinkId()).getFromNode().getCoord().getX(),
					links.get(agent.getCurrentLinkId()).getFromNode().getCoord().getY()
			};
			this.addDateServerEvent("initAgent",m);
		}
		//interfaceV.sendInitialAgentData(new ArrayList<MobsimAgent>());
	}

}
