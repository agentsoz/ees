package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
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
	Location dest;

	public PlanLeaveNow(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (ArchetypeAgent)agent;
		body = steps;
	}

	public boolean context() {
		setName(this.getClass().getSimpleName()); // give this plan a user friendly name for logging purposes
		boolean isExperienced = agent.getBelief(ArchetypeAgent.Beliefname.Archetype.name()).equals("ExperiencedIndependent");
		boolean isStuck = Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()));
		boolean applicable = !isStuck && !isExperienced;
		agent.out("thinks " + getFullName() + " is " + (applicable ? "" : "not ") + "applicable");
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				agent.out("will do #" + getFullName());

				// Leave for evac location
				xyEvac = agent.getEvacLocation();
				dest = xyEvac;
				agent.out("will go to evac location " + xyEvac + " #" + getFullName());
				subgoal(new GoalGoto(GoalGoto.class.getSimpleName(),
						xyEvac,
						Constants.EvacActivity.EvacPlace,
						Double.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.LagTimeInMinsForFinalResponse.name()))));
				// subgoal should be last call in any plan step
			},
			() -> {
			    // Abort if we are stuck
				if(Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()))) {
					drop();
					return;
				}
				try {
					// Check if we have arrived
					Location[] xy = ((Location[]) agent.getQueryPerceptInterface().queryPercept(
							String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null));
					if (Location.distanceBetween(xy[0], dest) == 0.0 || Location.distanceBetween(xy[1], dest) == 0.0 ||
							ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
						agent.out("reached evac location " + xyEvac + " #" + getFullName());
						this.drop(); // all done, drop the remaining plan steps
					} else {
						agent.out("is stuck between locations " + xy[0] + " and " + xy[1] + " #" + getFullName());
						xyInvac = agent.getInvacLocation();
						if (Location.distanceBetween(xy[0], xyEvac) > Location.distanceBetween(xy[0], xyInvac)) {
							dest = xyInvac;
							// Try invac location
							agent.out("will go to invac location " + xyInvac + " #" + getFullName());
							subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyInvac, Constants.EvacActivity.InvacPlace, 1));
							// subgoal should be last call in any plan step
						} else {
							dest = xyEvac;
							// Else try evac location again
							agent.out("will go to evac location " + xyEvac + " #" + getFullName());
							subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyEvac, Constants.EvacActivity.EvacPlace, 1));
							// subgoal should be last call in any plan step
						}
					}
				}   catch (AgentNotFoundException e) {
					agent.handleAgentNotFoundException(e.getMessage());
					drop();
					return;
				}
			},
			() -> {
				// Abort if we are stuck
				if(Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()))) {
					drop();
					return;
				}
				try {
					// Check if we have arrived
					Location[] xy = ((Location[]) agent.getQueryPerceptInterface().queryPercept(
							String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null));
					if (Location.distanceBetween(xy[0], dest) == 0.0 || Location.distanceBetween(xy[1], dest) == 0.0 ||
							ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
						agent.out("reached destination " + dest + " #" + getFullName());
						this.drop(); // all done, drop the remaining plan steps
					} else {
						agent.out("is stuck between locations " + xy[0] + " and " + xy[1] + " #" + getFullName());
						xyHome = agent.getHomeLocation();
						if (Location.distanceBetween(xy[0], xyInvac) > Location.distanceBetween(xy[0], xyHome)) {
							dest = xyHome;
							// Try home
							agent.out("will go home to " + xyHome + " #" + getFullName());
							subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyHome, Constants.EvacActivity.Home, 1));
							// subgoal should be last call in any plan step
						} else {
							dest = xyInvac;
							// Else try invac location again
							agent.out("will go to invac location " + xyInvac + " #" + getFullName());
							subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyInvac, Constants.EvacActivity.InvacPlace, 1));
							// subgoal should be last call in any plan step
						}
					}
				}   catch (AgentNotFoundException e) {
					agent.handleAgentNotFoundException(e.getMessage());
					drop();
					return;
				}
			},
			() -> {
				// Abort if we are stuck
				if(Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()))) {
					drop();
					return;
				}
				try {
					// Check if we have arrived
					Location[] xy = ((Location[]) agent.getQueryPerceptInterface().queryPercept(
							String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null));
					if (Location.distanceBetween(xy[0], dest) == 0.0 || Location.distanceBetween(xy[1], dest) == 0.0 ||
							ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
						agent.out("reached destination #" + getFullName());
					} else {
						agent.out("is stranded between locations " + xy[0] + " and " + xy[1] + " #" + getFullName());
					}
				}   catch (AgentNotFoundException e) {
					agent.handleAgentNotFoundException(e.getMessage());
				}
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
