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

import io.github.agentsoz.nonmatsim.BDIActionHandler;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.opengis.filter.expression.Add;

import static io.github.agentsoz.bdimatsim.DRIVETODefaultActionHandler.printPlan;

final class ActionHandlerXyz implements BDIActionHandler {
	private static final Logger log = Logger.getLogger( ActionHandlerXyz.class ) ;
	
	private final MATSimModel model;

	public ActionHandlerXyz( MATSimModel model ) {
		this.model = model;
	}

	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		
		// This is just example code for what could be done.
		
		// ---

		// 0. we normally need the agent:
		
		MobsimAgent agent = model.getMobsimAgentFromIdString(agentID) ;
		Gbl.assertNotNull(agent) ;
		
		// get current index (yy I should add this to editPlans):
		final Integer currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );;

		// ---
		
		// 1. Modify an activity location that is somewhere in the middle of a MATSim plan (not the next thing).modify future activity
		{
			// 1.1 need index of activity to be replaced:
			final int index = 25;
			// there are methods ...editPlans().find... to help with this
			
			// 1.2 need to construct new activity:
			final String actType = "abc";
			final Coord coord = new Coord( 0., 1. );
			final Activity newAct = model.getScenario().getPopulation().getFactory().createActivityFromCoord( actType, coord );
			
			// 1.3 insert:
			model.getReplanner().editPlans().replaceActivity( agent, index, newAct );
			
			// 1.3b sometimes can get away with just modifying the activity that is there:
			final double newEndTime = 12. * 3600.;
			model.getReplanner().editPlans().rescheduleActivityEndtime( agent, index, newEndTime );
		}
		// ---
		
		// 2. Modify the route within the current trip - but leave the rest of the plan after the trip intact.
		{
			final double now = model.getTime();
			final String routingMode = MATSimModel.EvacRoutingMode.carGlobalInformation.name();
			
			// This will force to replan, e.g. based on global knowledge (depending on what the routing mode is)
			model.getReplanner().editTrips().replanCurrentTrip( agent, now, routingMode );
			
			// if you want instead modify the location of the next activity, it should be as in 1.3  
			
		}
		// ---
		
		// 3. Add an activity and the trip to and from it, into an existing plan. E.g. the agent is on its way to activity at
		// location X. Now we want it to (immediately) go instead to activity at location Y, then to activity at X and whatever
		// followed.
		{
			
			// 3.1 construct the new activity:
			final String actType = "dummy" ;
			final Coord coord = new Coord( 0., 1. ) ;
			Activity activityY = model.getScenario().getPopulation().getFactory().createActivityFromCoord( actType, coord ) ;
			
			// 3.2 insert it:
			model.getReplanner().editPlans().insertActivity( agent, currentIndex+1, activityY );
			
			// in principle, this should be it.  Now if this is well (enough) debugged I don't know ...  kai, sep'18
			
		}
		
		
		return true;
	}
	
}
