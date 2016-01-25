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
import io.github.agentsoz.bushfire.datamodels.RouteAssignment;
import io.github.agentsoz.bushfire.jill.agents.BasicResident;
import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.bushfire.jill.goals.CheckplanGoal;
import io.github.agentsoz.bushfire.jill.goals.EvacMessage;
import io.github.agentsoz.bushfire.jill.goals.EvacTimeGoal;
import io.github.agentsoz.bushfire.jill.goals.ReplanRegionGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanInfo;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Sewwandi Perera
 *
 */
@PlanInfo(postsGoals = { "agentsoz.bushfire.jill.goals.CheckplanGoal",
		"agentsoz.bushfire.jill.goals.ReplaRegionGoal" })
public class EvacTimePlan extends Plan {
	final Logger logger = LoggerFactory.getLogger("");
	private EvacController evacController;
	private Region region;
	private double executionTime;

	public EvacTimePlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		evacController = (EvacController) getAgent();
		region = evacController.getRegions().get(
				((EvacTimeGoal) getGoal()).getRegionName());
		executionTime = ((EvacTimeGoal) getGoal()).getExecutionTime();
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
			post(new CheckplanGoal("Check Plan", region.getName()));
		}
	}, new PlanStep() {

		@Override
		public void step() {
			if (evacController.getPlanChecks().get(region.getName())
					.isCheckOK()) {
				// read the scheduled route
				RouteAssignment ra = evacController.getRouteAssignments().get(
						region.getName());

				// publish region schedule
				evacController.getBdiConnector().publishRegionSchedule(
						region.getName(), ra.getRouteName(),
						ra.getReliefCentreName(), executionTime);

				io.github.agentsoz.bdiabm.Agent[] residents = (io.github.agentsoz.bdiabm.Agent[]) evacController
						.getBdiConnector().getAllResidents();

				for (int i = 0; i < residents.length; i++) {
					BasicResident resident = (BasicResident) residents[i];

					if (resident.homeRegion != null
							&& region.getName().equals(resident.homeRegion)) {
						logger.debug(
								"Sending evac message to the agent {} to waypoints {}",
								resident.getId(), ra.getWaypointNames()
										.toString());
						evacController.send(
								resident.getId(),
								new EvacMessage("Evac Message", ra
										.getRouteCoords(), ra
										.getWaypointNames()));
					}
				}
			} else {
				// post re-plan event
				post(new ReplanRegionGoal("Replan", region.getName(),
						executionTime));
			}
		}
	} };

}
