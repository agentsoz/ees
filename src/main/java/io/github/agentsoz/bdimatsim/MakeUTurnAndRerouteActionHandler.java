package io.github.agentsoz.bdimatsim;

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
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.withinday.utils.ReplanningException;

public final class MakeUTurnAndRerouteActionHandler implements BDIActionHandler {
	private final MATSimModel model;
	public MakeUTurnAndRerouteActionHandler(MATSimModel model ) {
		this.model = model;
	}
	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		MobsimAgent agent1 = model.getMobsimAgentFromIdString(agentID) ;
		
		// find links:
		Id<Link> currentLinkId = agent1.getCurrentLinkId();
		Link otherLink = NetworkUtils.findLinkInOppositeDirection( model.getScenario().getNetwork().getLinks().get( currentLinkId ) ) ;
		if ( otherLink==null ) {
			throw new ReplanningException("we are on a one-way street, cannot make u-turn") ;
		}
		
//		// new departure time:
//		model.getReplanner().editPlans().rescheduleCurrentActivityEndtime(agent1, newDepartureTime);
//
//		// need to memorize the mode:
//		String mode = model.getReplanner().editPlans().getModeOfCurrentOrNextTrip(agent1) ;
//
//		// flush everything beyond current:
//		model.getReplanner().editPlans().flushEverythingBeyondCurrent(agent1) ;
//
//		// new destination
//		Activity newAct = model.getReplanner().editPlans().createFinalActivity( "work", newLinkId ) ;
//		model.getReplanner().editPlans().addActivityAtEnd(agent1, newAct, mode) ;
//
//		// beyond is already non-matsim:
//
//		// Now register a event handler for when the agent arrives at the destination
//		PAAgent agent = model.getAgentManager().getAgent( agentID );
//		agent.getPerceptHandler().registerBDIPerceptHandler( agent.getAgentID(), MonitoredEventType.ArrivedAtDestination,
//				newLinkId, new BDIPerceptHandler() {
//			@Override
//			public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
//				PAAgent agent = model.getAgentManager().getAgent( agentId.toString() );
//				Object[] params = { linkId.toString() };
//				agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
//				// yy (shouldn't this be earlier?)
//				agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
//				agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
//				return true;
//			}
//		}
//				);
		return true;
	}
}