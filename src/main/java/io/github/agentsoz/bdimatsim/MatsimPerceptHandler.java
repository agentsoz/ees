package io.github.agentsoz.bdimatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * @author Edmund Kemsley Processes percept/s from TaxiPerceptList by getting
 *         information from MATSim and then returning as an Object array
 */

final class MatsimPerceptHandler {
	private final MATSimModel matSimModel;

	/**
	 * Constructor
	 * 
	 * @param matSimModel
	 */
	MatsimPerceptHandler(MATSimModel matSimModel) {
		this.matSimModel = matSimModel;
	}

	/**
	 * Process percepts
	 * 
	 * @param agentIDg
	 *            ID of the agent
	 * @param perceptID
	 *            Percept IDs are defined in {@link MatsimPerceptList}. eg:
	 *            ARRIVED, REQUESTLOCATION.
	 * @return The response
	 */
	final Object[] processPercept(String agentID, String perceptID) {
		if (perceptID == MatsimPerceptList.ARRIVED) {
			return processArrived(this.matSimModel, agentID);
		}
		if (perceptID == MatsimPerceptList.REQUESTLOCATION) {
			return processRequestLocation(this.matSimModel, agentID);
		} else {
			return null;
		}
	}

	/**
	 * Process ARRIVED percept
	 * 
	 * @param matSimModel
	 *            {@link MATSimModel} instance
	 * @param agentID
	 *            ID of the agent
	 * @return An array of passed DRIVETO actions as the response
	 */
	public static Object[] processArrived(MATSimModel matSimModel,
			String agentID) {
		/*
		 * Returns all passed actions held in the MatsimAgent object as an array
		 * Designed to handle multiple arrivals in one percept
		 */
		MATSimAgent agent = matSimModel.getBDIAgent(Id.createPersonId(agentID));
		ArrayList<Id<Link>> passedActions = agent.getpassedDriveToActions();
		if (passedActions.isEmpty()) {
			return null;
		}
		String[] array = new String[passedActions.size()];
		Iterator<Id<Link>> it = passedActions.iterator();
		int i = 0;
		while (it.hasNext()) {
			Id<Link> action = it.next();
			array[i++] = action.toString();
		}
		agent.clearPassedDriveToActions();
		return array;
	}

	/**
	 * Process REQUESTLOCATION percept
	 * 
	 * @param matSimModel
	 *            {@link MATSimModel} instance
	 * @param agentID
	 *            ID of the agent
	 * @return Coordinates of the agent
	 */
	public static Object[] processRequestLocation(MATSimModel matSimModel,
			String agentID) {
		/*
		 * At times BDI will need the current position of agent Returns the
		 * current link coordinates
		 */
		MobsimAgent agent = matSimModel.getMobsimAgentMap().get(
				Id.createPersonId(agentID));
		Link currentLink = matSimModel.getScenario().getNetwork().getLinks()
				.get(agent.getCurrentLinkId());

		return new Object[] { currentLink.getCoord().getX(),
				currentLink.getCoord().getY() };
	}
}
