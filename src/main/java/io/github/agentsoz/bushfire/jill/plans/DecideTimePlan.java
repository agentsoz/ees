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

import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.bushfire.jill.goals.DecideTimeGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.HashMap;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Sewwandi Perera
 *
 */
public class DecideTimePlan extends Plan {
	final Logger logger = LoggerFactory.getLogger("");
	private EvacController evacController;
	private Region region;

	/**
	 * Evacuation delay in minutes
	 */
	private int[] evacDelays = new int[] { 0, 30, 60, 90 };

	public DecideTimePlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		evacController = (EvacController) getAgent();
		region = evacController.getRegions().get(
				((DecideTimeGoal) getGoal()).getRegionName());
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
			Double evacDelayInSeconds = evacController.getEvacTimes().get(
					region.getName());

			if (evacDelayInSeconds == null) {
				// this means, no time record can be found
				Random random = new Random();
				int randomIndex = random.nextInt(evacDelays.length);
				evacDelayInSeconds = evacController.getCurrentTime()
						+ evacDelays[randomIndex] * 60;
				evacController
						.addEvacTime(region.getName(), evacDelayInSeconds);
			}

			evacController.startNewSchedule(region.getName(),
					evacDelayInSeconds);
		}
	} };

}
