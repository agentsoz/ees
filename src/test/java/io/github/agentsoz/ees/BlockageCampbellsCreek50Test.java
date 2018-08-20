/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.util.TestUtils;
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
public class BlockageCampbellsCreek50Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

	private static final Logger log = LoggerFactory.getLogger(BlockageCampbellsCreek50Test.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testBlockage50() {

		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/campbells-creek-50/scenario_main.xml",
				"--logfile", utils.getOutputDirectory()+"../scenario.log",
				"--loglevel", "INFO",
				//	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
				MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:50}],"+
						"logLevel: WARN,"+
						"logFile: \""+utils.getOutputDirectory()+"../jill.log\","+
						"programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\","+
						"randomSeed: 12345,"+ // jill random seed
						"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}",
				"--x-disruptions-file", "scenarios/mount-alexander-shire/campbells-creek-50/scenario_disruptions.json",
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
		
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,10.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,10.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 10.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

		// ---
//		{
//			long[] expectedPlansCRCs = new long[]{
//					CRCChecksum.getCRCFromFile(utils.getInputDirectory() + "/output_plans.xml.gz")
//			};
//			TestUtils.checkSeveral(expectedPlansCRCs, actualPlansCRC);
//		}
		// (if we are getting different arrivals, we will also be getting different plans scores)

		//		{
		//			long expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" ) ;
		//			long actualCRC = CRCChecksum.getCRCFromFile( "scenarios/campbells-creek/jill.out" ) ;
		//			Assert.assertEquals (expectedCRC, actualCRC); 
		//		}
	}


}
