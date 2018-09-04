package io.github.agentsoz.ees;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
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
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Time;
import io.github.agentsoz.util.evac.PerceptList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PhoenixGridModel implements DataSource<SortedMap<Double, Double[][]>> {

	private final Logger logger = LoggerFactory.getLogger(PhoenixGridModel.class);

	// Model options ni ESS config XML
	private final String eFireGeoJson = "fireGeoJson";
	private final String eEmbersGeoJson = "smokeGeoJson";
	private final String eIgnitionHHMM = "ignitionHHMM";
	// Model options' values
	private String optFireShapefile = null;
	private String optEmbersShapefile = null;

	private DataServer dataServer = null;
	private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
	private double startTimeInSeconds = -1;
	private double ignitionTimeInMins = 0;
	private JSONObject json = null;
	private double lastUpdateTimeInMinutes = -1;
	private TreeMap<Double, Double[][]> embers;

	public PhoenixGridModel() {

		embers = new TreeMap<Double, Double[][]>();
	}

    public PhoenixGridModel(Map<String, String> opts, DataServer dataServer) {
		embers = new TreeMap<Double, Double[][]>();
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
				case eFireGeoJson:
					optFireShapefile  = opts.get(opt);
					break;
				case eEmbersGeoJson:
					optEmbersShapefile = opts.get(opt);
					break;
				case Config.eGlobalStartHhMm:
					String[] tokens = opts.get(opt).split(":");
					setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
					break;
				case eIgnitionHHMM:
					String[] hhmm = opts.get(opt).split(":");
					ignitionTimeInMins = Time.convertTime(Integer.parseInt(hhmm[0]), Time.TimestepUnit.HOURS, Time.TimestepUnit.MINUTES)
							+ Time.convertTime(Integer.parseInt(hhmm[1]), Time.TimestepUnit.MINUTES, Time.TimestepUnit.MINUTES);
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
	public void loadPhoenixGeoJson(String file) throws FileNotFoundException, IOException, ParseException, java.text.ParseException {
		logger.info("Loading GeoJSON file: " + file);
		// Create the JSON parsor
		JSONParser parser = new JSONParser();
		// Read in the JSON file
		json = (JSONObject) (parser.parse(new FileReader(file)));
		// Loop through the features (which contains the time-stamped embers
		// shapes)
		JSONArray features = (JSONArray) json.get("features");
		Iterator<JSONObject> iterator = features.iterator();
		while (iterator.hasNext()) {
			JSONObject feature = iterator.next();
			JSONObject properties = (JSONObject) feature.get("properties");
			JSONObject geometry = (JSONObject) feature.get("geometry");
			String type = (String) geometry.get("type");
			JSONArray jcoords = (JSONArray) geometry.get("coordinates");
			Double[][] coordinates = new Double[0][2];
			double hourOffset = (Double)properties.get("hour_spot");
			double minutes = ignitionTimeInMins + Time.convertTime(hourOffset, Time.TimestepUnit.HOURS, Time.TimestepUnit.MINUTES);
			if ("Polygon".equalsIgnoreCase(type)) {
				coordinates = getPolygonCoordinates(jcoords);
				embers.put(minutes, coordinates);
			} else if ("MultiPolygon".equalsIgnoreCase(type))  {
				Iterator<JSONArray> it = jcoords.iterator();
				while (it.hasNext()) {
					coordinates = getPolygonCoordinates(it.next());
					embers.put(minutes, coordinates);
				}

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
	public SortedMap<Double, Double[][]> sendData(double timestep, String dataType) {
		double time = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
		SortedMap<Double, Double[][]> shapes = embers.subMap(lastUpdateTimeInMinutes, time);
		// Setting 'lastUpdateTimeInMinutes = time' below will mean that only the new embers shapes since the
		// last update will be sent. However, this can cause problems for the model if the embers shapes are
		// discontiguous. See https://github.com/agentsoz/bdi-abm-integration/issues/36.
		// So instead, we send all the shapes up until now. This ensures that the state of the embers
		// in the model will always be a union of all shapes from the start of embers.
		// To send all shapes, just disable the lastUpdateTimeInMinutes update below
		// lastUpdateTimeInMinutes = time;
		Double nextTime = embers.higherKey(time);
		if (nextTime != null) {
			dataServer.registerTimedUpdate(PerceptList.EMBERS_DATA, this, Time.convertTime(nextTime, Time.TimestepUnit.MINUTES, timestepUnit));
		}
		logger.info("sending embers data at time={}", timestep);
		logger.info( "{}{}", new Gson().toJson(shapes).substring(0,Math.min(new Gson().toJson(shapes).length(),200)),
				"... use DEBUG to see full coordinates list") ;
		logger.debug( "{}", new Gson().toJson(shapes)) ;

		return shapes;
	}

	/**
	 * Start publishing embers data
	 */
	public void start() {
		if (optEmbersShapefile != null && !optEmbersShapefile.isEmpty()) {
			try {
				loadPhoenixGeoJson(optEmbersShapefile);
				dataServer.registerTimedUpdate(PerceptList.EMBERS_DATA, this, startTimeInSeconds);
			} catch (Exception e) {
				throw new RuntimeException("Could not load embers shapes from [" + optEmbersShapefile + "]", e);
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
