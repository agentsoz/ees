package io.github.agentsoz.ees.agents.bushfire;

import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.evac.ActionList;

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

public class PlanGotoInvacPlace extends Plan {

	BushfireAgent agent = null;

	public PlanGotoInvacPlace(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (BushfireAgent)agent;
		body = steps;
	}

	public boolean context() {
		boolean applicable = true;
		agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
				+ ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);
		return applicable;
	}

	PlanStep[] steps = {
			new PlanStep() {
				public void step() {
					agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(),
							BushfireAgent.MemoryEventValue.LEAVE_NOW.name());
					Object[] params = new Object[4];
					params[0] = ActionList.DRIVETO;
					params[1] = agent.getLocations().get(agent.LOCATION_INVAC_PREFERRED).getCoordinates();
					params[2] = agent.getTime() + 5.0; // five secs from now;
					params[3] = MATSimModel.EvacRoutingMode.carFreespeed;
					agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), ActionList.DRIVETO+"="+agent.getLocations().get(agent.LOCATION_INVAC_PREFERRED));
					EnvironmentAction action = new EnvironmentAction(Integer.toString(agent.getId()), ActionList.DRIVETO, params);
					agent.setActiveEnvironmentAction(action);
					post(action);
				}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					// Must suspend the agent when waiting for external stimuli
					agent.suspend(true);
					// All done, when we return from the above call
					agent.setActiveEnvironmentAction(null);
					agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(), BushfireAgent.MemoryEventValue.ARRIVED_LOCATION_INVAC.name());
				}
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
