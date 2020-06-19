package io.github.agentsoz.ees.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import io.github.agentsoz.util.Coordinates;
import io.github.agentsoz.util.Global;

/*
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2020 by its authors. See AUTHORS file.
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

public class CreatePersonsAtLocations {

	private enum PopulationLocationType {
		UNKNOWN, // Invalid value
		RECT, // Locations specified within a rectangle
		LIST // locations specified as a list in file
	}
	
	
	// Defaults
	private static String outDir = Paths.get(".").toAbsolutePath().normalize().toString();
	private static String matsimScenarioPrefix = "";
	private static String matsimPopulationFile = "population.xml";
	private static String matsimOutputCoordinateSystem = "WSG84";
	private static RectangularArea matsimPopulationLocationArea = null;
	private static boolean verbose = false;
	private static ArrayList<PopulationArea> populationAreas = new ArrayList<PopulationArea>();

	// Keeps the next unique person ID
	private static int nextUniquePersonId = 0;

	public static void main(String[] args) {

		// Parse the command line arguments
		parse(args);

		// Create the scenario with empty population
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		for (PopulationArea parea : populationAreas) {

			// Transformation
			// String from =
			// MGC.getCRS(matsimPopulationInputCoordinateSystem).toWKT();
			// String to = MGC.getCRS(matsimOutputCoordinateSystem).toWKT();
			CoordinateTransformation ct = new GeotoolsTransformation(
					parea.getCoordinateSystem(),
					matsimOutputCoordinateSystem);
			transform(parea.getArea(), ct);

			// Get the list of addresses from area
			ArrayList<Coordinates> locations = getRandomLocationsFrom(parea.getArea(), parea.getPersons(), Global.getRandom());

			// Now place the specified number of agents at work locations
			for (int i = 0; i < locations.size(); i++) {
				// System.out.println("X:" + addresses.get(i).getLongitude() + "
				// | Y:"
				// + addresses.get(i).getLatitude());
				addPersonWithActivity("home", locations.get(i), 21600, scenario);
			}
		}
		// Finally, write this population to file
		MatsimWriter popWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		Path outfile = Paths.get(outDir).toAbsolutePath().resolve(matsimScenarioPrefix + matsimPopulationFile);
		popWriter.write(outfile.toString());
	}

	private static void transform(RectangularArea area, CoordinateTransformation ct) {
		// NOTE: CoordinateTransformation expects LONG first then LAT!!!
		Coord xy1 = ct.transform(new  Coord(area.getX1(), area.getY1()));
		Coord xy2 = ct.transform(new  Coord(area.getX2(), area.getY2()));
		area.setX1(xy1.getX());
		area.setY1(xy1.getY());
		area.setX2(xy2.getX());
		area.setY2(xy2.getY());
	}

	private static ArrayList<Coordinates> getRandomLocationsFrom(RectangularArea area, int num, Random rand) {
		ArrayList<Coordinates> locations = new ArrayList<Coordinates>();
		for (int i = 0; i < num; i++) {
			double x = Math.abs(area.getX1() - area.getX2()) * rand.nextDouble();
			x *= (area.getX1() < area.getX2()) ? 1 : -1;
			x += area.getX1();
			double y = Math.abs(area.getY1() - area.getY2()) * rand.nextDouble();
			y *= (area.getY1() < area.getY2()) ? 1 : -1;
			y += area.getY1();
			
			Coordinates c = new Coordinates(x,y);
			locations.add(c);
		}
		return locations;
	}

	public static void addPersonWithActivity(
			String actType,
			Coordinates coordsOfAct, 
			double actEndTime, 
			Scenario scenario) 
	{
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		Coord matSimCoord = new Coord(coordsOfAct.getLongitude(), coordsOfAct.getLatitude());

		// Create a new plan
		Plan plan = populationFactory.createPlan();

		// Create a new activity with the end time and add it to the plan
		Activity act = populationFactory.createActivityFromCoord(actType, matSimCoord);
		act.setEndTime(actEndTime);
		plan.addActivity(act);

		// Add a new leg to the plan. Needed, otherwise MATSim won't add this
		// leg-less plan to the simulation
		plan.addLeg(populationFactory.createLeg("car"));

		// Create a second activity (all plans must end in an activity)
		act = populationFactory.createActivityFromCoord(actType.toString(), matSimCoord);
		plan.addActivity(act);

		Person person = populationFactory.createPerson(Id.createPersonId(matsimScenarioPrefix + nextUniquePersonId++));
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}


	private static String usage() {
		return "usage: "
				+ CreatePersonsAtLocations.class.getName()
				+ "  [options] \n"
				+ "   -prefix STRING   MATSim scenario prefix used for filenames and person IDs (default is '"+matsimScenarioPrefix+"')\n"
				+ "   -matsimpop       N/WKT/LOC/ARG  places N agents at LOC locations (specified in WKT coordinate system); valid examples are\n"
				+ "                      '10/WSG84/RECT/x1,y1&x2,y2' places 10 agents randomly within rectangle given by x1,y1 and x2,y2\n"
				+ "                      '50/EPSG:3857/LIST/FILE' places 50 agents ay x,y given in FILE (one per line)\n"
				+ "   -wkt STRING      Coordinate system to use for generated MATSim files (default is '"+matsimOutputCoordinateSystem+"')\n"
				+ "   -outdir DIR      Output all files to directory DIR; must exist (default is '"+outDir+"')\n"
				+ "   -verbose BOOL    Be verbose (default is '"+verbose+"')\n"
				;
	}

	private static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-matsimpop":
				if (i + 1 < args.length) {
					i++;
					String err = "Could not parse -matsimpop N,WKT,LOC,ARG ("+args[i]+")";
					try {
						
						Pattern p = Pattern.compile("(\\d+)/(.*)/(RECT|FILE)/(.*),(.*)&(.*),(.*)");
						Matcher m = p.matcher(args[i].toUpperCase());
						m.find();
						int numAgents = Integer.parseInt(m.group(1));
						String coordinateSystem = m.group(2);
						PopulationLocationType locationType = PopulationLocationType.valueOf(m.group(3));
						if (locationType == PopulationLocationType.RECT) {
							double x1 = Double.parseDouble(m.group(4));
							double y1 = Double.parseDouble(m.group(5));
							double x2 = Double.parseDouble(m.group(6));
							double y2 = Double.parseDouble(m.group(7));
							RectangularArea area = new RectangularArea(x1, y1, x2, y2);
							PopulationArea parea = new PopulationArea(area, numAgents, coordinateSystem);
							populationAreas.add(parea);
						} else {
							abort("Population generation from FILE locations not implemented yet.");
						}

					} catch (Exception e) {
						abort(err+"; " + e.getMessage());
					}
				}
				break;
			case "-outdir":
				if (i + 1 < args.length) {
					i++;
					outDir = Paths.get(args[i]).toAbsolutePath().normalize().toString();
				}
				break;
			case "-prefix":
				if (i + 1 < args.length) {
					i++;
					matsimScenarioPrefix = args[i];
				}
				break;
			case "-wkt":
				if (i + 1 < args.length) {
					i++;
					matsimOutputCoordinateSystem = args[i];
				}
				break;
			case "-verbose":
				if (i + 1 < args.length) {
					i++;
					verbose = true;
				}
				break;
			}
		}
	}

	private static void abort(String err) {
		System.err.println("\nERROR: " + err + "\n");
		System.out.println(usage());
		System.exit(0);
	}
}

class PopulationArea {
	private RectangularArea area;
	private int persons;
	private String coordinateSystem;
	
	public PopulationArea(RectangularArea area, int persons, String coordinateSystem) {
		super();
		this.area = area;
		this.persons = persons;
		this.coordinateSystem = coordinateSystem;
	}

	public RectangularArea getArea() {
		return area;
	}
	public void setArea(RectangularArea area) {
		this.area = area;
	}
	public int getPersons() {
		return persons;
	}
	public void setPersons(int persons) {
		this.persons = persons;
	}
	public String getCoordinateSystem() {
		return coordinateSystem;
	}
	public void setCoordinateSystem(String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

	
}

class RectangularArea {
	private double x1;
	private double y1;
	private double x2;
	private double y2;

	/**
	 * Captures a rectangular area between two points x1,y1 and x2,y2
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public RectangularArea(double x1, double y1, double x2, double y2) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2; 
		this.y2 = y2;
	}
	
	public double getX1() {
		return x1;
	}

	public double getY1() {
		return y1;
	}

	public double getX2() {
		return x2;
	}

	public double getY2() {
		return y2;
	}

	public void setX1(double x1) {
		this.x1 = x1;
	}

	public void setY1(double y1) {
		this.y1 = y1;
	}

	public void setX2(double x2) {
		this.x2 = x2;
	}

	public void setY2(double y2) {
		this.y2 = y2;
	}

	public String toString() {
		return x1 + "," + y1 + " " + x2 + "," + y2;
	}
}

