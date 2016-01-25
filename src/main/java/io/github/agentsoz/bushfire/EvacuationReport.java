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

import io.github.agentsoz.bushfire.DataTypes;
import io.github.agentsoz.bushfire.datamodels.RegionSchedule;
import io.github.agentsoz.dataInterface.*;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileWriter;

public class EvacuationReport implements DataClient {

	private static Date date = new Date();
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private static DataServer dataServer = DataServer.getServer("Bushfire");
	private static EvacuationReport instance;
	private static PrintWriter out;

	/**
	 * Return singleton
	 * @return
	 */
	private static EvacuationReport getInstance() {
		return (instance != null) ? instance : new  EvacuationReport();
	}
	
	/**
	 * Convert a double time in hours to a string in the format hh:mm:ss
	 * 
	 * @param time
	 * @return
	 */
	private static String hoursToTime(double hours) {

		int h = (int) hours;
		double minutes = (hours - h) * 60.0;
		int m = (int) minutes;
		int s = (int) ((minutes - m) * 60.0);

		return String.format("%02d", h) + ":" + String.format("%02d", m) + ":"
				+ String.format("%02d", s);
	}

	public static void close() {
		if (out == null) return;
		out.close();
	}

	public static void start() {
		try {
			out = new PrintWriter(
					new FileWriter(Config.getReportFile(), false), true);
		} catch (Exception e) {
			e.printStackTrace();
			out = null;
			return;
		}

		dataServer.subscribe(getInstance(), DataTypes.EVAC_SCHEDULE);
		dataServer.subscribe(getInstance(), DataTypes.BDI_CHOICE);

		out.println("Simulation report started at: " + sdf.format(date));
		String s = "Residents with kids: "
				+ String.format("%.2f", Config.getProportionWithKids() * 100.0)
				+ "%; with relatives: "
				+ String.format("%.2f", Config.getProportionWithRelatives() * 100.0) + "%";
		report(s);
	}

	public static void regionStart(String region, double startTime) {
		String s = "Region \"" + region + "\" started evacuating at "
				+ hoursToTime(startTime);
		report(s);
	}

	public static void regionEvacuated(String region, double startTime,
			double endTime, int num) {
		String s = "";
		s += "Region \"" + region + "\" finished evacuating at "
				+ hoursToTime(endTime);
		s += " (" + num + " residents,";
		s += " took " + hoursToTime(endTime - startTime)
				+ " hours)";
		report(s);
	}

	@Override
	public boolean dataUpdate(double time, String dataType, Object data) {
    	if (out == null) return true;
		if (dataType.equals(DataTypes.BDI_CHOICE)) {
			String[] params = (String[]) data;
			report ("Plan chosen for "+ params[0] +" is "+params[1]);
		} else if (dataType.equals(DataTypes.EVAC_SCHEDULE)) {
			RegionSchedule rs = (RegionSchedule)data;
			String s = "Region \"" + rs.getRegion() + "\" will evacuate to relief centre \""
					+ rs.getReliefCentre() + "\" along route \"" + rs.getRoute()
					+ "\" after " + hoursToTime(rs.getEvacTime()) + " hour(s)";
			report(s);
		}
		return true;
	}
	
	public static void report(String s) {
    	if (out == null) return;
    	out.println(String.format("%10s | %s", hoursToTime(dataServer.getTime()), s));
	}
}
