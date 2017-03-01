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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

/**
 * @author Edmund Kemsley Processes action/s from MatsimActionList by updating
 *         MatsimAgents and Matsim system
 */

public final class MATSimActionHandler {
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

				// Now register a event handler for when the agent arrives at the destination
				MATSimAgent agent = model.getBDIAgent(agentID);
				agent.getPerceptHandler().registerBDIPerceptHandler(
						agent.getAgentID(), 
						MonitoredEventType.ArrivedAtDestination, 
						newLinkId,
						new BDIPerceptHandler() {
							@Override
							public void handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
								MATSimAgent agent = model.getBDIAgent(agentId);
								Object[] params = { linkId.toString() };
								agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
								agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
								agent.getPerceptContainer().put(MatsimPerceptList.ARRIVED, params);
							}
						});
				return true;
			}
		});
	}

	/** 
	 * Registers a new BDI action. Typical usage example:<pre><code>
	 * registerBDIAction("MyNewAction", new BDIActionHandler() {
	 *    {@literal @}Override
	 *    public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
	 *       MATSimAgent agent = model.getBDIAgent(agentID);
	 *       // Code to handle MyNewAction goes here ...
	 *       return true;
	 *    }
	 * });
	 * </pre></code>
	 * @param actionID
	 * @param actionHandler
	 */
	public final void registerBDIAction(String actionID, BDIActionHandler actionHandler) {
		registeredActions.put(actionID, actionHandler);
	}

	/**
	 * Process incoming BDI actions by for this MATSim agent, by calling its
	 * registered {@link BDIActionHandler}.
	 * 
	 * @param agentID
	 *            ID of the agent
	 * @param actionID
	 *            ID of the action, defined in {@link MATSimActionList}
	 * @param parameters
	 *            Parameters associated with the action
	 * @return
	 */
	final boolean processAction(String agentID, String actionID, Object[] parameters) {
		for (String action : registeredActions.keySet()) {
			if (actionID.equals(action)) {
				return registeredActions.get(actionID).handle(agentID, actionID, parameters, matSimModel);
			}
		}
		return false;
	}
}
