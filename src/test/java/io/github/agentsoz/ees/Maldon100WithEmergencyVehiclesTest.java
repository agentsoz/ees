/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.util.TestUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dsingh
 *
 */
public class Maldon100WithEmergencyVehiclesTest {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = LoggerFactory.getLogger(Maldon100WithEmergencyVehiclesTest.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testMaldon10WithEmergencyVehiclesV2() {

		utils.getOutputDirectory(); // creates a clean one so need to call this first
		String[] args = {
				"--config", "scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/ees.xml",
		};
		// FIXME: io.github.agentsoz.ees.agents.Responder does not work as it does not react to fire/messaging
		// As a result, all three emergency responders in the test do not move. Accepting for now.
		Run.main(args);

		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,10.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,10.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 10.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

	}

	@SuppressWarnings("static-method")
	@Test
	@Ignore
	public void testMaldon10WithEmergencyVehicles() {
		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/scenario_main.xml",
				"--logfile", utils.getOutputDirectory()+"../scenario.log",
				"--loglevel", "INFO",
				//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						//"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:10},"+
						//		"{classname:io.github.agentsoz.ees.agents.Responder, args:[--respondToUTM, \"Tarrengower Prison,237100,5903400\"], count:3}],"+
						"agents:[],"+ // must be this string; will be replaced based on MATSim plans file
						"logLevel: WARN,"+
						"logFile: \""+utils.getOutputDirectory()+"../jill.log\","+
						"programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\","+
						"randomSeed: 12345"+ // jill random seed
						//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}",
				"--x-load-bdi-agents-from-matsim-plans-file", "true", // Load agents from MATSim plans file
		};

		Main.main(args);

		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
		System.err.println("actual(events)="+actualEventsCRC) ;

		long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		System.err.println("actual(plans)="+actualPlansCRC) ;

		// ---

		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";

		// ---

		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 5.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

	}
}
