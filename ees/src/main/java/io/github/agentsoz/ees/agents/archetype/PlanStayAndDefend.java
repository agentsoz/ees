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
public class PlanStayAndDefend extends Plan {

	ArchetypeAgent agent = null;
	Location xyHome;

	public PlanStayAndDefend(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (ArchetypeAgent)agent;
		body = steps;
	}

	public boolean context() {
		setName(this.getClass().getSimpleName()); // give this plan a user friendly name for logging purposes
		boolean isStuck = Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()));
		if (isStuck) {
			return false;
		}
		boolean hasDependents = Boolean.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.HasDependents.name()));
		boolean isExperienced = agent.getBelief(ArchetypeAgent.Beliefname.Archetype.name()).equals("Experienced.Independent");
		boolean applicable = isExperienced && !hasDependents;
		agent.out("thinks " + getFullName() + " is " + (applicable ? "" : "not ") + "applicable");
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				agent.out("will do #" + getFullName());

				xyHome = agent.getHomeLocation();
				try {
					double distHome = agent.getDrivingDistanceTo(xyHome);
					boolean atHome = (distHome <= 0);
					if (!atHome) {
						// Go home first
						agent.out("will go home to " + xyHome + " #" + getFullName());
						subgoal(new GoalGoto(GoalGoto.class.getSimpleName(),
								xyHome,
								Constants.EvacActivity.Home,
								Double.valueOf(agent.getBelief(ArchetypeAgent.Beliefname.LagTimeInMinsForInitialResponse.name()))));
						// subgoal should be last call in any plan step
					} else {
						agent.out("will stay and defend now #" + getFullName());
						this.drop(); // all done, drop the remaining plan steps
					}
				} catch (AgentNotFoundException e) {
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
				// Check if we have arrived
				if (ActionContent.State.PASSED.equals(agent.getLastBdiActionState())) {
					agent.out("will stay and defend now #" + getFullName());
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
