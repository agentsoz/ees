package io.github.agentsoz.ees.agents.bushfire;

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

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.Map;


public class PlanLeaveNow extends Plan {

	BushfireAgent agent = null;

	public PlanLeaveNow(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (BushfireAgent)agent;
		body = steps;
	}

	public boolean context() {
		boolean applicable = false;
		if (agent.isFinalResponseThresholdBreached() && agent.getDependentInfo() != null) {
			applicable = true;
		}
		agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
				+ ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), getGoal() + "|" + this.getClass().getSimpleName());
				subgoal(new GoalGotoEvacPlace("GoalGotoEvacPlace"));
				// Now wait till the next step for this goal to finish
			},
			() -> {
				// Arrived at the evac location
				agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(), BushfireAgent.MemoryEventValue.SAFE.name());
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
