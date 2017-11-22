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
public class MainMaldon600Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@SuppressWarnings("static-method")
	@Test
	public void testMaldon600() {

		/*
--config /var/www/data/user-data/2017-10-23-ds-maldon/scenario/scenario_main.xml 
--logfile /var/www/data/user-data/2017-10-23-ds-maldon/scenario/scenario.log 
--loglevel TRACE 
--jillconfig "--config={agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:600}],logLevel: WARN,logFile: \"/var/www/data/user-data/2017-10-23-ds-maldon/scenario/jill.log\",programOutputFile: \"/var/www/data/user-data/2017-10-23-ds-maldon/scenario/jill.out\"}"
		 */
		String [] args = {
				"--config",  "scenarios/maldon-2017-11-01/scenario_main.xml", 
				"--logfile", "scenarios/maldon-2017-11-01/scenario.log",
				"--loglevel", "INFO",
//				"--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
				"--seed", "12345",
                "--safeline-output-file-pattern", "scenarios/campbells-creek/safeline.%d%.out",
				"--matsim-output-directory", utils.getOutputDirectory(),
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:600}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/maldon-2017-11-01/jill.log\","+
						"programOutputFile: \"scenarios/maldon-2017-11-01/jill.out\","+
						"randomSeed: 12345,"+ // jill random seed
						"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
						"}"
		};

		Main.main(args);
		
	      long [] expectedEvents = new long [] {
              CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/0/output_events.xml.gz" ), 
              //CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/output_events.xml.gz" ), 
              //CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/output_events.xml.gz" ), 
              // 3214464728 mvn console, eclipse single
              //CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/2/output_events.xml.gz" ), 
              // 1316710466 eclipse in context
              //              CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/3/output_events.xml.gz" ) 
              4211426679L // travis
      } ;

      long [] expectedPlans = new long [] {
              CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/0/output_plans.xml.gz" ), 
              //CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/output_plans.xml.gz" ), 
              // 1884178769 mvn console, eclipse single
              //CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/2/output_plans.xml.gz" ),
              // eclipse in context
              //              CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/3/output_plans.xml.gz" )
              3715899067L // travis
      } ;

      long actualEvents = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_events.xml.gz" ) ;
      System.err.println("actual(events)="+actualEvents) ;

      long actualPlans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
      System.err.println("actual(plans)="+actualPlans) ;

      MainCampbellsCreek50Test.checkSeveral(expectedEvents, actualEvents);
      MainCampbellsCreek50Test.checkSeveral(expectedPlans, actualPlans);


//		long actualForEvents = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_events.xml.gz" ) ;
//		System.err.println("actual(events)="+actualForEvents) ;
//
//		long actualForPlans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
//		System.err.println("actual(plans)="+actualForPlans) ;
//
//		{
//			long [] expected = new long [] {
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/output_events.xml.gz" ) // 1380811447 eclipse single)
//					, CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/2/output_events.xml.gz" )  
//					, CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/3/output_events.xml.gz" ) // 721059509 mvn console 
//					, CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/4/output_events.xml.gz" ) // 4020076758 mvn console
//					, 1569262693L // travis
//					} ;
//
//			boolean found = false ;
//			for ( int ii=0 ; ii<expected.length ; ii++ ) {
//				final boolean b = actualForEvents==expected[ii];
//				System.err.println(b);
//				if ( b ) {
//					found = true ;
//					break ;
//				}
//			}
//			if ( !found ) {
//				Assert.fail(); 
//			}
//		}
//		{
//			long [] expected = new long [] {
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/output_plans.xml.gz" ) // 2704415442 eclipse single
//					, CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/2/output_plans.xml.gz" )  
//					, CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/3/output_plans.xml.gz" ) // 2575830431 mvn console  
//					, CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/4/output_plans.xml.gz" ) // 2355435033 mvn console
//					, 2424434648L // travis
//					} ;
//
//			boolean found = false ;
//			for ( int ii=0 ; ii<expected.length ; ii++ ) {
//				final boolean b = actualForPlans==expected[ii];
//				System.err.println(b);
//				if ( b ) {
//					found = true ;
//					break ;
//				}
//			}
//			if ( !found ) {
//				Assert.fail(); 
//			}
//		}


		// FIXME: still small differences in jill log. dsingh, 06/Nov/17
		// expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" ) ;
		// actualCRC = CRCChecksum.getCRCFromFile( "scenarios/maldon-2017-11-01/jill.out" ) ;
		// Assert.assertEquals (expectedCRC, actualCRC); 

	}
}
