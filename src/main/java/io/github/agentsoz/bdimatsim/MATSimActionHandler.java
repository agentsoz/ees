package io.github.agentsoz.bdimatsim;

import java.util.HashMap;

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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import io.github.agentsoz.bdimatsim.app.BDIActionHandler;

/**
 * @author Edmund Kemsley Processes action/s from MatsimActionList by updating
 *         MatsimAgents and Matsim system
 */

final class MATSimActionHandler {
	private final MATSimModel matSimModel;
	
	private final HashMap<String, BDIActionHandler> registeredActions;

	/**
	 * Constructor
	 * 
	 * @param matSimModel
	 */
	protected MATSimActionHandler(MATSimModel matSimModel) {
		this.matSimModel = matSimModel;
		
		this.registeredActions = new HashMap<String, BDIActionHandler>();
		
		// Register all the actions that we handle by default
		// The application can later add custom actions in a similar way
		// and indeed overwrite the default action handlers if needed
		this.registerBDIAction(MATSimActionList.DRIVETO, new BDIActionHandler() {
			@Override
			public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
				// Get nearest link ID and calls the Replanner to map to MATSim.
				Id<Link> newLinkId;
				if (args[1] instanceof double[]) {
					double[] coords = (double[]) args[1];
					newLinkId = ((NetworkImpl) model.getScenario()
							.getNetwork()).getNearestLinkExactly(
							new CoordImpl(coords[0], coords[1])).getId();
				} else {
					throw new RuntimeException("Destination coordinates are not given");
				}

				model.getReplanner().attachNewActivityAtEndOfPlan(newLinkId,
						Id.createPersonId(agentID));

				// Saving actionValue (which for DRIVETO actions is the link id)
				// We use this again in the AgentActivityEventHandler, to check
				// if the agent has arrived at this link, at which point we can
				// mark this BDI-action as PASSED
				MATSimAgent agent = model.getBDIAgent(agentID);
				agent.newDriveTo(newLinkId);
				return true;
			}
		});
	}

	/** 
	 * Registers a new BDI action
	 * @param actionID
	 * @param actionHandler
	 */
	public final void registerBDIAction(String actionID, BDIActionHandler actionHandler) {
		registeredActions.put(actionID, actionHandler);
	}

	/**
	 * Process BDI actions
	 * 
	 * @param agentID
	 *            ID of the agent
	 * @param actionID
	 *            ID of the action, defined in {@link MATSimActionList}
	 * @param parameters
	 *            Parameters associated with the action
	 * @return
	 */
	public final boolean processAction(String agentID, String actionID, Object[] parameters) {
		String actionType = (String) parameters[0];

		if (!actionType.equals(actionID)) {
			throw new RuntimeException(
					"actionType and actionID are not identical; not clear what that means; aborting");
		}

		for (String action : registeredActions.keySet()) {
			if (actionID.equals(action)) {
				return registeredActions.get(actionID).handle(agentID, actionID, parameters, matSimModel);
			}
		}
		return false;
	}
}
