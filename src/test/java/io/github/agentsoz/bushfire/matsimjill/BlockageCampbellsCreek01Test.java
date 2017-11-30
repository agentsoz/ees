/**
 * 
 */
package io.github.agentsoz.bushfire.matsimjill;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.util.TestUtils;
import org.junit.Ignore;
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

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testBlockage01() {

		String [] args = {
				"--config",  "scenarios/campbells-creek-01/scenario_main.xml", 
				"--logfile", "scenarios/campbells-creek-01/scenario.log",
				"--loglevel", "INFO",
				//	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", "scenarios/campbells-creek-01/safeline.%d%.out",
				MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
				Main.SETUP_INDICATOR, Main.Setup.blockage.name() ,
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
			};
			TestUtils.checkSeveral(expectedCRC, actualCRCevents);
		}
		{
			long [] expectedCRC = {
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" )
			} ;
			TestUtils.checkSeveral(expectedCRC, actualCRCplans); 
		}
		{
			long [] expectedCRC = {
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" )
			} ;
			TestUtils.checkSeveral(expectedCRC, actualCRCjill); 
		}
	}

}
