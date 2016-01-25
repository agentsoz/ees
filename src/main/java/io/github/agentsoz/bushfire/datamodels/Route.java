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

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class Route {

	private List<String> routeNames;
	private List<Coordinate> routeCoords;
	private String name;
	private String region;
	private String reliefCentre;
	private String description;
	private double timestamp;

	public Route(String name, String region, String reliefCentre, String desc,
			List<String> routeNames, List<Coordinate> routeCoords) {
		this.name = name;
		this.region = region;
		this.reliefCentre = reliefCentre;
		this.description = desc;
		this.routeNames = routeNames;
		this.routeCoords = routeCoords;
	}

	@Override
	public String toString() {
		return "name[" + name + "] names[" + routeNames + "] region[" + region
				+ "] reliefCentre[" + reliefCentre + "] desc[" + description
				+ "]";
	}

	public List<String> getRouteNames() {
		return routeNames;
	}

	public void setRouteNames(List<String> route) {
		this.routeNames = route;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getReliefCentre() {
		return reliefCentre;
	}

	public void setReliefCentre(String reliefCentre) {
		this.reliefCentre = reliefCentre;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	public List<Coordinate> getRouteCoords() {
		return routeCoords;
	}

	public void setRouteCoords(List<Coordinate> routeCoords) {
		this.routeCoords = routeCoords;
	}
}