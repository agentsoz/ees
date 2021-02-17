package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2021 by its authors. See AUTHORS file.
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
		boolean applicable = true;
		agent.out("thinks " + getFullName() + " is " + (applicable ? "" : "not ") + "applicable");
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
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
