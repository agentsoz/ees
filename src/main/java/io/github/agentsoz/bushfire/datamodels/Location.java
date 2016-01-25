package io.github.agentsoz.bushfire.datamodels;

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

import java.util.Collection;

/**
 * 
 * A general class to denote locations on the map. Each location has a name and
 * a coordinate (easting and northing) (latlong)
 *
 */
public class Location {

	/**
	 * the name of the location. This is also used as the key in the hashtable
	 * in Config, so should be unique
	 */
	private String name;

	public void setName(String n) {
		name = n;
	}

	public String getName() {
		return name;
	}

	/**
	 * Coordinates in lat long
	 */
	private double easting;

	public void setEasting(double e) {
		easting = e;
	}

	public double getEasting() {
		return easting;
	}

	private double northing;

	public void setNorthing(double n) {
		northing = n;
	}

	public double getNorthing() {
		return northing;
	}

	public double[] getCoordinates() {
		return new double[] { easting, northing };
	}

	/**
	 * A type attribute. Not currently used but may be needed to tell the
	 * visualiser how to display locations etc.
	 */
	private String type;

	public void setType(String s) {
		type = s;
	}

	public String getType() {
		return type;
	}

	/**
	 * Create a new location
	 * 
	 * @param n
	 *            name of the location
	 * @param t
	 *            type
	 * @param i
	 *            id
	 * @param east
	 *            easting part of the coordinate
	 * @param north
	 *            northing part of the coordinate
	 */
	public Location(String n, String t, double east, double north) {

		name = n;
		type = t;
		easting = east;
		northing = north;
	}

	public String toString() {
		return "(" + name + ", coords=" + easting + "," + northing + ")";
	}

	public static double[] average(Collection<Location> locations) {

		double lat = 0.0;
		double lon = 0.0;

		for (Location l : locations) {

			lat += l.getEasting();
			lon += l.getNorthing();
		}
		return new double[] { lat / locations.size(), lon / locations.size() };
	}

	public static double distance(Location a, Location b) {

		return distance(a.getCoordinates(), b.getCoordinates());
	}

	/**
	 * great circle distance in kilometres between two locations
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static double distance(double[] a, double[] b) {

		double dx = b[1] - a[1];
		double dy = b[0] - a[0];
		return (Math.sqrt(dx * dx + dy * dy)) / 1000;
	}
}
