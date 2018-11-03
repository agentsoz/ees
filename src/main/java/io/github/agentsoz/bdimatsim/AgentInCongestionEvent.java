package io.github.agentsoz.bdimatsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public final class AgentInCongestionEvent extends Event {
	private static final String AGENT_IN_CONGESTION_EVENT_NAME = "agentInCongestion";
	private final Id<Vehicle> vehicleId;
	private final Id<Person> driverId;
	private final Id<Link> currentLinkId;
	
	public AgentInCongestionEvent( double time, Id<Vehicle> vehicleId, Id<Person> driverId, Id<Link> currentLinkId ) {
		super(time);
		this.vehicleId = vehicleId;
		this.driverId = driverId ;
		this.currentLinkId = currentLinkId;
	}
	@Override public String getEventType() {
		return AGENT_IN_CONGESTION_EVENT_NAME;
	}
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}
	public Id<Link> getCurrentLinkId() {
		return this.currentLinkId ;
	}
}
