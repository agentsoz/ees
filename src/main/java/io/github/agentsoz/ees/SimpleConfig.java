package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
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

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.Global;


/**
 *
 * Utility class to hold configuration information
 *
 */
public class SimpleConfig {

	private static final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

	private static Random rand;

	private static String configFile = null;

	private static String matSimFile = null;
	private static String geographyFile = null;
	private static String fireFile = null;

	private static String disruptionsFile = null; // FIXME: should come from config XML
	private static String messagesFile = null;  // FIXME: should come from config XML
	private static String zonesFile = null; // FIXME: should come from config XML
	private static double congestionEvaluationInterval = 600; // secs between BDI agent evaluatiing if it is in congestion
	private static double congestionToleranceThreshold = 0.5; // as a proportion of the congestionEvaluationInterval
	private static String diffusionConfig = null; // FIXME: should come from config XML
	private static double congestionReactionProbability = 1.0; // likelihood that an agent will react when in congestion

	private static String bdiAgentTagInMATSimPopulationFile = "BDIAgentType"; // FIXME: no way to set this yet
	private static boolean loadBDIAgentsFromMATSimPlansFile = false;

	public static void setDisruptionsFile(String disruptionFile) {
		SimpleConfig.disruptionsFile = disruptionFile;
	}


	private static int numBDIAgents = 1;

	private static double proportionWithKids = 0.0;
	private static double proportionWithRelatives = 0.0;
	private static int maxDistanceToRelatives = 1000;

	private static int[] evacStartHHMM = new int[] {0, 0};
	private static int evacPeakMins = 0;
	private static TreeMap<String, Location> locations = new TreeMap<String, Location>();
	private static String fireCoordinateSystem = "longlat"; // FIXME: use EPSG:4326 here
	private static String fireFileFormat = "custom";
	private static String geographyCoordinateSystem = "longlat";  // FIXME: use EPSG:4326 here
    private static TreeMap<String, Location[]> safelines = new TreeMap<String, Location[]>();

	private static final String eSimulation = "simulation";
	private static final String eMATSimFile = "matsimfile";
	private static final String eFireFile = "firefile";
	private static final String eGeographyFile = "geographyfile";
	private static final String eNumBDI = "bdiagents";
	private static final String eName = "name";
    private static final String eSplit = "split";
	private static final String eCoordinates = "coordinates";
	private static final String eFormat = "format";
	private static final String eTrafficBehaviour = "trafficBehaviour";
	private static final String ePreEvacDetour = "preEvacDetour";
	private static final String eProportion = "proportion";
	private static final String eRadiusInMtrs = "radiusInMtrs";
	private static final String eGeography = "geography";
	private static final String eCoordinateSystem = "coordinateSystem";
	private static final String eDestinations = "destinations";
	private static final String eLocation = "location";
	private static final String eEvacuationTiming = "evacuationTiming";
	private static final String eStart = "start";
	private static final String ePeak = "peak";
    private static final String eSafeLines = "safelines";
    private static final String eSafeLine = "safeline";

	public static String getMatSimFile() {
		return matSimFile;
	}

	public static String getFireFile() {
		return fireFile;
	}

	public static int getNumBDIAgents() {
		return numBDIAgents;
	}

	public static double getProportionWithKids() {
		return proportionWithKids;
	}

	public static double getProportionWithRelatives() {
		return proportionWithRelatives;
	}

	public static int getMaxDistanceToRelatives() {
		return maxDistanceToRelatives;
	}

	public static int[] getEvacStartHHMM() {
		return evacStartHHMM;
	}

	public static int getEvacPeakMins() {
		return evacPeakMins;
	}

	public static String getGeographyCoordinateSystem() {
		return geographyCoordinateSystem;
	}

	public static String getFireFireCoordinateSystem() {
		return fireCoordinateSystem;
	}

	public static String getFireFireFormat() {
		return fireFileFormat;
	}

	public static Location getLocation(String name) {
		return locations.get(name);
	}

	public static String getConfigFile() {
		return configFile;
	}

	public static void setConfigFile(String string) {
		configFile = string;
	}

	public static TreeMap<String, Location[]> getSafeLines() {
	  return safelines;
	}

	/**
	 * Pick one of the listed evac points randomly
	 * @return evac location
	 */
	public static Location getRandomEvacLocation() {
	  Location[] locs = locations.values().toArray(new Location[0]);
	  double[] splits = new double[locs.length];
	  for (int i = 0; i < locs.length; i++) {
	    splits[i] = (Double)(locs[i].getAttributes());
	  }
	  int choice = selectIndexFromCumulativeProbabilities(cumulate(normalise(splits)));
	  return locs[choice];
	}

	public static Location getEvacLocation(String nearest) {
		Location[] locs = locations.values().toArray(new Location[0]);
		double[] splits = new double[locs.length];
		for (int i = 0; i < locs.length; i++) {
			if (locs[i].getName()==nearest){
				splits[i] = 6*(Double)(locs[i].getAttributes());
			}
			else
			splits[i] = (Double)(locs[i].getAttributes());
		}
		int choice = selectIndexFromCumulativeProbabilities(cumulate(normalise(splits)));
		return locs[choice];
	}


	public static void readConfig() {
		try {
			if (configFile == null) {
				throw new Exception("No configuration file given");
			}

			logger.info("Loading configuration from '" + configFile + "'");

			// Validate the XML against the schema first
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(); // schema specified in file
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(new File(configFile)));

			// Now we can start reading; we don't need to add many checks since
			// the XML is at this point already validated
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(configFile));

			// Normalisation is optional, but recommended
			// see http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			// Get the root element
			Element root = (Element)doc.getElementsByTagName(eSimulation).item(0);

			// Get MATSim config file name
			matSimFile = doc.getElementsByTagName(eMATSimFile).item(0).getTextContent().replaceAll("\n", "").trim();

			// Get the fire data
			Element element = (Element)root.getElementsByTagName(eFireFile).item(0);
			fireFile = element.getElementsByTagName(eName).item(0).getTextContent().replaceAll("\n", "").trim();
			fireCoordinateSystem = element.getElementsByTagName(eCoordinates).item(0).getTextContent().replaceAll("\n", "").trim();
			fireFileFormat = element.getElementsByTagName(eFormat).item(0).getTextContent();

			// Get the geography data
			element = (Element)root.getElementsByTagName(eGeographyFile).item(0);
			geographyFile = element.getElementsByTagName(eName).item(0).getTextContent().replaceAll("\n", "").trim();

			// Get the number of BDI agents
			numBDIAgents = Integer.parseInt(root.getElementsByTagName(eNumBDI).item(0).getTextContent().replaceAll("\n", "").trim());

			// Get the traffic detour behaviour
			element = (Element)root.getElementsByTagName(eTrafficBehaviour).item(0);
			element = (Element)element.getElementsByTagName(ePreEvacDetour).item(0);
			proportionWithRelatives = Double.parseDouble(root.getElementsByTagName(eProportion).item(0).getTextContent().replaceAll("\n", "").trim());
			maxDistanceToRelatives = Integer.parseInt(root.getElementsByTagName(eRadiusInMtrs).item(0).getTextContent().replaceAll("\n", "").trim());

			// Get the evacuation timing data
			element = (Element)root.getElementsByTagName(eEvacuationTiming).item(0);
			String[] tokens = element.getElementsByTagName(eStart).item(0).getTextContent().replaceAll("\n", "").trim().split(":");
			evacStartHHMM[0] = Integer.parseInt(tokens[0]);
			evacStartHHMM[1] = Integer.parseInt(tokens[1]);
			evacPeakMins= Integer.parseInt(element.getElementsByTagName(ePeak).item(0).getTextContent().replaceAll("\n", "").trim());

			// Now read the geography file
			readGeography();
		} catch(Exception e){
			throw new RuntimeException("ERROR while reading config", e);
		}


		}

	private static void readGeography() throws Exception {
		if (geographyFile == null) {
			throw new Exception("No geography file given");
		}

		logger.info("Loading geography from '" + geographyFile + "'");

		// Validate the XML against the schema first
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(); // schema specified in file
		Validator validator = schema.newValidator();
		validator.validate(new StreamSource(new File(geographyFile)));

		// Now we can start reading; we don't need to add many checks since
		// the XML is at this point already validated
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new FileInputStream(geographyFile));

	    // Normalisation is optional, but recommended
	    // see http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
	    doc.getDocumentElement().normalize();

	    // Get the root element
		Element root = (Element)doc.getElementsByTagName(eGeography).item(0);

		// Get the coordinate system
		geographyCoordinateSystem = root.getElementsByTagName(eCoordinateSystem).item(0).getTextContent().replaceAll("\n", "").trim();

		// Get the locations
		Element dests = (Element)root.getElementsByTagName(eDestinations).item(0);
		NodeList nl = dests.getElementsByTagName(eLocation);
		for (int i = 0; i < nl.getLength(); i++) {
			Element location = (Element)nl.item(i);
			String name = location.getElementsByTagName(eName).item(0).getTextContent().replaceAll("\n", "").trim();
            Double split = new Double(location.getElementsByTagName(eSplit).item(0).getTextContent().replaceAll("\n", "").trim());
			String s = location.getElementsByTagName(eCoordinates).item(0).getTextContent().replaceAll("\n", "").trim();
			String[] sCoords = s.split(",");
			double x = Double.parseDouble(sCoords[0]);
			double y = Double.parseDouble(sCoords[1]);
			locations.put(name, new Location(name, x, y, split));
		}

        // Get the safe lines
        Element slines = (Element)root.getElementsByTagName(eSafeLines).item(0);
        nl = slines.getElementsByTagName(eSafeLine);
        for (int i = 0; i < nl.getLength(); i++) {
          Location[] safeline = new Location[2];
          Element location = (Element)nl.item(i);
          String name = location.getElementsByTagName(eName).item(0).getTextContent().replaceAll("\n", "").trim();
          String s = location.getElementsByTagName(eCoordinates).item(0).getTextContent().replaceAll("\n", "").trim();
          String[] sCoords = s.split(",");
          double x = Double.parseDouble(sCoords[0]);
          double y = Double.parseDouble(sCoords[1]);
          safeline[0] = new Location("from", x, y);
          s = location.getElementsByTagName(eCoordinates).item(1).getTextContent().replaceAll("\n", "").trim();
          sCoords = s.split(",");
          x = Double.parseDouble(sCoords[0]);
          y = Double.parseDouble(sCoords[1]);
          safeline[1] = new Location("to", x, y);
          safelines.put(name, safeline);
        }

	}

    /**
     * Normalises the values of the given array, such that each value is divided
     * by the sum of all values.
     *
     * @param values
     * @return
     */
    private static double[] normalise(double[] values) {
            double sum = 0;
            if (values == null || values.length <= 1) {
                    return values;
            }
            for (double d : values) {
                    sum += Math.abs(d);
            }
            if (sum == 0) {
                    return values;
            }
            double[] norm = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                    norm[i] = values[i] / sum;
            }
            return norm;
    }

    /**
     * Cumulates the values of the given array, such that in the returned array,
     * the value at position i is the sum of all values from index 0..i in the
     * input array.
     *
     * @param values
     * @return
     */
    private static double[] cumulate(double[] values) {
            if (values == null || values.length <= 1) {
                    return values;
            }
            double[] out = new double[values.length];
            out[0] = values[0];
            for (int i = 1; i < values.length; i++) {
                    out[i] = out[i - 1] + values[i];
            }
            return out;
    }

    /**
     * Probabilistically selects an index from the provided array, where each
     * element of the array gives the cumulative probability associated with
     * that index. To ensure that at least one index is selected, make sure the
     * array is normalised, such that the cumulative probability for element
     * values[length-1] is 1.0.
     *
     * @param values
     *            cumulative probabilities associated with the indexes
     * @return the selected index in the range [0:values.length-1], or -1 if
     *         none was selected
     */
    static int selectIndexFromCumulativeProbabilities(double[] values) {
            int index = -1;
            double roll = Global.getRandom().nextDouble();
            if (values != null && values.length > 0) {
                    for (int i = 0; i < values.length; i++) {
                            if (roll <= values[i]) {
                                    index = i;
                                    break;
                            }
                    }
            }
            return index;
    }

	public static String getDisruptionsFile() {
		return disruptionsFile;
	}

    public static void setCongestionToleranceThreshold(double congestionToleranceThreshold) {
		SimpleConfig.congestionToleranceThreshold = congestionToleranceThreshold;
    }
	public static void setCongestionEvaluationInterval(double congestionEvaluationInterval) {
		SimpleConfig.congestionEvaluationInterval = congestionEvaluationInterval;
	}
	public static double getCongestionToleranceThreshold() {
		return congestionToleranceThreshold;
	}

	public static double getCongestionEvaluationInterval() {
		return congestionEvaluationInterval;
	}

	public static String getMessagesFile() {
		return messagesFile;
	}

	public static void setMessagesFile(String messagesFile) {
		SimpleConfig.messagesFile = messagesFile;
	}

	public static String getZonesFile() {
		return zonesFile;
	}

	public static void setZonesFile(String zonesFile) {
        SimpleConfig.zonesFile = zonesFile;
    }

	public static String getDiffusionConfig() { return diffusionConfig;  }

	public static void setDiffusionConfig(String config) {
		SimpleConfig.diffusionConfig = config;
	}

	public static double getCongestionReactionProbability() {
		return congestionReactionProbability;
	}

	public static void setCongestionReactionProbability(double congestionReactionProbability) {
        SimpleConfig.congestionReactionProbability = congestionReactionProbability;
    }

	public static String getBdiAgentTagInMATSimPopulationFile() {
		return bdiAgentTagInMATSimPopulationFile;
	}

	public static void setBdiAgentTagInMATSimPopulationFile(String bdiAgentTagInMATSimPopulationFile) {
		SimpleConfig.bdiAgentTagInMATSimPopulationFile = bdiAgentTagInMATSimPopulationFile;
	}

	public static boolean isLoadBDIAgentsFromMATSimPlansFile() {
		return loadBDIAgentsFromMATSimPlansFile;
	}

	public static void setLoadBDIAgentsFromMATSimPlansFile(boolean loadBDIAgentsFromMATSimPlansFile) {
		SimpleConfig.loadBDIAgentsFromMATSimPlansFile = loadBDIAgentsFromMATSimPlansFile;
	}
}
