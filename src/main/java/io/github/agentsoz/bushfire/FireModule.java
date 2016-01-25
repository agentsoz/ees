package io.github.agentsoz.bushfire;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* The FireModule class provides fire model data to the rest of the application.
 * The fire coordinates are read from a data text file and stored in a data structure for
 * sequential access.
 * This module is an active data source, and will provide a list of new fire coordinates when
 * probed by the data server with a new timestamp.
 */

public class FireModule implements DataSource {

	public static String FIREPHASE = "FirePhase";
	public static String FIREALERT = "FireAlert";
	private double startTime;
	private static double lastUpdateTime = 0.0f;
	private boolean sentFireAlert = false;
	private SortedMap<Double, List<double[]>> fire;
	private HashMap<Double, Double> fireDirections;	
	private DataServer dataServer = null;
	private double offsetX = 0.0;
	private double offsetY = 0.0;
	private static List<double[]> latestFireData;
	private static double latestFireProgressDirection;

	private final Logger logger = LoggerFactory.getLogger("");


	public static double getLastUpdateTime(){
		return lastUpdateTime;
	}
	
	public static List<double[]> getLatestFireData(){
		return latestFireData;
	}
	
	public FireModule() {
		fire = new ConcurrentSkipListMap<Double, List<double[]>>();
		fireDirections =  new HashMap<Double, Double>();
		latestFireData = new ArrayList<double[]>();
	}

	/**
	 * Used to offset the location of each fire point
	 * @param x
	 * @param y
	 */
	public void setFireOffset(double x, double y) {
		offsetX = x;
		offsetY = y;
	}

	/**
	 * Returns all fire data that is before the specified time and after the
	 * previous update time, formatted as a list of new fire phases, each phase
	 * being a list of pairs of coordinates
	 */
	@Override
	public Object getNewData(double time, Object parameters) {

		logger.debug("looking for data " + time);

		if (fire.size() == 0) {
			return null;
		}
		double adjustedTime = time + startTime;

		if (!sentFireAlert && time > dataServer.getTimeStep() * 4.0) {

			// these will be sent from the evacuation controller agent
			dataServer.publish(DataTypes.FIRE_ALERT, null);
			logger.debug("publishing fire alert");
			sentFireAlert = true;
		}
		// discard all data prior to previous update time
		if (adjustedTime != lastUpdateTime) {
			while (fire.size() > 0 && fire.firstKey() < lastUpdateTime) {
				fire.remove(fire.firstKey());
			}
			lastUpdateTime = adjustedTime;
		}

		// construct list of fire coordinates to return
		List<List<double[]>> phasesToSend = new ArrayList<List<double[]>>();
		Iterator<Entry<Double, List<double[]>>> i = fire.entrySet().iterator();

		if (!i.hasNext()) {
			return null;
		}

		logger.debug("sending fire data for time " + time);

		Entry<Double, List<double[]>> phase = i.next();

		while (i.hasNext() && phase.getKey() < adjustedTime) {
			// add all new phases before specified time
			phasesToSend.add(phase.getValue());
			if (i.hasNext()) {
				phase = i.next();
			}
		}
		
		if(phasesToSend != null){
			latestFireData = phase.getValue();
			latestFireProgressDirection = fireDirections.get(phase.getKey());
		}
		
		if (i.hasNext()) {
			dataServer.registerTimedUpdate(DataTypes.FIRE, this, i.next().getKey());
		}
		
		return phasesToSend.size() > 0 ? phasesToSend : null;
	}

	public void setDataServer(DataServer dataServer) {
		this.dataServer = dataServer;
	}

	/**
	 * Load file data from file
	 * @param file
	 */
	public void loadData(String file) {
		boolean isGlobalUTM = Config.getCoordinate_system().toLowerCase().equals("utm");
		boolean isFireUTM = Config.getFireFireCoordinateSystem().toLowerCase().equals("utm");
		loadData(file, isGlobalUTM, isFireUTM);
	}

	private void loadData(String file, boolean isGlobalUTM, boolean isFireUTM) {
		logger.info("loading fire data from {}", file);
		CoordinateConversion coordConvertor = new CoordinateConversion();
		try {
			HashMap<Double, double[]> fireCentres = new HashMap<Double, double[]>();
			String string;
			BufferedReader br = new BufferedReader(new FileReader(file));
			int count = 0;
			while ((string = br.readLine()) != null) {
				// read a single fire phase (a set of fire locations for a given
				// point in time)
				List<double[]> locations = new ArrayList<double[]>();
				double time = Double.parseDouble(string);
				if (count == 1) {
					startTime = time;
				}
				string = br.readLine();
				StringTokenizer st = new StringTokenizer(string, " ");
				while (st.hasMoreTokens()) {
					// read a single fire location (a pair of coordinates,
					// latitude and longitude)
					String pair = st.nextToken();
					StringTokenizer pt = new StringTokenizer(pair, ",");
					if (!pt.hasMoreTokens())
						continue; // lines end in a space, annoyingly
					double x = Double.parseDouble(pt.nextToken()) + offsetX;
					double y = Double.parseDouble(pt.nextToken()) + offsetY;
					if (isGlobalUTM && !isFireUTM) {
						// convert fire coords from latlong to utm
						String utm = coordConvertor.latLon2UTM(x,y);
						logger.trace("converted fire latlong to utm {}", utm);
						String[] split = utm.split(" ");
						x = Double.parseDouble(split[2]);
						y = Double.parseDouble(split[3]);
					} else if (!isGlobalUTM && isFireUTM) {
						// convert fire coords from utm to latlong
						double[] latlong = coordConvertor.utm2LatLon("");
						//<<--- finish this Dhi!
					}
					//if (Config.getFireFireCoordinateSystem().toLowerCase().equals("latlong"))
					locations.add(new double[] {x,y});
				}
				
				double xCoordsSum = 0;
				double yCoordsSum = 0;
				for (double[] l : locations) {
					xCoordsSum += l[0];
					yCoordsSum += l[1];
				}
				
				fireCentres.put(time, new double[]{xCoordsSum/locations.size(), yCoordsSum/locations.size()});
				
				fire.put(time, locations);
				count++;
			}
			br.close();

			logger.info("loaded fire progression data ("
					+ String.valueOf(fire.size()) + " time points)");
			
			//Calculating fire directions
			Object[] keys = fireCentres.keySet().toArray();
			
			for (int i = 0; i < keys.length -1; i++) {
				double[] currentCentre = fireCentres.get((double)keys[i]);
				double[] nextCentre = fireCentres.get((double)keys[i+1]);
				double angle = Math.toDegrees(Math.atan2(nextCentre[0]- currentCentre[0], nextCentre[1]- currentCentre[1]));
			       
		        if(angle< 0){
		            angle += 360;
		        }
				
		        fireDirections.put((double)keys[i], angle);
		        
		        if(i == keys.length -2){
		        	fireDirections.put((double)keys[i+1], angle);
		        }
			}
		} catch (Exception e) {
			logger.error("could not load fire data from '"+file+"' : " + e.getMessage());
		}
	}
	
	/**
	 * Writes the fire data to file
	 * @param file
	 */
	private void writeDataForSenozanVia(String file) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(file, false), true);
			for(Double time : fire.keySet()) {
				// convert time to timestamp in seconds
				double hours = time.doubleValue();
				int h = (int) hours;
				double minutes = (hours - h) * 60.0;
				int m = (int) minutes;
				int s = (int) ((minutes - m) * 60.0);
				int secs = h*3600 + m*60 + s;
				// write out the x,y points for the timestamp
				List<double[]> list = fire.get(time);
				for (int i = 0; i < list.size(); i++) {
					double x = list.get(i)[0];
					double y = list.get(i)[1];
					out.println(secs + "\t" + x + "\t" + y);
				}
			}
			out.close();
		} catch(Exception e) {
			
		}
	}
	
	/**
	 * Start publishing fire data
	 */
	public void start() {
		if (fire.isEmpty()) {
			logger.warn("fire module started, but has no data to publish, so will do nothing");
			return;
		}
		dataServer.registerTimedUpdate(DataTypes.FIRE, this, fire.firstKey());
	}

	public static void main(String[] args) {
		if (args.length < 2 ) {
			System.out.println("usage: <input-file> <output-file>");
		}
		FireModule fm = new FireModule();
		System.out.println("Loading fire data from "+ args[0]);
		fm.loadData(args[0], true, false);
		System.out.println("Writing fire data to "+ args[0]);
		fm.writeDataForSenozanVia(args[1]);
	}

	public static double getLatestFireProgressDirection() {
		return latestFireProgressDirection;
	}

	public static void setLatestFireProgressDirection(
			double latestFireProgressDirection) {
		FireModule.latestFireProgressDirection = latestFireProgressDirection;
	}
}
