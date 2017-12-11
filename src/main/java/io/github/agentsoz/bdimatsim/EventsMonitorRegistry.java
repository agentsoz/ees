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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

/**
 * Acts as a simple listener for Matsim agent events then
 *         passes to MATSimAgentManager
 *
 * @author Edmund Kemsley
 */

public final class EventsMonitorRegistry implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
PersonArrivalEventHandler,
PersonDepartureEventHandler,
ActivityEndEventHandler,
		VehicleEntersTrafficEventHandler,
VehicleLeavesTrafficEventHandler,
		BasicEventHandler {
	
	private Map<Id<Vehicle>,Double> linkEnterEventsMap = new LinkedHashMap<>() ;
	
	/**
	 * if these event types were sitting in bdi-abm, I would understand this.  But given that they are sitting here,
	 * why not use the matsim events directly?  kai, nov'17
	 * Well, in the end this here is an events concentrator/filter: it listens to all events once, and decides which ones to
	 * pass on to where.  We would not want all BDI agents subscribe to all events directly.  kai, nov'17
	 */
	public enum MonitoredEventType {
		EnteredNode,
		ExitedNode,
		ArrivedAtDestination,
		DepartedDestination,
		EndedActivity,
		AgentInCongestion, NextLinkBlocked
	}
	
	private static final Logger log = Logger.getLogger(EventsMonitorRegistry.class ) ;
	
	private List<Monitor> monitors = new CopyOnWriteArrayList<>() ;
    private List<Monitor> toAdd = new ArrayList<>();
    
    public EventsMonitorRegistry() {
//    	log.setLevel(Level.DEBUG);
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2Driver.handleEvent(event);
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2Driver.handleEvent(event);
	}
	
	public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
		return vehicle2Driver.getDriverOfVehicle(vehicleId);
	}
	
	private Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler() ;

	@Override
	public final void reset(int iteration) {

	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		callRegisteredHandlers(event);
		linkEnterEventsMap.put( event.getVehicleId(), event.getTime() ) ;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		callRegisteredHandlers(event);
	}

	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		callRegisteredHandlers(event);
	}

	@Override
	public final void handleEvent(PersonArrivalEvent event) {
		callRegisteredHandlers(event);
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		callRegisteredHandlers(event);
	}

	@Override
	public void handleEvent( Event event ) {
		if ( event instanceof NextLinkBlockedEvent) {
			callRegisteredHandlers(event) ;
		}
	}

	
	private void callRegisteredHandlers(Event ev) {
		// Register any new monitors waiting to be added
		// Synchronise on toAdd which is allowed to be updated by other threads
		synchronized (toAdd) {
			for(Monitor monitor : toAdd) {
				monitors.add(monitor);
			}
			toAdd.clear();
		}
     
		List<Monitor> toRemove = new ArrayList<>();
  
		// yyyyyy in the following, monitor.getLinkId can now be null.  Need to hedge against it!  kai, nov'17
  
		// yy the following feels rather slow.  Say we have 6000 monitors of "something", then
		// each link enter and link leave event means to go through all 6000 monitors to
		// find out that they are not involved.  kai, dec'17
		for (Monitor monitor : monitors) {
	        switch (monitor.getEvent()) {
	        	// (switch according to the type of monitor)
				case AgentInCongestion:
					if ( ev instanceof AgentInCongestionEvent ) {
						log.warn("agent stuck in congestion, need to do something with it.");
					}
					break ;
				case NextLinkBlocked:
					if ( ev instanceof NextLinkBlockedEvent ) {
						log.debug("catching a nextLinkBlocked event"); ;
						NextLinkBlockedEvent event = (NextLinkBlockedEvent) ev;
						if ( monitor.getAgentId().equals(event.getDriverId() ) ) {
							if ( monitor.getHandler().handle( monitor.getAgentId(), event.currentLinkId(), monitor.getEvent() ) ) {
								toRemove.add(monitor) ;
								Monitor arrivedMonitor = findMonitor(monitor.getAgentId(), MonitoredEventType.ArrivedAtDestination);
								if (arrivedMonitor != null) {
									toRemove.add(arrivedMonitor);
								}
							}
						}
					}
					break;
			// yy from a matsim perspective, the following are plugged together in a weird way: in matsim, enterLink
			// would really be the same as leaveNode.  And not enterNode, as the bdi framework keyword implies.
			// Do you really want the naming like this?  kai, nov'17
			case EnteredNode:
				if (ev instanceof LinkEnterEvent) {
					LinkEnterEvent event = (LinkEnterEvent)ev;
					Id<Person> driverId = this.getDriverOfVehicle(event.getVehicleId());
					Gbl.assertNotNull(driverId);
					if (monitor.getAgentId().equals(driverId) && event.getLinkId().equals(monitor.getLinkId())) {
						if(monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
                            toRemove.add(monitor);
						}
					}
				}
				break;
			case ExitedNode:
				if (ev instanceof LinkLeaveEvent) {
					LinkLeaveEvent event = (LinkLeaveEvent)ev;
//					if (monitor.getAgentId() == event.getDriverId() && monitor.getLinkId() == event.getLinkId()) {
					if (monitor.getAgentId().equals(event.getDriverId()) && monitor.getLinkId().equals(event.getLinkId())) {
						if(monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
                            toRemove.add(monitor);
						}
					}
				}
				break;
			case ArrivedAtDestination:
				if (ev instanceof PersonArrivalEvent) {
					PersonArrivalEvent event = (PersonArrivalEvent)ev;
//					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
					if (monitor.getAgentId().equals(event.getPersonId()) && monitor.getLinkId().equals(event.getLinkId())) {
						if(monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
                            toRemove.add(monitor);
						}
						Monitor blockedMonitor = findMonitor(monitor.getAgentId(), MonitoredEventType.NextLinkBlocked);
						if (blockedMonitor != null) {
							toRemove.add(blockedMonitor);
						}
					}
				}
				break;
			case DepartedDestination:
				if (ev instanceof PersonDepartureEvent) {
					PersonDepartureEvent event = (PersonDepartureEvent)ev;
//					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
					if (monitor.getAgentId().equals(event.getPersonId()) && monitor.getLinkId().equals(event.getLinkId())) {
						if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
                            toRemove.add(monitor);
						}
					}
				}
				break;
			case EndedActivity:
				if (ev instanceof ActivityEndEvent) {
					ActivityEndEvent event = (ActivityEndEvent)ev;
//					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
					if (monitor.getAgentId().equals(event.getPersonId()) && monitor.getLinkId().equals(event.getLinkId())) {
						if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
                            toRemove.add(monitor);
						}
					}
				}
				break;
			default:
				throw new RuntimeException("missing case statement") ;
			}
      	}

		// Remove any monitors scheduled to be removed
		monitors.removeAll(toRemove);
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
	public int registerMonitor(String agentId, MonitoredEventType event,String linkId, BDIPerceptHandler handler) {
		synchronized (toAdd) {
			toAdd.add(new Monitor(agentId, linkId, event, handler));
			return toAdd.size();
		}
	}

	/**
	 * Returns a matching monitor for the given parameters
	 * @param agentId
	 * @param event
	 * @return
	 */
	private Monitor findMonitor(Id<Person> agentId, MonitoredEventType event) {
		for (Monitor monitor : monitors) {
			if (monitor.getAgentId().equals(agentId) && monitor.getEvent() == event) {
				return monitor;
			}
		}
		return null;
	}
	/**
	 * Internal structure used to store information about MATSim events to monitor
	 * @author dsingh
	 *
	 */
	private class Monitor {

		private Id<Person> agentId;
		private Id<Link> linkId;
		private MonitoredEventType event;
		private BDIPerceptHandler handler;
		
		public Monitor(String agentId2, String linkId, MonitoredEventType event, BDIPerceptHandler handler) {
			super();

			this.agentId = Id.createPersonId(agentId2);
			// (this is now one of the places where the bdi ids (= Strings) get translated into matsim ids)
			
			if ( linkId!=null ) {
				this.linkId = Id.createLinkId(linkId);
			}
			this.event = event;
			this.handler = handler;
		}
		
		public Id<Person> getAgentId() {
			return agentId;
		}
		public Id<Link> getLinkId() {
			return linkId;
		}
		public MonitoredEventType getEvent() {
			return event;
		}
		public BDIPerceptHandler getHandler() {
			return handler;
		}
	}
}
