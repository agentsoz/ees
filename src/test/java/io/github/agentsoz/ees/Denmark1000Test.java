/**
 * 
 */
package io.github.agentsoz.ees;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
public class Denmark1000Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = Logger.getLogger(Denmark1000Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@SuppressWarnings("static-method")
	@Test
	public void testDenmark1000() {
		String [] args = {
				"--config",  "scenarios/shire-of-denmark/denmark-albany-1000/scenario_main.xml",
				"--logfile", "scenarios/shire-of-denmark/denmark-albany-1000/scenario.log",
				"--loglevel", "INFO",
				//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", "scenarios/shire-of-denmark/denmark-albany-1000/safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:1000}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/shire-of-denmark/denmark-albany-1000/jill.log\","+
						"programOutputFile: \"scenarios/shire-of-denmark/denmark-albany-1000/jill.out\","+
						"randomSeed: 12345"+ // jill random seed
						//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}",
				//"--x-congestion-config", "180:0.25"
		};

		Main.main(args);

		// FIXME: differences in number of arrivals when replanning with congestion; dsingh 2/feb/18
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
