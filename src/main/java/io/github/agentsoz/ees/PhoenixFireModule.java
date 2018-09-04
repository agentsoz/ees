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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import io.github.agentsoz.util.Time;
import io.github.agentsoz.util.evac.PerceptList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;

public class PhoenixFireModule implements DataSource {

	private final Logger logger = LoggerFactory.getLogger(PhoenixFireModule.class);

	private final String eSendFireAlertOnFireStart = "sendFireAlertOnFireStart";
	private final String eFireGeoJson = "fireGeoJson";
	private final String eSmokeGeoJson = "smokeGeoJson";

	private String optFireShapefile = null;
	private String optSmokeShapefile = null;

	private DataServer dataServer = null;
	private JSONObject json = null;
	private double lastUpdateTimeInMinutes = -1;
	private TreeMap<Double, Double[][]> fire;
	private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
	private double evacStartInSeconds = 0.0;
	private boolean fireAlertSent = false;

	public PhoenixFireModule(boolean sendFireAlertOnFireStart) {

		fire = new TreeMap<Double, Double[][]>();
		fireAlertSent = !sendFireAlertOnFireStart;
	}

    public PhoenixFireModule(Map<String, String> opts, DataServer dataServer) {
		fire = new TreeMap<Double, Double[][]>();
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
				case eSendFireAlertOnFireStart:
					try {
						fireAlertSent = !Boolean.parseBoolean(opts.get(opt));
					} catch (Exception e) {
						throw new RuntimeException("Could not parse option " + opt + "=" + opts.get(opt), e);
					}
					break;
				case eFireGeoJson:
					optFireShapefile  = opts.get(opt);
					break;
				case eSmokeGeoJson:
					optSmokeShapefile = opts.get(opt);
					break;
				case Config.eGlobalStartHhMm:
					String[] tokens = opts.get(opt).split(":");
					setEvacStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
					break;
				default:
					logger.warn("Ignoring option: " + opt + "=" + opts.get(opt));
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void loadGeoJson(String file) throws FileNotFoundException, IOException, ParseException, java.text.ParseException {
		logger.info("Loading GeoJSON fire file: " + file);
		// Create the JSON parsor
		JSONParser parser = new JSONParser();
		// Read in the JSON file
		json = (JSONObject) (parser.parse(new FileReader(file)));
		// Loop through the features (which contains the time-stamped fire
		// shapes)
		JSONArray features = (JSONArray) json.get("features");
		Iterator<JSONObject> iterator = features.iterator();
		while (iterator.hasNext()) {
			JSONObject feature = iterator.next();
			JSONObject properties = (JSONObject) feature.get("properties");
			JSONObject geometry = (JSONObject) feature.get("geometry");
			JSONArray jcoords = (JSONArray) geometry.get("coordinates");
			Double[][] coordinates = new Double[jcoords.size()][2];
			Iterator<JSONArray> it = jcoords.iterator();
			int i = 0;
			while (it.hasNext()) {
				coordinates[i++] = (Double[]) it.next().toArray(new Double[2]);
			}
			DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
			Date date = format.parse((String)properties.get("CURRENT_AT"));
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);  
			double minutes = 60*cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE);
			//double minutes = (double) properties.get("MINUTES");
			fire.put(minutes, coordinates);
		}
	}

	@Override
	public Object sendData(double timestep, String dataType) {
		double time = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
		SortedMap<Double, Double[][]> shapes = fire.subMap(lastUpdateTimeInMinutes, time);
		// if evac start time was explicitly set, then send alert at that time
		// irrespective of when the fire actually starts
		if (!fireAlertSent && evacStartInSeconds > 0.0 && timestep >= evacStartInSeconds) { 
			logger.info("step {} ({} mins): sending fire alert!!", String.format("%.0f", timestep), String.format("%.0f", time));
			dataServer.publish(PerceptList.FIRE_ALERT, null);
			fireAlertSent = true;
		}
		if (!shapes.isEmpty()) {
			// If evac start not explicitly set, then send alert at fire start
			if (!fireAlertSent && evacStartInSeconds == 0.0) {
				logger.info("step {} ({} mins): sending fire alert!!", String.format("%.0f", timestep), String.format("%.0f", time));
				dataServer.publish(PerceptList.FIRE_ALERT, null);
			}
			dataServer.publish(PerceptList.FIRE_DATA, shapes);
		}

		// Setting 'lastUpdateTimeInMinutes = time' below will mean that only the new fire shapes since the
		// last update will be sent. However, this can cause problems for the model if the fire shapes are
		// discontiguous. See https://github.com/agentsoz/bdi-abm-integration/issues/36.
		// So instead, we send all the shapes up until now. This ensures that the state of the fire
		// in the model will always be a union of all shapes from the start of fire.
		// To send all shapes, just disable the lastUpdateTimeInMinutes update below
		// lastUpdateTimeInMinutes = time;

		Double nextTime = fire.higherKey(time);
		if (nextTime != null) {
			dataServer.registerTimedUpdate(PerceptList.FIRE, this, Time.convertTime(nextTime, Time.TimestepUnit.MINUTES, timestepUnit));
		}
		logger.info("sending {} data: {}", PerceptList.FIRE, shapes);
		return shapes;
	}

	public void setDataServer(DataServer dataServer) {
		this.dataServer = dataServer;
	}

	/**
	 * Start publishing fire data
	 */
	public void start() {
		if (optFireShapefile != null && !optFireShapefile.isEmpty()) {
			try {
				loadGeoJson(optFireShapefile);
			} catch (Exception e) {
				throw new RuntimeException("Could not load fire shapes from [" + optFireShapefile + "]", e);
			}
		} else if (json==null) {
			logger.warn("started but will be idle forever!!");
		}
		dataServer.registerTimedUpdate(PerceptList.FIRE, this, evacStartInSeconds);
	}

	public void convertLatLongToUtm() {
		logger.warn("Function not implemented yet!");
	}

	public void setTimestepUnit(Time.TimestepUnit unit) {
		timestepUnit = unit;
	}

	public void setEvacStartHHMM(int[] evacStartHHMM) {
		evacStartInSeconds = Time.convertTime(evacStartHHMM[0], Time.TimestepUnit.HOURS, timestepUnit) 
				+ Time.convertTime(evacStartHHMM[1], Time.TimestepUnit.MINUTES, timestepUnit);
	}
}
