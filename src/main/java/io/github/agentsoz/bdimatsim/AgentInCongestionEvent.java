package io.github.agentsoz.bdimatsim;

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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public final class AgentInCongestionEvent extends Event {
	private static final String AGENT_IN_CONGESTION_EVENT_NAME = "agentInCongestion";
	private final Id<Person> driverId;
	private final Id<Link> currentLinkId;
	
	public AgentInCongestionEvent(double time, Id<Vehicle> vehicleId, Id<Person> driverId, Id<Link> currentLinkId ) {
		super(time);
		this.driverId = driverId ;
		this.currentLinkId = currentLinkId;
	}
	@Override public String getEventType() {
		return AGENT_IN_CONGESTION_EVENT_NAME;
	}
	public Id<Person> getDriverId() {
		return this.driverId ;
	}
	public Id<Link> currentLinkId() {
		return this.currentLinkId ;
	}
}
