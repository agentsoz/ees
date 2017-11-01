package io.github.agentsoz.bushfire.jill.agents;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bushfire.bdi.BDIConstants;
import io.github.agentsoz.bushfire.bdi.IBdiConnector;
import io.github.agentsoz.bushfire.datamodels.FireInfo;
import io.github.agentsoz.bushfire.datamodels.PlanCheck;
import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.datamodels.ReliefCentre;
import io.github.agentsoz.bushfire.datamodels.ReliefCentreVector;
import io.github.agentsoz.bushfire.datamodels.Route;
import io.github.agentsoz.bushfire.datamodels.RouteAssignment;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;

/**
 * 
 * @author Sewwandi Perera
 *
 */
@AgentInfo(hasGoals = { "agentsoz.bushfire.jill.goals.RespondFireGoal",
		"agentsoz.bushfire.jill.goals.DoScheduleGoal",
		"agentsoz.bushfire.jill.goals.CalcViableReliefCentresGoal",
		"agentsoz.bushfire.jill.goals.CalculateFireVectorGoal",
		"agentsoz.bushfire.jill.goals.CalculateReliefCentresGoal",
		"agentsoz.bushfire.jill.goals.DecideReliefCentreGoal",
		"agentsoz.bushfire.jill.goals.DecideRouteGoal",
		"agentsoz.bushfire.jill.goals.DecideTimeGoal",
		"agentsoz.bushfire.jill.goals.EvacTimeGoal",
		"agentsoz.bushfire.jill.goals.CheckplanGoal",
		"agentsoz.bushfire.jill.goals.PlanAllAreasGoal",
		"agentsoz.bushfire.jill.goals.ReplanRegionGoal" })
public class EvacController extends Agent implements
		io.github.agentsoz.bdiabm.Agent {
	private IBdiConnector bdiConnector;
	final Logger logger = LoggerFactory.getLogger("");

	// Beliefs of the agent

	/**
	 * Key = route name + relief centre name + region name
	 */
	private HashMap<String, Route> routes;

	/**
	 * Key = region name
	 */
	private HashMap<String, Region> regions;

	/**
	 * Key = region name + relief centre name
	 */
	private HashMap<String, ReliefCentreVector> reliefCentreVectors;

	/**
	 * Key = relief centre name
	 */
	private HashMap<String, ReliefCentre> reliefCentres;

	/**
	 * Key = ID
	 */
	private HashMap<String, FireInfo> fire;

	/**
	 * Key = region name Value = relief centre name
	 */
	private HashMap<String, String> reliefCentreAssignments;

	/**
	 * Key = region name
	 */
	private HashMap<String, RouteAssignment> routeAssignments;

	/**
	 * Key = region name
	 */
	private HashMap<String, Double> evacTimes;

	/**
	 * Key = region name
	 */
	private HashMap<String, PlanCheck> planChecks;

	public EvacController(String name) {
		super(name);
	}

	public EvacController(String name, IBdiConnector bdiConnector) {
		super(name);
		this.routes = new LinkedHashMap<String, Route>();
		this.regions = new LinkedHashMap<String, Region>();
		this.reliefCentres = new LinkedHashMap<String, ReliefCentre>();
		this.fire = new LinkedHashMap<String, FireInfo>();
		this.reliefCentreAssignments = new LinkedHashMap<String, String>();
		this.reliefCentreVectors = new LinkedHashMap<String, ReliefCentreVector>();
		this.routeAssignments = new LinkedHashMap<String, RouteAssignment>();
		this.evacTimes = new LinkedHashMap<String, Double>();
		this.setPlanChecks(new LinkedHashMap<String, PlanCheck>());
		this.bdiConnector = bdiConnector;
		updateBeliefs();
	}

	public void startFireResponse() {
		// TODO:postEvent(ev.post());
	}

	public void startNewSchedule(final String regionName,
			final double executeTime) {
		logger.debug("EvacController creating a schedule for region "
				+ regionName + " at " + executeTime);
		final ScheduledExecutorService executor = Executors
				.newScheduledThreadPool(1);

		Runnable newTask = new Runnable() {

			public void run() {
				// TODO: test this new code
				if (executeTime <= getCurrentTime()) {
					executor.shutdown();
					logger.debug("Excecuting the Evac-Time for region "
							+ regionName + ", Now time: " + getCurrentTime());
					// TODO:postEvent(p_Evac_timeG.post(regionName,
					// executeTime));
				}
			}
		};

		executor.scheduleAtFixedRate(newTask, 0, 3, TimeUnit.MILLISECONDS);
	}

	/**
	 * This is used only for testing purposes. Will be removed when the system
	 * is finalized.
	 */
	private void updateBeliefs() {
		updateRoutes();
		updateRegions();
		updateReliefCentres();
		updateReliefCentreVectors();
		updateFire();
	}

	private void updateRoutes() {
		HashMap<String, Route> routesMap = bdiConnector.getRoutes();

		for (String routeName : routesMap.keySet()) {
			Route route = routesMap.get(routeName);
			route.setTimestamp(getCurrentTime());
			routes.put(routeName + route.getReliefCentre() + route.getRegion(),
					route);
		}
	}

	private void updateRegions() {
		HashMap<String, Region> regionMap = bdiConnector.getRegions();

		for (String regionName : regionMap.keySet()) {
			Region region = regionMap.get(regionName);
			region.setTimeViable(getCurrentTime());
			regions.put(regionName, region);
		}
	}

	private void updateFire() {
		fire.put(
				BDIConstants.FIRE_BELIEF_KEY,
				new FireInfo(bdiConnector.getFirePolygon(), bdiConnector
						.getFireDirectionFromNorth(), getCurrentTime()));
	}

	private void updateReliefCentres() {
		HashMap<String, ReliefCentre> reliefCentreMap = bdiConnector
				.getReliefCentres();
		for (String reliefCentreName : reliefCentreMap.keySet()) {
			ReliefCentre rc = reliefCentreMap.get(reliefCentreName);
			reliefCentres.put(reliefCentreName, rc);
		}
	}

	private void updateReliefCentreVectors() {
		// for all regions
		for (String regionName : this.regions.keySet()) {

			// for all reliefcentres
			for (String reliefCentreName : this.reliefCentres.keySet()) {
				reliefCentreVectors.put(regionName + reliefCentreName,
						new ReliefCentreVector(regionName, reliefCentreName,
								regions.get(regionName).getCentre(),
								reliefCentres.get(reliefCentreName)
										.getLocationCoords()));
			}

		}
	}

	public double getCurrentTime() {
		return bdiConnector.getCurrentTime();
	}

	@Override
	public void init(String[] args) {

	}

	@Override
	public void start() {

	}

	@Override
	public void handlePercept(String perceptID, Object parameters) {

	}

	@Override
	public void packageAction(String actionID, Object[] parameters) {

	}

	@Override
	public void updateAction(String actionID, ActionContent content) {

	}

	@Override
	public void kill() {
		super.finish();
	}

	public IBdiConnector getBdiConnector() {
		return bdiConnector;
	}

	public HashMap<String, Route> getRoutes() {
		return this.routes;
	}

	public void setRoutes(HashMap<String, Route> routes) {
		this.routes = routes;
	}

	public HashMap<String, ReliefCentre> getReliefCentres() {
		return reliefCentres;
	}

	public void setReliefCentres(HashMap<String, ReliefCentre> reliefCentres) {
		this.reliefCentres = reliefCentres;
	}

	public HashMap<String, ReliefCentreVector> getReliefCentreVectors() {
		return reliefCentreVectors;
	}

	public void setReliefCentreVectors(
			HashMap<String, ReliefCentreVector> reliefCentreVectors) {
		this.reliefCentreVectors = reliefCentreVectors;
	}

	public HashMap<String, String> getReliefCentreAssignments() {
		return reliefCentreAssignments;
	}

	public void setReliefCentreAssignments(
			HashMap<String, String> reliefCentreAssignments) {
		this.reliefCentreAssignments = reliefCentreAssignments;
	}

	public HashMap<String, RouteAssignment> getRouteAssignments() {
		return routeAssignments;
	}

	public void setRouteAssignments(
			HashMap<String, RouteAssignment> routeAssignments) {
		this.routeAssignments = routeAssignments;
	}

	public HashMap<String, Region> getRegions() {
		return this.regions;
	}

	public void addRegion(Region region) {
		if (regions == null) {
			regions = new LinkedHashMap<String, Region>();
		}

		regions.put(region.getName(), region);
	}

	public HashMap<String, FireInfo> getFire() {
		return fire;
	}

	public void setFire(HashMap<String, FireInfo> fire) {
		this.fire = fire;
	}

	public HashMap<String, Double> getEvacTimes() {
		return evacTimes;
	}

	public void setEvacTimes(HashMap<String, Double> evacTimes) {
		this.evacTimes = evacTimes;
	}

	public void removeEntryFromRCAssignments(String key) {
		this.reliefCentreAssignments.remove(key);
	}

	public void addReliefCentre(ReliefCentre rc) {
		this.reliefCentres.put(rc.getName(), rc);
	}

	public void addReliefCentreAssignment(String region, String rc) {
		this.reliefCentreAssignments.put(region, rc);
	}

	public void addRouteAssignment(RouteAssignment ra) {
		this.routeAssignments.put(ra.getRegionName(), ra);
	}

	public void addEvacTime(String regionName, double time) {
		this.evacTimes.put(regionName, time);
	}

	public HashMap<String, PlanCheck> getPlanChecks() {
		return planChecks;
	}

	public void setPlanChecks(HashMap<String, PlanCheck> planChecks) {
		this.planChecks = planChecks;
	}

	public void addPlanCheck(String regionName, boolean check) {
		this.planChecks.put(regionName,
				new PlanCheck(regionName, check, this.getCurrentTime()));
	}

	public void removeEvacTime(String regionName) {
		this.evacTimes.remove(regionName);
	}
}
