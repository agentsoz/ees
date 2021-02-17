package io.github.agentsoz.ees.util;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2021 by its authors. See AUTHORS file.
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;

public class SynthpopToMatsim {

    private static String inputCsvGzip = null;
    private static String matsimPopulationFile = "plans.xml";
    private static String outputCoordinateSystem = "EPSG:4326";
    private static String inputCoordinateSystem = "EPSG:4326";

    private static String usage() {
        return "usage: "
                + SynthpopToMatsim.class.getName()
                + "  [options] \n"
                + "   --incsv FILE     Synthetic population CSV to convert (.csv.gz from https://github.com/agentsoz/synthetic-population)\n"
                + "   --outxml FILE    Output file in MATSim plans XML format (default is '"+matsimPopulationFile+"')\n"
                + "   --incrs STRING   Coordinate reference system of input (default is '"+ inputCoordinateSystem +"')\n"
                + "   --outcrs STRING  Coordinate reference system to use for generated output (default is '"+ outputCoordinateSystem +"')\n"
                ;
    }

    private static void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--incsv":
                    if (i + 1 < args.length) {
                        i++;
                        inputCsvGzip = Paths.get(args[i]).toAbsolutePath().normalize().toString();
                    }
                    break;
                case "--outxml":
                    if (i + 1 < args.length) {
                        i++;
                        matsimPopulationFile = Paths.get(args[i]).toAbsolutePath().normalize().toString();
                    }
                    break;
                case "--incrs":
                    if (i + 1 < args.length) {
                        i++;
                        inputCoordinateSystem = args[i];
                    }
                    break;
                case "--outcrs":
                    if (i + 1 < args.length) {
                        i++;
                        outputCoordinateSystem = args[i];
                    }
                    break;
                default:
                    break;
            }
        }
        if (inputCsvGzip == null) {
            abort(null);
        }
    }

    private static void abort(String err) {
        if (err != null && !err.isEmpty()) {
            System.err.println("\nERROR: " + err + "\n");
        }
        System.out.println(usage());
        System.exit(0);
    }

    public static void main(String[] args) {
        parse(args);

        Iterable<CSVRecord> records = null;
        CSVFormat format = CSVFormat.DEFAULT.withQuote('\"').withFirstRecordAsHeader();
        try {
            records = format.parse(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputCsvGzip))));
        } catch (IOException e) {
            e.printStackTrace();
            abort("Could not read input CSV file '"+ inputCsvGzip +"'");
        }


        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        CoordinateTransformation ct = new GeotoolsTransformation(
                inputCoordinateSystem,
                outputCoordinateSystem);

        int personId = 0;
        for (CSVRecord record : records) {
            Map<String,String> map = record.toMap();
            String[] carr = map.get("Geographical.Coordinate")
                    .replaceAll("[\\[\\]]", "")
                    .trim()
                    .split(",");
            Coord coord = ct.transform(new Coord(Double.valueOf(carr[0]), Double.valueOf(carr[1])));
            Person person = createPersonWithActivity(
                    Integer.toString(personId++),
                    "home",
                    coord.getX(),
                    coord.getY(),
                    (23*60*60)+(59*60)+59,
                    scenario.getPopulation().getFactory());
            for (String key : map.keySet()) {
                String value = map.get(key);
                if ("HasDependentsAtLocation".equals(key)) {
                    try {
                        String[] loc = map.get(key)
                                .replaceAll("[\\[\\]]", "")
                                .trim()
                                .split(",");
                        Coord coords = ct.transform(new Coord(Double.valueOf(loc[0]), Double.valueOf(loc[1])));
                        value = String.format("[%f,%f]", coord.getX(), coords.getY());

                    } catch (Exception e) {
                    }
                }
                person.getAttributes().putAttribute(key, value) ;
            }
            //person.getAttributes().putAttribute("BDIAgentType", "io.github.agentsoz.ees.agents.bushfire.Resident" );

            //Random rand = new Random(12345);
            //person.getAttributes().putAttribute("EvacLocationPreference", "Elphinstone,262869,5890813" );
            //person.getAttributes().putAttribute("InitialResponseThreshold", rand.nextDouble() );
            //person.getAttributes().putAttribute("FinalResponseThreshold", rand.nextDouble() );

            scenario.getPopulation().addPerson(person);
        }

        // Finally, write this population to file
        MatsimWriter popWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        popWriter.write(matsimPopulationFile);

    }

    public static Person createPersonWithActivity(
            String id,
            String actType,
            double x,
            double y,
            double actEndTime,
            PopulationFactory populationFactory) {

        Coord matSimCoord = new Coord(x,y);

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

        Person person = populationFactory.createPerson(Id.createPersonId(id));
        person.addPlan(plan);
        return person;
    }


}
