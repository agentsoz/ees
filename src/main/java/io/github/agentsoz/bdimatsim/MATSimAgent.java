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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author Edmund Kemsley Agent Object on the ABM side Holds information: an
 *         Agent's actionContainer and perceptContainer, the correct
 *         PerceptHandler and ActionHandler, and lists of driveToActions
 */

public final class MATSimAgent {
	private MATSimPerceptHandler perceptHandler;
	private MATSimActionHandler actionHandler;
	private Id<Person> agentID;
	private String agentType;

	private final List<Id<Link>> driveToActions = new ArrayList<>() ;

	private ActionContainer actionContainer;
	private PerceptContainer perceptContainer;

	public final Id<Person> getAgentID() {
		return agentID;
	}

	public final MATSimPerceptHandler getPerceptHandler() {
		return perceptHandler;
	}

	final MATSimActionHandler getActionHandler() {
		return actionHandler;
	}

	public final ActionContainer getActionContainer() {
		return this.actionContainer;
	}

	public final PerceptContainer getPerceptContainer() {
		return this.perceptContainer;
	}

	MATSimAgent(MATSimActionHandler actionHandler,
			MATSimPerceptHandler perceptHandler, Id<Person> agentID,
			ActionContainer actionContainer, PerceptContainer perceptContainer) {
		this.perceptHandler = perceptHandler;
		this.actionHandler = actionHandler;
		this.agentID = agentID;
		this.actionContainer = actionContainer;
		this.perceptContainer = perceptContainer;
	}

	public final void newDriveTo(Id<Link> newAction) {
		driveToActions.add(newAction);
	}

	final void removeActions(Id<Link> action) {
		driveToActions.remove(action);
	}

	final void clearAllActions() {
		driveToActions.clear();
	}

	/*
	 * Checks the location and returns which driveToAction has been
	 * PASSED/ARRIVED
	 */
	final Id<Link> arrivedAtDriveTo(Id<Link> location) {
		Iterator<Id<Link>> actions = driveToActions.iterator();
		while (actions.hasNext()) {
			Id<Link> dest = actions.next();

			if (dest.equals(location)) {
				driveToActions.remove(dest);
				return dest;
			}
		}
		return Id.createLinkId(0);
	}

	public String getAgentType() {
		return agentType;
	}

	public void setAgentType(String agentType) {
		this.agentType = agentType;
	}
}
