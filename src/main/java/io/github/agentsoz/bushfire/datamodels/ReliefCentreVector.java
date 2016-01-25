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

import java.awt.geom.Line2D;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * This class is used to store the vector information from a region to a relief
 * centre.
 * 
 * @author Sewwandi Perera
 */
public class ReliefCentreVector {
	private String regionName;
	private String reliefCentreName;
	private Line2D path;

	public ReliefCentreVector(String regionName, String reliefCentreName,
			Coordinate areaCentre, Coordinate reliefCentreCoordinate) {
		Line2D path = new Line2D.Float((float) areaCentre.x,
				(float) areaCentre.y, (float) reliefCentreCoordinate.x,
				(float) reliefCentreCoordinate.y);

		this.setRegionName(regionName);
		this.setPath(path);
		this.setReliefCentreName(reliefCentreName);
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String areaName) {
		this.regionName = areaName;
	}

	public String getReliefCentreName() {
		return reliefCentreName;
	}

	public void setReliefCentreName(String reliefCentreName) {
		this.reliefCentreName = reliefCentreName;
	}

	public Line2D getPath() {
		return path;
	}

	public void setPath(Line2D path) {
		this.path = path;
	}

}
