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

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPInputStream;



public class CycloneModel implements DataSource<Geometry[]> {

    // Model options in ESS config XML
    private final String efileGeoJson = "fileGeoJson";
    private final String eoffsetFromSimStart = "offsetFromSimStart";
    private final Logger logger = LoggerFactory.getLogger(CycloneModel.class);


    // Model options' values
    private String optGeoJsonFile = null;
    private JSONObject json = null;
    private String optOffsetFromSimStart = null ;
    private Date startDate = null ;
    private TreeMap<Double, ArrayList<Geometry>> cyclone;
    private String optCrs = "EPSG:28356";
    private String cycloneGeoJsonCRS = "EPSG:4326";
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private double startTimeInSeconds = -1;
    private DataServer dataServer = null;


    public CycloneModel(Map<String, String> opts, DataServer dataServer) {
        cyclone = new TreeMap<>();
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
                case eoffsetFromSimStart:
                    optOffsetFromSimStart = opts.get(opt);
                    break;
                case Config.eGlobalStartHhMm:
                    String[] tokens = opts.get(opt).split(":");
                    setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
                    break;
                case Config.eGlobalCoordinateSystem:
                    optCrs = opts.get(opt);
                    break;
                default:
                    logger.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }

    }


    /**
     * Start publishing cyclone data
     */
    public void start() {
        if (optGeoJsonFile != null && !optGeoJsonFile.isEmpty()) {
            try {
                loadCycloneFileGeoJson(optGeoJsonFile);
                dataServer.registerTimedUpdate(Constants.CYCLONE_DATA, this, startTimeInSeconds);
            } catch (Exception e) {
                throw new RuntimeException("Could not load cyclone data from [" + optGeoJsonFile + "]", e);
            }
        }
        else if (json==null) {
            logger.warn("started but will be idle forever!!");
        }
    }

    private void loadCycloneFileGeoJson(String file) throws Exception {
        logger.info("Loading GeoJSON file: " + file);
        MathTransform utmTransform = CRS.findMathTransform(CRS.decode(cycloneGeoJsonCRS), CRS.decode(optCrs), false);

        // Create the JSON parsor
        JSONParser parser = new JSONParser();
        Reader reader = (file.endsWith(".gz")) ?
                new InputStreamReader(new GZIPInputStream(new FileInputStream(file))) :
                new FileReader(file);
        // Read in the JSON file
        json = (JSONObject) (parser.parse(reader));
        // close the reader
        reader.close();
        // Loop through the features (which contains the time-stamped cyclone
        // shapes)
        JSONArray features = (JSONArray) json.get("features");
        Iterator<JSONObject> iterator = features.iterator();

        while (iterator.hasNext()) {
            JSONObject feature = iterator.next();
            JSONObject properties = (JSONObject) feature.get("properties");
            String timestamp = (String) properties.get("timestamp");
            JSONObject geometry = (JSONObject) feature.get("geometry");
            JSONArray jcoords = (JSONArray) geometry.get("coordinates");


            // create cyclone map
            if (timestamp != null) {
                double secs = getTimeInSeconds(timestamp);

                if(!cyclone.containsKey(secs)) {
                    ArrayList<Geometry> polygonList = new  ArrayList<Geometry>();
                    cyclone.put(secs,polygonList);
                }
                Iterator<JSONArray> it = jcoords.iterator();
                while(it.hasNext()){
                    Geometry shape = getGeometryFromCoords(getPolygonCoordinates(it.next()));
                    ArrayList<Geometry> polyList = cyclone.get(secs);
                    polyList.add(shape);
                }

            }

        }
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
    public Geometry[] sendData(double timestep, String dataType) {
        double time = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.SECONDS);
        ArrayList<Geometry> polygonList = new ArrayList<Geometry>();


        if (Constants.CYCLONE_DATA.equals(dataType)) {
            SortedMap<Double, ArrayList<Geometry>> shapes = cyclone.subMap(0.0, time);
            for (ArrayList<Geometry> list : shapes.values()) { // add all shapes into a list
                polygonList.addAll(list);
            }
//            Geometry shape = getGeometry(shapes);
//            shape = (shape==null) ? null : new ConvexHull(shape).getConvexHull();
            while (shapes.size() >= 1) {
                cyclone.remove(cyclone.firstKey());
            }
            Double nextTime = cyclone.higherKey(time);
            if (nextTime != null) {
                dataServer.registerTimedUpdate(Constants.CYCLONE_DATA, this, Time.convertTime(nextTime, Time.TimestepUnit.SECONDS, timestepUnit));
            }
            logger.info("sending {} cyclone polygons at time {}", polygonList.size(), timestep);
            return  polygonList.toArray(new Geometry[polygonList.size()]);

        }
        return null;
    }

    // returns a 2D array of all json polygon points
    private Double[][] getPolygonCoordinates(JSONArray jcoords) {
        Double[][] coordinates = new Double[jcoords.size()][2];
        // has a [[[x,y],[x,y]]] form ie one extra nesting
        Iterator<JSONArray> it = jcoords.iterator();
        int i = 0;
        while (it.hasNext()) {
            coordinates[i++] = (Double[]) it.next().toArray(new Double[2]);
//            Iterator<JSONArray> cit = obj.iterator();
//            while (cit.hasNext()) {
//                coordinates[i++] = (Double[]) cit.next().toArray(new Double[2]);
//            }
//            break; // get the first element only of outer array
        }
        return coordinates;
    }



    private Geometry getGeometryFromCoords(Double[][] pairs) throws Exception{
        MathTransform utmTransform = CRS.findMathTransform(CRS.decode(cycloneGeoJsonCRS), CRS.decode(optCrs), false);

        int i = 0;
        double[] flatarray = new double[pairs.length*2];
        int o = 0;
        for (Double[] pair : pairs) {

            Coordinate coord = new Coordinate(pair[1],pair[0]); // lat,lon
            JTS.transform(coord, coord, utmTransform); // transform EPSG:4326 to global CRS EPSG: 28356 (EPSSG:7856)
            flatarray[i++] = coord.getX();
            flatarray[i++] = coord.getY();
        }
        return new GeometryBuilder().polygon(flatarray);
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
