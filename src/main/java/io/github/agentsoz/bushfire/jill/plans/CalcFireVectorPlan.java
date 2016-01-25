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

import java.awt.Polygon;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bushfire.bdi.BDIConstants;
import io.github.agentsoz.bushfire.bdi.IBdiConnector;
import io.github.agentsoz.bushfire.datamodels.FireInfo;
import io.github.agentsoz.bushfire.jill.agents.EvacController;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

/**
 * 
 * @author Sewwandi Perera
 *
 */
public class CalcFireVectorPlan extends Plan {
	final Logger logger = LoggerFactory.getLogger("");

	public CalcFireVectorPlan(Agent agent, Goal goal, String name) {
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
			HashMap<String, FireInfo> fire = evacController.getFire();
			double currentTimestamp = evacController.getCurrentTime();
			double fireTimestamp = fire.get(BDIConstants.FIRE_BELIEF_KEY)
					.getTimestamp();

			logger.debug("Current time: " + currentTimestamp
					+ " | Fire data update at: " + fireTimestamp);

			if (currentTimestamp != fireTimestamp) {
				IBdiConnector bdiConnector = evacController.getBdiConnector();
				Polygon firePolygon = bdiConnector.getFirePolygon();
				double fireDirection = bdiConnector.getFireDirectionFromNorth();

				fire.put(BDIConstants.FIRE_BELIEF_KEY, new FireInfo(
						firePolygon, fireDirection, currentTimestamp));
				evacController.setFire(fire);
			}
		}
	} };
}
