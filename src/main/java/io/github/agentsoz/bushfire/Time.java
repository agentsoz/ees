package io.github.agentsoz.bushfire;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

public class Time {

	public enum TimestepUnit {
		SECONDS, MINUTES, HOURS, DAYS
	}

	public Time() {
		// TODO Auto-generated constructor stub
	}

	public static double convertTime(double time, Time.TimestepUnit from, Time.TimestepUnit to) {
		double convertedTime = time;
		switch (from) {
		case DAYS:
			convertedTime *= 24;
		case HOURS:
			convertedTime *= 60;
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
