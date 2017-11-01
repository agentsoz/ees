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

import java.awt.Polygon;

/**
 * The data structure to store information about fire.
 * 
 * @author Sewwandi Perera
 *
 */
public class FireInfo {
	private Polygon fire;
	private double direction;
	private double timestamp;

	public FireInfo(Polygon firePolygon, double direction, double timeStamp) {
		this.setDirection(direction);
		this.setFire(firePolygon);
		this.setTimestamp(timeStamp);
	}

	public Polygon getFirePolygon() {
		return fire;
	}

	public void setFire(Polygon fire) {
		this.fire = fire;
	}

	public double getDirection() {
		return direction;
	}

	public void setDirection(double direction) {
		this.direction = direction;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

}
