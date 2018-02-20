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
import io.github.agentsoz.util.Disruption;
import io.github.agentsoz.util.Time;
import io.github.agentsoz.util.evac.PerceptList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DisruptionModel implements DataSource {

	private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

	private DataServer dataServer = null;
	private double lastUpdateTimeInMinutes = -1;
	private TreeMap<Double, Disruption> disruptions;
	private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;

	public DisruptionModel() {
		disruptions = new TreeMap<>();
	}

	void loadJson(String file) throws IOException, ParseException, java.text.ParseException {
		Gson gson = new Gson();
		logger.info("Loading JSON disruptions file: " + file);
		// Create the JSON parsor
		JSONParser parser = new JSONParser();
		// Read in the JSON file
		JSONObject json = (JSONObject) (parser.parse(new FileReader(file)));
		JSONArray disruptionsList = (JSONArray) json.get("disruptions");
		for (JSONObject aDisruptionsList : (Iterable<JSONObject>) disruptionsList) {
			String str = aDisruptionsList.toJSONString();
			Disruption disruption = gson.fromJson(str, Disruption.class);
			DateFormat format = new SimpleDateFormat("HHmm", Locale.ENGLISH);
			Date date = format.parse(disruption.getStartHHMM());
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			double minutes = 60 * cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE);
			disruptions.put(minutes, disruption);
		}
	}

	@Override
	public Object getNewData(double timestep, Object parameters) {
		double time = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
		SortedMap<Double, Disruption> effectiveDisruptions = disruptions.subMap(lastUpdateTimeInMinutes, time);
		lastUpdateTimeInMinutes = time;
		Double nextTime = disruptions.higherKey(time);
		if (nextTime != null) {
			dataServer.registerTimedUpdate(PerceptList.DISRUPTION, this, Time.convertTime(nextTime, Time.TimestepUnit.MINUTES, timestepUnit));
		}
		return effectiveDisruptions;
	}

	/**
	 * Sets the publish/subscribe data server
	 * @param dataServer the server to use
	 */
	void setDataServer(DataServer dataServer) {
		this.dataServer = dataServer;
	}

	/**
	 * Start publishing disruptions data
	 */
	public void start() {
		Double nextTime = disruptions.higherKey(0.0);
		if (nextTime != null) {
			dataServer.registerTimedUpdate(PerceptList.DISRUPTION, this, Time.convertTime(nextTime, Time.TimestepUnit.MINUTES, timestepUnit));
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

