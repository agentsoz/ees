package io.github.agentsoz.bushfire.matsimjill;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.handler.EventHandler;
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

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bushfire.PhoenixFireModule;
import io.github.agentsoz.bushfire.Time;
import io.github.agentsoz.bushfire.datamodels.Location;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.util.Global;

public class Main {

    private static Logger logger;

    private JillBDIModel jillmodel; // BDI model
	private MATSimModel matsimModel; // ABM model
	private PhoenixFireModule fireModel; // Fire model

	// Defaults
	private static String logFile = Main.class.getSimpleName() + ".log";
	private static Level logLevel = Level.INFO;
	private static String[] jillInitArgs = null;
	private static String matsimOutputDirectory;
	private static String safelineOutputFilePattern = "./safeline.%d%.csv"; 

	public Main() {

	}

	public void start(String[] cargs) throws FileNotFoundException, IOException, ParseException, java.text.ParseException {

		// Parse the command line args
		parse(cargs);

		// Create the logger
		logger = createLogger("io.github.agentsoz.bushfire", logFile);

		// Read in the configuration
		try { 
			SimpleConfig.readConfig();
		} catch(Exception e){
//			abort(e.getMessage());
			abort(e) ;
			// (if you just pass the message, the exception type is not passed on, so e.g. for a null pointer error one does not
			// see anything.  kai, nov'17)
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

		// Register the safe lines monitors for MATSim
		List<SafeLineMonitor> monitors = registerSafeLineMonitors(SimpleConfig.getSafeLines(), matsimModel);
		
		// Start the MATSim controller
		List<String> config = new ArrayList<>() ;
		config.add( SimpleConfig.getMatSimFile() ) ;
		if ( matsimOutputDirectory != null ) { 
			config.add( MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR ) ;
			config.add( matsimOutputDirectory ) ;
		}
		Scenario scenario = null ;
		matsimModel.run( config.toArray(new String[config.size()] ), scenario, bdiAgentIds ) ;

		// Write safe line statistics to file
		writeSafeLineMonitors(monitors, safelineOutputFilePattern);
		
		// All done, so terminate now
		jillmodel.finish();
		//		System.exit(0);
		// get rid of System.exit(...) so that tests run through ...
	}
	
  /**
   * Registers a {@link SafeLineMonitor} per safe line with MATSim
   * 
   * @param safeLines
   * @param matsimModel
   */
  private List<SafeLineMonitor> registerSafeLineMonitors(TreeMap<String, Location[]> safeLines,
      MATSimModel matsimModel) {

    if (safeLines == null) {
      return null;
    }

    List<SafeLineMonitor> slMonitors = new ArrayList<SafeLineMonitor>();
    List<EventHandler> monitors = new ArrayList<EventHandler>();
    for (String name : safeLines.keySet()) {
      Location[] safeline = safeLines.get(name);
      SafeLineMonitor monitor = new SafeLineMonitor(name, safeline[0], safeline[1], matsimModel);
      monitors.add(monitor);
      slMonitors.add(monitor);
    }
    matsimModel.setEventHandlers(monitors);
    return slMonitors;
  }

  /**
   * Writes the safe lines statistics to file
   * 
   * @param monitors to write, one per file
   * @param pattern output file pattern, something like "/path/to/file.%d%.out" where %d% is
   *        replaced by the monitor index number i.e., 0,1,...
   * @throws FileNotFoundException
   */
  private void writeSafeLineMonitors(List<SafeLineMonitor> monitors, String pattern)
      throws FileNotFoundException {
    for (int i = 0; i < monitors.size(); i++) {
      String filepath = pattern.replace("%d%", monitors.get(i).getName().replace(' ', '-'));
      logger.info("Writing safe line statistics to file: " + filepath);
      PrintWriter writer = new PrintWriter(filepath);
      Collection<List<Double>> exitTimes = monitors.get(i).getExitTimes();
      if (exitTimes != null) {
        List<Double> all = new ArrayList<Double>();
        for (List<Double> list : exitTimes) {
          all.addAll(list);
        }
        Collections.sort(all);
        for (Double time : all) {
          writer.println(time);
        }
      }
      writer.close();
    }
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
				abort("");
				break;
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
            case "--safeline-output-file-pattern":
              if (i + 1 < args.length) {
                  i++;
                  safelineOutputFilePattern = args[i];
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
			case MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR:
				if (i + 1 < args.length) {
					i++;
					matsimOutputDirectory = args[i];
				}
				break;
			default:
				throw new RuntimeException("unknown config option: " + args[i]) ;
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
				+ "  --safeline-output-file-pattern  pattern for safeline output files (default is '"+safelineOutputFilePattern+"')\n "
				+ "  --seed SEED       seed to use for the random number generator (optional)\n"
				;
	}

	static void abort(String err) {
		if (err != null) {
			System.err.println("\nERROR: " + err + "\n");
		}
		System.out.println(usage());
//		System.exit(0);
		throw new RuntimeException("abort") ; // throw exception, otherwise test gets stuck.  Sorry ...
	}

	static void abort(Exception err) {
		throw new RuntimeException(err) ; // throw exception, otherwise test gets stuck.  Sorry ...
	}

	public static void main(String[] args) {
		Main sim = new Main();
		try {
			sim.start(args);
		} catch(Exception e) {
			System.err.println("\nERROR: somethig went wrong, see details below:\n");
//			e.printStackTrace();
			throw new RuntimeException(e) ; // throw exception, otherwise test counts as passed.  Sorry ...
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
