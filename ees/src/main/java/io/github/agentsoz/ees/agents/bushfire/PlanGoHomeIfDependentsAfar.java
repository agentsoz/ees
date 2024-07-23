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

import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Location;


import java.util.Map;


public class PlanGoHomeIfDependentsAfar extends Plan {

	BushfireAgent agent = null;
	boolean goingHomeAfterVisitingDependents;

	public PlanGoHomeIfDependentsAfar(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (BushfireAgent)agent;
		body = steps;
	}

	/**
     * Applies if distance to dependents is less than distance to home
	 * @return true if distance to dependents is less than distance to home
     */
	public boolean context() {
		boolean applicable = false;
		if (agent.isInitialResponseThresholdBreached() && agent.getDependentInfo() != null) {
			Location homeLocation = agent.getLocations().get(agent.LOCATION_HOME);
			Location dependentsLocation = agent.getDependentInfo().getLocation();
			if (!(homeLocation == null || dependentsLocation == null)) {
				try {
					Location from = ((Location[]) agent.getQueryPerceptInterface().queryPercept(
							String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null))[0];
					// Using beeline distance which is more natural and not computationally expensive
					double distanceToHome = Location.distanceBetween(from, homeLocation);
					double distanceToDependents = Location.distanceBetween(from, dependentsLocation);
					applicable = (distanceToDependents >= distanceToHome);
				} catch (AgentNotFoundException e) {
					agent.log(e.getMessage());
				}
			}
		}
		agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
				+ ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + applicable);
		return applicable;
	}

	PlanStep[] steps = {
			// 1. Go home (will visit dependents later)
			() -> {
				agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), getGoal() + "|" + this.getClass().getSimpleName());
				subgoal(new GoalGoHome("GoalGoHome"));
				// Now wait till the next step for this goal to finish
			},
			// 2. Arrived home, so go visit dependents now
			() -> {
				subgoal(new GoalGotoDependents("GoalGotoDependents"));
				// Now wait till the next step for this goal to finish
			},
			// 3. Arrived at Dependents. Go home with some probability
			() -> {
				if (!agent.isFinalResponseThresholdBreached() && agent.getWillGoHomeAfterVisitingDependents()) {
					goingHomeAfterVisitingDependents =true;
					subgoal(new GoalGoHome("GoalGoHome"));
					// Now wait till the next step for this goal to finish
				} else {
					agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.DONE_FOR_NOW.name());
				}
			},
			() -> {
				if (goingHomeAfterVisitingDependents) {
					// Arrived home
				}
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
