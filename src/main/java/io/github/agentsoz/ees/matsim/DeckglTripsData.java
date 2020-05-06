package io.github.agentsoz.ees.matsim;

/*-
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeckglTripsData {

    private static final Logger log = LoggerFactory.getLogger(DeckglTripsData.class);

    Map<String, List<List<Double>>> coordsMap;
    Map<String, List<Integer>> timesMap;
    Map<String, List<List<Integer>>> coloursMap;

    public DeckglTripsData() {
        coordsMap = new HashMap<>();
        timesMap = new HashMap<>();
        coloursMap = new HashMap<>();
    }

    public void addEvent(Integer timeInSecs, String vehicleId, List<Double> latlon, List<Integer> colour) {
        if (timeInSecs == null ||
                vehicleId == null ||
                latlon == null || latlon.size() != 2 ||
                colour == null || colour.size() != 3) {
            log.warn("Ignoring invalid DeckGl event: " +
                    "timeInSecs=["+timeInSecs+"]" +
                    "vehicleId=["+vehicleId+"]" +
                    "latlon=["+(latlon==null?"null":latlon.size())+"]" +
                    "colour=["+(colour==null?"null":colour.size())+"]");
            return;
        }
        // add the timestamp to the vehicle's path timestamps
        List<Integer> times = timesMap.get(vehicleId);
        if (times == null) { times = new ArrayList<>(); }
        times.add(timeInSecs);
        timesMap.put(vehicleId, times);

        // add the coordinates to the vehicle's path
        List<List<Double>> coords = coordsMap.get(vehicleId);
        if (coords == null) { coords = new ArrayList<>(); }
        coords.add(latlon);
        coordsMap.put(vehicleId, coords);

        // add the colour to the vehicle's path colours
        List<List<Integer>> colours = coloursMap.get(vehicleId);
        if (colours == null) { colours = new ArrayList<>(); }
        colours.add(colour);
        coloursMap.put(vehicleId, colours);

    }

    public void saveToFile(String str) {
        if (str == null || str.isEmpty()) return;
        DeckglTrip[] trips = new DeckglTrip[0];
        saveToFile(str, trips);
    }

    private void saveToFile(String str, DeckglTrip[] trips) {
        try {
            Writer writer = Files.newBufferedWriter(Paths.get(str));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(trips, writer);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DeckglTrip[] loadFromFile(String str) {
        Gson gson = new Gson();
        DeckglTrip[] obj = null;
        try {
            obj = gson.fromJson(new BufferedReader(new FileReader(str)), DeckglTrip[].class);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Invalid JSON syntax in " + str + ": " + e.getMessage());
        } catch (JsonIOException e) {
            throw new RuntimeException("Could not read config from " + str + ": " + e.getMessage());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Config file " + str + " not found: " + e.getMessage());
        }
        return obj;
    }

    public static void main(String[] args) {
        DeckglTripsData deckglTripsData = new DeckglTripsData();
        DeckglTrip[] trips = deckglTripsData.loadFromFile("/Users/sin122/Documents/work/ees/tmp/output_events_maldon.json");
        deckglTripsData.saveToFile("/Users/sin122/Documents/work/ees/tmp/output_events_maldon_2.json", trips);
        System.out.println(trips.length);
    }

    public class DeckglTrip {
        private Integer start;
        private Integer finish;
        private String vehicle;
        private List<List<Double>> path;
        private List<Integer> timestamps;
        private List<List<Integer>> colours;

        public DeckglTrip() {
            path = new ArrayList<>();
            timestamps = new ArrayList<>();
            colours = new ArrayList<>();
        }
    }

}
