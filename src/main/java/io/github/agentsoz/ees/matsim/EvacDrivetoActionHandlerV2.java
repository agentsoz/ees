package io.github.agentsoz.ees.matsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.ees.ActionList;
import io.github.agentsoz.ees.PerceptList;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EvacDrivetoActionHandlerV2 implements BDIActionHandler {
	private static final Logger log = LoggerFactory.getLogger(EvacDrivetoActionHandlerV2.class ) ;

	private final MATSimModel model;
	public EvacDrivetoActionHandlerV2(MATSimModel model ) {
//		log.setLevel(Level.DEBUG);
		this.model = model;
	}
	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		log.debug("------------------------------------------------------------------------------------------");
		{
			// dsingh 16/may/18 - towards #12
			if (args.length < 4) {
				log.error("agent " + agentID + " DRIVETO handler has " +args.length + " args (>=4 expected); will continue without handling this event");
				return true;
			}
		}
		// assertions:
		Gbl.assertIf( args.length >= 4 ) ;
		Gbl.assertIf( args[1] instanceof double[] ) ;
		double[] coords = (double[]) args[1];
		Coord coord = new Coord( coords[0], coords[1] ) ;
		Gbl.assertIf( args[3] instanceof MATSimEvacModel.EvacRoutingMode ) ; // could have some default
		
		log.debug("replanning; time step=" + model.getTime()
		+ "; agentId=" + agentID + "; routing mode " + args[3] ) ;

		// preparations:
		
		final Link nearestLink = NetworkUtils.getNearestLink(model.getScenario().getNetwork(), coord );
		Gbl.assertNotNull(nearestLink);
		Id<Link> newLinkId = nearestLink.getId();
		//  could give just coordinates to matsim, but for time being need the linkId in the percept anyways
		
		MobsimAgent mobsimAgent = model.getMobsimAgentFromIdString(agentID) ;
		Gbl.assertNotNull(mobsimAgent) ;
		
		// new departure time:
		if ( model.getReplanner().editPlans().isAtRealActivity(mobsimAgent) ) {
			model.getReplanner().editPlans().rescheduleCurrentActivityEndtime(mobsimAgent, (double)args[2]);
		}
		
		// routingMode:
		String routingMode = null ; // could have some default
		switch (((MATSimEvacModel.EvacRoutingMode) args[3])) {
			case carFreespeed:
				routingMode = MATSimEvacModel.EvacRoutingMode.carFreespeed.name() ;
				break;
			case carGlobalInformation:
				routingMode = MATSimEvacModel.EvacRoutingMode.carGlobalInformation.name() ;
				break;
			case emergencyVehicle:
				routingMode = MATSimEvacModel.EvacRoutingMode.emergencyVehicle.name();
				break;
			default:
				throw new RuntimeException("not implemented" ) ;
		}
		
		// flush everything beyond current:
		printPlan("before flush: ", mobsimAgent);
		model.getReplanner().editPlans().flushEverythingBeyondCurrent(mobsimAgent) ;
		printPlan("after flush: " , mobsimAgent) ;

		// new destination
		Activity newAct = model.getReplanner().editPlans().createFinalActivity( "driveTo", newLinkId ) ;
		model.getReplanner().editPlans().addActivityAtEnd(mobsimAgent, newAct, routingMode) ;
		printPlan("after adding act: " , mobsimAgent ) ;
		
		// beyond is already non-matsim:

		// Now register an event handler for when the agent arrives at the destination
		PAAgent paAgent = model.getAgentManager().getAgent( agentID );
		paAgent.getEventsMonitorRegistry().registerMonitor( paAgent.getAgentID(), MonitoredEventType.PersonArrivalEvent, newLinkId.toString(), new BDIPerceptHandler() {
					@Override
					public boolean handle( Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent ) {
						PAAgent agent = model.getAgentManager().getAgent(agentId.toString());
						Object[] params = {linkId.toString()};
						ActionContent ac = new ActionContent(params, ActionContent.State.PASSED, ActionList.DRIVETO);
						model.getAgentManager().getAgentDataContainerV2().putAction(agent.getAgentID(), ActionList.DRIVETO, ac);
						PerceptContent pc = new PerceptContent(PerceptList.ARRIVED, params[0]);
						model.getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.ARRIVED, pc);
						return true;
					}
				} );

		log.debug("------------------------------------------------------------------------------------------"); ;
		return true;
	}
	
	private void printPlan(String str ,MobsimAgent agent1) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent1) ;
		log.debug(str + plan ); ;
		for ( PlanElement pe : plan.getPlanElements() ) {
			log.debug("    " + pe ); ;
		}
	}
}
