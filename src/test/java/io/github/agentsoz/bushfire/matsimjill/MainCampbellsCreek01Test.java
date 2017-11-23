/**
 * 
 */
package io.github.agentsoz.bushfire.matsimjill;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import io.github.agentsoz.bushfire.matsimjill.Main;
/**
 * @author dsingh
 *
 */
public class MainCampbellsCreek01Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testCampbellsCreek01() {

		String [] args = {
				"--config",  "scenarios/campbells-creek-01/scenario_main.xml", 
				"--logfile", "scenarios/campbells-creek-01/scenario.log",
				"--loglevel", "INFO",
				//	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", "scenarios/campbells-creek-01/safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:1}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/campbells-creek-01/jill.log\","+
						"programOutputFile: \"scenarios/campbells-creek-01/jill.out\","+
						"randomSeed: 12345,"+ // jill random seed
						"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
		"}"};

		Main.main(args);
		long actualCRCevents = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_events.xml.gz" ) ;
		System.err.println( "actual(events)=" + actualCRCevents ) ;
		long actualCRCplans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		System.err.println( "actual(plans)=" + actualCRCplans ) ;
		long actualCRCjill = CRCChecksum.getCRCFromFile( "scenarios/campbells-creek-01/jill.out" ) ;
		System.err.println( "actual(jill)=" + actualCRCjill ) ;

		{
			long [] expectedCRC = {
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_events.xml.gz" ) 
					, 3372222273L // build server
			};
			TestUtils.checkSeveral(expectedCRC, actualCRCevents);
		}
		{
			long [] expectedCRC = {
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" )
					, 2777753481L // build server
			} ;
			TestUtils.checkSeveral(expectedCRC, actualCRCplans); 
		}
		{
			long [] expectedCRC = {
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" )
					, 2611213709L // build server
			} ;
			TestUtils.checkSeveral(expectedCRC, actualCRCjill); 
		}
	}

}
