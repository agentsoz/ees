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

import io.github.agentsoz.bdiabm.data.ActionContainer;
import io.github.agentsoz.bdiabm.data.PerceptContainer;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author Edmund Kemsley Agent Object on the ABM side Holds information: an
 *         Agent's actionContainer and perceptContainer, the correct
 *         PerceptHandler and ActionHandler, and lists of driveToActions
 */

public class MATSimAgent {
	private MatsimPerceptHandler perceptHandler;
	private MATSimActionHandler actionHandler;
	private Id<Person> agentID;
	private String agentType;

	private ArrayList<Id<Link>> passedDriveToActions;
	private ArrayList<Id<Link>> driveToActions;

	private ActionContainer actionContainer;
	private PerceptContainer perceptContainer;

	public final Id<Person> getAgentID() {
		return agentID;
	}

	public final MatsimPerceptHandler getPerceptHandler() {
		return perceptHandler;
	}

	final MATSimActionHandler getActionHandler() {
		return actionHandler;
	}

	final ArrayList<Id<Link>> getpassedDriveToActions() {
		return passedDriveToActions;
	}

	public final ActionContainer getActionContainer() {
		return this.actionContainer;
	}

	public final PerceptContainer getPerceptContainer() {
		return this.perceptContainer;
	}

	final void setPerceptContainer(PerceptContainer perceptContainer) {
		this.perceptContainer = perceptContainer;
	}

	final void setPerceptHandler(MatsimPerceptHandler perceptHandler) {
		this.perceptHandler = perceptHandler;
	}

	final void setActionContainer(ActionContainer actionContainer) {
		this.actionContainer = actionContainer;

	}

	MATSimAgent(MATSimActionHandler actionHandler,
			MatsimPerceptHandler perceptHandler, Id<Person> agentID,
			ActionContainer actionContainer, PerceptContainer perceptContainer) {
		this.perceptHandler = perceptHandler;
		this.actionHandler = actionHandler;
		this.agentID = agentID;
		this.actionContainer = actionContainer;
		this.perceptContainer = perceptContainer;
		passedDriveToActions = new ArrayList<Id<Link>>();
		driveToActions = new ArrayList<Id<Link>>();
	}

	public final void newDriveTo(Id<Link> newAction) {
		driveToActions.add(newAction);
	}

	final void removeActions(Id<Link> action) {
		driveToActions.remove(action);
	}

	final void clearAllActions() {
		passedDriveToActions.clear();
		driveToActions.clear();
	}

	public final void clearPassedDriveToActions() {
		passedDriveToActions.clear();
	}

	final Object[] getPerceptValue(String perceptID) {
		return perceptHandler.processPercept(agentID.toString(), perceptID);
	}

	/*
	 * Checks the location and returns which driveToAction has been
	 * PASSED/ARRIVED
	 */
	final Id<Link> arrivedAtDriveTo(Id<Link> location) {
		Iterator<Id<Link>> actions = driveToActions.iterator();
		while (actions.hasNext()) {
			Id<Link> dest = actions.next();
			// Id value = new IdImpl
			// ((String)actionContainer.get(action.toString()).getParameters()[1]);
			// Id<Link> value =
			// Id.createLinkId((String)actionContainer.get(action.toString()).getParameters()[1]);

			if (dest.equals(location)) {
				driveToActions.remove(dest);
				passedDriveToActions.add(dest);
				return dest;
			}
		}
		// return new IdImpl(0);
		return Id.createLinkId(0);
	}

	public String getAgentType() {
		return agentType;
	}

	public void setAgentType(String agentType) {
		this.agentType = agentType;
	}
}
