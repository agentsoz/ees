package io.github.agentsoz.ees.matsim;

/*
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
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
