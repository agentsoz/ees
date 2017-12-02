/**
 * 
 */
package io.github.agentsoz.bushfire.matsimjill;

import org.apache.log4j.Logger;
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
	
	private static final Logger log = Logger.getLogger(MainCampbellsCreek01Test.class) ;
	public static final String SEPARATOR = "---------------------------------------------" ;

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
						"randomSeed: 12345"+ // jill random seed
						//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
		"}"};

		Main.main(args);
		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualCRCevents = CRCChecksum.getCRCFromFile(actualEventsFilename) ;
		log.warn( "actual(events)=" + actualCRCevents ) ;
		long actualCRCplans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		log.warn( "actual(plans)=" + actualCRCplans ) ;
		long actualCRCjill = CRCChecksum.getCRCFromFile( "scenarios/campbells-creek-01/jill.out" ) ;
		log.warn( "actual(jill)=" + actualCRCjill ) ;
		
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		{
			log.info(SEPARATOR) ;
			log.info("START comparing departures ...") ;
			SortedMap<Id<Person>, List<Double>> expecteds = TestUtils.collectDepartures(primaryExpectedEventsFilename);
			SortedMap<Id<Person>, List<Double>> actuals = TestUtils.collectDepartures(actualEventsFilename);
			TestUtils.compareEventsWithSlack( expecteds, actuals, 5. );
			log.info("... comparing departures DONE.") ;
			log.info(SEPARATOR) ;
		}
		{
			log.info(SEPARATOR) ;
			log.info("START comparing arrivals ...") ;
			SortedMap<Id<Person>, List<Double>> expecteds = TestUtils.collectArrivals(primaryExpectedEventsFilename);
			SortedMap<Id<Person>, List<Double>> actuals = TestUtils.collectArrivals(actualEventsFilename);
			TestUtils.compareEventsWithSlack( expecteds, actuals, 5. );
			log.info("... comparing arrivals DONE.") ;
			log.info(SEPARATOR) ;
		}
		{
			log.info(SEPARATOR) ;
			log.info("Comparing activity starts ...") ;
			SortedMap<Id<Person>, List<Double>> expecteds = TestUtils.collectActivityStarts(primaryExpectedEventsFilename);
			SortedMap<Id<Person>, List<Double>> actuals = TestUtils.collectActivityStarts(actualEventsFilename);
			TestUtils.compareEventsWithSlack( expecteds, actuals, 5. );
			log.info("... comparing activity starts done.") ;
			log.info(SEPARATOR) ;
		}
		{
			log.info(SEPARATOR) ;
			log.info("START comparing all events ...") ;
			EventsFileComparator.Result result = EventsFileComparator.compare(primaryExpectedEventsFilename, actualEventsFilename);
			log.info("result=" + result);
			Assert.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result );
			log.info("... comparing all events DONE.") ;
			log.info(SEPARATOR) ;
		}
		
//		{
//			long [] expectedCRC = {
//					CRCChecksum.getCRCFromFile(primaryExpectedEventsFilename)
//			};
//			TestUtils.checkSeveral(expectedCRC, actualCRCevents);
//		}
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
