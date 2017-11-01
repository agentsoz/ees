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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.bushfire.jill.goals.CalculateFireVectorGoal;
import io.github.agentsoz.bushfire.jill.goals.CalculateReliefCentresGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanInfo;
import io.github.agentsoz.jill.lang.PlanStep;

/**
 * 
 * @author Sewwandi Perera
 *
 */
@PlanInfo(postsGoals = {
		"agentsoz.bushfire.jill.goals.CalculateFireVectorGoal",
		"agentsoz.bushfire.jill.goals.CalculateReliefCentresGoal" })
public class CalcViableRelifCentresPlan extends Plan {
	Logger logger = LoggerFactory.getLogger("");

	public CalcViableRelifCentresPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		body = betPlanSteps();
	}

	@Override
	public boolean context() {
		return true;
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	private PlanStep[] betPlanSteps() {
		List<PlanStep> steps = new ArrayList<PlanStep>();

		// Add a plan step to post CalculateFireVectorGoal
		steps.add(new PlanStep() {
			@Override
			public void step() {
				post(new CalculateFireVectorGoal("Calculate Fire Vector"));
			}
		});

		EvacController evacController = (EvacController) getAgent();

		for (String regionName : evacController.getRegions().keySet()) {
			Region region = evacController.getRegions().get(regionName);

			logger.debug("Region " + regionName + "'s viable relief centres:"
					+ region.getViableReliefCentres());

			logger.debug("Viable relief centres info was updated at:"
					+ region.getTimeViable() + ". Current time:"
					+ evacController.getCurrentTime());

			if (null == region.getViableReliefCentres()
					|| region.getViableReliefCentres().isEmpty()
					|| evacController.getCurrentTime() - region.getTimeViable() > 1800) {

				logger.debug("Started calculating vible relief centres for the region "
						+ regionName);
				post(new CalculateReliefCentresGoal("Calculate Relief Centres",
						regionName));

			}
		}

		return (PlanStep[]) steps.toArray();
	}
}
