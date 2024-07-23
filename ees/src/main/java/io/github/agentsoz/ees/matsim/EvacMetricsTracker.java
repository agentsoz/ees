package io.github.agentsoz.ees.matsim;

/*
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EvacMetricsTracker implements
		LinkEnterEventHandler,
		LinkLeaveEventHandler,
		ActivityStartEventHandler,
		ActivityEndEventHandler
{
	private final String crs;
	private final CoordinateTransformation transform;
	private final Network network;
	private final int binSizeInSecs;

	private Map<Integer,Map<Id<Link>,LinkData>> store;

	public EvacMetricsTracker(String crs, Network network, int binSizeInSecs) {
		this.crs = crs;
		this.network = network;
		this.binSizeInSecs = (binSizeInSecs < 1 ) ? 1 : (binSizeInSecs > 60*60*24) ? 60*60*24 : binSizeInSecs;
		this.transform = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, crs);
		store = Collections.synchronizedMap(new HashMap<>());
	}

	@Override
	public void reset(int iteration) {
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		synchronized (store) {
			LinkData data = getBinData((int) event.getTime(), event);
			data.setActivitiesStarted(data.getActivitiesStarted() + 1);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		synchronized (store) {
			LinkData data = getBinData((int) event.getTime(), event);
			data.setActivitiesEnded(data.getActivitiesEnded() + 1);
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		synchronized (store) {
			LinkData data = getBinData((int) event.getTime(), event);
			data.setVehiclesEntered(data.getVehiclesEntered() + 1);
		}
	}

	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		synchronized (store) {
			LinkData data = getBinData((int) event.getTime(), event);
			data.setVehiclesExited(data.getVehiclesExited() + 1);
		}
	}

	private LinkData getBinData(int time, HasLinkId hasLinkId) {
		int bin = (time / binSizeInSecs) * binSizeInSecs;
		if (!store.containsKey(bin)) {
			store.put(bin, new HashMap<>());
		}
		Map<Id<Link>, LinkData> linksMap = store.get(bin);
		if (!linksMap.containsKey(hasLinkId.getLinkId())) {
			Link link = network.getLinks().get(hasLinkId.getLinkId());
			CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
					crs, TransformationFactory.WGS84);
			Coord from = transform.transform(link.getFromNode().getCoord());
			Coord to = transform.transform(link.getToNode().getCoord());
			linksMap.put(hasLinkId.getLinkId(), new LinkData(
					new Coord(from.getX(), from.getY(), 0),
					new Coord(to.getX(), to.getY(), 0),
					Math.round(link.getCapacity()*100.0)/100.0,
					Math.round(link.getFreespeed()*100.0)/100.0));
		}
		return linksMap.get(hasLinkId.getLinkId());
	}


	public void saveToFile(String str) {
		try {
			Writer writer = Files.newBufferedWriter(Paths.get(str));
			Gson gson = new GsonBuilder()
					.setPrettyPrinting()
					.create();
			gson.toJson(store, writer);
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class LinkData {
		final private Coord from;
		final private Coord to;
		final private double capacity;
		final private double freespeed;
		private int vehiclesEntered;
		private int vehiclesExited;
		private int activitiesStarted;
		private int activitiesEnded;

		public LinkData(Coord from, Coord to, double capacity, double freespeed) {
			this.from = from;
			this.to = to;
			this.capacity = capacity;
			this.freespeed = freespeed;
		}

		public int getVehiclesEntered() {
			return vehiclesEntered;
		}

		public void setVehiclesEntered(int vehiclesEntered) {
			this.vehiclesEntered = vehiclesEntered;
		}

		public int getVehiclesExited() {
			return vehiclesExited;
		}

		public void setVehiclesExited(int vehiclesExited) {
			this.vehiclesExited = vehiclesExited;
		}

		public int getActivitiesStarted() {
			return activitiesStarted;
		}

		public void setActivitiesStarted(int activitiesStarted) {
			this.activitiesStarted = activitiesStarted;
		}

		public int getActivitiesEnded() {
			return activitiesEnded;
		}

		public void setActivitiesEnded(int activitiesEnded) {
			this.activitiesEnded = activitiesEnded;
		}
	}
}
