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
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:1}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/campbells-creek-01/jill.log\","+
						"programOutputFile: \"scenarios/campbells-creek-01/jill.out\","+
						"randomSeed: 12345,"+ // jill random seed
						"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
		"}"};

		Main.main(args);
		
        {
          long expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_events.xml.gz" ) ;
          long actualCRC = CRCChecksum.getCRCFromFile( "output/output_events.xml.gz" ) ;
          Assert.assertEquals (expectedCRC, actualCRC); 
        }
        {
          long expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" ) ;
          long actualCRC = CRCChecksum.getCRCFromFile( "output/output_plans.xml.gz" ) ;
          Assert.assertEquals (expectedCRC, actualCRC); 
        }
        {
          long expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" ) ;
          long actualCRC = CRCChecksum.getCRCFromFile( "scenarios/campbells-creek-01/jill.out" ) ;
          Assert.assertEquals (expectedCRC, actualCRC); 
        }
	}

}
