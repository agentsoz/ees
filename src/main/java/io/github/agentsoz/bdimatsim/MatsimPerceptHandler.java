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
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;

import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

/**
 * @author Edmund Kemsley, Dhirendra Singh
 */

public final class MatsimPerceptHandler {
	private final MATSimModel matSimModel;

	private final HashMap<String, BDIPerceptHandler> registeredPercepts;

	/**
	 * Constructor
	 * 
	 * @param matSimModel
	 */
	protected MatsimPerceptHandler(MATSimModel matSimModel) {
		this.matSimModel = matSimModel;
		this.registeredPercepts = new HashMap<String, BDIPerceptHandler>();
		
		// Process ARRIVED; returns an array of passed DRIVETO actions
		this.registerBDIPercepts(MatsimPerceptList.ARRIVED, new BDIPerceptHandler() {
			@Override
			public Object[] process(String agentID, String perceptID, MATSimModel model) {
				// Returns all passed actions held in the MatsimAgent object as
				// an array Designed to handle multiple arrivals in one percept
				MATSimAgent agent = model.getBDIAgent(Id.createPersonId(agentID));
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
		});
		
		// Process REQUESTLOCATION; returns the coordinates of the agent
		registerBDIPercepts(MatsimPerceptList.REQUESTLOCATION, new BDIPerceptHandler() {
			@Override
			public Object[] process(String agentID, String perceptID, MATSimModel model) {
				// At times BDI will need the current position of agent 
				// Returns the current link coordinates
				MobsimAgent agent = model.getMobsimAgentMap().get(
						Id.createPersonId(agentID));
				Link currentLink = model.getScenario().getNetwork().getLinks()
						.get(agent.getCurrentLinkId());

				return new Object[] { currentLink.getCoord().getX(),
						currentLink.getCoord().getY() };
			}
		});
	}

	/** 
	 * Registers a new BDI percept. Typical usage example:<pre><code>
	 * registerBDIPercepts("MyNewPercept", new BDIPerceptHandler() {
	 *    {@literal @}Override
	 *    public Object[] process(String agentID, String perceptID, MATSimModel model) {
	 *       MATSimAgent agent = model.getBDIAgent(agentID);
	 *       // Code to process MyNewPercept goes here ...
	 *       return true;
	 *    }
	 * });
	 * </pre></code>
	 * @param perceptID
	 * @param perceptHandler
	 */
	public final void registerBDIPercepts(String perceptID, BDIPerceptHandler perceptHandler) {
		registeredPercepts.put(perceptID, perceptHandler);
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
		for (String percept : registeredPercepts.keySet()) {
			if (perceptID.equals(percept)) {
				return registeredPercepts.get(perceptID).process(agentID, perceptID, matSimModel);
			}
		}
		return null;
	}
	
}
