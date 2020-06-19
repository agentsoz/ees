package io.github.agentsoz.ees;

/*-
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Id;


import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Utils;
import io.github.agentsoz.util.Location;

public class SafeLineMonitor implements LinkEnterEventHandler, LinkLeaveEventHandler,
    PersonArrivalEventHandler, PersonDepartureEventHandler {

  private String name;
  private Location from;
  private Location to;
  private MATSimModel model; // actually only need access to network but dunno how, dsingh 14/nov/17
  // --> just pass model.getScenario().getNetwork()?  But in fact, the new initialization sequence
  // at some point explicitly has the scenario, so it would be scenario.getNetwork(). kai, dec'17
  private Map<Id<Link>,List<Double>> exitTimes;

  public SafeLineMonitor(String name, Location from, Location to, MATSimModel model) {
    this.name = name;
    this.from = from;
    this.to= to;
    this.model = model;
  }

  @Override
  public void handleEvent(PersonArrivalEvent event) {
  }

  @Override
  public void handleEvent(LinkLeaveEvent event) {
    if (exitTimes == null) {
      determineMonitoredLinks();
    }
    Id<Link> id = event.getLinkId();
    if (!exitTimes.containsKey(id)) {
      return;
    }
     List<Double> times = exitTimes.get(id);
    times.add(event.getTime());
    exitTimes.put(id, times);
  }

  @Override
  public void handleEvent(PersonDepartureEvent event) {
  }

  @Override
  public void handleEvent(LinkEnterEvent event) {
  }

  public String getName() {
    return name;
  }
  
  public Collection<List<Double>> getExitTimes() {
    return (exitTimes != null) ? exitTimes.values() : null;
  }
  /**
   * FIXME: This should ideally be called once at the start, but network is
   * unavailable when this is created, so doing it on first event. 
   * dsingh, 14/nov/17
   */
  private void determineMonitoredLinks() {
    LineString line = new GeometryFactory().createLineString(new Coordinate[] {
        new Coordinate(from.getX(), from.getY()), new Coordinate(to.getX(), to.getY())});
    Network network = model.getScenario().getNetwork();
    List<Link> links = Utils.findIntersectingLinks(line, network);
    if (links != null && !links.isEmpty()) {
      exitTimes = new TreeMap<Id<Link>, List<Double>>();
      for (Link link : links) {
        exitTimes.put(link.getId(), new ArrayList<Double>());
      }
    }
  }
  
}
