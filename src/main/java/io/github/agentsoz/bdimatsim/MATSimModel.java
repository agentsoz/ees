package io.github.agentsoz.bdimatsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
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
import io.github.agentsoz.bdiabm.BDIServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdimatsim.app.MATSimApplicationInterface;
import io.github.agentsoz.bdimatsim.app.StubPlugin;
import io.github.agentsoz.bdimatsim.moduleInterface.data.SimpleMessage;
import io.github.agentsoz.dataInterface.DataServer;

/**
 * @author QingyuChen, KaiNagel, Dhi Singh
 */
public final class MATSimModel implements ABMServerInterface {
	final Logger logger = LoggerFactory.getLogger("");

	private Scenario scenario ;

	private DataServer dataServer;
	private List<SimpleMessage> agentsUpdateMessages;

	private List<Id<Person>> bdiAgentIDs;

	private double time;

	//	private final MatsimParameterHandler matSimParameterManager;
	private MATSimAgentManager agentManager ;
	private MobsimDataProvider mobsimDataProvider = new MobsimDataProvider() ;

	final BDIServerInterface bdiServer;

	private MATSimApplicationInterface application;

	private AgentActivityEventHandler eventsHandler;

	public final Replanner getReplanner() {
		return agentManager.getReplanner() ;
	}

	final MATSimAgentManager getAgentManager() {
		return agentManager;
	}
	@Override
	public final void takeControl(AgentDataContainer agentDataContainer){
		logger.trace("Received {}", agentManager.getAgentDataContainer());
		agentManager.updateActions(agentManager.getAgentDataContainer());
	}
	final void setTime(double time) {
		this.time = time;
	}

	final synchronized void addExternalEvent(String type,SimpleMessage newEvent){
		//This does mean that the visualiser should be registered very early or events may be thrown away
		if(dataServer != null) agentsUpdateMessages.add(newEvent);
	}

	public final List<Id<Person>> getBDIAgentIDs(){
		return bdiAgentIDs;
	}

	final Map<Id<Person>,MobsimAgent> getAgentMap(){
		return this.mobsimDataProvider.getAgents() ;
	}

	public final Scenario getScenario() {
		return this.scenario ;
	}

	final MobsimDataProvider getMobsimDataProvider() {
		return this.mobsimDataProvider ;
	}

	public MATSimModel( BDIServerInterface bidServer) {
		this.bdiServer = bidServer ;
		this.agentsUpdateMessages = new ArrayList<SimpleMessage>();
		this.agentManager = new MATSimAgentManager( this ) ;
		this.registerPlugin(new StubPlugin());
	}

	public final void registerPlugin(MATSimApplicationInterface app) {
		this.application = app;
	}

	public final void run(String[] args) {
		// (this needs to be public)

		Config config = ConfigUtils.loadConfig( args[0] ) ;

		config.network().setTimeVariantNetwork(true);

		// Normally, the qsim starts at the earliest activity end time.  The following tells the mobsim to start
		// at 2 seconds before 6:00, no matter what is in the initial plans file:
		config.qsim().setStartTime( 1.00 );
		config.qsim().setSimStarttimeInterpretation( StarttimeInterpretation.onlyUseStarttime );
		//config.qsim().setEndTime( 8.*3600 + 1800. );

		config.controler().setWritePlansInterval(1);
		config.planCalcScore().setWriteExperiencedPlans(true);

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );
		
		config.planCalcScore().setWriteExperiencedPlans(true);
		
		// ---

		scenario = ScenarioUtils.loadScenario(config) ;
		
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			final double veryLargeSpeed = 9999999999.;
			if ( link.getFreespeed() > veryLargeSpeed ) {
				link.setFreespeed(veryLargeSpeed);
			}
		}

		bdiAgentIDs = Utils.getBDIAgentIDs( scenario );

		// ---

		final Controler controller = new Controler( scenario );

		eventsHandler = new AgentActivityEventHandler(MATSimModel.this);
		controller.getEvents().addHandler(eventsHandler);

		controller.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.addMobsimListenerBinding().toInstance( new MobsimInitializedListener() {
					@Override public void notifyMobsimInitialized(MobsimInitializedEvent e) {
						// for the time being doing this here since from a matsim perspective we would like to
						// re-create them in every iteration. kai, oct;17

						QSim qSim = (QSim) e.getQueueSimulation() ;

						//Utils.initialiseVisualisedAgents(MATSimModel.this) ;

						// Allow the application to adjust the BDI agents list prior to creating agents
						application.notifyBeforeCreatingBDICounterparts(bdiAgentIDs);

						for(Id<Person> agentId: bdiAgentIDs) {
							/*Important - add agent to agentManager */
							agentManager.createAndAddBDIAgent(agentId);
							//MATSimModel.this.getAgentManager().getReplanner().removeActivities(agentId);
						}

						// Allow the application to configure the freshly baked agents
						application.notifyAfterCreatingBDICounterparts(MATSimModel.this.getBDIAgentIDs());

						// Register new BDI actions and percepts from the application
						// Must be done after the agents have been created since new 
						// actions/percepts are registered with each BDI agent
						agentManager.registerApplicationActionsPercepts(application);

						// passes important matsim qsim functionality to the agent manager:
						agentManager.setUpReplanner(application.getReplanner(qSim), qSim);
						// yy "qSim" is too powerful an object here. kai, mar'15

						// add stub agent to keep simulation alive.  yyyy find nicer way to do this.
						Id<Link> dummyLinkId = qSim.getNetsimNetwork().getNetsimLinks().keySet().iterator().next() ;
						MobsimVehicle dummyVeh = null ;
						qSim.insertAgentIntoMobsim(new StubAgent(dummyLinkId,Id.createPersonId("StubAgent"),dummyVeh));
					}
				}) ;

				this.addMobsimListenerBinding().toInstance( new MobsimBeforeSimStepListener() {
					/**
					 * The most important method - called each time step during the iteration
					 */		
					@Override
					public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
						MATSimModel.this.setTime(e.getSimulationTime());

						// the real work is done here:

						// On 25 Aug 2016 dsingh said:
						// The notifyMobsimBeforeSimStep(e) function essentially provides
						// the simulation clock in any MATSim-BDI integration, since there
						// is no external controller (MATSim acts as the controller).
						// So this is where we control and synchronise all models
						// (ABM, BDI, Fire, other) with respect to data transfer
						//
						// 1. First step the data server so that it can conduct
						//    the data transfer between all connected models
						publishDataToExternalListeners();
						// 2. Next, call the BDI model that will populate the 
						//    agent data container with any action/percepts per agent
						bdiServer.takeControl(agentManager.getAgentDataContainer());
						// 3. Finally, call the MATSim model and process 
						//    the BDI actions/percepts, and re-populate the 
						//    agent data container, ready to pass to the BDI system in the 
						//    next cycle.
						MATSimModel.this.takeControl(agentManager.getAgentDataContainer());
					}
				} ) ; // end anonymous class MobsimListener
				
				this.addMobsimListenerBinding().toInstance( mobsimDataProvider );
			}
		}) ;

		// FIXME: dsingh, 25aug16: BDI init and start should be done outside of MATSim model 
		this.bdiServer.init(this.agentManager.getAgentDataContainer(),
				this.agentManager.getAgentStateList(), this,
				Utils.getPersonIDsAsArray(this.bdiAgentIDs));
		// (yy "this" is too powerful an object here, but it saves many lines of code. kai, mar'15)

		this.bdiServer.start();

		controller.run();
	}

	/**
	 * Publish updates from this model to any connected listeners, such
	 * as visualisers. Updates buffer is then flushed. 
	 */
	final void publishDataToExternalListeners() {
		synchronized(this) {
			if (dataServer != null) {
				dataServer.stepTime();
				dataServer.publish( "matsim_agent_updates", agentsUpdateMessages.toArray(new SimpleMessage[agentsUpdateMessages.size()]) );
			}
			agentsUpdateMessages.clear();
		}
	}

	final void setFreeSpeedExample(){
		// example how to set the freespeed of some link to zero:
		if ( this.time == 6.*3600. + 10.*60. ) {
			NetworkChangeEvent event = new NetworkChangeEvent( this.time ) ;
			event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  0. ));
			event.addLink( scenario.getNetwork().getLinks().get( Id.createLinkId( 6876 )));
			NetworkUtils.addNetworkChangeEvent( scenario.getNetwork(),event);

			for ( MobsimAgent agent : this.getAgentMap().values() ) {
				if ( !(agent instanceof StubAgent) ) {
					this.getReplanner().reRouteCurrentLeg(agent, time);
				}
			}
		}
	}

	public final void registerDataServer( DataServer server ) {
		dataServer = server;
	}

	// the following seems (or is?) a bit confused since different pieces of the program are accessing different containers.  Seems
	// that agentManager maintains the list of BDI agents, while mobsimDataProvider maintains a list of the matsim agents.  
	// The former is a subset of the latter. But it is not so clear if this has to matter, i.e. should there really be two lists, or
	// just one (like a centralized ldap server)? kai, mar'15

	public final MATSimAgent getBDIAgent(Id<Person> personId ) {
		return this.agentManager.getAgent( personId ) ;
	}

	public final MATSimAgent getBDIAgent(String agentID) {
		return this.agentManager.getAgent( agentID ) ;
	}

	public final Map<Id<Person>, MobsimAgent> getMobsimAgentMap() {
		return this.mobsimDataProvider.getAgents() ;
	}

	public final double getTime() {
		return time ;
	}

	AgentActivityEventHandler getEventHandler() {
		return eventsHandler;
	}

}
