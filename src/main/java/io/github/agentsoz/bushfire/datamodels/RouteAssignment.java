package io.github.agentsoz.bushfire.datamodels;

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

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * The data structure, which stores information about the reliefcentres assigned
 * to each region.
 * 
 * @author Sewwandi Perera
 *
 */
public class RouteAssignment {

	private String regionName;
	private String reliefCentreName;
	private String routeName;
	private List<Coordinate> routeCoords;
	private List<String> waypointNames;

	public RouteAssignment(String regionName, String reliefCentreName,
			String routeName, List<Coordinate> routeCoords,
			List<String> waypointNames) {
		this.setRegionName(regionName);
		this.setReliefCentreName(reliefCentreName);
		this.setRouteName(routeName);
		this.setRouteCoords(routeCoords);
		this.setWaypointNames(waypointNames);
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public String getReliefCentreName() {
		return reliefCentreName;
	}

	public void setReliefCentreName(String reliefCentreName) {
		this.reliefCentreName = reliefCentreName;
	}

	public String getRouteName() {
		return routeName;
	}

	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}

	public List<Coordinate> getRouteCoords() {
		return routeCoords;
	}

	public void setRouteCoords(List<Coordinate> routeCoords) {
		this.routeCoords = routeCoords;
	}

	public List<String> getWaypointNames() {
		return waypointNames;
	}

	public void setWaypointNames(List<String> waypointNames) {
		this.waypointNames = waypointNames;
	}

}
