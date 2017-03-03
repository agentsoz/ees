package io.github.agentsoz.bdimatsim.app;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;

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

import io.github.agentsoz.bdimatsim.MATSimModel;

public interface BDIPerceptHandler {

	/**
	 * Handles the given {@link MonitoredEventType}.
	 * <p>
	 * Returning true will cause this handler to be removed (seful for handling
	 * some event once). Returning false will keep this handler registered such
	 * that it gets called again on subsequent events that match. 
	 * @param agentId
	 * @param linkId
	 * @param monitoredEvent
	 * @param model
	 * @return
	 */
	public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model); 

}
