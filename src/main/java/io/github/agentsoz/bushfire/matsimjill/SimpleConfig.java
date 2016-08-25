package io.github.agentsoz.bushfire.matsimjill;

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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * Utility class to hold configuration information
 *
 */
public class SimpleConfig {

	private static final Logger logger = LoggerFactory.getLogger("");

	// all the config info is held statically so it can be accessed globally
	private static String configFile = null;

	private static String reportFile = "bushfire-evacuation-report.txt";
	private static String bdiSimFile = null;
	private static String matSimFile = null;
	private static String geographyFile = null;
	private static int numBDIAgents = 1;
	private static String fireFile = null;
	private static String controllerFile = null;
	private static boolean useGUI = false;
	private static boolean bypassController = false;
	private static boolean dieOnDisconnect = false;
	private static int port = -1;
	private static double proportionWithKids = 0.0;
	private static double proportionWithRelatives = 0.0;
	private static int maxDistanceToRelatives = 1000;
	private static double[] evacDelay = new double[] { 0.0, 0.0 };
	private static HashMap<String, Location> locations = new HashMap<String, Location>();
	private static HashMap<String, Region> regions = new HashMap<String, Region>();
	private static HashMap<String, Route> routes = new HashMap<String, Route>();
	public static HashMap<String, ReliefCentre> reliefCentres = new HashMap<String, ReliefCentre>();
	private static Image image = new Image();
	private static String fireFireCoordinateSystem = "longlat";
	private static String coordinate_system = "longlat";
	private static int distanceToNewShelters = 50000;
	private static int safeDistance = 100;
	private static HashMap<String, Boolean> choicePoints = new HashMap<String, Boolean>();

	public static class Image {
		public String file;
		public double west, east, north, south;

		public Image() {
		}
		
		@Override
		public String toString() {
			return "west[" + west + "] east[" + east + "] north[" + north + "] south[" + south + "]";
		}
	}
	
	public static HashMap<String, Location> getLocationMap(){
		return locations;
	}
	
	public static boolean getChoicePoint(String choice){
		return choicePoints.get(choice);
	}

	public static HashMap<String, Route> getRouteMap(){
		return routes;
	}
	
	public static HashMap<String, Region> getRegionMap(){
		return regions;
	}
	
	public static HashMap<String, ReliefCentre> getReliefCentreMap(){
		return reliefCentres;
	}

	public static Image getImage() {
		return image;
	}

	// basic get/set for simple data
	public static String getReportFile() {
		return reportFile;
	}

	public static String getMatSimFile() {
		return matSimFile;
	}

	public static String getFireFile() {
		return fireFile;
	}

	public static int getPort() {
		return port;
	}

	public static int getNumBDIAgents() {
		return numBDIAgents;
	}

	public static boolean getBypassController() {
		return bypassController;
	}

	public static boolean getDieOnDisconnect() {
		return dieOnDisconnect;
	}

	public static double getProportionWithKids() {
		return proportionWithKids;
	}

	public static double getProportionWithRelatives() {
		return proportionWithRelatives;
	}

	public static int getMaxDistanceToRelatives() {
		return maxDistanceToRelatives;
	}

	public static double[] getEvacDelay() {
		return evacDelay;
	}

	public static String getCoordinate_system() {
		return coordinate_system;
	}

	public static String getFireFireCoordinateSystem() {
		return fireFireCoordinateSystem;
	}

	public static Location getLocation(String name) {
		return locations.get(name);
	}

	public static Set<String> getLocations() {
		return locations.keySet();
	}

	public static Region getRegion(String name) {
		return regions.get(name);
	}

	public static Set<String> getRegionsByName() {
		return regions.keySet();
	}

	public static Collection<Region> getRegions() {
		return regions.values();
	}

	public static Route getRoute(String name) {
		return routes.get(name);
	}

	public static Set<String> getRoutes() {
		return routes.keySet();
	}

	public static ReliefCentre getReliefCentre(String name) {
		return reliefCentres.get(name);
	}

	public static Set<String> getReliefCentres() {
		return reliefCentres.keySet();
	}

	public static boolean getUseGUI() {
		return useGUI;
	}

	public static void setUseGUI(boolean b) {
		useGUI = b;
	}

	public static String getConfigFile() {
		return configFile;
	}

	public static void setConfigFile(String string) {
		configFile = string;
	}

	/**
	 * Pick one of the listed evac points
	 */
	public static String getRandomEvacPoint() {
		int n = new Random().nextInt(reliefCentres.size());
		ReliefCentre rc = (ReliefCentre) reliefCentres.values().toArray()[n];
		return rc.getName();
	}

	/**
	 * read the config file (xml) and save data
	 * 
	 * @return false if there was a problem
	 * @throws Exception 
	 */
	public static boolean readConfig() throws Exception {
		if (configFile == null) {
			throw new Exception("No configuration file given");
		}
		logger.info("Loading configuration from '" + configFile + "'");
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(configFile));

			NodeList nl = doc.getDocumentElement().getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					String nodeName = node.getNodeName();
					logger.trace("found node " + nodeName);
					if (nodeName.equals("reportfile")) {
						reportFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
					}
					if (nodeName.equals("bdisimfile")) {
						bdiSimFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
					}
					if (nodeName.equals("matsimfile")) {
						matSimFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
					}
					if (nodeName.equals("firefile")) {
						fireFile = node.getAttributes().getNamedItem("name")
								.getNodeValue();
						fireFireCoordinateSystem = node.getAttributes()
								.getNamedItem("coordinates").getNodeValue();
					}
					if (nodeName.equals("controllerfile")) {
						controllerFile = node.getAttributes()
								.getNamedItem("name").getNodeValue();
					}
					if (nodeName.equals("geographyfile")) {
						geographyFile = node.getAttributes()
								.getNamedItem("name").getNodeValue();
						coordinate_system = node.getAttributes()
								.getNamedItem("coordinates").getNodeValue();
					}
					if (nodeName.equals("port") && useGUI) {
						String p = node.getAttributes().getNamedItem("number")
								.getNodeValue();
						port = Integer.parseInt(p);
						try {
							String d = node.getAttributes()
									.getNamedItem("die_on_disconnect")
									.getNodeValue();
							dieOnDisconnect = Boolean.parseBoolean(d);
						} catch (Exception e) {
						}
					}
					if (nodeName.equals("bdiagents")) {
						String n = node.getAttributes().getNamedItem("number")
								.getNodeValue();
						try {
							numBDIAgents = Integer.parseInt(n);
						} catch (Exception e) {
							System.err
									.println("WARNING: Could not read number of BDI agents from configuration file (will use default '"
											+ numBDIAgents
											+ "'): "
											+ e.getMessage());
						}
					}
					if (nodeName.equals("demographics")) {

						String k = node.getAttributes().getNamedItem("kids")
								.getNodeValue();
						proportionWithKids = Double.parseDouble(k);
						String r = node.getAttributes()
								.getNamedItem("relatives").getNodeValue();
						proportionWithRelatives = Double.parseDouble(r);
						String d = node.getAttributes()
								.getNamedItem("max_distance_to_relatives")
								.getNodeValue();
						maxDistanceToRelatives = Integer.parseInt(d);
					}
					if (nodeName.equals("evac_info")) {

						String shelterDistance = node.getAttributes().getNamedItem("ad_hoc_evac_dist")
								.getNodeValue();
						setDistanceToNewShelters(Integer.parseInt(shelterDistance));
						String safeDistance = node.getAttributes().getNamedItem("safe_distance_to_fire")
								.getNodeValue();
						setSafeDistance(Integer.parseInt(safeDistance));						
					}
					if (nodeName.equals("evac_delay")) {

						String k = node.getAttributes().getNamedItem("min")
								.getNodeValue();
						evacDelay[0] = Double.parseDouble(k);
						String r = node.getAttributes().getNamedItem("max")
								.getNodeValue();
						evacDelay[1] = Double.parseDouble(r);
					}
					if (nodeName.equals("image")) {
						try {
							String w = node.getAttributes()
									.getNamedItem("west").getNodeValue();
							String e = node.getAttributes()
									.getNamedItem("east").getNodeValue();
							String n = node.getAttributes()
									.getNamedItem("north").getNodeValue();
							String s = node.getAttributes()
									.getNamedItem("south").getNodeValue();
							image.file = node.getAttributes()
									.getNamedItem("file").getNodeValue();
							image.west = Double.parseDouble(w);
							image.east = Double.parseDouble(e);
							image.north = Double.parseDouble(n);
							image.south = Double.parseDouble(s);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			throw new Exception("while reading config: " + e.getMessage());
		}
		if (controllerFile != null) {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new FileInputStream(controllerFile));

				NodeList nl = doc.getDocumentElement().getChildNodes();
				for (int i = 0; i < nl.getLength(); i++) {
					Node node = nl.item(i);

					if (node instanceof Element) {

						String nodeName = node.getNodeName();
						logger.trace("found node " + nodeName);

						if (nodeName.equals("bypassing")) {

							String s = node.getAttributes()
									.getNamedItem("bypass").getNodeValue();
							bypassController = Boolean.parseBoolean(s);
							s = node.getAttributes()
									.getNamedItem("userControl").getNodeValue();
						}
						if (nodeName.equals("choicePoints")) {
							NodeList choicePointNodes = node.getChildNodes();
							for (int j = 0; j < choicePointNodes.getLength(); j++) {
								readChoicePoint(choicePointNodes.item(j));
							}
						}
					}
				}
			} catch (Exception e) {
				throw new Exception("unable to read controller file '"
						+ controllerFile + "' :" + e.getMessage());
			}
		} else {
			bypassController = true;
		}

		logger.debug("matSimFilefile = " + matSimFile);
		logger.debug("bdiSimFile = " + bdiSimFile);
		logger.debug("fireFile = " + fireFile);
		logger.debug("geographyFile = " + geographyFile);

		// TODO should check existence of all files and stop gracefully
		// TODO should also allow some files to not exist and setup
		// functionality accordingly
		// eg if no controller file, do not initiate controller

		if (geographyFile != null) {
			if (readGeography()) {
				printLocations();
				printRegions();
				printRoutes();
				printShelters();
			} else {
				return false;
			}
		}

		if (matSimFile == null) {
			return false;
		}
		return true;
	}

	private static boolean readChoicePoint(Node node) {
		if (node instanceof Element) {
			String nodeName = node.getNodeName();
			if (nodeName.equals("goal")) {
				try {
					String goal = node.getAttributes().getNamedItem("name").getNodeValue();
					String m = node.getAttributes().getNamedItem("manual").getNodeValue();
					boolean manual = Boolean.parseBoolean(m);
					choicePoints.put(goal, manual);
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * read the config file (xml) and save data
	 * 
	 * @return false if there was a problem
	 * @throws Exception 
	 */
	private static boolean readGeography() throws Exception {
		boolean result = true;
		logger.info("loading geography file " + geographyFile);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(geographyFile));

			NodeList nl = doc.getDocumentElement().getChildNodes();
			List<Node> locationNodes = new ArrayList<Node>();
			List<Node> regionNodes = new ArrayList<Node>();
			List<Node> routeNodes = new ArrayList<Node>();
			List<Node> reliefCentreNodes = new ArrayList<Node>();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					String nodeName = node.getNodeName();
					logger.trace("found node " + nodeName);
					if (nodeName.equals("location")) {
						locationNodes.add(node);
					}
					if (nodeName.equals("region")) {
						regionNodes.add(node);
					}
					if (nodeName.equals("route")) {
						routeNodes.add(node);
					}
					if (nodeName.equals("relief_centre")) {
						reliefCentreNodes.add(node);
					}
				}
			}
			for (Node n : locationNodes)
				if (!readLocation(n)) {
					result = false;
				}
			for (Node n : routeNodes)
				if (!readRoute(n)) {
					result = false;
				}
			for (Node n : reliefCentreNodes)
				if (!readReliefCentre(n)) {
					result = false;
				}
			for (Node n : regionNodes)
				if (!readRegion(n)) {
					result = false;
				}
		} catch (Exception e) {
			throw new Exception("Unable to read geography file " + geographyFile);
		}
		if (locations.size() == 0) {
			throw new Exception("No locations configured in " + geographyFile);
		}
		if (reliefCentres.size() == 0) {
			throw new Exception("No relief centres configured in " + geographyFile);
		}

		return result;

	}

	/**
	 * read the list of locations and store them.
	 * 
	 * @param parent
	 *            The node containing the list
	 * @return false if there was a problem
	 * @throws Exception 
	 */
	private static boolean readLocation(Node node) throws Exception {
		boolean result = true;
		try {
			String name = node.getAttributes().getNamedItem("name")
					.getNodeValue();
			String type = node.getAttributes().getNamedItem("type")
					.getNodeValue();
			String eastStr = node.getAttributes().getNamedItem("easting")
					.getNodeValue();
			String northStr = node.getAttributes().getNamedItem("northing")
					.getNodeValue();
			double easting = Double.parseDouble(eastStr);
			double northing = Double.parseDouble(northStr);
			Location l = new Location(name, type, easting, northing);
			locations.put(name, l);
		} catch (Exception e) {
			throw new Exception("Could not read location from config file: "
					+ e.getMessage());
		}
		return result;
	}

	/**
	 * read a region from the geography file and store it
	 * 
	 * @param parent
	 * @return
	 * @throws Exception 
	 */
	private static boolean readRegion(Node parent) throws Exception {
		boolean result = true;
		Region region = new Region();
		NodeList nl = parent.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				String nodeName = node.getNodeName();
				if (nodeName.equals("data")) {
					try {
						String name = node.getAttributes().getNamedItem("name")
								.getNodeValue();
						String eastStr = node.getAttributes()
								.getNamedItem("easting").getNodeValue();
						String northStr = node.getAttributes()
								.getNamedItem("northing").getNodeValue();
						String populationStr = node.getAttributes()
								.getNamedItem("population").getNodeValue();
						int population = Integer.parseInt(populationStr);
						if(((Element) node).hasAttribute("regionid")){
							String regionId = node.getAttributes()
									.getNamedItem("regionid").getNodeValue();
							region.setRegionId(regionId);
						}
						double easting = Double.parseDouble(eastStr);
						double northing = Double.parseDouble(northStr);
						region.setName(name);
						region.setCentre(easting, northing);
						region.setPopulation(population);
						regions.put(name, region);
					} catch (Exception e) {
						throw new Exception("Could not parse <data> node in config file: "
								+ e.getMessage());
					}
				}
				if (nodeName.equals("polygon")) {
					if (region != null) {
						readPolygon(node, region);
					}
				}
			}
		}
		return result;
	}

	// get region encompassing a particular location
	public static Region getRegion(double easting, double northing) {
		Region smallest = null;
		for (Region r : regions.values()) {
			// TODO this check assumes regions are rectangular - may want to
			// improve this later
			if (easting > r.getWestEdge() && easting < r.getEastEdge()
					&& northing > r.getSouthEdge()
					&& northing < r.getNorthEdge()) {
				if (smallest == null || r.getArea() < smallest.getArea()) {
					smallest = r;
				}
			}
		}
		return smallest;
	}

	private static void readPolygon(Node parent, Region r) throws Exception {
		NodeList nl = parent.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				String nodeName = node.getNodeName();
				if (nodeName.equals("vertex")) {
					try {
						// double[] vertex = new double[2];
						String eastStr = node.getAttributes()
								.getNamedItem("easting").getNodeValue();
						String northStr = node.getAttributes()
								.getNamedItem("northing").getNodeValue();
						double easting = Double.parseDouble(eastStr);
						double northing = Double.parseDouble(northStr);
						r.add(new double[] { easting, northing });
					} catch (Exception e) {
						throw new Exception("Could not parse <vertex> data: "
								+ e.getMessage());
					}
				}
			}
		}
	}

	private static void printRegions() {
		for (String s : regions.keySet()) {
			Region r = regions.get(s);
			logger.debug("region " + r.toString() + ":" + r.getArea() + " shelters: " +r.getViableReliefCentres());
		}
	}
	
	private static void printLocations(){
		for (String key : locations.keySet()) {
			Location l = locations.get(key);
			logger.debug("location " + l.getName() + ":" + l.getCoordinates());
		}
	}
	
	private static void printShelters(){
		for (String key : reliefCentres.keySet()) {
			ReliefCentre r = reliefCentres.get(key);
			logger.debug("Shelter " + r.getName() + ":" + r.getCapacity());
		}
	}

	private static boolean readRoute(Node parent) {
		boolean result = true;
		NodeList nl = parent.getChildNodes();
		String name = "";
		String shelter = "";
		String region = "";
		String desc = "";
		List<String> routeNames = new ArrayList<String>();
		List<Coordinate> routeCoords = new ArrayList<Coordinate>();

		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				String nodeName = node.getNodeName();
				if (nodeName.equals("data")) {
					try {
						name = node.getAttributes().getNamedItem("name")
								.getNodeValue();
						shelter = node.getAttributes().getNamedItem("shelter")
								.getNodeValue();
						region = node.getAttributes().getNamedItem("region")
								.getNodeValue();
						desc = node.getAttributes().getNamedItem("description")
								.getNodeValue();

					} catch (Exception e) {
						result = false;
					}
				}
				if (nodeName.equals("path")) {
					readPath(node, routeNames, routeCoords);
				}
			}
		}

		if (result) {
			routes.put(name, new Route(name, region, shelter, desc, routeNames,
					routeCoords));
		}

		return result;
	}

	private static void readPath(Node parent, List<String> routeNames,
			List<Coordinate> routeCoords) {
		NodeList nl = parent.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				String nodeName = node.getNodeName();
				if (nodeName.equals("waypoint")) {
					try {
						String s = node.getAttributes().getNamedItem("name")
								.getNodeValue();
						if (locations.containsKey(s)) {
							Location l = getLocation(s);
							routeNames.add(s);
							routeCoords.add(new Coordinate(l.getEasting(), l
									.getNorthing()));
						} else {
							logger.warn("Waypoint '" + s
									+ "' is not in the list of known locations");
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}

	private static boolean readReliefCentre(Node parent) throws Exception {
		boolean result = true;
		NodeList nl = parent.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				String nodeName = node.getNodeName();
				if (nodeName.equals("data")) {
					/**
					 * All the attributes are currently required
					 */
					try {
						String name = node.getAttributes().getNamedItem("name")
								.getNodeValue();
						String location = node.getAttributes()
								.getNamedItem("location").getNodeValue();
						Coordinate locationCoords = new Coordinate();
						
						if(locations.containsKey(location)){
							Location l = locations.get(location);
							locationCoords = new Coordinate(l.getEasting(), l.getNorthing());
						} else{
							logger.warn("Shelter location '" + location
									+ "' is not in the list of known locations");
							result = false;
						}
						
						String capacityString = node.getAttributes()
								.getNamedItem("capacity").getNodeValue();
						int capacity = Integer.parseInt(capacityString);

						ReliefCentre r = new ReliefCentre(name, location, locationCoords,
								capacity);
						reliefCentres.put(name, r);
					} catch (Exception e) {
						throw new Exception("An error occured reading a relief_centre from the config file");
					}
				}
			}
		}
		return result;
	}

	private static void printRoutes() {
		for (String s : routes.keySet()) {
			logger.debug("found route " + s);
		}
	}

	public static int getDistanceToNewShelters() {
		return distanceToNewShelters;
	}

	public static void setDistanceToNewShelters(int distanceToNewShelters) {
		SimpleConfig.distanceToNewShelters = distanceToNewShelters;
	}

	public static int getSafeDistance() {
		return safeDistance;
	}

	public static void setSafeDistance(int safeDistance) {
		SimpleConfig.safeDistance = safeDistance;
	}
}
