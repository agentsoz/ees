package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2020 by its authors. See AUTHORS file.
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
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.jill.lang.*;
import io.github.agentsoz.util.Location;

import java.util.Map;


@PlanInfo(postsGoals = {
		"io.github.agentsoz.ees.agents.archetype.GoalGoto",
})
public class PlanLeaveNow extends Plan {

	ArchetypeAgent agent = null;
	Location xyEvac;
	Location xyInvac;
	Location xyHome;

	public PlanLeaveNow(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (ArchetypeAgent)agent;
		body = steps;
	}

	public boolean context() {
		setName(this.getClass().getSimpleName()); // give this plan a user friendly name for logging purposes
		boolean isExperienced = agent.getBelief(ArchetypeAgent.Beliefname.Archetype.name()).equals("ExperiencedIndependent");
		boolean applicable = !isExperienced;
		agent.out("thinks " + getFullName() + " is " + (applicable ? "" : "not ") + "applicable");
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				// Leave for evac location
				xyEvac = agent.parseLocation(agent.getBelief(ArchetypeAgent.Beliefname.LocationEvacuationPreference.name()));
				agent.out("will go to evac location " + xyEvac + " #" + getFullName());
				subgoal(new GoalGoto(GoalGoto.class.getSimpleName(),
						xyEvac,
						Constants.EvacActivity.EvacPlace,
						Double.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.LagTimeInMinsForFinalResponse.name()))));
				// subgoal should be last call in any plan step
			},
			() -> {
				// Check if we have arrived
				if (ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
					agent.out("reached evac location " + xyEvac + " #" + getFullName());
					this.drop(); // all done, drop the remaining plan steps
				} else {
					Location[] xy = ((Location[])agent.getQueryPerceptInterface().queryPercept(
							String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null));
					agent.out("is stuck between locations " + xy[0] + " and " + xy[1] + " #" + getFullName());
					xyInvac = agent.parseLocation(agent.getBelief(ArchetypeAgent.Beliefname.LocationInvacPreference.name()));
					if (Location.distanceBetween(xy[0],xyEvac) > Location.distanceBetween(xy[0],xyInvac)) {
						// Try invac location
						agent.out("will go to invac location " + xyInvac + " #" + getFullName());
						subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyInvac, Constants.EvacActivity.InvacPlace, 1));
						// subgoal should be last call in any plan step
					} else {
						// Else try evac location again
						agent.out("will go to evac location " + xyEvac + " #" + getFullName());
						subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyEvac, Constants.EvacActivity.EvacPlace, 1));
						// subgoal should be last call in any plan step
					}
				}
			},
			() -> {
				// Check if we have arrived
				if (ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
					agent.out("reached invac location " + xyEvac + " #" + getFullName());
					this.drop(); // all done, drop the remaining plan steps
				} else {
					Location[] xy = ((Location[])agent.getQueryPerceptInterface().queryPercept(
							String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null));
					agent.out("is stuck between locations " + xy[0] + " and " + xy[1] + " #" + getFullName());
					xyHome = agent.parseLocation(agent.getBelief(ArchetypeAgent.Beliefname.LocationHome.name()));
					if (Location.distanceBetween(xy[0],xyInvac) > Location.distanceBetween(xy[0],xyHome)) {
						// Try home
						agent.out("will go home to " + xyHome + " #" + getFullName());
						subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyHome, Constants.EvacActivity.Home, 1));
						// subgoal should be last call in any plan step
					} else {
						// Else try invac location again
						agent.out("will go to invac location " + xyInvac + " #" + getFullName());
						subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyInvac, Constants.EvacActivity.InvacPlace, 1));
						// subgoal should be last call in any plan step
					}
				}
			},
			() -> {
				// Check if we have arrived
				if (ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
					agent.out("reached destination #" + getFullName());
				} else {
					Location[] xy = ((Location[])agent.getQueryPerceptInterface().queryPercept(
							String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null));
					agent.out("is stranded between locations " + xy[0] + " and " + xy[1] + " #" + getFullName());
				}
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
