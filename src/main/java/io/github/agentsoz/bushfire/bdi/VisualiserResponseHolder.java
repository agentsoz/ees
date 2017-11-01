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

public class VisualiserResponseHolder {
	/**
	 * This holds the singleton instance of the class
	 */
	private static VisualiserResponseHolder instance = new VisualiserResponseHolder();
	
	public static String settingsResponse;
	
	/**
	 * This holds the visualiser response
	 */
	public static int userResponse;
	
	/**
	 * Method to get singleton instance of the class
	 * @return
	 */
	public static VisualiserResponseHolder getInstance(){
		return instance;
	}
	
	/**
	 * private constructor of the singleton class
	 */
	private VisualiserResponseHolder(){
		settingsResponse = null;
	}
}
