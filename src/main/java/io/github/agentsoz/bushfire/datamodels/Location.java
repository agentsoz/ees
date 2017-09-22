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
 * a coordinate (x and y) (latlong)
 * <p><strong>
 * FIXME: DS, 28/Oct/16: Easting/Northing and Lat/Long are not the same thing, and
 * really the location should be X,Y instead, since this class
 * is not specific to any one coordinate system.
 * </strong>
 */
public class Location {

	private String name;
	private double x;
	private double y;
	private Object attributes;

	/**
	 * Create a new location
	 * 
	 * @param n
	 *            name of the location
	 * @param x
	 *            x part of the coordinate
	 * @param y
	 *            y part of the coordinate
	 */
	public Location(String n, double x, double y) {

		name = n;
		this.x = x;
		this.y = y;
	}

	   public Location(String n, double x, double y, Object attributes) {
	        name = n;
	        this.x = x;
	        this.y = y;
	        this.attributes = attributes;
	    }

	
	public String getName() {
		return name;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double[] getCoordinates() {
		return new double[] { x, y };
	}
	
	public Object getAttributes() {
	  return attributes;
	}

	public String toString() {
		return "(" + name + ", coords=" + x + "," + y + ")";
	}

}
