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
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class DeckglTripsData {

    private static final Logger log = LoggerFactory.getLogger(DeckglTripsData.class);

    private Map<String, List<List<Double>>> coordsMap;
    private Map<String, List<Integer>> timesMap;
    private Map<String, List<List<Integer>>> coloursMap;
    private final CoordinateTransformation ct;

    private final Color colorGreen = new Color(0, 255, 0);
    private final Color colorRed = new Color(255, 0, 0);
    private final Color colorAmber = new Color(255, 191, 0);

    public DeckglTripsData(String crs) {
        coordsMap = new HashMap<>();
        timesMap = new HashMap<>();
        coloursMap = new HashMap<>();
        ct = new GeotoolsTransformation(crs, "EPSG:4326");
    }

    public void addEvent(Integer timeInSecs, String vehicleId, Coord coord, double relativeSpeed) {
        if (timeInSecs == null || vehicleId == null || coord == null) {
            log.warn("Ignoring invalid DeckGl event: " +
                    "timeInSecs=["+timeInSecs+"]" +
                    "vehicleId=["+vehicleId+"]" +
                    "coord=["+coord+"]");
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

        Coord xy = ct.transform(coord);
        coords.add(Arrays.asList(xy.getX(), xy.getY()));
        coordsMap.put(vehicleId, coords);

        // add the colour to the vehicle's path colours
        List<List<Integer>> colours = coloursMap.get(vehicleId);
        if (colours == null) { colours = new ArrayList<>(); }
        Color colour = mixColors(colorRed, colorAmber, colorGreen, relativeSpeed);
        colours.add(Arrays.asList(colour.getRed(), colour.getGreen(), colour.getBlue()));
        coloursMap.put(vehicleId, colours);

    }

    public DeckglTrip[] trips() {
        String[] vehicles = coordsMap.keySet().toArray(new String[0]);
        Arrays.sort(vehicles);
        DeckglTrip[] trips = new DeckglTrip[vehicles.length+1];
        int minStart = Integer.MAX_VALUE, maxFinish = Integer.MIN_VALUE;
        for(int i = 0; i < vehicles.length; i++) {
            int start = Integer.MAX_VALUE, finish = Integer.MIN_VALUE;
            Integer[] times = timesMap.get(vehicles[i]).toArray(new Integer[0]);
            Arrays.sort(times);
            if (times[0] < start) start = times[0];
            if (times[times.length-1] > finish) finish = times[times.length-1];
            trips[i] = new DeckglTrip(start, finish, vehicles[i],
                    coordsMap.get(vehicles[i]), timesMap.get(vehicles[i]), coloursMap.get(vehicles[i]));
            if (minStart > start) minStart = start;
            if (maxFinish < finish) maxFinish = finish;
        }
        trips[trips.length-1] = new DeckglTrip(minStart, maxFinish, "",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        return trips;
    }

    public void saveToFile(String str) {
        if (str == null || str.isEmpty()) return;
        saveToFile(str, trips());
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

    /**
     * Interpolates between low and high colour, optionally using an interim mid colour.
     * @param low
     * @param mid
     * @param high
     * @param highPercent
     * @return
     */
    public Color mixColors(Color low, Color mid, Color high, double highPercent){
        if (highPercent < 0 || highPercent > 1 || low == null || high == null) {
            return new Color(255, 0, 255);
        }
        double hp = (highPercent <= 0.5) ? highPercent/0.5 : (highPercent-0.5)/0.5;
        double lp = 1.0 - hp;
        Color c1 = (highPercent <= 0.5) ? low : ((mid != null) ? mid : low);
        Color c2 = (highPercent <= 0.5) ? ((mid != null) ? mid : high) : high;
        int r = (int) (c1.getRed()*lp + c2.getRed()*hp);
        int g = (int) (c1.getGreen()*lp + c2.getGreen()*hp);
        int b = (int) (c1.getBlue()*lp + c2.getBlue()*hp);
        Color c3 = new Color(r, g, b);
        return c3;
    }

    public static void main(String[] args) {
        DeckglTripsData deckglTripsData = new DeckglTripsData("EPSG:4326");
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

        public DeckglTrip(Integer start,
                          Integer finish,
                          String vehicle,
                          List<List<Double>> path,
                          List<Integer> timestamps,
                          List<List<Integer>> colours) {
            this.start = start;
            this.finish = finish;
            this.vehicle = vehicle;
            this.path = path;
            this.timestamps = timestamps;
            this.colours = colours;
        }
    }

}
