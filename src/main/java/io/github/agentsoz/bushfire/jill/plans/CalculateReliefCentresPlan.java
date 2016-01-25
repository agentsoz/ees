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

import io.github.agentsoz.bushfire.bdi.BDIConstants;
import io.github.agentsoz.bushfire.datamodels.FireInfo;
import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.bushfire.jill.goals.CalculateReliefCentresGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Sewwandi Perera
 *
 */
public class CalculateReliefCentresPlan extends Plan {
	final Logger logger = LoggerFactory.getLogger("");

	public CalculateReliefCentresPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
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
			EvacController evacController = (EvacController) getAgent();
			Region region = evacController.getRegions().get(
					((CalculateReliefCentresGoal) getGoal()).getRegionName());

			FireInfo fire = evacController.getFire().get(
					BDIConstants.FIRE_BELIEF_KEY);
			double safeDistance = evacController.getBdiConnector()
					.getSafeDistanceInMeters();
			Rectangle2D boundingBox = fire.getFirePolygon().getBounds2D();
			Rectangle2D safeBoundary = new Rectangle2D.Double(
					boundingBox.getX() - safeDistance, boundingBox.getY()
							- safeDistance, boundingBox.getWidth() + 2
							* safeDistance, boundingBox.getHeight() + 2
							* safeDistance);

			for (String reliefCentreName : evacController.getReliefCentres()
					.keySet()) {

				if (reliefCentreName.startsWith(evacController
						.getBdiConnector().getNewShelterNamePrefix())) {
					// This is used to skip the newly added shelters during
					// previous iterations
					continue;
				}

				Line2D path = evacController.getReliefCentreVectors()
						.get(region.getName() + reliefCentreName).getPath();

				// Check if the path intersects the fire polygon
				if (path.intersects(boundingBox)) {
					logger.debug("Path to " + reliefCentreName + " from "
							+ region.getName() + " intersects fire");
				} else {
					if (path.intersects(safeBoundary)) {
						logger.debug("Path to " + reliefCentreName + " from "
								+ region.getName()
								+ " is not within safe boundary");
					} else {
						logger.debug("Path to " + reliefCentreName + " from "
								+ region.getName() + " is within safe boundary");
						region.addViableReliefCentre(reliefCentreName);
					}
				}
			}

			// Update region
			region.setTimeViable(evacController.getCurrentTime());
			evacController.addRegion(region);
		}
	} };
}
