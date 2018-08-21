package io.github.agentsoz.ees.agents.bushfire;

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Global;

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

public class PlanGoHomeThenLeave extends Plan {

	BushfireAgent agent = null;
	public boolean goingHomeBeforeLeaving = false;

	public PlanGoHomeThenLeave(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (BushfireAgent)agent;
		body = steps;
	}

	public boolean context() {
		boolean applicable = false;
		if (agent.isFinalResponseThresholdBreached() && agent.getDependentInfo() == null) {
			applicable = true;
		}
		agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
				+ ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + applicable);
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				if (Global.getRandom().nextDouble() < agent.getProbHomeBeforeLeaving()) {
					goingHomeBeforeLeaving =true;
					post(new GoalGoHome("GoalGoHome"));
					// Now wait till the next step for this goal to finish
				}
			},
			() -> {
				post(new GoalGotoEvacPlace("GoalGotoEvacPlace"));
				// Now wait till the next step for this goal to finish
			},
			() -> {
				// Arrived at evac location
				agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(), BushfireAgent.MemoryEventValue.SAFE.name());
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
