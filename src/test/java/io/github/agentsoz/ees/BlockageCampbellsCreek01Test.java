/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.bdimatsim.MATSimModel;
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
public class BlockageCampbellsCreek01Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	Logger log = Logger.getLogger(BlockageCampbellsCreek50Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testBlockage01() {

		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/campbells-creek-1/scenario_main.xml",
				"--logfile", "scenarios/mount-alexander-shire/campbells-creek-1/scenario.log",
				"--loglevel", "INFO",
				//	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", "scenarios/mount-alexander-shire/campbells-creek-1/safeline.%d%.out",
				MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
				EvacConfig.SETUP_INDICATOR, EvacConfig.Setup.blockage.name() ,
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:1}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/mount-alexander-shire/campbells-creek-1/jill.log\","+
						"programOutputFile: \"scenarios/mount-alexander-shire/campbells-creek-1/jill.out\","+
						"randomSeed: 12345,"+ // jill random seed
						"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}",
				"--x-disruptions-file", "scenarios/mount-alexander-shire/campbells-creek-01/scenario_disruptions.json",
		};
		Main.main(args);
		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualCRCevents = CRCChecksum.getCRCFromFile(actualEventsFilename) ;
		System.err.println( "actual(events)=" + actualCRCevents ) ;
		long actualCRCplans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		System.err.println( "actual(plans)=" + actualCRCplans ) ;
		long actualCRCjill = CRCChecksum.getCRCFromFile( "scenarios/mount-alexander-shire/campbells-creek-1/jill.out" ) ;
		System.err.println( "actual(jill)=" + actualCRCjill ) ;
		
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";

		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,5.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 5.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);


//		{
//			long [] expectedCRC = {
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_events.xml.gz" )
//			};
//			TestUtils.checkSeveral(expectedCRC, actualCRCevents);
//		}
//		{
//			long [] expectedCRC = {
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" )
//			} ;
//			TestUtils.checkSeveral(expectedCRC, actualCRCplans);
//		}
//		{
//			long [] expectedCRC = {
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" )
//			} ;
//			TestUtils.checkSeveral(expectedCRC, actualCRCjill);
//		}
	}

}
