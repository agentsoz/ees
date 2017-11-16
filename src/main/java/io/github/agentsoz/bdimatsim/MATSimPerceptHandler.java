package io.github.agentsoz.bdimatsim;

import javax.inject.Inject;

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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;

import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

/**
 * @author Edmund Kemsley, Dhirendra Singh
 */

public final class MATSimPerceptHandler {

	private final EventsMonitorRegistry eventHandler;

	/**
	 * Constructor
	 * 
	 * @param matSimModel
	 */
	@Inject 
	protected MATSimPerceptHandler(final MATSimModel matSimModel) {
		this.eventHandler = matSimModel.getEventMonitors();
		Gbl.assertNotNull( this.eventHandler );
	}

	/**
	 * For a given agent, registers a {@link BDIPerceptHandler} to be called 
	 * whenever an event of type {@link MonitoredEventType} is triggered
	 * for {@code linkId}. 
	 * @param agentId
	 * @param linkId
	 * @param event
	 * @param handler
	 * @return
	 */
	public int registerBDIPerceptHandler(Id<Person> agentId, MonitoredEventType event,Id<Link> linkId, BDIPerceptHandler handler) {
		return eventHandler.registerMonitor(agentId, event, linkId, handler);
	}

}
