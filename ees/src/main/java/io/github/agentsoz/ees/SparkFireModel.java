package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
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

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.Time;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class SparkFireModel implements DataSource<Geometry> {

    private final Logger logger = LoggerFactory.getLogger(SparkFireModel.class);

    // Model options in ESS config XML
    private final String eIgnitionHHMM = "ignitionHHMM";
    private final String eCsvFile = "csv";
    private final String eCsvDelimiter = "csvDelimiter";
    private final String eGridSizeInMetres = "gridSizeInMetres";

    // Model options' values
    private String optCsvFile = null;
    private int optGridSizeInMetres = -1;
    private String optCsvDelimiter = ",";
    private String optCrs = "EPSG:4326";

    private TreeMap<Double, Geometry> fire;
    private DataServer dataServer = null;
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private double startTimeInSeconds = -1;
    private double ignitionTimeInSecs = 0;

    public SparkFireModel(Map<String, String> opts, DataServer dataServer) {
        fire = new TreeMap<>();
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
                case eCsvFile:
                    optCsvFile = opts.get(opt);
                    break;
                case eCsvDelimiter:
                    optCsvDelimiter = opts.get(opt);
                    break;
                case eGridSizeInMetres:
                    optGridSizeInMetres = Integer.parseInt(opts.get(opt));
                    break;
                case Config.eGlobalStartHhMm:
                    String[] tokens = opts.get(opt).split(":");
                    setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
                    break;
                case Config.eGlobalCoordinateSystem:
                    optCrs = opts.get(opt);
                    break;
                case eIgnitionHHMM:
                    String[] hhmm = opts.get(opt).split(":");
                    ignitionTimeInSecs = Time.convertTime(Integer.parseInt(hhmm[0]), Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
                            + Time.convertTime(Integer.parseInt(hhmm[1]), Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
                    break;
                default:
                    logger.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }
        if (optGridSizeInMetres == -1) {
            new RuntimeException("Spark model required option '" + eGridSizeInMetres + "' not found");
        }
    }

    public void setStartHHMM(int[] hhmm) {
        startTimeInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, timestepUnit)
                + Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, timestepUnit);
    }

     @Override
    public Geometry sendData(double forTime, String dataType) {
        double time = Time.convertTime(forTime, timestepUnit, Time.TimestepUnit.SECONDS);
        if (Constants.FIRE_DATA.equals(dataType)) {
            SortedMap<Double, Geometry> shapes = fire.subMap(0.0, time);
            Geometry shape = getGeometry(shapes);
            shape = (shape==null) ? null : new ConvexHull(shape).getConvexHull();
            while (shapes.size() > 1) {
                fire.remove(fire.firstKey());
            }
            Double nextTime = fire.higherKey(time);
            if (nextTime != null) {
                dataServer.registerTimedUpdate(Constants.FIRE_DATA, this, Time.convertTime(nextTime, Time.TimestepUnit.SECONDS, timestepUnit));
            }
            logger.debug("sending fire data at time {}: {}", forTime, shape);
            return shape;

        }
        return null;
    }

    private Geometry getGeometry(SortedMap<Double, Geometry> shapes) {
        Geometry polygon = null;
        if (shapes != null && !shapes.isEmpty()) {
            for (Geometry shape : shapes.values()) {
                // Fix for JTS #288 requires reduction to floating.
                // https://github.com/locationtech/jts/issues/288#issuecomment-396647804
                polygon = (polygon==null) ?
                        shape :
                        GeometryPrecisionReducer.reduce(polygon.union(shape),new PrecisionModel(PrecisionModel.FLOATING));
            }
        }
        return polygon;
    }

    private Geometry getGeometryFromSquareCentroids(MathTransform utmTransform, List<Location> centroids, double squareSideInMetres) throws TransformException {
        Geometry shape = null ;
        int i = 0;
        for (Location centroid : centroids) {
            double delta = squareSideInMetres/2;
            Coordinate coord = new Coordinate(centroid.getX(), centroid.getY());
            JTS.transform(coord, coord, utmTransform);
            Coordinate cornerA = new Coordinate(coord.getX()-delta, coord.getY()-delta);
            Coordinate cornerB = new Coordinate(coord.getX()+delta, coord.getY()+delta);
            Geometry gridCell = new GeometryBuilder().box(
                    cornerA.getX(), cornerA.getY(),
                    cornerB.getX(), cornerB.getY());
            // Fix for JTS #288 requires reduction to floating.
            // https://github.com/locationtech/jts/issues/288#issuecomment-396647804
            shape = (shape==null) ?
                    gridCell :
                    GeometryPrecisionReducer.reduce(shape.union(gridCell),new PrecisionModel(PrecisionModel.FLOATING));
        }
        return shape;
    }

    /**
     * Start publishing embers data
     */
    public void start() {
        if (optCsvFile != null && !optCsvFile.isEmpty()) {
            try {
                logger.info("Loading Spark fire from " + optCsvFile);
                loadSparkCsv(optCsvFile);
                dataServer.registerTimedUpdate(Constants.FIRE_DATA, this, startTimeInSeconds);
            } catch (Exception e) {
                throw new RuntimeException("Could not load Spark fire data from [" + optCsvFile + "]", e);
            }
        } else {
            logger.warn("started but will be idle forever!!");
        }
    }

    /**
     * Set the time step unit for this model
     * @param unit the time step unit to use
     */
    void setTimestepUnit(Time.TimestepUnit unit) {
        timestepUnit = unit;
    }

    public void loadSparkCsv(String file) throws Exception {
        MathTransform utmTransform = CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode(optCrs), false);

        BufferedReader reader = new BufferedReader((file.endsWith(".gz")) ?
                new InputStreamReader(new GZIPInputStream(new FileInputStream(file))) :
                new FileReader(file));
        Map<Double, List<Location>> map = new TreeMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split(optCsvDelimiter);
            Location centroid = new Location(
                    columns[2], // using time as name
                    Double.parseDouble(columns[1]),
                    Double.parseDouble(columns[0]));
            Double time = Double.parseDouble(columns[2]);
            if (time >= 0) {
                List<Location> centroids = (map.containsKey(time)) ?
                        map.get(time) : new ArrayList<>();
                centroids.add(centroid);
                map.put(time, centroids);
            }
        }

        for (double time : map.keySet()) {
            List<Location> centroids = map.get(time);
            Geometry shape = getGeometryFromSquareCentroids(utmTransform, centroids, optGridSizeInMetres);
            double secs = ignitionTimeInSecs + time;
            fire.put(secs, shape);
        }
    }
}
