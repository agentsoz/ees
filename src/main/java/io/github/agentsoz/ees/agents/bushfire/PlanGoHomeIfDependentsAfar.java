package io.github.agentsoz.ees.agents.bushfire;

import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Global;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;

import java.util.Arrays;
import java.util.Map;

/*
 * #%L
 * Jill Cognitive Agents Platform
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
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

public class PlanGoHomeIfDependentsAfar extends Plan {

	BushfireAgent agent = null;
	boolean goingHomeAfterVisitingDependents;

	public PlanGoHomeIfDependentsAfar(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (BushfireAgent)agent;
		body = steps;
	}

	/**
     * Returns true if distance to dependents is less than distance to home
	 * @return
     */
	public boolean context() {
		boolean applicable = false;
		if (agent.getDependentInfo() != null) {
			Location homeLocation = agent.getLocations().get(agent.HOME_LOCATION);
			Location dependentsLocation = agent.getDependentInfo().getLocation();
			if (!(homeLocation == null || dependentsLocation == null)) {
				double distanceToHome = (double) agent.getQueryPerceptInterface().queryPercept(
						String.valueOf(agent.getId()),
						PerceptList.REQUEST_DRIVING_DISTANCE_TO,
						homeLocation.getCoordinates());
				double distanceToDependents = (double) agent.getQueryPerceptInterface().queryPercept(
						String.valueOf(agent.getId()),
						PerceptList.REQUEST_DRIVING_DISTANCE_TO,
						dependentsLocation.getCoordinates());
				applicable = (distanceToDependents >= distanceToHome);
			}
		}
		agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
				+ ":" + this.getClass().getSimpleName() + "=" + applicable);
		return applicable;
	}

	PlanStep[] steps = {
			// 1. Go home (will visit dependents later)
			new PlanStep() {
				public void step() {
					agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.GO_HOME_NOW.name());
					Object[] params = new Object[4];
					params[0] = ActionList.DRIVETO;
					params[1] = agent.getLocations().get(agent.HOME_LOCATION).getCoordinates();
					params[2] = agent.getTime() + 5.0; // five secs from now;
					params[3] = MATSimModel.EvacRoutingMode.carFreespeed;
					agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), ActionList.DRIVETO+"="+ agent.getLocations().get(agent.HOME_LOCATION));
					post(new EnvironmentAction(Integer.toString(agent.getId()), ActionList.DRIVETO, params));
				}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					// Must suspend the agent when waiting for external stimuli
					agent.suspend(true);
					// All done, when we return from the above call
				}
			},
			// 2. Arrived home, so go visits dependents now
			new PlanStep() {
				public void step() {
					agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(), BushfireAgent.MemoryEventValue.ARRIVED_HOME.name());
					agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.GO_VISIT_DEPENDENTS_NOW.name());
					Object[] params = new Object[4];
					params[0] = ActionList.DRIVETO;
					params[1] = agent.getDependentInfo().getLocation().getCoordinates();
					params[2] = agent.getTime() + 5.0; // five secs from now;
					params[3] = MATSimModel.EvacRoutingMode.carFreespeed;
					agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), ActionList.DRIVETO+"="+agent.getDependentInfo().getLocation());
					post(new EnvironmentAction(Integer.toString(agent.getId()), ActionList.DRIVETO, params));
				}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					// Must suspend the agent when waiting for external stimuli
					agent.suspend(true);
					// All done, when we return from the above call
				}
			},
			// 3. Arrived at Dependents. Go home with some probability
			new PlanStep() {
				public void step() {
					agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(), BushfireAgent.MemoryEventValue.ARRIVED_AT_DEPENDENTS.name());
					if (Global.getRandom().nextDouble() < agent.getProbHomeAfterDependents()) {
						agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.GO_HOME_NOW.name());
						goingHomeAfterVisitingDependents =true;
						Object[] params = new Object[4];
						params[0] = ActionList.DRIVETO;
						params[1] = agent.getLocations().get(agent.HOME_LOCATION).getCoordinates();
						params[2] = agent.getTime() + 5.0; // five secs from now;
						params[3] = MATSimModel.EvacRoutingMode.carFreespeed;
						agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), ActionList.DRIVETO+"="+agent.getLocations().get(agent.HOME_LOCATION));
						post(new EnvironmentAction(Integer.toString(agent.getId()), ActionList.DRIVETO, params));
					} else {
						agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.DONE_FOR_NOW.name());
					}
				}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					if (goingHomeAfterVisitingDependents) {
						// Must suspend the agent when waiting for external stimuli
						agent.suspend(true);
						// All done, when we return from the above call
					}
				}
			},
			new PlanStep() {
				public void step() {
					if (goingHomeAfterVisitingDependents) {
						agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(), BushfireAgent.MemoryEventValue.ARRIVED_HOME.name());
					}
				}
			},
			// NOTE:
			// Since Jill does not support multiple intentions yet, any repeated postings of the
			// RespondToFireAlert goal (if the agent gets a blocked percept and retries) will end up being treated as
			// sub-goals of this plan, i.e., recursive sub-goals.
			// Therefore any plan steps below this point will be executed AFTER returning from the recursive call,
			// and is probably not what we want/expect from the execution engine. For now, avoid adding anything
			// beyond this point.
			// dsingh, 5/dec/17
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
