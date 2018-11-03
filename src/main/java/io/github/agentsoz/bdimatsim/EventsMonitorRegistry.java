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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType.*;

/**
 * Acts as a simple listener for Matsim agent events then
 *         passes to MATSimAgentManager
 *
 * @author Edmund Kemsley
 */

public final class EventsMonitorRegistry implements LinkEnterEventHandler, LinkLeaveEventHandler,
				   PersonArrivalEventHandler, PersonDepartureEventHandler, ActivityEndEventHandler,
				   VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, BasicEventHandler
{
	
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
	
	private static final Logger log = LoggerFactory.getLogger(EventsMonitorRegistry.class ) ;

	private final Map<MonitoredEventType, Map<Id<Person>,Monitor>> monitors = Collections.synchronizedMap(new LinkedHashMap<>());

	private final Map<MonitoredEventType, Map<Id<Person>,Monitor>> toAdd = Collections.synchronizedMap(new LinkedHashMap<>());
    
	public EventsMonitorRegistry() {
    	// supposedly works like this:
//		((ch.qos.logback.classic.Logger) log).setLevel(ch.qos.logback.classic.Level.DEBUG);
		// so the slf4j interface does not have these commands, but the logback implementation does.
		// kai, based on pointer by dhirendra
	}
	
	@Override public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2Driver.handleEvent(event);
	}
	
	@Override public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2Driver.handleEvent(event);
	}
	
	public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
		return vehicle2Driver.getDriverOfVehicle(vehicleId);
	}
	
	private Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler() ;

	@Override public final void reset(int iteration) { }

	@Override public final void handleEvent(LinkEnterEvent event) { callRegisteredHandlers(event); }

	@Override public void handleEvent(LinkLeaveEvent event) {
		callRegisteredHandlers(event);
	}

	@Override public final void handleEvent(PersonDepartureEvent event) {
		callRegisteredHandlers(event);
	}

	@Override public final void handleEvent(PersonArrivalEvent event) {
		callRegisteredHandlers(event);
	}
	
	@Override public void handleEvent(ActivityEndEvent event) {
		callRegisteredHandlers(event);
	}

	@Override public void handleEvent( Event event ) {
		if (event instanceof NextLinkBlockedEvent || event instanceof AgentInCongestionEvent ) {
			callRegisteredHandlers(event);
		}
	}

	
	private void callRegisteredHandlers(Event ev) {
		// Register any new monitors waiting to be added
		// Synchronise on toAdd which is allowed to be updated by other threads
		synchronized (toAdd) {
//			for(Id<Person> agentId : toAdd.keySet()) {
//				Monitor monitor = toAdd.get(agentId);
//				if (!monitors.containsKey(monitor.getEvent())) {
//					monitors.put(monitor.getEvent(), new ConcurrentHashMap<>());
//				}
//				monitors.get(monitor.getEvent()).put(agentId,monitor);
//			}
//			toAdd.clear();
			for(MonitoredEventType eventType : toAdd.keySet()) {
				if (!monitors.containsKey(eventType)) {
					monitors.put(eventType, new ConcurrentHashMap<>());
				}
				Map<Id<Person>, Monitor> map = toAdd.get(eventType);
				for (Id<Person> agentId : map.keySet()) {
					Monitor monitor = map.get(agentId);
					monitors.get(eventType).put(agentId,monitor);
				}
			}
			toAdd.clear();
		}

		if (ev instanceof AgentInCongestionEvent && monitors.containsKey(AgentInCongestion)) {
			handleAgentInCongestionEvent((AgentInCongestionEvent) ev);

		} else if (ev instanceof NextLinkBlockedEvent && monitors.containsKey(NextLinkBlocked)) {
			handleNextLinkBlockedEvent((NextLinkBlockedEvent) ev);

		} else if (ev instanceof LinkEnterEvent && monitors.containsKey(EnteredNode)) {
			handleLinkEnterEvent((LinkEnterEvent) ev);

		} else if (ev instanceof LinkLeaveEvent && monitors.containsKey(ExitedNode)) {
			handleLinkLeaveEvent((LinkLeaveEvent) ev);

		} else if (ev instanceof PersonArrivalEvent && monitors.containsKey(ArrivedAtDestination)) {
			handlePersonArrivalEvent((PersonArrivalEvent) ev);

		} else if (ev instanceof PersonDepartureEvent && monitors.containsKey(DepartedDestination)) {
			handlePersonDepartureEvent((PersonDepartureEvent) ev);

		} else if (ev instanceof ActivityEndEvent && monitors.containsKey(EndedActivity)) {
			handleActivityEndEvent((ActivityEndEvent) ev);
		}
	}

	private void handleAgentInCongestionEvent( AgentInCongestionEvent ev) {
		Map<Id<Person>, Monitor> toRemove = new HashMap<>();
		Id<Person> driverId = this.getDriverOfVehicle( ev.getVehicleId());
		Gbl.assertNotNull(driverId);
		Monitor monitor = monitors.get(AgentInCongestion).get(driverId);
		if (monitor != null) {
			log.debug("handling AgentInCongestion event");
			if (monitor.getAgentId().equals(driverId)) {
				if (monitor.getHandler().handle(monitor.getAgentId(), ev.getCurrentLinkId(), monitor.getEvent())) {
					toRemove.put(driverId, monitor);
					Monitor arrivedMonitor = monitors.get(ArrivedAtDestination).get(driverId);
					if (arrivedMonitor != null) {
						toRemove.put(driverId,arrivedMonitor);
					}
				}
			}
			synchronized (monitors.get(AgentInCongestion)) {
				monitors.get(AgentInCongestion).entrySet().removeAll(toRemove.entrySet());
			}
		}
	}

	private void handleNextLinkBlockedEvent(NextLinkBlockedEvent ev) {
		Map<Id<Person>, Monitor>  toRemove = new HashMap<>();
		NextLinkBlockedEvent event = ev;
		Id<Person> driverId = event.getDriverId();
		Monitor monitor = monitors.get(NextLinkBlocked).get(driverId);
		if (monitor != null) {
			log.debug("catching a nextLinkBlocked event");
			if (monitor.getAgentId().equals(event.getDriverId())) {
				if (monitor.getHandler().handle(monitor.getAgentId(), event.currentLinkId(), monitor.getEvent())) {
					toRemove.put(driverId,monitor);
					Monitor arrivedMonitor = monitors.get(ArrivedAtDestination).get(driverId);
					if (arrivedMonitor != null) {
						toRemove.put(driverId,arrivedMonitor);
					}
				}
			}
			synchronized (monitors.get(NextLinkBlocked)) {
				monitors.get(NextLinkBlocked).entrySet().removeAll(toRemove.entrySet());
			}
		}
	}

	private void handleLinkEnterEvent(LinkEnterEvent ev) {
		Map<Id<Person>, Monitor>  toRemove = new HashMap<>();
		LinkEnterEvent event = ev;
		Id<Person> driverId = this.getDriverOfVehicle(event.getVehicleId());
		Gbl.assertNotNull(driverId);
		Monitor monitor = monitors.get(EnteredNode).get(driverId);
		if (monitor != null) {
			if (monitor.getAgentId().equals(driverId) && event.getLinkId().equals(monitor.getLinkId())) {
				if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
					toRemove.put(driverId,monitor);
				}
			}
		}
		synchronized (monitors.get(EnteredNode)) {
			monitors.get(EnteredNode).entrySet().removeAll(toRemove.entrySet());
		}
	}

	private void handleLinkLeaveEvent(LinkLeaveEvent ev) {
		Map<Id<Person>, Monitor> toRemove = new HashMap<>();
		LinkLeaveEvent event = ev;
		Id<Person> driverId = this.getDriverOfVehicle(event.getVehicleId());
		Gbl.assertNotNull(driverId);
		Monitor monitor = monitors.get(ExitedNode).get(driverId);
		if (monitor != null) {
			if (monitor.getAgentId().equals(event.getDriverId()) && monitor.getLinkId().equals(event.getLinkId())) {
				if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
					toRemove.put(driverId,monitor);
				}
			}
		}
		synchronized (monitors.get(ExitedNode)) {
			monitors.get(ExitedNode).entrySet().removeAll(toRemove.entrySet());
		}
	}

	private void handlePersonArrivalEvent(PersonArrivalEvent ev) {
		Map<Id<Person>, Monitor> toRemove = new HashMap<>();
		PersonArrivalEvent event = ev;
		Id<Person> driverId = event.getPersonId();
		Gbl.assertNotNull(driverId);
		Monitor monitor = monitors.get(ArrivedAtDestination).get(driverId);
		if (monitor != null) {
			if (monitor.getAgentId().equals(event.getPersonId()) && monitor.getLinkId().equals(event.getLinkId())) {
				if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
					toRemove.put(driverId,monitor);
				}
				Monitor blockedMonitor = monitors.get(NextLinkBlocked).get(driverId);
				if (blockedMonitor != null) {
					toRemove.put(driverId,blockedMonitor);
				}
			}
		}
		synchronized (monitors.get(ArrivedAtDestination)) {
			monitors.get(ArrivedAtDestination).entrySet().removeAll(toRemove.entrySet());
		}
	}

	private void handlePersonDepartureEvent(PersonDepartureEvent ev) {
		Map<Id<Person>, Monitor> toRemove = new HashMap<>();
		PersonDepartureEvent event = ev;
		Id<Person> driverId = event.getPersonId();
		Gbl.assertNotNull(driverId);
		Monitor monitor = monitors.get(DepartedDestination).get(driverId);
		if (monitor != null) {
			if (monitor.getAgentId().equals(event.getPersonId()) && monitor.getLinkId().equals(event.getLinkId())) {
				if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
					toRemove.put(driverId,monitor);
				}
			}
		}
		synchronized (monitors.get(DepartedDestination)) {
			monitors.get(DepartedDestination).entrySet().removeAll(toRemove.entrySet());
		}
	}

	private void handleActivityEndEvent(ActivityEndEvent ev) {
		Map<Id<Person>, Monitor> toRemove = new HashMap<>();
		Id<Person> driverId = ev.getPersonId();
		Gbl.assertNotNull(driverId);
		Monitor monitor = monitors.get(EndedActivity).get(driverId);
		if (monitor != null) {
			if (monitor.getAgentId().equals( ev.getPersonId()) && monitor.getLinkId().equals( ev.getLinkId())) {
				if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
					toRemove.put(driverId,monitor);
				}
			}
		}
		synchronized (monitors.get(EndedActivity)) {
			monitors.get(EndedActivity).entrySet().removeAll(toRemove.entrySet());
		}
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
			if (!toAdd.containsKey(event)) {
				toAdd.put(event, new ConcurrentHashMap<>());
			}
			Map<Id<Person>, Monitor> map = toAdd.get(event);

			map.put(Id.createPersonId(agentId), new Monitor(agentId, linkId, event, handler));
			toAdd.put(event, map);
			return toAdd.size();
		}
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
