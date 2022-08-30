package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Time;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class FloodModel implements DataSource<Geometry[]> {

    // Model options in ESS config XML
    private final String efileGeoJson = "fileGeoJson";
    private final String etimestampColumnName = "timestampColumnName";
    private final String eoffsetFromSimStart = "offsetFromSimStart";
    private final Logger logger = LoggerFactory.getLogger(FloodModel.class);


    // Model options' values
    private String optGeoJsonFile = null;
    private String optOffsetFromSimStart = null ;
    private String timestampColumnName = "timestamp";
    private JSONObject json = null;
    private TreeMap<Double, ArrayList<Geometry>> flood;
    private LocalDateTime startDate = null ;
    private String optCrs = "EPSG:28356";
    private String floodGeoJsonCRS = "EPSG:4326";
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private double startTimeInSeconds = -1;
    private DataServer dataServer = null;


    public FloodModel(Map<String, String> opts, DataServer dataServer) {
        flood = new TreeMap<>();
        this.dataServer = dataServer;
        parse(opts);
    }


    private void parse(Map<String, String> opts) {
        if (opts == null) {
            return;
        }
        for (String opt : opts.keySet()) {
            logger.info("Found option: {}={}", opt, opts.get(opt));
            switch(opt) {
                case efileGeoJson:
                    optGeoJsonFile = opts.get(opt);
                    break;
                case Config.eGlobalStartHhMm:
                    String[] tokens = opts.get(opt).split(":");
                    setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
                    break;
                case Config.eGlobalCoordinateSystem:
                    optCrs = opts.get(opt);
                    break;
                case etimestampColumnName:
                    timestampColumnName = opts.get(opt);
                    break;
                case eoffsetFromSimStart:
                        optOffsetFromSimStart = opts.get(opt);
                    break;
                default:
                    logger.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }

    }

    /**
     * Start publishing flood data
     */
    public void start() {
        if (optGeoJsonFile != null && !optGeoJsonFile.isEmpty()) {
            try {
                loadFloodFileGeoJson(optGeoJsonFile);
                dataServer.registerTimedUpdate(Constants.FLOOD_DATA, this, startTimeInSeconds);
            } catch (Exception e) {
                throw new RuntimeException("Could not load flood data from [" + optGeoJsonFile + "]", e);
            }
        }
        else if (json==null) {
            logger.warn("started but will be idle forever!!");
        }
    }

    private void loadFloodFileGeoJson(String file) throws Exception {
        logger.info("Loading GeoJSON file: " + file);

        // Create the JSON parsor
        JSONParser parser = new JSONParser();
        Reader reader = (file.endsWith(".gz")) ?
                new InputStreamReader(new GZIPInputStream(new FileInputStream(file))) :
                new FileReader(file);
        // Read in the JSON file
        json = (JSONObject) (parser.parse(reader));
        // close the reader
        reader.close();
        // Loop through the features (which contains the time-stamped flood shapes
        JSONArray features = (JSONArray) json.get("features");
        Iterator<JSONObject> iterator = features.iterator();
        while (iterator.hasNext()) {
            JSONObject feature = iterator.next();
            JSONObject properties = (JSONObject) feature.get("properties");
            String time = (String) properties.get(timestampColumnName);
            JSONObject geometry = (JSONObject) feature.get("geometry");
            JSONArray jcoords = (JSONArray) geometry.get("coordinates");

            //Iterator<JSONArray> it = jcoords.iterator();
            JSONArray jArr = (JSONArray) jcoords.get(0); // first element contains the coords
            Iterator<JSONArray>  it = jArr.iterator();
            Double[][] coordinates = new Double[jArr.size()][2];

            int i = 0;
            while (it.hasNext()) {
                coordinates[i++] = (Double[]) it.next().toArray(new Double[2]);
            }

            if (time != null) {
                double secs = getTimeInSeconds(time);
                //double secs = 0.0; // flood model starts at time 0.0
                if(!flood.containsKey(secs)) {
                    ArrayList<Geometry> polygonList = new  ArrayList<Geometry>();
                    flood.put(secs,polygonList);
                }
                ArrayList<Geometry> list = flood.get(secs);
                list.add(getGeometryFromCoords(coordinates));

            }

        }
    }

    private Geometry getGeometryFromCoords(Double[][] pairs) throws Exception{
        MathTransform utmTransform = CRS.findMathTransform(CRS.decode(floodGeoJsonCRS), CRS.decode(optCrs), false);

        int i = 0;
        double[] flatarray = new double[pairs.length*2];
        int o = 0;
        for (Double[] pair : pairs) {

            Coordinate coord = new Coordinate(pair[1],pair[0]);
            JTS.transform(coord, coord, utmTransform); // transform EPSG:4326 to global CRS EPSG: 28356 (EPSSG:7856)
            flatarray[i++] = coord.getX();
            flatarray[i++] = coord.getY();
        }
        return new GeometryBuilder().polygon(flatarray);
    }



    // timestamp format: HH:MM:SS
    private double getTimeInSeconds(String hhmm) throws Exception{


        double total_secs = 0.0;

        // get hours and minutes
        Double  hh = Double.valueOf(hhmm.split(":")[0]);
        Double  mm = Double.valueOf(hhmm.split(":")[1]);
        total_secs +=  hh * 3600 + mm * 60;

        // now add the time offset
        Double  offsetHH = Double.valueOf(optOffsetFromSimStart.split(":")[0]);
        Double  offsetMM = Double.valueOf(optOffsetFromSimStart.split(":")[1]);
        total_secs +=  offsetHH * 3600 + offsetMM * 60;


        return total_secs;

    }

    @Override
    public Geometry[] sendData(double forTime, String dataType) {

        ArrayList<Geometry> polygonList = new ArrayList<Geometry>();


        if (Constants.FLOOD_DATA.equals(dataType)) {
            double time = Time.convertTime(forTime, timestepUnit, Time.TimestepUnit.SECONDS);
            SortedMap<Double, ArrayList<Geometry>> shapes = flood.subMap(0.0, time);
            logger.debug("sending fire data at time {}: {}", forTime, shapes);

            for (ArrayList<Geometry> list : shapes.values()) {
                polygonList.addAll(list);
            }
            Double nextTime = flood.higherKey(time);
            if (nextTime != null) {
                dataServer.registerTimedUpdate(Constants.FLOOD_DATA, this, Time.convertTime(nextTime, Time.TimestepUnit.SECONDS, timestepUnit));
            }
            logger.info("sending {} flood polygons at time {}", polygonList.size(), forTime);
            return  polygonList.toArray(new Geometry[polygonList.size()]);
        }
        return null;

    }

    public void setStartHHMM(int[] hhmm) {
        startTimeInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, timestepUnit)
                + Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, timestepUnit);
    }

    /**
     * Set the time step unit for this model
     * @param unit the time step unit to use
     */
    void setTimestepUnit(Time.TimestepUnit unit) {
        timestepUnit = unit;
    }


}
