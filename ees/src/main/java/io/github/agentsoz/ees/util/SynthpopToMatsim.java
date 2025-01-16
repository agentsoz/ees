package io.github.agentsoz.ees.util;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
