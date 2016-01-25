package io.github.agentsoz.bushfire.jill.plans;

/*
 * #%L
 * BDI-ABM Integration Package
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

import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.bushfire.jill.goals.CalcViableReliefCentresGoal;
import io.github.agentsoz.bushfire.jill.goals.DecideReliefCentreGoal;
import io.github.agentsoz.bushfire.jill.goals.DecideRouteGoal;
import io.github.agentsoz.bushfire.jill.goals.DoScheduleGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanInfo;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Sewwandi Perera
 *
 */
@PlanInfo(postsGoals = { "agentsoz.bushfire.jill.goals.DoScheduleGoal",
		"agentsoz.bushfire.jill.goals.DecideReliefCentreGoal",
		"agentsoz.bushfire.jill.goals.DecideRouteGoal",
		"agentsoz.bushfire.jill.goals.CalcViableReliefCentresGoal" })
public class PlanAreasPlan extends Plan {

	private EvacController evacController;

	public PlanAreasPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		evacController = (EvacController) getAgent();
		body = getPlanSteps();
	}

	@Override
	public boolean context() {
		return true;
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	private PlanStep[] getPlanSteps() {
		List<PlanStep> steps = new ArrayList<PlanStep>();

		// Add the first step : posting a goal to calculate all viable relief
		// centres for all regions
		steps.add(new PlanStep() {
			@Override
			public void step() {
				post(new CalcViableReliefCentresGoal(
						"Calculate Viable ReliefCentres"));
			}
		});

		// Add plan steps to decide relief centres and routes for all regions
		for (final String regionName : evacController.getRegions().keySet()) {
			steps.add(new PlanStep() {
				@Override
				public void step() {
					post(new DecideReliefCentreGoal("Decide Relief Centres",
							regionName));
				}
			});

			steps.add(new PlanStep() {
				@Override
				public void step() {
					post(new DecideRouteGoal("Decide Route", regionName));
				}
			});
		}

		// Finally add a plan step to post DoScheduleGoal
		steps.add(new PlanStep() {
			@Override
			public void step() {
				post(new DoScheduleGoal("Do Schedule"));
			}
		});

		return (PlanStep[]) steps.toArray();

	}

}
