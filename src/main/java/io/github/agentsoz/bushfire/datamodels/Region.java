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

import io.github.agentsoz.bushfire.EvacuationReport;
import io.github.agentsoz.dataInterface.DataServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import aos.jack.jak.agent.Agent;

public class Region {

	private static DataServer dataServer = DataServer.getServer("Bushfire");
	final Logger logger = LoggerFactory.getLogger("");

	private String name;
	private double area;
	@SuppressWarnings("unused")
	private boolean visible = true;
	public transient List<Agent> agents = new ArrayList<Agent>();
	private transient int numEvacuated = 0;
	private transient double startTime = 1e10;
	private transient double endTime = 0.0;
	private static double bigNumber = 10000000.0; // should be bigger than any utm coord;
	private double westEdge = bigNumber, eastEdge = -bigNumber,
			northEdge = -bigNumber, southEdge = bigNumber;
	Vector<double[]> polygon = new Vector<double[]>();
	private Coordinate centre;
	private int population = 0;
	private List<String> viableReliefCentres;
	private double timeViable;
	
	public Region(){
		area = 0.0;
		viableReliefCentres = new ArrayList<String>();
	}
	
	/**
	 * Optional parameter
	 */
	private String regionId;

	public void setName(String n) {
		name = n;
	}

	public String getName() {
		return name;
	}

	public void setVisibility(boolean v) {
		visible = v;
	}

	public double getArea() {

		if (area == 0.0) {

			double lat = eastEdge - westEdge;
			double lon = northEdge - southEdge;
			area = lat * lon;
		}
		return area;
	}

	public void agentStart() {

		double time = dataServer.getTime();

		if (time < startTime) {

			startTime = time;
			EvacuationReport.regionStart(name, startTime);
		}
	}

	public void agentEvacuated() {

		numEvacuated++;
		logger.debug("region " + name + " agent evacuated: " + numEvacuated
				+ " of " + agents.size() + " done");

		if (numEvacuated == agents.size()) {

			endTime = dataServer.getTime();
			EvacuationReport.regionEvacuated(name, startTime, endTime,
					numEvacuated);
		}
	}

	public double[] getEvacTimes() {
		return new double[] { startTime, endTime };
	}

	public double getWestEdge() {
		return westEdge;
	}

	public double getEastEdge() {
		return eastEdge;
	}

	public double getNorthEdge() {
		return northEdge;
	}

	public double getSouthEdge() {
		return southEdge;
	}

	public void setCentre(double e, double n) {
		centre = new Coordinate(e, n);
	}

	public Coordinate getCentre() {
		return centre;
	}

	// add a corner and update boundaries
	// TODO this could now be changed to be more explicit as config file now
	// knows boundaries for image registration
	public void add(double[] coord) {
		polygon.add(coord);
		if (coord[0] < westEdge) {
			westEdge = coord[0];
		}
		if (coord[0] > eastEdge) {
			eastEdge = coord[0];
		}
		if (coord[1] < southEdge) {
			southEdge = coord[1];
		}
		if (coord[1] > northEdge) {
			northEdge = coord[1];
		}
	}

	public String toString() {
		String s = "Region: ";
		s = s + this.name + ": " + this.westEdge + ", " + this.eastEdge + ", "
				+ this.northEdge + ", " + this.southEdge +" | id:" + this.regionId;
		return s;
	}

	public int getPopulation() {
		return population;
	}

	public void setPopulation(int population) {
		this.population = population;
	}

	public List<String> getViableReliefCentres() {
		return viableReliefCentres;
	}

	public void setViableReliefCentres(List<String> reliefCentres) {
		this.viableReliefCentres = reliefCentres;
	}
	
	public void addViableReliefCentre(String reliefCentre) {
		if(this.viableReliefCentres == null){
			this.viableReliefCentres = new ArrayList<String>();
		}
		
		this.viableReliefCentres.add(reliefCentre);
	}

	public double getTimeViable() {
		return timeViable;
	}

	public void setTimeViable(double timeViable) {
		this.timeViable = timeViable;
	}

	public String getRegionId() {
		return regionId;
	}

	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}
}
