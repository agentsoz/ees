/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.EvacConfig;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import io.github.agentsoz.util.TestUtils;

/**
 * @author dsingh
 *
 */
public class MainMaldon600Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = Logger.getLogger(MainMaldon600Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@SuppressWarnings("static-method")
	@Test
	public void testMaldon600Reduced() {

		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/maldon-600/scenario_main_reduced.xml",
				"--logfile", utils.getOutputDirectory()+"../scenario.log",
				"--loglevel", "INFO",
				"--seed", "12345",
				"--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
				"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:30}],"+
				"logLevel: WARN,"+
				"logFile: \""+utils.getOutputDirectory()+"../jill.log\","+
				"programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\","+
				"randomSeed: 12345"+ // jill random seed
				//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
				"}"
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

		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 1.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, true);
	}


	@SuppressWarnings("static-method")
	@Test
	@Ignore
	public void testMaldon600() {

		/*
--config /var/www/data/user-data/2017-10-23-ds-maldon/scenario/scenario_main.xml
--logfile /var/www/data/user-data/2017-10-23-ds-maldon/scenario/scenario.log
--loglevel TRACE
--jillconfig "--config={agents:[{classname:Resident, args:null, count:600}],logLevel: WARN,logFile: \"/var/www/data/user-data/2017-10-23-ds-maldon/scenario/jill.log\",programOutputFile: \"/var/www/data/user-data/2017-10-23-ds-maldon/scenario/jill.out\"}"
		 */
		String [] args = {
				"--config",  "scenarios/mount-alexander-shire/maldon-600/scenario_main.xml",
				"--logfile", utils.getOutputDirectory()+"../scenario.log",
				"--loglevel", "INFO",
				//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
				"--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:600}],"+
						"logLevel: WARN,"+
						"logFile: \""+utils.getOutputDirectory()+"../jill.log\","+
						"programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\","+
						"randomSeed: 12345"+ // jill random seed
						//"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}"
		};

		Main.main(args);

		// print these so that we have them:
		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
		log.warn("actual(events)="+actualEventsCRC) ;

		long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
		log.warn("actual(plans)="+actualPlansCRC) ;
		
		// look into events:

		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 1.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, true);

		
		// comparing the plain files, but maybe more than one:
//		long [] expectedEvents = new long [] {
//				CRCChecksum.getCRCFromFile( primaryExpectedEventsFilename )
//		} ;
//		TestUtils.checkSeveral(expectedEvents, actualEventsCRC);

		// look into plans:

		long [] expectedPlans = new long [] {
				CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" ),
				CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/win/output_plans.xml.gz" )
		} ;
		
		TestUtils.checkSeveral(expectedPlans, actualPlansCRC);



		// FIXME: still small differences in jill log. dsingh, 06/Nov/17
		// expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" ) ;
		// actualCRC = CRCChecksum.getCRCFromFile( "scenarios/maldon-2017-11-01/jill.out" ) ;
		// Assert.assertEquals (expectedCRC, actualCRC); 

	}
}
