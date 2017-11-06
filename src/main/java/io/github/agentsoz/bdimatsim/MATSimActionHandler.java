package io.github.agentsoz.bdimatsim;

import java.util.LinkedHashMap;

import io.github.agentsoz.bdimatsim.app.BDIActionHandler;

/**
 * @author Edmund Kemsley Processes action/s from MatsimActionList by updating
 *         MatsimAgents and Matsim system
 */

public final class MATSimActionHandler {
	private final MATSimModel matSimModel;
	
	private final LinkedHashMap<String, BDIActionHandler> registeredActions;

	/**
	 * Constructor
	 * 
	 * @param matSimModel
	 */
	protected MATSimActionHandler(MATSimModel matSimModel) {
		this.matSimModel = matSimModel;
		
		this.registeredActions = new LinkedHashMap<>();
		
		// Register all the actions that we handle by default
		// The application can later add custom actions in a similar way
		// and indeed overwrite the default action handlers if needed
		this.registerBDIAction(MATSimActionList.DRIVETO, new DRIVETODefaultActionHandler());
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
