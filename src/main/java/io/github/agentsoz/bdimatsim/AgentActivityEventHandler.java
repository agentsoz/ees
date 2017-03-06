package io.github.agentsoz.bdimatsim;

import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

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

/**
 * @author Edmund Kemsley Acts as a simple listener for Matsim agent events then
 *         passes to MATSimAgentManager
 */

public final class AgentActivityEventHandler implements 
LinkEnterEventHandler,
LinkLeaveEventHandler,
PersonArrivalEventHandler, 
PersonDepartureEventHandler,
ActivityEndEventHandler{

	private MATSimModel model;

	public enum MonitoredEventType {
		EnteredNode,
		ExitedNode,
		ArrivedAtDestination,
		DepartedDestination,
		EndedActivity;
	};
	
	private ArrayList<Monitor> monitors;

	AgentActivityEventHandler(MATSimModel model) {
		this.model = model;
		monitors = new ArrayList<Monitor>();
	}

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


	
	private void callRegisteredHandlers(Event ev) {
		ArrayList<Monitor> toRemove = new ArrayList<Monitor>();
		for (Monitor monitor : monitors) {
			switch (monitor.getEvent()) {
			case EnteredNode:
				if (ev instanceof LinkEnterEvent) {
					LinkEnterEvent event = (LinkEnterEvent)ev;
					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
						if(monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent(), model)) {
							toRemove.add(monitor);
						}
					}
				}
				break;
			case ExitedNode:
				if (ev instanceof LinkLeaveEvent) {
					LinkLeaveEvent event = (LinkLeaveEvent)ev;
					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
						if(monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent(), model)) {
							toRemove.add(monitor);
						}
					}
				}
				break;
			case ArrivedAtDestination:
				if (ev instanceof PersonArrivalEvent) {
					PersonArrivalEvent event = (PersonArrivalEvent)ev;
					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
						if(monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent(), model)) {
							toRemove.add(monitor);
						}
					}
				} 
				break;
			case DepartedDestination:
				if (ev instanceof PersonDepartureEvent) {
					PersonDepartureEvent event = (PersonDepartureEvent)ev;
					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
						if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent(), model)) {
							toRemove.add(monitor);
						}
					}
				}
				break;
			case EndedActivity:
				if (ev instanceof ActivityEndEvent) {
					ActivityEndEvent event = (ActivityEndEvent)ev;
					if (monitor.getAgentId() == event.getPersonId() && monitor.getLinkId() == event.getLinkId()) {
						if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent(), model)) {
							toRemove.add(monitor);
						}
					}
				}
				break;
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
	int registerMonitor(Id<Person> agentId, MonitoredEventType event,Id<Link> linkId, BDIPerceptHandler handler) {
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
		
		public Monitor(Id<Person> agentId, Id<Link> linkId, MonitoredEventType event, BDIPerceptHandler handler) {
			super();
			this.agentId = agentId;
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
