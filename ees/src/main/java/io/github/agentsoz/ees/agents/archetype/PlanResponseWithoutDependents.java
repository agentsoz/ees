package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
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
public class PlanResponseWithoutDependents extends Plan {

	ArchetypeAgent agent = null;
	Location xyHome;

	public PlanResponseWithoutDependents(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (ArchetypeAgent)agent;
		body = steps;
	}

	/**
	 * Applies during initial response, when there are no dependents to attend to and agent is not at home
	 * @return true if the agent has no dependents, false otherwise
	 */
	public boolean context() {
		boolean applicable = false;
		setName(this.getClass().getSimpleName()); // give this plan a user friendly name for logging purposes
		boolean isStuck = Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()));
		if (isStuck) {
			return false;
		}
		boolean hasDependents = Boolean.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.HasDependents.name()));
		if (!hasDependents) {
			xyHome = agent.getHomeLocation();
			try {
				double distHome = agent.getDrivingDistanceTo(xyHome);
				applicable = distHome > 0;
			}  catch (AgentNotFoundException e) {
				agent.handleAgentNotFoundException(e.getMessage());
			}
		}
		agent.out("thinks " + getFullName() + " is " + (applicable ? "" : "not ") + "applicable");
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				agent.out("will do #" + getFullName());

				boolean willGoHomeBeforeLeaving = Boolean.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.WillGoHomeBeforeLeaving.name()));
				if (willGoHomeBeforeLeaving) {
					// Go home
					agent.out("will go home to " + xyHome + " #" + getFullName());
					subgoal(new GoalGoto(GoalGoto.class.getSimpleName(),
							xyHome,
							Constants.EvacActivity.Home,
							Double.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.LagTimeInMinsForInitialResponse.name()))));
					// subgoal should be last call in any plan step
				} else {
					// Continue doing what it is doing
					agent.out("will wait and see #" + getFullName());
					this.drop(); // all done, drop the remaining plan steps
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
