package io.github.agentsoz.ees;

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

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Time;
import org.geotools.geometry.jts.GeometryBuilder;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class SparkFireModel implements DataSource<Geometry> {

    private final Logger logger = LoggerFactory.getLogger(SparkFireModel.class);

    // Model options in ESS config XML
    private final String eIgnitionHHMM = "ignitionHHMM";
    private final String eCsvFile = "csv";

    // Model options' values
    private String optCsvFile = null;

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
                case Config.eGlobalStartHhMm:
                    String[] tokens = opts.get(opt).split(":");
                    setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
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

    private Geometry getGeometryFromSquareCentroids(Double[][] pairs, double squareSideInMetres) {
        Geometry shape = null ;
        int i = 0;
        for (Double[] pair : pairs) {
            double delta = squareSideInMetres/2;
            Geometry gridCell = new GeometryBuilder().box(
                    pair[0]-delta, pair[1]-delta,
                    pair[0]+delta, pair[1]+delta);
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

    public void loadSparkCsv(String filename) throws IOException {
     }



}
