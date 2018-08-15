package io.github.agentsoz.ees;

import io.github.agentsoz.util.Global;
import io.github.agentsoz.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Random;
import java.util.TreeMap;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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


/**
 *
 * Utility class to hold configuration information
 *
 */
public class Config {

	private final String eSimulation = "simulation";
	private final String eConfig = "config";
	private final String eModels = "models";
	private final String eModel = "model";
	private final String eOption = "opt";
	private final String eId = "id";

	private Map<String, String> config;
	private Map<String, Map<String,String>> models;

	public Config() {
		config = new HashMap<>();
		models = new HashMap<>();
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
			NodeList opts = root.getElementsByTagName(eConfig).item(0).getChildNodes();
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
