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

import io.github.agentsoz.bushfire.bdi.IBdiConnector;
import io.github.agentsoz.bushfire.datamodels.Location;
import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.datamodels.RegionSchedule;
import io.github.agentsoz.bushfire.datamodels.ReliefCentre;
import io.github.agentsoz.bushfire.datamodels.Route;

import java.awt.Polygon;
import java.util.HashMap;
import java.util.List;

import aos.jack.jak.agent.Agent;

/**
 * This class is used by BDI programs to get information from application and
 * abm
 * 
 * @author Sewwandi Perera
 */
public class BdiConnector implements IBdiConnector {
	private int newShelterNumber = 0;
	private String newShelterNamePrefix = "NEW_SHELTER_";
	private BushfireApplication application;

	public int getShelterDistanceInMeters() {
		return Config.getDistanceToNewShelters();
	}

	public double getFireDirectionFromNorth() {
		return FireModule.getLatestFireProgressDirection();
	}

	public int getSafeDistanceInMeters() {
		return Config.getSafeDistance();
	}

	public BdiConnector(BushfireApplication application) {
		this.application = application;
	}

	public Polygon getFirePolygon() {
		List<double[]> fireData = FireModule.getLatestFireData();
		Polygon firePolygon = new Polygon();

		for (double[] ds : fireData) {
			firePolygon.addPoint((int) ds[0], (int) ds[1]);
		}

		return firePolygon;
	}

	public boolean isVisualiserActive() {
		return Config.getUseGUI();
	}

	public String getNewShelterName() {
		newShelterNumber++;
		return newShelterNamePrefix + newShelterNumber;
	}

	public String getNewShelterNamePrefix() {
		return newShelterNamePrefix;
	}

	public Agent[] getAllResidents() {
		return application.getAgents();
	}

	@Override
	public double[] getSchoolCoordinates() {
		return Config.getLocation("school").getCoordinates();
	}

	public int getMaxDistanceToRelatives() {
		return Config.getMaxDistanceToRelatives();
	}

	@Override
	public void publishEvacTime(String region, String shelter, String route,
			long executeTime) {
		RegionSchedule rs = new RegionSchedule();
		Long longTime = executeTime;
		rs.setEvacTime(longTime.doubleValue());
		rs.setRegion(region);
		rs.setReliefCentre(shelter);
		rs.setRoute(route);
		application.publishSchedule(rs);
	}

	@Override
	public HashMap<String, Route> getRoutes() {
		return Config.getRouteMap();
	}

	@Override
	public HashMap<String, Region> getRegions() {
		return Config.getRegionMap();
	}

	@Override
	public HashMap<String, ReliefCentre> getReliefCentres() {
		return Config.getReliefCentreMap();
	}

	@Override
	public boolean isManualConfigurationAllowed(String eventName) {
		return Config.getChoicePoint(eventName);
	}

	@Override
	public HashMap<String, Location> getLocations() {
		return Config.getLocationMap();
	}

	@Override
	public void publishRegionSchedule(String areaName, String routeName,
			String shelterName, double evacDelay) {
		RegionSchedule rs = new RegionSchedule();
		rs.setEvacTime(evacDelay);
		rs.setRegion(areaName);
		rs.setReliefCentre(shelterName);
		rs.setRoute(routeName);

		application.publishSchedule(rs);

	}

	@Override
	public double getCurrentTime() {
		return application.getSimTime();
	}
}
