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


import com.google.gson.Gson;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Time;
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

public class DisruptionModel implements DataSource<SortedMap<Double, Disruption>> {

	private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

	private static final String eJsonFile = "fileJson";

	private String optJsonFile = null;

	private double startTimeInSeconds = -1;
	private DataServer dataServer = null;
	private double lastUpdateTimeInMinutes = -1;
	private TreeMap<Double, Disruption> disruptions;
	private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;

	public DisruptionModel() {
		disruptions = new TreeMap<>();
	}

    public DisruptionModel(Map<String, String> opts, DataServer dataServer) {
		this();
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
				case eJsonFile:
					optJsonFile = opts.get(opt);
					break;
				case Config.eGlobalStartHhMm:
					String[] tokens = opts.get(opt).split(":");
					setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
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
	public SortedMap<Double, Disruption> sendData(double timestep, String dataType) {
		double time = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
		SortedMap<Double, Disruption> effectiveDisruptions = disruptions.subMap(lastUpdateTimeInMinutes, time);
		lastUpdateTimeInMinutes = time;
		Double nextTime = disruptions.higherKey(time);
		if (nextTime != null) {
			dataServer.registerTimedUpdate(Constants.DISRUPTION, this, Time.convertTime(nextTime, Time.TimestepUnit.MINUTES, timestepUnit));
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
	 * @param hhmm an array of size 2 with hour and minutes representing start time
	 */
	public void start(int[] hhmm) {
		if (disruptions.isEmpty()) {
			return;
		}
		double startTimeInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
				+ Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
		dataServer.registerTimedUpdate(Constants.DISRUPTION, this, Time.convertTime(startTimeInSeconds, Time.TimestepUnit.SECONDS, timestepUnit));
	}

	public void start() {
		if (optJsonFile != null && !optJsonFile.isEmpty()) {
			try {
				loadJson(optJsonFile);
				dataServer.registerTimedUpdate(Constants.DISRUPTION, this, Time.convertTime(startTimeInSeconds, Time.TimestepUnit.SECONDS, timestepUnit));
			} catch (Exception e) {
				throw new RuntimeException("Could not load json from [" + optJsonFile + "]", e);
			}
		} else {
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

