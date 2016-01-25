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

import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import io.github.agentsoz.bushfire.bdi.BDIConstants;
import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.datamodels.ReliefCentre;
import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.bushfire.jill.goals.DecideReliefCentreGoal;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

/**
 * 
 * @author Sewwandi Perera
 *
 */
public class NoReliefCentresPlan extends Plan {
	final Logger logger = LoggerFactory.getLogger("");
	private EvacController evacController;
	private Region region;
	private Coordinate fireCentre;
	private Double fireDirection;

	public NoReliefCentresPlan(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		evacController = (EvacController) getAgent();
		region = evacController.getRegions().get(
				((DecideReliefCentreGoal) getGoal()).getRegionName());
		body = steps;
	}

	@Override
	public boolean context() {
		return region.getViableReliefCentres() == null
				|| region.getViableReliefCentres().isEmpty();
	}

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {

	}

	PlanStep[] steps = new PlanStep[] { new PlanStep() {

		@Override
		public void step() {
			// Remove already assigned relief centre
			removeAssignedReliefCentre();

			// Read fire centre coordinates and fire direction
			getFireInfo();

			// Get the viable relief centre with highest remaining capacity
			ReliefCentre pickedShelter = pickShelter();

			// allocate the relief centre with highest remaining capacity to the
			// region
			pickedShelter.allocateCapacity(region.getPopulation());
			evacController.addReliefCentre(pickedShelter);
			evacController.addReliefCentreAssignment(region.getName(),
					pickedShelter.getName());

		}

	} };

	private void removeAssignedReliefCentre() {
		String assignedRC = evacController.getReliefCentreAssignments().get(
				region.getName());
		if (assignedRC != null) {
			ReliefCentre rc = evacController.getReliefCentres().get(assignedRC);
			rc.deAllocateCapacity(region.getPopulation());
			evacController.removeEntryFromRCAssignments(region.getName());
			evacController.addReliefCentre(rc);
		}
	}

	protected ReliefCentre pickShelter() {
		int distance = evacController.getBdiConnector()
				.getShelterDistanceInMeters();
		double coordX = fireCentre.x + distance * Math.cos(90 + fireDirection);
		double coordY = fireCentre.y + distance * Math.sin(90 + fireDirection);
		String name = evacController.getBdiConnector().getNewShelterName();

		ReliefCentre rc = new ReliefCentre(name, name, new Coordinate(
				(float) coordX, (float) coordY), region.getPopulation());

		return rc;
	}

	protected void getFireInfo() {
		Rectangle2D fireBox = evacController.getFire()
				.get(BDIConstants.FIRE_BELIEF_KEY).getFirePolygon()
				.getBounds2D();
		fireDirection = evacController.getFire()
				.get(BDIConstants.FIRE_BELIEF_KEY).getDirection();
		fireCentre = new Coordinate((float) fireBox.getCenterX(),
				(float) fireBox.getCenterY());
	}

}
