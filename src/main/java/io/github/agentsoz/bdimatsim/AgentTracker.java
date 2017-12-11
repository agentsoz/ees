package io.github.agentsoz.bdimatsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Deprecated // try inside EvacAgent
class AgentTracker implements
		LinkEnterEventHandler,
				LinkLeaveEventHandler,
				PersonArrivalEventHandler,
				PersonDepartureEventHandler,
				ActivityEndEventHandler,
				VehicleEntersTrafficEventHandler,
				VehicleLeavesTrafficEventHandler,
				BasicEventHandler
{
	private final Network network;
	private Map<Id<Vehicle>,Double> linkEnterEventsMap = Collections.synchronizedMap( new LinkedHashMap<>() );
	private Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler() ;
	
	AgentTracker( Network network ) {
		this.network = network ;
	}
	
	double getDelay(Id<Person> personId, double now) {
		return Double.NaN ;
	}
	
	@Override
	public void reset(int iteration) {
		vehicle2Driver.reset(iteration);
		linkEnterEventsMap.clear();
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2Driver.handleEvent(event);
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2Driver.handleEvent(event);
		linkEnterEventsMap.remove(event.getVehicleId()) ;
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
	
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Link link = network.getLinks().get( event.getLinkId() ) ;
		linkEnterEventsMap.put( event.getVehicleId(), event.getTime() + link.getLength()/link.getFreespeed(event.getTime())) ;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		linkEnterEventsMap.remove( event.getVehicleId() ) ;
		// (might not be there, e.g. when vehicle just started on link)
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
	
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
	
	}
	
	@Override
	public void handleEvent(Event event) {
	
	}
}
