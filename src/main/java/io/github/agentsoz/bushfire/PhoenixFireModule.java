package io.github.agentsoz.bushfire;

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
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;

public class PhoenixFireModule implements DataSource {

	public enum TimestepUnit {
		SECONDS, MINUTES, HOURS, DAYS
	}

	private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.bushfire");

	private DataServer dataServer = null;
	private JSONObject json = null;
	private double lastUpdateTimeInMinutes = -1;
	private TreeMap<Double, Double[][]> fire;
	private TimestepUnit timestepUnit = TimestepUnit.SECONDS;

	public PhoenixFireModule() {
		fire = new TreeMap<Double, Double[][]>();
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
	public Object getNewData(double timestep, Object parameters) {
		double time = convertTime(timestep, timestepUnit, TimestepUnit.MINUTES);
		SortedMap<Double, Double[][]> shapes = fire.subMap(lastUpdateTimeInMinutes, time);
		if (!shapes.isEmpty()) {
			// TODO: Improve reasoning about when to send fire alert
			if (lastUpdateTimeInMinutes == -1) {
				logger.info("step {} ({} mins): sending fire alert!!", String.format("%.0f", timestep), String.format("%.0f", time));
				dataServer.publish(DataTypes.FIRE_ALERT, null);
			}
			lastUpdateTimeInMinutes = time;
		}
		Double nextTime = fire.higherKey(time);
		if (nextTime != null) {
			dataServer.registerTimedUpdate(DataTypes.FIRE, this, convertTime(nextTime, TimestepUnit.MINUTES, timestepUnit));
		}
		return shapes;
	}

	public void setDataServer(DataServer dataServer) {
		this.dataServer = dataServer;
	}

	/**
	 * Start publishing fire data
	 */
	public void start() {
		if (fire.isEmpty()) {
			logger.warn("Fire module started, but has no data to publish, so will do nothing");
			return;
		}
		dataServer.registerTimedUpdate(DataTypes.FIRE, this,
				convertTime(fire.firstKey(), TimestepUnit.MINUTES, timestepUnit));
	}

	public void convertLatLongToUtm() {
		logger.warn("Function not implemented yet!");
	}

	public void setTimestepUnit(TimestepUnit unit) {
		timestepUnit = unit;
	}

	private double convertTime(double time, TimestepUnit from, TimestepUnit to) {
		double convertedTime = time;
		switch (from) {
		case DAYS:
			convertedTime *= 1;
		case HOURS:
			convertedTime *= 24;
		case MINUTES:
			convertedTime *= 60;
		case SECONDS:
			convertedTime *= 1;
		}
		switch (to) {
		case DAYS:
			convertedTime /= 24;
		case HOURS:
			convertedTime /= 60;
		case MINUTES:
			convertedTime /= 60;
		case SECONDS:
			convertedTime /= 1;
		}
		return convertedTime;
	}
}
