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
import io.github.agentsoz.bushfire.jill.goals.CalculateFireVectorGoal;
import io.github.agentsoz.bushfire.jill.goals.CalculateReliefCentresGoal;
import io.github.agentsoz.bushfire.jill.goals.DecideReliefCentreGoal;
import io.github.agentsoz.bushfire.jill.goals.DecideRouteGoal;
import io.github.agentsoz.bushfire.jill.goals.DecideTimeGoal;
import io.github.agentsoz.bushfire.jill.goals.ReplanRegionGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanInfo;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * 
 * @author Sewwandi Perera
 *
 */
@PlanInfo(postsGoals = { "agentsoz.bushfire.jill.goals.DecideReliefCentreGoal",
		"agentsoz.bushfire.jill.goals.DecideRouteGoal",
		"agentsoz.bushfire.jill.goals.CalculateFireVectorGoal",
		"agentsoz.bushfire.jill.goals.CalculateReliefCentresGoal",
		"agentsoz.bushfire.jill.goals.DecideTimeGoal" })
public class ReplanRegionPlan extends Plan {
	final Logger logger = LoggerFactory.getLogger("");
	private EvacController evacController;
	private String regionName;

	public ReplanRegionPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		evacController = (EvacController) getAgent();
		regionName = ((ReplanRegionGoal) getGoal()).getRegionName();
		body = steps;
	}

	@Override
	public boolean context() {
		return true;
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = new PlanStep[] { new PlanStep() {

		@Override
		public void step() {
			evacController.removeEvacTime(regionName);
			post(new CalculateFireVectorGoal("Calculate Fire Vector"));
		}
	}, new PlanStep() {

		@Override
		public void step() {
			post(new CalculateReliefCentresGoal("Calculate RS", regionName));
		}
	}, new PlanStep() {

		@Override
		public void step() {
			post(new DecideReliefCentreGoal("Decide RC", regionName));
		}
	}, new PlanStep() {

		@Override
		public void step() {
			post(new DecideRouteGoal("Decide Route", regionName));
		}
	}, new PlanStep() {

		@Override
		public void step() {
			post(new DecideTimeGoal("Decide Time", regionName));
		}
	} };

}
