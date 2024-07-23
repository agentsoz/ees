package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
public class PlanResponseWhenDependentsNearby extends Plan {

	ArchetypeAgent agent = null;
	Location xyHome;
	Location xyDeps;
	double distDeps;
	double distHome;

	public PlanResponseWhenDependentsNearby(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (ArchetypeAgent)agent;
		body = steps;
	}

	/**
	 * Applies during initial response, if distance to dependents is less than or equal to distance to home
	 * @return true if distance to dependents is less than or equal to distance to home, false otherwise
	 */
	public boolean context() {
		setName(this.getClass().getSimpleName()); // give this plan a user friendly name for logging purposes
		boolean applicable = false;
		boolean isStuck = Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()));
		if (isStuck) {
			return false;
		}
		boolean hasDependents = Boolean.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.HasDependents.name()));
		if (hasDependents) {
			xyHome = agent.getHomeLocation();
			xyDeps = agent.getDependentsLocation();
			if (xyHome != null && xyDeps != null) {
				try {
					// Using beeline distance which is more natural and not computationally expensive
					distHome = agent.getDrivingDistanceTo(xyHome);
					distDeps = agent.getDrivingDistanceTo(xyDeps);
					applicable = (distDeps <= distHome);
				}  catch (AgentNotFoundException e) {
					agent.handleAgentNotFoundException(e.getMessage());
				}
			}
		}
		agent.out("thinks " + getFullName() + " is " + (applicable ? "" : "not ") + "applicable");
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				agent.out("will do #" + getFullName());

				if (distDeps > 0) {
					// Go visits dependents now
					agent.out("will go to dependents nearby at " + xyDeps + " #" + getFullName());
					subgoal(new GoalGoto(GoalGoto.class.getSimpleName(),
							xyDeps,
							Constants.EvacActivity.DependentsPlace,
							Double.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.LagTimeInMinsForInitialResponse.name()))));
					// subgoal should be last call in any plan step
				}
            },
			() -> {
				// Abort if we are stuck
				if(Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()))) {
					drop();
					return;
				}
				// Check if we have arrived
                try {
					distDeps = agent.getDrivingDistanceTo(xyDeps);
				}  catch (AgentNotFoundException e) {
					agent.handleAgentNotFoundException(e.getMessage());
					drop();
					return;
				}
				boolean reached = (distDeps <= 0);
				agent.out((reached ? "is with" : "did not reach") + " dependents at " + xyDeps + " #" + getFullName());
				// Decide if we will go home from here
				boolean willGoHomeAfterVisitingDependents = Boolean.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.WillGoHomeAfterVisitingDependents.name()));
				if (reached && !willGoHomeAfterVisitingDependents) {
					agent.out("will wait with dependents at " + xyDeps + " #" + getFullName());
					this.drop(); // all done, drop the remaining plan steps
				} else {
					agent.out("will go home to " + xyHome + " #" + getFullName());
					subgoal(new GoalGoto(GoalGoto.class.getSimpleName(), xyHome, Constants.EvacActivity.Home, 5));
					// subgoal should be last call in any plan step
				}
            },
			() -> {
				// Abort if we are stuck
				if(Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()))) {
					drop();
					return;
				}
				// Check if we have arrived
				if (ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
					agent.out("reached home at " + xyHome + " #" + getFullName());
				} else {
				    try {
						Location[] xy = ((Location[]) agent.getQueryPerceptInterface().queryPercept(
								String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null));
						agent.out("is stuck between locations " + xy[0] + " and " + xy[1] + " #" + getFullName());
					}  catch (AgentNotFoundException e) {
						agent.handleAgentNotFoundException(e.getMessage());
					}
				}
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
