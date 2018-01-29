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
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.LastEventOfSimStep;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class EvacAgentTracker implements
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
	private final EventsManager events;
	private Map<Id<Vehicle>,Double> linkEnterEventsMap = Collections.synchronizedMap( new LinkedHashMap<>() );
	private Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler() ;
	
	public EvacAgentTracker(Network network, EventsManager events) {
		this.network = network ;
		this.events = events ;
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
		if ( event instanceof LastEventOfSimStep ) {
			if ( false ) { // replace by congestion condition
				Id<Vehicle> vehicleId = null ;
				Id<Person> driverId = null ;
				Id<Link> currentLinkId = null ;
				this.events.processEvent( new AgentInCongestionEvent(event.getTime(), vehicleId, driverId, currentLinkId));
			}
		}
	}
}
