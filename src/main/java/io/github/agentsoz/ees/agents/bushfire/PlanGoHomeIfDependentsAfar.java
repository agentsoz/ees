package io.github.agentsoz.ees.agents.bushfire;

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
				Location from = ((Location[])agent.getQueryPerceptInterface().queryPercept(
						String.valueOf(agent.getId()), Constants.REQUEST_LOCATION, null))[0];
				// Using beeline distance which is more natural and not computationally expensive
				double distanceToHome = Location.distanceBetween(from,homeLocation);
				double distanceToDependents = Location.distanceBetween(from,dependentsLocation);
				applicable = (distanceToDependents >= distanceToHome);
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
