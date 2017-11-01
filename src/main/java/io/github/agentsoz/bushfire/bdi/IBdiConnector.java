package io.github.agentsoz.bushfire.bdi;

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

import io.github.agentsoz.bushfire.datamodels.Location;
import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.datamodels.ReliefCentre;
import io.github.agentsoz.bushfire.datamodels.Route;

import java.awt.Polygon;
import java.util.HashMap;

import aos.jack.jak.agent.Agent;

/**
 * This interface is used to connect the BDI program with the application. BDI
 * program calls this interface when it wants to request data from the
 * application
 * 
 * @author Sewwandi Perera
 *
 */
public interface IBdiConnector {

	public Polygon getFirePolygon();

	public double getFireDirectionFromNorth();

	public int getSafeDistanceInMeters();

	public boolean isVisualiserActive();

	public int getShelterDistanceInMeters();

	public String getNewShelterName();

	public String getNewShelterNamePrefix();

	public Agent[] getAllResidents();

	public double[] getSchoolCoordinates();

	public int getMaxDistanceToRelatives();

	public void publishEvacTime(String region, String shelter, String route,
			long executeTime);

	public HashMap<String, Route> getRoutes();

	public HashMap<String, Region> getRegions();

	public HashMap<String, Location> getLocations();

	public HashMap<String, ReliefCentre> getReliefCentres();

	public boolean isManualConfigurationAllowed(String eventName);

	public void publishRegionSchedule(String areaName, String routeName,
			String shelterName, double evacDelay);

	public double getCurrentTime();
}
