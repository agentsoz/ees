package io.github.agentsoz.ees;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdimatsim.EvacAgentTracker;
import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Utils;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
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

import io.github.agentsoz.util.Time;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.util.Global;

public class Main {

	private static Logger logger;
	
	private static EvacConfig.Setup setup = EvacConfig.Setup.standard ;

	// Defaults
	private static String logFile = Main.class.getSimpleName() + ".log";
	private static Level logLevel = Level.INFO;
	private static String[] jillInitArgs = null;
	private static String matsimOutputDirectory;
	private static String safelineOutputFilePattern = "./safeline.%d%.csv";


	private static int blockedLinkId = -1; // FIXME: temp fix on 31/01/18; remove once #1 is in place

	// yyyyyy careful; the above all stay from one test to the next (if not in separate
	// JVMs).  kai, dec'17

	public Main() {

	}

	public void start(String[] cargs) throws IOException, ParseException, java.text.ParseException {

		// Parse the command line args
		parse(cargs);

		// Create the logger
		logger = createLogger();
		
		logger.warn("setup=" + setup ) ;
		// (I need this "setup" switch to switch between the test cases.  Some other
		// config design would clearly be ok, but I don't want to impose some config
		// design on you. kai, dec'17)
		
		// Read in the configuration
		SimpleConfig.readConfig();
			
		// Create and initialise the data server used for passing
		// several different types of data around the application 
		// using a publish/subscribe or pull mechanism
		DataServer dataServer = DataServer.getServer("Bushfire");
		
		// get the fire model out of the way:
		initializeAndStartFireModel(dataServer);

		// initialise the disruptions module
		initializeAndStartDisruptionModel(dataServer);
		
		MATSimModel matsimModel = new MATSimModel( SimpleConfig.getMatSimFile(), matsimOutputDirectory );

		// --- do some things for which you need a handle to matsim:
		matsimModel.registerDataServer(dataServer);
		// (yyyy this syntax indicates that there might be use cases without dataServer.  Is that the case?
		// If not, I would rather pass it as a "forced" argument in matsimModel.init(...).  kai, dec'17)
		
		List<SafeLineMonitor> monitors = registerSafeLineMonitors(SimpleConfig.getSafeLines(), matsimModel);
		
		// --- do some things for which you need access to the matsim config:
		Config config = matsimModel.loadAndPrepareConfig() ;
		setSimStartTimesRelativeToAlert(dataServer, config, -10*60 );
		EvacConfig evacConfig = ConfigUtils.addOrGetModule(config, EvacConfig.class);
		evacConfig.setSetup(setup);
		evacConfig.setCongestionEvaluationInterval(SimpleConfig.getCongestionEvaluationInterval());
		evacConfig.setCongestionToleranceThreshold(SimpleConfig.getCongestionToleranceThreshold());
		
		// --- do some things for which you need a handle to the matsim scenario:
		Scenario scenario = matsimModel.loadAndPrepareScenario() ;
		// move everything into the far future (yy maybe better repair input files?)
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements() ;
			int offset = planElements.size();
			for ( PlanElement pe : planElements ) {
				if ( pe instanceof Activity) {
					((Activity) pe).setEndTime( Double.MAX_VALUE - offset );
					offset-- ;
				}
			}
		}
		/* Dhirendra, it might be cleaner to do this in the input xml.  I have tried to remove the "fake" leg completely.
		 * But then the agents don't have vehicles (yyyy although, on second thought, why??  we need to maintain
		 * vehicles for mode choice).
		 */
		
		List<String> bdiAgentIDs = Utils.getBDIAgentIDs( scenario );

		// --- initialize and start jill (need the bdiAgentIDs, for which we need the material from before)
		JillBDIModel jillmodel = initializeAndStartJillModel(dataServer, bdiAgentIDs, matsimModel, matsimModel.getAgentManager().getAgentDataContainer());
		// (matsimModel is passed in because of its query interface.  which is, however, never used.)
		
		// --- initialize and start matsim (maybe rename?):
		matsimModel.init(bdiAgentIDs);
		
		EvacAgentTracker tracker = new EvacAgentTracker(evacConfig, matsimModel.getScenario().getNetwork(), matsimModel.getEvents() ) ;
		matsimModel.getEvents().addHandler( tracker );
		// yyyy try to replace this by injection. because otherwise it again needs to be added "late enough", which we
		// wanted to get rid of.  kai, dec'17
		
		while ( true ) {
			
			jillmodel.takeControl( matsimModel.getAgentManager().getAgentDataContainer() );
			if( matsimModel.isFinished() ) {
				break;
			}
			//matsimModel.takeControl(matsimModel.getAgentManager().getAgentDataContainer());
			matsimModel.runUntil((long)dataServer.getTime(), matsimModel.getAgentManager().getAgentDataContainer());

			// increment time
			dataServer.stepTime();
			
			// firemodel is updated elsewhere
		}

		// Write safe line statistics to file
		writeSafeLineMonitors(monitors, safelineOutputFilePattern);

		// All done, so terminate now
		jillmodel.finish();
		matsimModel.finish() ;
		//		System.exit(0);
		// get rid of System.exit(...) so that tests run through ...
		DataServer.cleanup() ;
	}
	
	private static void setSimStartTimesRelativeToAlert(DataServer dataServer, Config config, int offset ) {
		dataServer.setTime(getEvacuationStartTimeInSeconds(offset));
		
		config.qsim().setStartTime(getEvacuationStartTimeInSeconds(offset)) ;
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		// yy in the longer run, a "minOfStarttimeAndEarliestActivityEnd" would be good. kai, nov'17
	}
	
	private static JillBDIModel initializeAndStartJillModel(DataServer dataServer, List<String> bdiAgentIDs,
															ABMServerInterface matsimModel, AgentDataContainer agentDataContainer) {
		
		// Create the Jill BDI model
		JillBDIModel jillmodel = new JillBDIModel(jillInitArgs);
		// Set the query percept interface to use
		jillmodel.setQueryPerceptInterface((QueryPerceptInterface)matsimModel);
		// register the server to use for transferring data between models
		jillmodel.registerDataServer(dataServer);
		// initialise and start
		jillmodel.init(agentDataContainer,
				null, // agentList is not used
				matsimModel,
				bdiAgentIDs.toArray( new String[bdiAgentIDs.size()] ));
		jillmodel.start();
		return jillmodel ;
	}
	
	private static void initializeAndStartFireModel(DataServer dataServer) throws IOException, ParseException, java.text.ParseException {
		// Create fire module, load fire data, and register the fire module as
		// an active data source
		boolean isGeoJson = SimpleConfig.getFireFireFormat().equals("geojson");
		if (!isGeoJson) {
			abort("Fire file must be in GeoJson format.");
		}
		PhoenixFireModule fireModel = new PhoenixFireModule();
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
	}

	/**
	 * Create the disruption model, load the disruption data, and register the model as an active data source
	 * @param dataServer
	 * @throws IOException
	 * @throws ParseException
	 * @throws java.text.ParseException
	 */
	private static void initializeAndStartDisruptionModel(DataServer dataServer) throws IOException, ParseException, java.text.ParseException {
		DisruptionModel model = new DisruptionModel();
		model.setDataServer(dataServer);
		if (SimpleConfig.getDisruptionsFile() != null) {
			model.loadJson(SimpleConfig.getDisruptionsFile());
		}
		// If the disruptions file was not given, then the model starts but does nothing
		model.setTimestepUnit(Time.TimestepUnit.SECONDS);
		model.start();
	}

	/**
	 * Gets the simulation start time from the config, with an added delay
	 * @param delay in seconds to add to the start (can be negative)
	 * @return the start time with delay added, value always >= 0
	 */
	private static double getEvacuationStartTimeInSeconds(int delay) {
		int[] evacStartHHMM = SimpleConfig.getEvacStartHHMM();
		double startInSeconds = Time.convertTime(evacStartHHMM[0], Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
				+ Time.convertTime(evacStartHHMM[1], Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS)
				+ delay;
		startInSeconds = (startInSeconds <= 0) ? 0 : startInSeconds;
		return startInSeconds;
	}

	/**
	 * Registers a {@link SafeLineMonitor} per safe line with MATSim
	 * 
	 * @param safeLines the map of safelines to register for monitoring
	 * @param matsimModel the model to register with
	 */
	private static List<SafeLineMonitor> registerSafeLineMonitors(TreeMap<String, Location[]> safeLines,
			MATSimModel matsimModel) {

		if (safeLines == null) {
			return null;
		}

		List<SafeLineMonitor> slMonitors = new ArrayList<>();
		List<EventHandler> monitors = new ArrayList<>();
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
	 * @throws FileNotFoundException if file cannot be written to
	 */
	private void writeSafeLineMonitors(List<SafeLineMonitor> monitors, String pattern)
			throws FileNotFoundException {
		for (SafeLineMonitor monitor : monitors) {
			String filepath = pattern.replace("%d%", monitor.getName().replace(' ', '-'));
			logger.info("Writing safe line statistics to file: " + filepath);
			PrintWriter writer = new PrintWriter(filepath);
			Collection<List<Double>> exitTimes = monitor.getExitTimes();
			if (exitTimes != null) {
				List<Double> all = new ArrayList<>();
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
				case EvacConfig.SETUP_INDICATOR:
					if(i+1<args.length) {
						i++ ;
						setup = EvacConfig.Setup.valueOf(args[i]) ;
					}
					break;
				case "--x-disruptions-file": //FIXME: remove; make part of main config
					if(i+1<args.length) {
						i++ ;
						SimpleConfig.setDisruptionsFile(args[i]) ;
					}
					break;
				case "--x-congestion-config": //FIXME: remove; make part of main config
					// expect format double:double to indicate interval:threshold
					if(i+1<args.length) {
						i++ ;
						String[] vals = args[i].split(":");
						try {
							SimpleConfig.setCongestionEvaluationInterval(Double.parseDouble(vals[0]));
							SimpleConfig.setCongestionToleranceThreshold(Double.parseDouble(vals[1]));

						} catch (Exception e) {
							System.err.println("Could not parse congestion config '"
									+ args[i] + "' : " + e.getMessage());
						}

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

	private static void abort(String err) {
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
		System.out.println("called with args=" ) ;
		for (String arg : args) {
			System.out.println(arg);
		}
		Main sim = new Main();
		try {
			sim.start(args);
		} catch(Exception e) {
			System.err.println("\nERROR: somethig went wrong, see details below:\n");
						e.printStackTrace();
			throw new RuntimeException(e) ; // throw exception, otherwise test counts as passed.  Sorry ...
		}
	}

	private static Logger createLogger() {
		final String string = "io.github.agentsoz.ees";
		final String file = logFile;
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder ple = new PatternLayoutEncoder();

		ple.setPattern("%date %level [%thread] %caller{1}%msg%n%n");
		ple.setContext(lc);
		ple.start();
		FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
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
