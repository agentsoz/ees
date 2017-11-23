/**
 * 
 */
package io.github.agentsoz.bushfire.matsimjill;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import io.github.agentsoz.bushfire.matsimjill.Main;
/**
 * @author dsingh
 *
 */
public class MainCampbellsCreek50Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testCampbellsCreek50() {

		String [] args = {
				"--config",  "scenarios/campbells-creek/scenario_main.xml", 
				"--logfile", "scenarios/campbells-creek/scenario.log",
				"--loglevel", "INFO",
				//	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", "scenarios/campbells-creek/safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:50}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/campbells-creek/jill.log\","+
						"programOutputFile: \"scenarios/campbells-creek/jill.out\","+
						"randomSeed: 12345,"+ // jill random seed
						"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
		"}"};

		Main.main(args);

		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
		System.err.println("actual(events)="+actualEventsCRC) ;

		long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		System.err.println("actual(plans)="+actualPlansCRC) ;
		
		// ---
		
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/0/output_events.xml.gz";
		long [] expectedEventsCRCs = new long [] {
				CRCChecksum.getCRCFromFile( primaryExpectedEventsFilename ), 
				3114851905L // 3695594801L // travis
		} ;

		long [] expectedPlansCRCs = new long [] {
				CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/0/output_plans.xml.gz" ), 
				2234597649L // 547669447L // travis
		} ;

		// ---
		
		SortedMap<Id<Person>, List<Double>> arrivalsExpected = TestUtils.collectArrivals(primaryExpectedEventsFilename) ;
		SortedMap<Id<Person>, List<Double>> arrivalsActual = TestUtils.collectArrivals(actualEventsFilename) ;
		TestUtils.compareEventsWithSlack(arrivalsExpected, arrivalsActual, 600.);
		
		// ---

		TestUtils.checkSeveral(expectedEventsCRCs, actualEventsCRC);
		TestUtils.checkSeveral(expectedPlansCRCs, actualPlansCRC);

		//		{
		//			long expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" ) ;
		//			long actualCRC = CRCChecksum.getCRCFromFile( "scenarios/campbells-creek/jill.out" ) ;
		//			Assert.assertEquals (expectedCRC, actualCRC); 
		//		}
	}


}
