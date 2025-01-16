package io.github.agentsoz.ees;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;



/**
 *
 * Utility class to hold configuration information
 *
 */
public class Config {

	// EES command line options
	static final String OPT_CONFIG = "--config";
	// EES Command line usage
	private static final String USAGE = "usage:\n"
            + String.format("%10s %-6s %s\n", OPT_CONFIG, "FILE", "EES config file (v2)")
            ;
	// XML structure tags
	private final String eSimulation = "simulation";
	private final String eGlobal = "global";
	private final String eModels = "models";
	private final String eModel = "model";
	private final String eOption = "opt";
	private final String eId = "id";
	// Global options in XML
	static final String eGlobalRandomSeed= "randomSeed";
	static final String eGlobalCoordinateSystem= "crs";
	static final String eGlobalStartHhMm = "startHHMM";
	static final String eGlobalTimeStep = "timestepInSecs";
	static final String eGlobalDeckGlOutFile = "deckGlOutFile";
	static final String eGlobalMetricsOutFile = "metricsOutFile";
	static final String eGlobalMetricsBinSizeInSecs = "metricsBinSizeInSecs";
	// Model IDs in XML
	static final String eModelCyclone = "cyclone";
	static final String eModelFlood = "flood";
	static final String eModelFire = "phoenix";
	static final String eModelFireSpark = "spark";
	static final String eModelDisruption = "disruption";
	static final String eModelMessaging = "messaging";
	static final String eModelMatsim = "matsim";
	static final String eModelBdi = "bdi";
	static final String eModelDiffusion = "diffusion";


	private Map<String, String> config;
	private Map<String, Map<String,String>> models;

	public Config() {
		config = new HashMap<>();
		models = new HashMap<>();
	}

	/**
	 * Returns all global and local config options for the named model
	 * @param model the named model
	 * @return map of all applicable config options
	 */
	public Map<String, String> getModelConfig(String model) {
		Map<String, String> map = new HashMap<>(config);
		Map<String, String> modelMap = models.get(model);
		if (modelMap != null) {
			map.putAll(models.get(model));
		}
		return map;
	}

	public String getGlobalConfig(String opt) {
		return config.get(opt);
	}

	/**
     * Parse the command line options
     * @param args command line options
     * @return key value pairs of known options
     */
	Map<String,String> parse(String[] args) {
        Map<String,String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case OPT_CONFIG:
                    if (i + 1 < args.length) {
                        i++;
                        opts.put(args[i-1],args[i]);
                    }
                    break;
                default:
                    throw new RuntimeException("unknown config option: " + args[i]) ;
            }
        }
        if (opts.isEmpty() || !opts.containsKey(OPT_CONFIG)) {
            throw new RuntimeException(USAGE);
        }
        return opts;
    }

	public void loadFromFile(String file) {
		try {
			// Validate the XML against the schema first
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(); // schema specified in file
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(new File(file)));

			// Now we can start reading; we don't need to any checks since
			// the XML is at this point already validated
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new FileInputStream(file));

			// Normalisation is optional, but recommended
			// see http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			// Get the root element
			Element root = (Element)doc.getElementsByTagName(eSimulation).item(0);

			// Get the config options
			NodeList opts = root.getElementsByTagName(eGlobal).item(0).getChildNodes();
			for(int i=0; i < opts.getLength(); i++) {
				Node node = opts.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) node;
					config.put(eElement.getAttribute(eId), eElement.getTextContent());
				}
			}

			// Get the models and their options
			NodeList modelsList = root.getElementsByTagName(eModels).item(0).getChildNodes();
			for(int i=0; i < modelsList.getLength(); i++) {
				if (modelsList.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element model = (Element) modelsList.item(i);
					String name = model.getAttribute(eId);
					Map<String,String> modelMap = new HashMap<>();
					NodeList modelOpts = model.getChildNodes();
					for(int j=0; j < modelOpts.getLength(); j++) {
						if (modelOpts.item(j).getNodeType() == Node.ELEMENT_NODE) {
							Element opt = (Element) modelOpts.item(j);
							modelMap.put(opt.getAttribute(eId), opt.getTextContent());
						}
					}
					models.put(name, modelMap);
				}
			}

		} catch(Exception e){
			throw new RuntimeException(e) ;
		}
	}

}
