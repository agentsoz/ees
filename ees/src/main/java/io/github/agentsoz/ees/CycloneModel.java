package io.github.agentsoz.ees;

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.Time;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

public class CycloneModel implements DataSource<Geometry> {

    // Model options in ESS config XML
    private final String efileGeoJson = "fileGeoJson";
    private final String eGridSquareSideInMetres = "gridSquareSideInMetres";
    private final Logger logger = LoggerFactory.getLogger(CycloneModel.class);


    // Model options' values
    private String optGeoJsonFile = null;
    private JSONObject json = null;

    private TreeMap<Double, Geometry> cyclone;
    private String optCrs = "EPSG:4326";
//    private String cycloneGeoJsonCRS = "EPSG:4326";
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private double startTimeInSeconds = -1;
    private DataServer dataServer = null;
    private double optGridSquareSideInMetres = 180;

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
                case eGridSquareSideInMetres:
                    optGridSquareSideInMetres = Double.parseDouble(opts.get(opt));
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
     * Start publishing embers data
     */
    public void start() {
        if (optGeoJsonFile != null && !optGeoJsonFile.isEmpty()) {
            try {
                loadCycloneFileGeoJson(optGeoJsonFile);
                dataServer.registerTimedUpdate(Constants.CYCLONE_DATA, this, startTimeInSeconds);
            } catch (Exception e) {
                throw new RuntimeException("Could not load cyclone  geojson from [" + optGeoJsonFile + "]", e);
            }
        }
        else if (json==null) {
            logger.warn("started but will be idle forever!!");
        }
    }

    private void loadCycloneFileGeoJson(String file) throws Exception {
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
        // Loop through the features (which contains the time-stamped embers
        // shapes)
        JSONArray features = (JSONArray) json.get("features");
        Iterator<JSONObject> iterator = features.iterator();
        while (iterator.hasNext()) {
            JSONObject feature = iterator.next();
            JSONObject properties = (JSONObject) feature.get("properties");
            String timestamp = (String) properties.get("timestamp");
            JSONObject geometry = (JSONObject) feature.get("geometry");
            JSONArray jcoords = (JSONArray) geometry.get("coordinates");
            if (timestamp != null) {

                //extract time (hhmmss) from timestamp format, e.g.: 2076-02-14T23:45:00.000000000
                double secs = getTimeInSeconds(timestamp.split("T")[1]);
                Geometry shape = getGeometryFromSquareCentroids(getPolygonCoordinates(jcoords), optGridSquareSideInMetres);
                cyclone.put(secs,shape);
            }

        }
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


    @Override
    public Geometry sendData(double timestep, String dataType) {
        double time = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.SECONDS);
        if (Constants.CYCLONE_DATA.equals(dataType)) {
            SortedMap<Double, Geometry> shapes = cyclone.subMap(0.0, time);
            Geometry shape = getGeometry(shapes);
            shape = (shape==null) ? null : new ConvexHull(shape).getConvexHull();
            while (shapes.size() > 1) {
                cyclone.remove(cyclone.firstKey());
            }
            Double nextTime = cyclone.higherKey(time);
            if (nextTime != null) {
                dataServer.registerTimedUpdate(Constants.CYCLONE_DATA, this, Time.convertTime(nextTime, Time.TimestepUnit.SECONDS, timestepUnit));
            }
            logger.debug("sending cyclone data at time {}: {}", timestep, shape);
            return shape;

        }
        return null;
    }


// return the union of all shape submaps for a specific timestamp

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


    // convert hh:mm:ss to seconds
    private double getTimeInSeconds(String hhmmss) {
        String[] time = hhmmss.split(":"); // remove decimals and split into hh,mm and ss
        double hh =     Time.convertTime(Integer.parseInt(time[0]), Time.TimestepUnit.HOURS, timestepUnit) ;
        double mm =   Time.convertTime(Integer.parseInt(time[1]), Time.TimestepUnit.MINUTES, timestepUnit) ;
        double ss =  Double.parseDouble(time[2]);

        return (hh + mm + ss);

    }

    // returns a 2D array of all polygon points
    private Double[][] getPolygonCoordinates(JSONArray jcoords) {
        Double[][] coordinates = null;
        // has a [[[x,y],[x,y]]] form ie one extra nesting
        Iterator<JSONArray> it = jcoords.iterator();
        int i = 0;
        while (it.hasNext()) {
            JSONArray obj = it.next();
            coordinates = new Double[obj.size()][2];
            Iterator<JSONArray> cit = obj.iterator();
            while (cit.hasNext()) {
                coordinates[i++] = (Double[]) cit.next().toArray(new Double[2]);
            }
            break; // get the first element only of outer array
        }
        return coordinates;
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
