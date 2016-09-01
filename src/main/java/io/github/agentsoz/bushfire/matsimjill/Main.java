package io.github.agentsoz.bushfire.matsimjill;

import ch.qos.logback.classic.Level;

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
import io.github.agentsoz.bdiabm.data.AgentState;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bushfire.Config;
import io.github.agentsoz.bushfire.FireModule;
import io.github.agentsoz.bushfire.MATSimBDIParameterHandler;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.jill.util.Log;

public class Main {
	
	private JillBDIModel jillmodel; // BDI model
	private MATSimModel matsimModel; // ABM model
	private FireModule fireModel; // Fire model
	
	private AgentDataContainer adc; // BDI-ABM data container
	private AgentStateList asl; // BDI-ABM states container

	// Defaults
	private static String logFile = Main.class.getSimpleName() + ".log";
	private static Level logLevel = Level.INFO;
	private static String[] jillInitArgs = null;

	
	public Main() {
		
	}
	
	public void start(String[] cargs) {
		
		// Parse the command line args
		parse(cargs);
		
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

		
		// Create fire module, load fire data, and register the fire module as
		// an active data source
		fireModel = new FireModule();
		fireModel.loadData(SimpleConfig.getFireFile());
		fireModel.setDataServer(dataServer);
		fireModel.start();
		
		// Create the Jill BDI model
		jillmodel = new JillBDIModel(jillInitArgs);
		jillmodel.registerDataServer(dataServer);

		// Create the MATSim ABM model and register the data server
		matsimModel = new MATSimModel(jillmodel, null);
		matsimModel.registerDataServer(dataServer);

		// Start the MATSim controller
		String[] margs = { SimpleConfig.getMatSimFile() };
		matsimModel.run(null, margs);
		
		// All done, so terminate now
		jillmodel.finish();
		System.exit(0);
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
		sim.start(args);
	}

}
