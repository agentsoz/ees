package io.github.agentsoz.ees.agents.archetype;

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
import io.github.agentsoz.ees.agents.archetype.ArchetypeAgent.Beliefname;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Location;

import java.util.Map;


public class PlanGoto extends Plan {

	private static final int maximumTries = 3;

	ArchetypeAgent agent = null;
	Constants.EvacActivity destinationType = null;
	private Location destination = null;

	private int tries;

	public PlanGoto(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (ArchetypeAgent)agent;
		destination = ((GoalGoto)getGoal()).getDestination();
		destinationType = ((GoalGoto)getGoal()).getDestinationType();
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
				double distToDest = agent.getDrivingDistanceTo(destination);
				// All done if already at the destination,
				// or tried enough times,
				// or were driving but the last bdi action (presumably the drive action) was dropped
				if (distToDest <= 0.0 ||
						tries >= maximumTries ||
						(!ActionContent.State.DROPPED.equals(agent.getLastBdiActionState())
								&& agent.hasBelief(Beliefname.isDriving.name(), new Boolean(true).toString()))) {
					agent.out("finished driving to "
							+ destination + String.format(" %.0f", distToDest) + "m away"
							+ " after " + tries + " tries"
							+ " #" + getFullName());
					agent.believe(Beliefname.isDriving.name(), new Boolean(false).toString());
					drop();
					return;
				}
				// Not there yet, so start driving
				agent.out("will start driving to "
						+ destination + String.format(" %.0f", distToDest) + "m away"
						+ " #" + getFullName());
				double replanningActivityDurationInMins = (tries==0) ? ((GoalGoto)getGoal()).getReplanningActivityDurationInMins() : 1;
				Goal action = agent.prepareDrivingGoal(
						destinationType,
						destination,
						Constants.EvacRoutingMode.carFreespeed,
						(int)Math.round(replanningActivityDurationInMins));
				tries++;
				agent.believe(Beliefname.isDriving.name(), new Boolean(action != null).toString());
				subgoal(action); // should be last call in any plan step
			},
			() -> {
				// Must suspend agent in plan step subsequent to starting bdi action
				agent.suspend(true); // should be last call in any plan step
			},
			() -> {
				// reset the plan to the start (loop)
				reset();
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
