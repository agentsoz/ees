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

import io.github.agentsoz.jill.lang.*;

import java.util.Map;

@PlanInfo(postsGoals = {
		"io.github.agentsoz.ees.agents.archetype.GoalInitialResponse",
		"io.github.agentsoz.ees.agents.archetype.GoalFinalResponse",
})
public class PlanFullResponse extends Plan {

	ArchetypeAgent agent = null;

	public PlanFullResponse(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (ArchetypeAgent)agent;
		body = steps;
	}

	public boolean context() {
		setName(this.getClass().getSimpleName()); // give this plan a user friendly name for logging purposes
		boolean isStuck = Boolean.valueOf(agent.getBelief(ArchetypeAgent.State.isStuck.name()));
		boolean applicable = !isStuck;
		agent.out("thinks " + getFullName() + " is " + (applicable ? "" : "not ") + "applicable");
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				agent.out("will do #" + getFullName());
				// Do initial response
				subgoal(new GoalInitialResponse(GoalInitialResponse.class.getSimpleName()));
				// subgoal should be last call in any plan step
			},
			() -> {
				// Then do final response
				subgoal(new GoalFinalResponse(GoalFinalResponse.class.getSimpleName()));
				// subgoal should be last call in any plan step
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
