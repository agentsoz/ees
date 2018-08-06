/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
public class Castlemaine1000Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = Logger.getLogger(Castlemaine1000Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@SuppressWarnings("static-method")
	@Test
	public void testCastlemaine1000Reduced() {
		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/castlemaine-1000/scenario_main_reduced.xml",
				"--logfile", utils.getOutputDirectory()+"../scenario.log",
				"--loglevel", "INFO",
				//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
				"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:10}],"+
				"logLevel: WARN,"+
				"logFile: \""+utils.getOutputDirectory()+"../jill.log\","+
				"programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\","+
				"randomSeed: 12345"+ // jill random seed
				//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
				"}",
				"--x-congestion-config", "180:0.25"
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

	@SuppressWarnings("static-method")
	@Test
	@Ignore
	public void testCastlemaine1000() {
		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/castlemaine-1000/scenario_main.xml",
				"--logfile", utils.getOutputDirectory()+"../scenario.log",
				"--loglevel", "INFO",
				//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:1000}],"+
						"logLevel: WARN,"+
						"logFile: \""+utils.getOutputDirectory()+"../jill.log\","+
						"programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\","+
						"randomSeed: 12345"+ // jill random seed
						//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}",
				"--x-congestion-config", "180:0.25:1.0"
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
