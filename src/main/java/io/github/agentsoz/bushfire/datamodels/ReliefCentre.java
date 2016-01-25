package io.github.agentsoz.bushfire.datamodels;

import com.vividsolutions.jts.geom.Coordinate;

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

/**
 * The data structure to store information about relief centres
 * 
 * @author Sewwandi Perera
 *
 */
public class ReliefCentre {

	private String name;
	private String location;
	private int capacity;
	private int assignedCapacity;
	private int remainingCapacity;
	private Coordinate locationCoords;

	public ReliefCentre(String name, String location,
			Coordinate locationCoords, int capacity) {
		this.name = name;
		this.location = location;
		this.capacity = capacity;
		this.locationCoords = locationCoords;
		this.assignedCapacity = 0;
		this.remainingCapacity = capacity;
	}

	public void allocateCapacity(int allocation) {
		this.assignedCapacity += allocation;
		this.remainingCapacity -= allocation;
	}

	public void deAllocateCapacity(int allocation) {
		this.assignedCapacity -= allocation;
		this.remainingCapacity += allocation;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacit) {
		this.capacity = capacit;
	}

	public Coordinate getLocationCoords() {
		return locationCoords;
	}

	public void setLocationCoords(Coordinate locationCoords) {
		this.locationCoords = locationCoords;
	}

	public int getAssignedCapacity() {
		return assignedCapacity;
	}

	public void setAssignedCapacity(int assignedCapacity) {
		this.assignedCapacity = assignedCapacity;
	}

	public int getRemainingCapacity() {
		return remainingCapacity;
	}

	public void setRemainingCapacity(int remainingCapacity) {
		this.remainingCapacity = remainingCapacity;
	}

}
