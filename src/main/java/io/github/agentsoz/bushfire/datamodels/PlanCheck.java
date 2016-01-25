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

/**
 * The data structure to store information about reviews made for BDI plans by
 * users
 * 
 * @author Sewwandi Perera
 *
 */
public class PlanCheck {
	private String regionName;
	private boolean checkOK;
	private double timestamp;

	public PlanCheck(String regionName, boolean check, double timestamp) {
		this.regionName = regionName;
		this.checkOK = check;
		this.timestamp = timestamp;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public boolean isCheckOK() {
		return checkOK;
	}

	public void setCheckOK(boolean checkOK) {
		this.checkOK = checkOK;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

}
