package io.github.agentsoz.nonmatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.agentsoz.bdimatsim.MATSimActionList;

/**
 * @author Edmund Kemsley Processes action/s from MatsimActionList by updating
 *         MatsimAgents and Matsim system
 */

public final class ActionHandler {
	
	private final Map<String, BDIActionHandler> registeredActions
			= Collections.synchronizedMap(new LinkedHashMap<>() );

	/** 
	 * Registers a new BDI action. Typical usage example:<pre><code>
	 * registerBDIAction("MyNewAction", new BDIActionHandler() {
	 *    {@literal @}Override
	 *    public boolean handle(String agentID, String actionID, Object[] args ) {
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
		if ( registeredActions.isEmpty() ) {
			throw new RuntimeException("no BDI action registered at all; this is probably not what you want; aborting ... " ) ;
		}
		for (String action : registeredActions.keySet()) {
			if (actionID.equals(action)) {
				return registeredActions.get(actionID).handle(agentID, actionID, parameters);
			}
		}
		return false;
	}
}
