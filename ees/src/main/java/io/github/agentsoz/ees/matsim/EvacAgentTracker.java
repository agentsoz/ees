package io.github.agentsoz.ees.matsim;

/*
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2020 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdimatsim.AgentInCongestionEvent;
import io.github.agentsoz.util.Global;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.Arrays;
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
	private final EvacConfig evacConfig;
	private Map<Id<Vehicle>,VehicleTrackingData> linkEnterEventsMap = Collections.synchronizedMap( new LinkedHashMap<>() );
	private Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler() ;
	private DeckglTripsData deckglTripsData;

	public EvacAgentTracker(EvacConfig evacConfig, Network network, EventsManager events, DeckglTripsData deckglTripsData) {
		this.evacConfig = evacConfig;
		this.network = network ;
		this.events = events ;
		this.deckglTripsData = deckglTripsData;
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
		Id<Vehicle> vehicleId = event.getVehicleId();
		// Retrieve vehicle data if it exists, else start afresh
		VehicleTrackingData vehicleData = linkEnterEventsMap.containsKey(vehicleId) ?
				linkEnterEventsMap.get(vehicleId) : new VehicleTrackingData(vehicleId, event.getTime());
		// Track estimated time to end of this link
		vehicleData.setEstArrivalTimeAtCurrentLinkEnd(vehicleData.getEstArrivalTimeAtCurrentLinkEnd()
				+ Math.ceil(link.getLength() / link.getFreespeed(event.getTime())));
		// Also record the time we entered this link
		vehicleData.setLastLinkEnterTime(event.getTime());
		linkEnterEventsMap.put(event.getVehicleId(), vehicleData);
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// To save checking at every time step which would be expensive, we currently
		// calculate congestion on link leave events. This has the downside that if the agent is for
		// whatever reason stuck on a link for a very very long time then it will not "realise" it is
		// in congestion until it eventually leaves the link; dsingh 31/01/18
		Link link = network.getLinks().get( event.getLinkId() ) ;
		Id<Vehicle> vehicleId = event.getVehicleId();
		// Tracking should always start at the start of some link, so if the agent
		// was initialised on a link then don't count that
		if (linkEnterEventsMap.containsKey(vehicleId)) {
			// Record the travel stats on this link
			VehicleTrackingData vehicleData = linkEnterEventsMap.get(vehicleId);
			vehicleData.setDistanceSinceStart(vehicleData.getDistanceSinceStart() + link.getLength());
			vehicleData.setTimeSinceStart(event.getTime() - vehicleData.getTrackingStartTime());
			// If it has been long enough since the BDI agent last evaluated if it is in congestion
			if (vehicleData.getTimeSinceStart() > evacConfig.getCongestionEvaluationInterval()) {
				// Calculate delay against the estimated travel time for this interval (could be over several links)
				double totalDelayInThisInterval = event.getTime() - vehicleData.getEstArrivalTimeAtCurrentLinkEnd();
				// If the delay is greater than the agent's tolerance threshold, then the agent will
				// now realise (or "believe" in the BDI sense) that it is in congestion
				if(totalDelayInThisInterval > evacConfig.getCongestionEvaluationInterval() * evacConfig.getCongestionToleranceThreshold()) {
					if(Global.getRandom().nextDouble() < evacConfig.getCongestionReactionProbability()) {
						// ding ding!
						this.events.processEvent(new AgentInCongestionEvent(event.getTime(), vehicleId, null, event.getLinkId()));
					}
				}
				// remove this vehicle from the map so that we can start tracking it in the next interval afresh
				linkEnterEventsMap.remove(vehicleId) ;
			}
			if (deckglTripsData != null) {
				// Calculate the relative speed in this interval
				double totalDelayInThisInterval = event.getTime() - vehicleData.getEstArrivalTimeAtCurrentLinkEnd();
				double totalTimeInThisInterval = vehicleData.getTimeSinceStart();
				double expectedTimeInThisInterval = totalTimeInThisInterval - totalDelayInThisInterval;
				double relativeSpeed2 = expectedTimeInThisInterval/totalTimeInThisInterval;

				double expectedTimeOnLink = Math.ceil(link.getLength() / link.getFreespeed(event.getTime()));
				double actualTimeOnLink = event.getTime() - vehicleData.getLastLinkEnterTime();
				double relativeSpeed = expectedTimeOnLink/actualTimeOnLink;

				// Below we amplify the relative speed difference (squared)
				// to make the colours more sensitive to drops in speed
				//relativeSpeed = Math.pow(relativeSpeed,2);
				deckglTripsData.addEvent(Double.valueOf(event.getTime()).intValue(),
						vehicleId.toString(),
						link.getToNode().getCoord(),
						relativeSpeed);
			}

		}
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

	class VehicleTrackingData {
		private Id<Vehicle> vehicleIid;
		private double trackingStartTime;
		private double distanceSinceStart;
		private double timeSinceStart;
		private double estArrivalTimeAtCurrentLinkEnd;
		private double lastLinkEnterTime;

		public VehicleTrackingData(Id<Vehicle> vehicleIid, double trackingStartTime) {
			this.vehicleIid = vehicleIid;
			this.trackingStartTime = trackingStartTime;
			this.estArrivalTimeAtCurrentLinkEnd = trackingStartTime;
			lastLinkEnterTime = trackingStartTime;
		}

		public Id<Vehicle> getVehicleIid() {
			return vehicleIid;
		}

		public void setVehicleIid(Id<Vehicle> vehicleIid) {
			this.vehicleIid = vehicleIid;
		}

		public double getTrackingStartTime() {
			return trackingStartTime;
		}

		public double getDistanceSinceStart() {
			return distanceSinceStart;
		}

		public void setDistanceSinceStart(double distanceSinceStart) {
			this.distanceSinceStart = distanceSinceStart;
		}

		public double getTimeSinceStart() {
			return timeSinceStart;
		}

		public void setTimeSinceStart(double timeSinceStart) {
			this.timeSinceStart = timeSinceStart;
		}

		public double getEstArrivalTimeAtCurrentLinkEnd() {
			return estArrivalTimeAtCurrentLinkEnd;
		}

		public void setEstArrivalTimeAtCurrentLinkEnd(double estTimeToLinkEnd) {
			this.estArrivalTimeAtCurrentLinkEnd = estTimeToLinkEnd;
		}

		public double getLastLinkEnterTime() {
			return lastLinkEnterTime;
		}

		public void setLastLinkEnterTime(double lastLinkEnterTime) {
			this.lastLinkEnterTime = lastLinkEnterTime;
		}
	}
	
}
