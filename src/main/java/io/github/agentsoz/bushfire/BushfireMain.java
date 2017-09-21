package io.github.agentsoz.bushfire;

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

import io.github.agentsoz.dataInterface.DataServer;

import java.io.IOException;
import java.util.StringTokenizer;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.github.agentsoz.bdimatsim.MATSimModel;

/**
 * Main class of the application.
 */
public class BushfireMain {

	// Defaults
	private static String logFile = "./bushfire-evacuation.log";
	private static Level logLevel = Level.INFO;
	private static double[] fireOffset = new double[] { 0.0, 0.0 };

	private static Logger logger = null;

	public static void main(final String[] args) throws IOException {

		// Parse the command line arguments
		parse(args);

		// Create the logger
		logger = createLogger("", logFile);

		// Read in the configuration
		if (!Config.readConfig()) {
			logger.error("Failed to load configuration from '"
					+ Config.getConfigFile() + "'. Aborting");
			System.exit(-1);
		}

		// Initialise the evacuation report file
		EvacuationReport.start();

		// Create and initialise the data server - this is used for passing
		// several different types
		// of data around the application without needing to set up a ton of
		// references between
		// different parts of the application
		DataServer dataServer = DataServer.getServer("Bushfire");

		/*
		 * FIXME this is a fudge to get matsim and the fire data file in synch
		 * matsim appears to use seconds as a time increment, while the fire
		 * data is in hours. This number is 1/3600 to convert hours to secs.
		 * Ideally, we need to have both values in a config file. ie. how many
		 * seconds per matsim increment (1) and how many seconds per fire file
		 * time unit (3600) and let this code work it out as required. That way
		 * other components (with other time ratios) can be added.
		 */
		dataServer.setTimeStep(0.000277778);

		// Initialise and hook up the BDI side
		BushfireApplication bushfireApp = new BushfireApplication();

		// Initialise the MATSim model
		MATSimModel matsimManager = new MATSimModel(bushfireApp);
		matsimManager.registerDataServer(dataServer);

		// Initialise and register the visualiser for percepts and other data
		if (Config.getUseGUI()) {
			new FireVisualiser(Config.getPort());
		}

		// Create fire module, load fire data, and register the fire module as
		// an active data source
		FireModule fireModule = new FireModule();
		fireModule.loadData(Config.getFireFile());
		fireModule.setDataServer(dataServer);
		fireModule.setFireOffset(fireOffset[0], fireOffset[1]);
		fireModule.start();

		// Finally, start the MATSim controller
		String[] margs = { Config.getMatSimFile() };

		String s = "starting matsim with args:";
		for (int i = 0; i < margs.length; i++) {
			s += margs[i];
		}
		logger.info(s);
		matsimManager.run(margs);

		// Close the report
		logger.info("simulation ended");
		EvacuationReport.report("Simulation ended");
		EvacuationReport.close();

		// All done
		System.exit(0);
	}

	/**
	 * Parse the command line arguments
	 */
	public static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-c":
				if (i + 1 < args.length) {
					i++;
					Config.setConfigFile(args[i]);
				}
				break;
			case "-fire_offset":
				try {
					if (i + 1 < args.length) {
						i++;
						StringTokenizer tokenizer = new StringTokenizer(
								args[i], ",");
						fireOffset[0] = Double.parseDouble(tokenizer
								.nextToken());
						fireOffset[1] = Double.parseDouble(tokenizer
								.nextToken());
					}
				} catch (Exception e) {
					System.err.println("Could not parse fire offset '"
							+ args[i] + "'; expecting 'x,y' : "
							+ e.getMessage());
				}
				break;
			case "-gui":
				Config.setUseGUI(true);
				break;
			case "-h":
				exit(null);
			case "-l":
				if (i + 1 < args.length) {
					i++;
					logFile = args[i];
				}
				break;
			case "-level":
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
			}
		}
	}

	/**
	 * Just print the command line usage example.
	 */
	public static String usage() {
		return "usage:\n"
				+ "  -c <config_file>     simulation configuration file" + "\n"
				+ "  -fire_offset <x,y>   an x,y offset to apply to the fire progression\n"
				+ "  -gui                 use if connecting to the Unity-based visualiser\n"
				+ "  -h                   print this help message and exit\n"
				+ "  -l <logfile>         logging output file name (default is '"+logFile+"')\n"
				+ "  -level <level>       log level; one of ERROR,WARN,INFO,DEBUG,TRACE (default is '"+logLevel+"')\n"
				;
	}

	private static void exit(String err) {
		if (err != null) {
			System.err.println("\nERROR: " + err + "\n");
		}
		System.out.println(usage());
		System.exit(0);
	}

	private static Logger createLogger(String string, String file) {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder ple = new PatternLayoutEncoder();

		ple.setPattern("%date %level [%thread] %logger{10} [%file:%line]%n%msg%n%n");
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
