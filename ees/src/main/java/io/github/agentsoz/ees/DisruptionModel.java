package io.github.agentsoz.ees;

/*-
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
			double minutes = 60 * 24 * (cal.get(Calendar.DAY_OF_YEAR) - 1) + 60 * cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE);
			if (disruptions.containsKey(minutes)) {
				throw new RuntimeException("\n\nCONFIGURATION ERROR:" +
						"\nFound multiple disruption blocks in " + file + " for time " + disruption.getStartHHMM() +
						".\nDisruption model does not support multiple disruption blocks " +
						"for the same time step. " +
						"\nPlease either combine the blocks or change the time for one of them.\n\n");
			}
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

