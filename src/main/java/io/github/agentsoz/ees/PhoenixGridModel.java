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


import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Time;
import org.geotools.geometry.jts.GeometryBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class PhoenixGridModel implements DataSource<Geometry> {

	private final Logger logger = LoggerFactory.getLogger(PhoenixGridModel.class);

	// Model options ni ESS config XML
	private final String eGridGeoJson = "gridGeoJson";
	private final String eGridSquareSideInMetres = "gridSquareSideInMetres";
	private final String eFireGeoJson = "fireGeoJson";
	private final String eEmbersGeoJson = "smokeGeoJson";
	private final String eIgnitionHHMM = "ignitionHHMM";
	// Model options' values
	private String optGridGeoJsonFile = null;
	private double optGridSquareSideInMetres = 180;
	private DataServer dataServer = null;
	private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
	private double startTimeInSeconds = -1;
	private double ignitionTimeInSecs = 0;
	private JSONObject json = null;
	private double lastFireUpdateInSecs = -1;
	private double lastEmbersUpdateInSecs = -1;
	private TreeMap<Double, Geometry> fire;
	private TreeMap<Double, Geometry> embers;

	public PhoenixGridModel() {
		fire = new TreeMap<>();
		embers = new TreeMap<>();
	}

    public PhoenixGridModel(Map<String, String> opts, DataServer dataServer) {
		fire = new TreeMap<>();
		embers = new TreeMap<>();
		this.dataServer = dataServer;
		parse(opts);
    }

	private void parse(Map<String, String> opts) {
		if (opts == null) {
			return;
		}
		for (String opt : opts.keySet()) {
			logger.info("Found option: {}={}", opt, opts.get(opt));
			switch(opt) {
				case eGridGeoJson:
					optGridGeoJsonFile = opts.get(opt);
					break;
				case eGridSquareSideInMetres:
					optGridSquareSideInMetres = Double.parseDouble(opts.get(opt));
					break;
				case eFireGeoJson:
					logger.warn("Deprecated option {}, please use {} instead", eFireGeoJson, eGridGeoJson);
					break;
				case eEmbersGeoJson:
					logger.warn("Deprecated option {}, please use {} instead", eEmbersGeoJson, eGridGeoJson);
					break;
				case Config.eGlobalStartHhMm:
					String[] tokens = opts.get(opt).split(":");
					setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
					break;
				case eIgnitionHHMM:
					String[] hhmm = opts.get(opt).split(":");
					ignitionTimeInSecs = Time.convertTime(Integer.parseInt(hhmm[0]), Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
							+ Time.convertTime(Integer.parseInt(hhmm[1]), Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
					break;
				default:
					logger.warn("Ignoring option: " + opt + "=" + opts.get(opt));
			}
		}
	}

	public void setStartHHMM(int[] hhmm) {
		startTimeInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, timestepUnit)
				+ Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, timestepUnit);
	}


	@SuppressWarnings("unchecked")
	public void loadPhoenixGridGeoJson(String file) throws FileNotFoundException, IOException, ParseException, java.text.ParseException {
		logger.info("Loading GeoJSON file: " + file);
		// Create the JSON parsor
		JSONParser parser = new JSONParser();
		Reader reader = (file.endsWith(".gz")) ?
				new InputStreamReader(new GZIPInputStream(new FileInputStream(file))) :
				new FileReader(file);
		// Read in the JSON file
		json = (JSONObject) (parser.parse(reader));
		// close the reader
		reader.close();
		// Loop through the features (which contains the time-stamped embers
		// shapes)
		JSONArray features = (JSONArray) json.get("features");
		Iterator<JSONObject> iterator = features.iterator();
		while (iterator.hasNext()) {
			JSONObject feature = iterator.next();
			JSONObject properties = (JSONObject) feature.get("properties");
			JSONObject geometry = (JSONObject) feature.get("geometry");
			JSONArray jcoords = (JSONArray) geometry.get("coordinates");
			Double hourSpotOffset = (Double)properties.get("HOUR_SPOT");
			Double hourBurntOffset = (Double)properties.get("HOUR_BURNT");
			if (hourBurntOffset != null) {
				Geometry shape = getGeometryFromSquareCentroids(getPolygonCoordinates(jcoords), optGridSquareSideInMetres);
				double secs = Math.floor(ignitionTimeInSecs + Time.convertTime(hourBurntOffset, Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS));
				fire.put(secs, shape);
			}
			if (hourSpotOffset != null) {
				Geometry shape = getGeometryFromSquareCentroids(getPolygonCoordinates(jcoords), optGridSquareSideInMetres);
				double secs = Math.floor(ignitionTimeInSecs + Time.convertTime(hourSpotOffset, Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS));
				embers.put(secs, shape);
			}
		}
	}

	private Double[][] getPolygonCoordinates(JSONArray jcoords) {
		Double[][] coordinates = null;
		// has a [[[x,y],[x,y]]] form ie one extra nesting
		Iterator<JSONArray> it = jcoords.iterator();
		int i = 0;
		while (it.hasNext()) {
            JSONArray obj = it.next();
            coordinates = new Double[obj.size()][2];
            Iterator<JSONArray> cit = obj.iterator();
            while (cit.hasNext()) {
                coordinates[i++] = (Double[]) cit.next().toArray(new Double[2]);
            }
            break; // get the first element only of outer array
        }
		return coordinates;
	}

	private static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	@Override
	public Geometry sendData(double timestep, String dataType) {
		double time = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.SECONDS);
		if (Constants.EMBERS_DATA.equals(dataType)) {
			SortedMap<Double, Geometry> shapes = embers.subMap(0.0, time);
			Geometry shape = getGeometry(shapes);
			shape = (shape==null) ? null : new ConvexHull(shape).getConvexHull();
			while (shapes.size() > 1) {
				embers.remove(embers.firstKey());
			}
			Double nextTime = embers.higherKey(time);
			if (nextTime != null) {
				dataServer.registerTimedUpdate(Constants.EMBERS_DATA, this, Time.convertTime(nextTime, Time.TimestepUnit.SECONDS, timestepUnit));
			}
			logger.debug("sending embers data at time {}: {}", timestep, shape);
 			return shape;

		} else if (Constants.FIRE_DATA.equals(dataType)) {
			SortedMap<Double, Geometry> shapes = fire.subMap(0.0, time);
			Geometry shape = getGeometry(shapes);
			shape = (shape==null) ? null : new ConvexHull(shape).getConvexHull();
			while (shapes.size() > 1) {
				fire.remove(fire.firstKey());
			}
			Double nextTime = fire.higherKey(time);
			if (nextTime != null) {
				dataServer.registerTimedUpdate(Constants.FIRE_DATA, this, Time.convertTime(nextTime, Time.TimestepUnit.SECONDS, timestepUnit));
			}
			logger.debug("sending fire data at time {}: {}", timestep, shape);
 			return shape;

		}
		return null;
	}

	private Geometry getGeometry(SortedMap<Double, Geometry> shapes) {
		Geometry polygon = null;
		if (shapes != null && !shapes.isEmpty()) {
			for (Geometry shape : shapes.values()) {
				// Fix for JTS #288 requires reduction to floating.
				// https://github.com/locationtech/jts/issues/288#issuecomment-396647804
				polygon = (polygon==null) ?
						shape :
						GeometryPrecisionReducer.reduce(polygon.union(shape),new PrecisionModel(PrecisionModel.FLOATING));
			}
		}
		return polygon;
	}

	private Geometry getGeometryFromSquareCentroids(Double[][] pairs, double squareSideInMetres) {
		Geometry shape = null ;
		int i = 0;
		for (Double[] pair : pairs) {
			double delta = squareSideInMetres/2;
			Geometry gridCell = new GeometryBuilder().box(
					pair[0]-delta, pair[1]-delta,
					pair[0]+delta, pair[1]+delta);
			// Fix for JTS #288 requires reduction to floating.
			// https://github.com/locationtech/jts/issues/288#issuecomment-396647804
			shape = (shape==null) ?
					gridCell :
					GeometryPrecisionReducer.reduce(shape.union(gridCell),new PrecisionModel(PrecisionModel.FLOATING));
		}
		return shape;
	}

	/**
	 * Start publishing embers data
	 */
	public void start() {
		if (optGridGeoJsonFile != null && !optGridGeoJsonFile.isEmpty()) {
			try {
				loadPhoenixGridGeoJson(optGridGeoJsonFile);
				dataServer.registerTimedUpdate(Constants.EMBERS_DATA, this, startTimeInSeconds);
				dataServer.registerTimedUpdate(Constants.FIRE_DATA, this, startTimeInSeconds);
			} catch (Exception e) {
				throw new RuntimeException("Could not load phoenix grid shapes from [" + optGridGeoJsonFile + "]", e);
			}
		} else if (json==null) {
			logger.warn("started but will be idle forever!!");
		}
	}

	/**
	 * Set the time step unit for this model
	 * @param unit the time step unit to use
	 */
	void setTimestepUnit(Time.TimestepUnit unit) {
		timestepUnit = unit;
	}

}
