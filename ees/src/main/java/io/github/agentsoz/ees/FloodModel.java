package io.github.agentsoz.ees;

import com.google.common.collect.Iterators;
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
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
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
    private final String eGridSquareSideInMetres = "gridSquareSideInMetres";
    private final Logger logger = LoggerFactory.getLogger(FloodModel.class);


    // Model options' values
    private String optGeoJsonFile = null;
    private JSONObject json = null;
    private TreeMap<Double, ArrayList<Geometry>> flood;
    private LocalDateTime startDate = null ;
    private String optCrs = "EPSG:28356";
    private String cycloneGeoJsonCRS = "EPSG:4326";
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private double startTimeInSeconds = -1;
    private DataServer dataServer = null;
    private double optGridSquareSideInMetres = 180;


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
     * Start publishing cyclone data
     */
    public void start() {
        if (optGeoJsonFile != null && !optGeoJsonFile.isEmpty()) {
            try {
                loadCycloneFileGeoJson(optGeoJsonFile);
                dataServer.registerTimedUpdate(Constants.FLOOD_DATA, this, startTimeInSeconds);
            } catch (Exception e) {
                throw new RuntimeException("Could not load flood  geojson data from [" + optGeoJsonFile + "]", e);
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
        // Loop through the features (which contains the time-stamped flood shapes
        JSONArray features = (JSONArray) json.get("features");
        Iterator<JSONObject> iterator = features.iterator();
        while (iterator.hasNext()) {
            JSONObject feature = iterator.next();
            JSONObject properties = (JSONObject) feature.get("properties");
            String end_time = (String) properties.get("end_dt");
            JSONObject geometry = (JSONObject) feature.get("geometry");
            JSONArray jcoords = (JSONArray) geometry.get("coordinates");

            //Iterator<JSONArray> it = jcoords.iterator();
            JSONArray jArr = (JSONArray) jcoords.get(0); // get the next iterator
            Double[][] coordinates = new Double[jArr.size()][2];
            Iterator<JSONArray>  it = jArr.iterator();
            int i = 0;
            while (it.hasNext()) {
                coordinates[i++] = (Double[]) it.next().toArray(new Double[2]);
            }


//            int i = 0;
//            for(int j =0; j < jArr.size();j++) {
//                coordinates[j] = (Double[]) jArr.get(j); //toArray(new Double[2]);
//
//            }

            if (end_time != null) {
                double secs = getTimeInSeconds(end_time);
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
        MathTransform utmTransform = CRS.findMathTransform(CRS.decode(cycloneGeoJsonCRS), CRS.decode(optCrs), false);

        int i = 0;
        double[] flatarray = new double[pairs.length*2];
        int o = 0;
        for (Double[] pair : pairs) {

            Coordinate coord = new Coordinate(pair[0], pair[1]);
            JTS.transform(coord, coord, utmTransform); // transform EPSG:4326 to global CRS EPSG: 28356
            flatarray[i++] = coord.getX();
            flatarray[i++] = coord.getY();
        }
        return new GeometryBuilder().polygon(flatarray);
    }


//    private Geometry getGeometryFromSquareCentroids(MathTransform utmTransform, Double[][] pairs, double squareSideInMetres) throws TransformException {
//        Geometry shape = null ;
//        int i = 0;
//        for (Double[] pair : pairs) {
//            double delta = squareSideInMetres/2;
//            Coordinate coord = new Coordinate(pair[0], pair[1]);
//            JTS.transform(coord, coord, utmTransform);
//            Geometry gridCell = new GeometryBuilder().box(
//                    coord.getX()-delta, coord.getY()-delta,
//                    coord.getX()+delta, coord.getY()+delta);
//            // Fix for JTS #288 requires reduction to floating.
//            // https://github.com/locationtech/jts/issues/288#issuecomment-396647804
//            shape = (shape==null) ?
//                    gridCell :
//                    GeometryPrecisionReducer.reduce(shape.union(gridCell),new PrecisionModel(PrecisionModel.FLOATING));
//        }
//        return shape;
//    }

    // returns a 2D array of all polygon points
//    private Double[][] getPolygonCoordinates(JSONArray jcoords) {
//        Double[][] coordinates = null;
//        // has a [[[x,y],[x,y]]] form ie one extra nesting
//        Iterator<JSONArray> it = jcoords.iterator();
//        int i = 0;
//        while (it.hasNext()) {
//            JSONArray obj = it.next();
//            coordinates = new Double[obj.size()][2];
//            Iterator<JSONArray> cit = obj.iterator();
//            while (cit.hasNext()) {
//                coordinates[i++] = (Double[]) cit.next().toArray(new Double[2]);
//            }
//            break; // get the first element only of outer array
//        }
//        return coordinates;
//    }

    private double getTimeInSeconds(String timestamp) {

        String[] arr = timestamp.split(" "); // received format: 9/02/2076 00:00:00 (E. Australia Standard Time)

        //reformat d/MM/yyyy to dd/MM/yyyy
        String[] date_arr = arr[0].split("/");
        if((date_arr[0]).length() == 1) {
            date_arr[0] = "0".concat(date_arr[0]);
        }

        //format to fit DateTimeFormatter.ISO_LOCAL_DATE_TIME: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
        Collections.reverse(Arrays.asList(date_arr));
        String  d = String.join("-", date_arr);
        String datetime = String.join("T",d,arr[1]);

        DateTimeFormatter formatter2 = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.parse(datetime,formatter2);

        if(startDate == null) {
            startDate =  date; // store first timestamp as startdate
        }

        double total_secs = 0.0;
        int day_gap = date.getDayOfMonth() - startDate.getDayOfMonth();

        if (day_gap > 0) {
            total_secs += Time.convertTime(day_gap, Time.TimestepUnit.DAYS,timestepUnit);
        }
        total_secs +=     Time.convertTime(date.getHour(), Time.TimestepUnit.HOURS, timestepUnit) ;
        total_secs +=  Time.convertTime(date.getMinute(), Time.TimestepUnit.MINUTES, timestepUnit) ;
        total_secs += date.getSecond();

        return total_secs;

    }

    @Override
    public Geometry[] sendData(double forTime, String dataType) {
        return new Geometry[0];
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
