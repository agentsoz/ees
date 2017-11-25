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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.network.NetworkUtils;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.PAAgent;

public final class DRIVETODefaultActionHandler implements BDIActionHandler {
	private final MATSimModel model;
	public DRIVETODefaultActionHandler(MATSimModel model ) {
		this.model = model;
	}
	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		// assertions:
		if ( args.length < 2 ) {
			throw new RuntimeException("not enough information; we need coordinate-to-go-to and departure time") ;
		}
		if ( ! ( args[1] instanceof double[] ) ) {
			throw new RuntimeException("argument #1 not of type double[]; cannot be interpreted as coordinate" ) ;
		}

		// preparations:
		double[] coords = (double[]) args[1];
		Coord newCoord = new Coord(coords[0], coords[1]);
		Id<Link> newLinkId = NetworkUtils.getNearestLink(model.getScenario().getNetwork(), newCoord).getId();
		//  could give just coordinates to matsim, but for time being need the linkId in the percept anyways

		double newDepartureTime = (double)args[2];

		MobsimAgent agent1 = model.getMobsimAgentFromIdString(agentID) ;
		
		// new departure time:
		model.getReplanner().editPlans().rescheduleCurrentActivityEndtime(agent1, newDepartureTime);
		
		// need to memorize the mode:
		String mode = model.getReplanner().editPlans().getModeOfCurrentOrNextTrip(agent1) ;
		
		// flush everything beyond current:
		model.getReplanner().editPlans().flushEverythingBeyondCurrent(agent1) ;

		// new destination
		Activity newAct = model.getReplanner().editPlans().createAgentThatLetsMatsimEnd( "work", newLinkId ) ;
		model.getReplanner().editPlans().addActivityAtEnd(agent1, newAct, mode) ;
		
		// beyond is already non-matsim:

		// Now register a event handler for when the agent arrives at the destination
		PAAgent agent = model.getAgentManager().getAgent( agentID );
		agent.getPerceptHandler().registerBDIPerceptHandler( agent.getAgentID(), MonitoredEventType.ArrivedAtDestination,
				newLinkId, new BDIPerceptHandler() {
			@Override
			public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
				PAAgent agent = model.getAgentManager().getAgent( agentId.toString() );
				Object[] params = { linkId.toString() };
				agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
				// yy (shouldn't this be earlier?)
				agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
				agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
				return true;
			}
		}
				);
		return true;
	}
}