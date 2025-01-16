package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
