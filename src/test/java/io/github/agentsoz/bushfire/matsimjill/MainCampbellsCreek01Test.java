/**
 * 
 */
package io.github.agentsoz.bushfire.matsimjill;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import io.github.agentsoz.bushfire.matsimjill.Main;
import io.github.agentsoz.util.TestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.util.List;
import java.util.SortedMap;

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
		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualCRCevents = CRCChecksum.getCRCFromFile(actualEventsFilename) ;
		System.err.println( "actual(events)=" + actualCRCevents ) ;
		long actualCRCplans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		System.err.println( "actual(plans)=" + actualCRCplans ) ;
		long actualCRCjill = CRCChecksum.getCRCFromFile( "scenarios/campbells-creek-01/jill.out" ) ;
		System.err.println( "actual(jill)=" + actualCRCjill ) ;
		
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		{
			System.out.println("\nComparing all events ...") ;
			EventsFileComparator.Result result = EventsFileComparator.compare(primaryExpectedEventsFilename, actualEventsFilename);
			System.out.println("result=" + result);
			System.out.println("... comparing all events done.\n") ;
		}
		{
			System.out.println("\nComparing departures ...") ;
			SortedMap<Id<Person>, List<Double>> expecteds = TestUtils.collectDepartures(primaryExpectedEventsFilename);
			SortedMap<Id<Person>, List<Double>> actuals = TestUtils.collectDepartures(actualEventsFilename);
			TestUtils.compareEventsWithSlack( expecteds, actuals, 5. );
			System.out.println("... comparing departures done.\n") ;
		}
		{
			System.out.println("\nComparing arrivals ...") ;
			SortedMap<Id<Person>, List<Double>> expecteds = TestUtils.collectArrivals(primaryExpectedEventsFilename);
			SortedMap<Id<Person>, List<Double>> actuals = TestUtils.collectArrivals(actualEventsFilename);
			TestUtils.compareEventsWithSlack( expecteds, actuals, 5. );
			System.out.println("... comparing arrivals done.\n") ;
		}
		{
			System.out.println("\nComparing activity starts ...") ;
			SortedMap<Id<Person>, List<Double>> expecteds = TestUtils.collectActivityStarts(primaryExpectedEventsFilename);
			SortedMap<Id<Person>, List<Double>> actuals = TestUtils.collectActivityStarts(actualEventsFilename);
			TestUtils.compareEventsWithSlack( expecteds, actuals, 5. );
			System.out.println("... comparing activity starts done.\n") ;
		}
		
		{
			long [] expectedCRC = {
					CRCChecksum.getCRCFromFile(primaryExpectedEventsFilename)
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
