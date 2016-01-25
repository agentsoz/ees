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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.datamodels.Route;
import io.github.agentsoz.bushfire.datamodels.RouteAssignment;
import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.bushfire.jill.goals.DecideRouteGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

/**
 * 
 * @author Sewwandi Perera
 *
 */
public class FastRoutePlan extends Plan {
	final Logger logger = LoggerFactory.getLogger("");
	private EvacController evacController;
	private Region region;
	private String reliefCentre;
	private Route fastRoute;

	public FastRoutePlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		evacController = (EvacController) getAgent();
		region = evacController.getRegions().get(
				((DecideRouteGoal) getGoal()).getRegionName());
		body = steps;
	}

	@Override
	public boolean context() {
		reliefCentre = evacController.getReliefCentreAssignments().get(
				region.getName());

		for (String r : evacController.getRoutes().keySet()) {
			fastRoute = evacController.getRoutes().get(r);
			if (fastRoute.getRegion() == region.getName()
					&& fastRoute.getReliefCentre() == reliefCentre
					&& fastRoute.getDescription() == "FAST") {
				return true;
			}
		}

		return false;
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = new PlanStep[] { new PlanStep() {

		@Override
		public void step() {
			evacController.addRouteAssignment(new RouteAssignment(region
					.getName(), reliefCentre, fastRoute.getName(), fastRoute
					.getRouteCoords(), fastRoute.getRouteNames()));
		}
	} };

}
