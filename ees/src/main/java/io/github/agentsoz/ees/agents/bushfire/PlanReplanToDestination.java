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

import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.Map;


public class PlanReplanToDestination extends Plan {

	BushfireAgent agent = null;
	boolean isReplanning = false;

	public PlanReplanToDestination(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		body = steps;
		this.agent = (BushfireAgent)agent;

	}

	public boolean context() {
		((BushfireAgent)getAgent()).memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
				+ ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);
		return true;
	}

	PlanStep[] steps = {
			() -> {
				((BushfireAgent)getAgent()).memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.DONE_FOR_NOW.name());
				agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), getGoal() + "|" + this.getClass().getSimpleName());
				isReplanning = agent.replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
			},
			() -> {
				if (isReplanning) {
					// Step subsequent to post must suspend agent when waiting for external stimuli
					// Will be reset by updateAction()
					agent.suspend(true);
					// Do not add any checks here since the above call is non-blocking
					// Suspend will happen once this step is finished
				}
			},
			() -> {
				agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(),
						BushfireAgent.MemoryEventValue.STATE_CHANGED.name()
								+ ": replanned route to destination");
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
