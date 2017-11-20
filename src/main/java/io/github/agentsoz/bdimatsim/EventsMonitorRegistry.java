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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

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
ActivityEndEventHandler{

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
		EndedActivity;
	}
	
	private List<Monitor> monitors = new ArrayList<>() ;

	@Override
	public final void reset(int iteration) {

	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		callRegisteredHandlers(event);
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


	
	synchronized private void callRegisteredHandlers(Event ev) {
		// putting in the "synchronized" on 20-nov-2017.  Might be an issue when parallel events handling is active.  kai, nov'17
		
		ArrayList<Monitor> toRemove = new ArrayList<>();
		for (Monitor monitor : monitors) {
			switch (monitor.getEvent()) {
			// yy from a matsim perspective, the following are plugged together in a weird way: in matsim, enterLink
			// would really be the same as leaveNode.  And not enterNode, as the bdi framework keyword implies.  ???
			// kai, nov'17
			case EnteredNode:
				if (ev instanceof LinkEnterEvent) {
					LinkEnterEvent event = (LinkEnterEvent)ev;
//					if (monitor.getAgentId() == event.getDriverId() && monitor.getLinkId() == event.getLinkId()) {
					if (monitor.getAgentId().equals(event.getDriverId()) && monitor.getLinkId().equals(event.getLinkId())) {
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
		for (Monitor monitor : toRemove) {
			monitors.remove(monitor);
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
	public int registerMonitor(String agentId, MonitoredEventType event,Id<Link> linkId, BDIPerceptHandler handler) {
		synchronized (monitors) {
			monitors.add(new Monitor(agentId, linkId, event, handler));
			return monitors.size();
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
		
		public Monitor(String agentId2, Id<Link> linkId, MonitoredEventType event, BDIPerceptHandler handler) {
			super();

			this.agentId = Id.createPersonId(agentId2);
			// (this is now one of the places where the bdi ids (= Strings) get translated into matsim ids)
			
			this.linkId = linkId;
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
