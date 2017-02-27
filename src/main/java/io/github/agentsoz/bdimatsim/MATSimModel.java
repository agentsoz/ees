package io.github.agentsoz.bdimatsim;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

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

	private final MatsimParameterHandler matSimParameterManager;
	private MatsimAgentManager agentManager ;
	private MobsimDataProvider mobsimDataProvider = new MobsimDataProvider() ;

	final BDIServerInterface bdiServer;

	private MATSimApplicationInterface plugin;
	
	public final Replanner getReplanner() {
		return agentManager.getReplanner() ;
	}

	final MatsimAgentManager getAgentManager() {
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

	public MATSimModel( BDIServerInterface bidServer, MatsimParameterHandler matsimParams) {
		this.bdiServer = bidServer ;
		this.matSimParameterManager = matsimParams ;
		this.agentsUpdateMessages = new ArrayList<SimpleMessage>();
		this.agentManager = new MatsimAgentManager( this ) ;
		this.registerPlugin(new StubPlugin());
	}

	public final void registerPlugin(MATSimApplicationInterface app) {
		this.plugin = app;
	}
	
	public final void run(String parameterFile,String[] args) {
		// (this needs to be public)

		if (parameterFile != null) {
			matSimParameterManager.readParameters(parameterFile);
		}
		Config config = ConfigUtils.loadConfig( args[0] ) ;

		config.network().setTimeVariantNetwork(true);

		// Normally, the qsim starts at the earliest activity end time.  The following tells the mobsim to start
		// at 2 seconds before 6:00, no matter what is in the initial plans file:
		config.qsim().setStartTime( 1.00 );
		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.ONLY_USE_STARTTIME );
		//config.qsim().setEndTime( 8.*3600 + 1800. );

		config.controler().setWritePlansInterval(1);
		config.planCalcScore().setWriteExperiencedPlans(true);
		
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );

		// ---

		scenario = ScenarioUtils.loadScenario(config) ;

		// FIXME: Get rid of the matSimParameterManager altogether (dsingh, 25aug16)
		// At the moment it is used in the default bushfire application, but has
		// already been removed for the jill version (where it is initialised as null).
		// Will need a way to get the #bdi agents from the config to here (needed to
		// allow both BDI and MATSim agents to co exist).
		// For now, if matSimParameterManager is null then all MATSim agents
		// will have BDI counterparts
		if (this.matSimParameterManager != null) {
			// this is some conversion of nice matsim objects into ugly conventional data structures :-):
			final Collection<Id<Link>> allLinkIDs = this.getScenario().getNetwork().getLinks().keySet() ;
			this.matSimParameterManager.setNETWORKIDS(Utils.createFlatLinkIDs(allLinkIDs)); //Link IDs in string[] format
		}
		final Collection<? extends Link> links = this.getScenario().getNetwork().getLinks().values();
		Utils.generateLinkCoordsAsArray(links);
		Utils.computeBoundingBox(links);

		this.bdiAgentIDs = Utils.getBDIAgentIDs( scenario, matSimParameterManager );

		// ---

		final Controler controller = new Controler( scenario );

		final EventsManager eventsManager = controller.getEvents();
		eventsManager.addHandler(new AgentActivityEventHandler(MATSimModel.this));

		// having anonymous classes stuck into each other is not very pleasing to read, but it makes for short code and
		// avoids going overboard with passing object references around. kai, mar'15  & may'15
		controller.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.bindMobsim().toProvider(new Provider<Mobsim>(){
					@Override
					public Mobsim get() {
						QSim qSim = QSimUtils.createDefaultQSim(scenario, eventsManager) ;

						// ===
						qSim.addQueueSimulationListeners( new MobsimBeforeSimStepListener() {
							boolean setupFlag = true ;
							/**
							 * The most important method - called each time step during the iteration
							 */		
							@Override
							public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
								MATSimModel.this.setTime(e.getSimulationTime());
								if(setupFlag) {
									// should be possible to do the initialization in some other way ...
									
									//Utils.initialiseVisualisedAgents(MATSimModel.this) ;

									// Allow the application to adjust the BDI agents list prior to creating agents
									plugin.notifyBeforeCreatingBDICounterparts(MATSimModel.this.getBDIAgentIDs());
									
									for(Id<Person> agentId: MATSimModel.this.getBDIAgentIDs()) {
										/*Important - add agent to agentManager */
										MATSimModel.this.getAgentManager().createAndAddBDIAgent(agentId);
										//MATSimModel.this.getAgentManager().getReplanner().removeActivities(agentId);
									}
									
									// Allow the application to configure the freshly baked agents
									plugin.notifyAfterCreatingBDICounterparts(MATSimModel.this.getBDIAgentIDs());
									
									// Register new BDI actions and percepts from the application
									// Must be done after the agents have been created since new 
									// actions/percepts are registered with each BDI agent
									MATSimModel.this.getAgentManager().registerApplicationActionsPercepts(plugin);
									
									// Flag that setup is done
									setupFlag = false;
								}
								// the real work is done here:
								//MATSimModel.this.runBDIModule();
								
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
								MATSimModel.this.bdiServer.takeControl(agentManager.getAgentDataContainer());
								// 3. Finally, call the MATSim model and process 
								//    the BDI actions/percepts, and re-populate the 
								//    agent data container, ready to pass to the BDI system in the 
								//    next cycle.
								MATSimModel.this.takeControl(agentManager.getAgentDataContainer());
							}
						} ) ; // end anonymous class MobsimListener
						// ===
						
						// passes important matsim qsim functionality to the agent manager:
						agentManager.setUpReplanner(qSim);
						// yy "qSim" is too powerful an object here. kai, mar'15

						// add stub agent to keep simulation alive:
						Id<Link> dummyLinkId = qSim.getNetsimNetwork().getNetsimLinks().keySet().iterator().next() ;
						MobsimVehicle veh = null ;
						StubAgent stubAgent = new StubAgent(dummyLinkId,Id.createPersonId("StubAgent"),veh);
						qSim.insertAgentIntoMobsim(stubAgent);

						// return qsim (this is a factory)
						return qSim ;
					}
				}) ;
			}
		});
		

		controller.getMobsimListeners().add(mobsimDataProvider);

		// FIXME: dsingh, 25aug16: BDI init and start should be done outside of MATSim model 
		this.bdiServer.init(this.agentManager.getAgentDataContainer(),
				this.agentManager.getAgentStateList(), this,
				Utils.getPersonIDsAsArray(this.bdiAgentIDs));
		
		this.bdiServer.start();
		// (yy "this" is too powerful an object here, but it saves many lines of code. kai, mar'15)

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
			NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl() ;
			NetworkChangeEvent event = cef.createNetworkChangeEvent( this.time ) ;
			event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  0. ));
			event.addLink( scenario.getNetwork().getLinks().get( Id.createLinkId( 6876 )));
			((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);

			for ( MobsimAgent agent : this.getAgentMap().values() ) {
				if ( !(agent instanceof StubAgent) ) {
					this.getReplanner().reRouteCurrentLeg(agent, time);
				}
			}
		}
	}

	@Override
	public final Object[] queryPercept(String agentID, String perceptID) {
		return this.getBDIAgent(agentID).getPerceptHandler().processPercept(agentID, perceptID);
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

	final Map<Id<Person>, MobsimAgent> getMobsimAgentMap() {
		return this.mobsimDataProvider.getAgents() ;
	}

	double getTime() {
		return time ;
	}

}
