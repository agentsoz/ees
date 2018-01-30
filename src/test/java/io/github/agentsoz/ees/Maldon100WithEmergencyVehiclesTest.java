/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
public class Maldon100WithEmergencyVehiclesTest {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = Logger.getLogger(Maldon100WithEmergencyVehiclesTest.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@SuppressWarnings("static-method")
	@Test
	public void testMaldon100WithEmergencyVehicles() {
		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/scenario_main.xml",
				"--logfile", "scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/scenario.log",
				"--loglevel", "INFO",
				//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", "scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:100},"+
								"{classname:io.github.agentsoz.ees.agents.Responder, args:[--respondToUTM, \"Tarrengower Prison,237302.52,5903341.11\"], count:3}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/jill.log\","+
						"programOutputFile: \"scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/jill.out\","+
						"randomSeed: 12345"+ // jill random seed
						//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}"
		};

		Main.main(args);

		// FIXME: enable the checks below when expected results are verified and saved; DS 23/jan/18
		/*
		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
		log.warn("actual(events)="+actualEventsCRC) ;
		long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		log.warn("actual(plans)="+actualPlansCRC) ;
		
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 1.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, true);

		
		long [] expectedPlans = new long [] {
				CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" )
		};
		TestUtils.checkSeveral(expectedPlans, actualPlansCRC);
		*/
	}
}
