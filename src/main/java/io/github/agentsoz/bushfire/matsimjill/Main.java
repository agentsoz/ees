package io.github.agentsoz.bushfire.matsimjill;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bushfire.PhoenixFireModule;
import io.github.agentsoz.bushfire.Time;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.util.Global;

public class Main {
	
	private JillBDIModel jillmodel; // BDI model
	private MATSimModel matsimModel; // ABM model
	private PhoenixFireModule fireModel; // Fire model
	
	private AgentDataContainer adc; // BDI-ABM data container
	private AgentStateList asl; // BDI-ABM states container

	// Defaults
	private static String logFile = Main.class.getSimpleName() + ".log";
	private static Level logLevel = Level.INFO;
	private static String[] jillInitArgs = null;
	
	public Main() {
		
	}
	
	public void start(String[] cargs) throws FileNotFoundException, IOException, ParseException, java.text.ParseException {
		
		// Parse the command line args
		parse(cargs);

		// Create the logger
        createLogger("io.github.agentsoz.bushfire", logFile);
		
		// Read in the configuration
		try { 
			SimpleConfig.readConfig();
		} catch(Exception e){
			abort(e.getMessage());
		}

		// Create and initialise the data server used for passing
		// several different types of data around the application 
		// using a publish/subscribe or pull mechanism
		DataServer dataServer = DataServer.getServer("Bushfire");
		
		// Create fire module, load fire data, and register the fire module as
		// an active data source
		boolean isGeoJson = SimpleConfig.getFireFireFormat().equals("geojson");
		if (!isGeoJson) {
			abort("Fire file must be in GeoJson format.");
		} 
		fireModel = new PhoenixFireModule();
		fireModel.setDataServer(dataServer);
		fireModel.loadGeoJson(SimpleConfig.getFireFile());
		fireModel.setEvacStartHHMM(SimpleConfig.getEvacStartHHMM());
		boolean isGlobalUTM = SimpleConfig.getGeographyCoordinateSystem().toLowerCase().equals("utm");
		boolean isFireUTM = SimpleConfig.getFireFireCoordinateSystem().toLowerCase().equals("utm");
		if (isGlobalUTM && !isFireUTM) {
			fireModel.convertLatLongToUtm();
		}
		fireModel.setTimestepUnit(Time.TimestepUnit.SECONDS);
		fireModel.start();
		
		// Create the Jill BDI model
		jillmodel = new JillBDIModel(jillInitArgs);
		jillmodel.registerDataServer(dataServer);

		// Create the MATSim ABM model and register the data server
		matsimModel = new MATSimModel(jillmodel);
		matsimModel.registerDataServer(dataServer);

		// Start the MATSim controller
		String[] margs = { SimpleConfig.getMatSimFile() };
		matsimModel.run(margs);
		
		// All done, so terminate now
		jillmodel.finish();
//		System.exit(0);
		// get rid of System.exit(...) so that tests run through ...
	}

	/**
	 * Parse the command line arguments
	 */
	public static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--config":
				if (i + 1 < args.length) {
					i++;
					SimpleConfig.setConfigFile(args[i]);
				}
				break;
			case "--help":
				abort(null);
			case "--jillconfig":
				if (i + 1 < args.length) {
					i++;
					jillInitArgs = args[i].split("=");
				}
				break;
			case "--logfile":
				if (i + 1 < args.length) {
					i++;
					logFile = args[i];
				}
				break;
			case "--loglevel":
				if (i + 1 < args.length) {
					i++;
					try {
						logLevel = Level.toLevel(args[i]);
					} catch (Exception e) {
						System.err.println("Could not parse log level '"
								+ args[i] + "' : " + e.getMessage());
					}
				}
				break;
			case "--seed":
              if (i + 1 < args.length) {
                i++;
                try {
                    Global.setRandomSeed(Long.parseLong(args[i]));
                } catch (Exception e) {
                    System.err.println("Could not parse random seed '"
                            + args[i] + "' : " + e.getMessage());
                }
              }
              break;
			}
		}
		
		if (jillInitArgs == null) {
			abort("Some required options were not given");
		}
	}

	/**
	 * Just print the command line usage example.
	 */
	public static String usage() {
		return "usage:\n"
				+ "  --config FILE     simulation configuration file" + "\n"
				+ "  --help            print this help message and exit\n"
				+ "  --jillconfig STR  The string '--config={...}' passed to JILL (required)\n"
				+ "  --logfile FILE    logging output file name (default is '"+logFile+"')\n"
				+ "  --loglevel LEVEL  log level; one of ERROR,WARN,INFO,DEBUG,TRACE (default is '"+logLevel+"')\n"
                + "  --seed SEED       seed to use for the random number generator (optional)\n"
				;
	}

	static void abort(String err) {
		if (err != null) {
			System.err.println("\nERROR: " + err + "\n");
		}
		System.out.println(usage());
		System.exit(0);
	}

	public static void main(String[] args) {
		Main sim = new Main();
		try {
			sim.start(args);
		} catch(Exception e) {
			System.err.println("\nERROR: somethig went wrong, see details below:\n");
			e.printStackTrace();
		}
	}

    private static Logger createLogger(String string, String file) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %caller{1}%msg%n%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setFile(file);
        fileAppender.setEncoder(ple);
        fileAppender.setAppend(false);
        fileAppender.setContext(lc);
        fileAppender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(string);
        logger.detachAndStopAllAppenders(); // detach console (doesn't seem to
                                                                                // work)
        logger.addAppender(fileAppender); // attach file appender
        logger.setLevel(logLevel);
        logger.setAdditive(true); /* set to true if root should log too */

        return logger;
}
}
