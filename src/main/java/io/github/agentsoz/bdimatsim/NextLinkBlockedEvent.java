package io.github.agentsoz.bdimatsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public final class NextLinkBlockedEvent extends Event {
	private static final String NEXT_LINK_BLOCKED_EVENT_NAME = "nextLinkBlockedEvent";
	private final Id<Person> driverId;
	private final Id<Link> currentLinkId;
	
	public NextLinkBlockedEvent(double time, Id<Vehicle> vehicleId, Id<Person> driverId, Id<Link> currentLinkId, Id<Link> blockedLinkId) {
		super(time);
		this.driverId = driverId ;
		this.currentLinkId = currentLinkId;
	}
	@Override public String getEventType() {
		return NEXT_LINK_BLOCKED_EVENT_NAME;
	}
	public Id<Person> getDriverId() {
		return this.driverId ;
	}
	public Id<Link> currentLinkId() {
		return this.currentLinkId ;
	}
}
