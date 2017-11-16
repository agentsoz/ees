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
				"--jillconfig", "--config={"+
						"agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:50}],"+
						"logLevel: WARN,"+
						"logFile: \"scenarios/campbells-creek/jill.log\","+
						"programOutputFile: \"scenarios/campbells-creek/jill.out\","+
						"randomSeed: 12345,"+ // jill random seed
						"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
		"}"};

		Main.main(args);

		long actualEvents = CRCChecksum.getCRCFromFile( "output/output_events.xml.gz" ) ;
		System.err.println("actual(events)="+actualEvents) ;

		long actualPlans = CRCChecksum.getCRCFromFile( "output/output_plans.xml.gz" ) ;
		System.err.println("actual(plans)="+actualPlans) ;

		{
			long [] expected = new long [] {
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/output_events.xml.gz" ), 
					// 3214464728 mvn console, eclipse single
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/2/output_events.xml.gz" ) 
					// 1316710466 eclipse in context
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/3/output_events.xml.gz" ) 
					, 3695594801L // travis
					} ;

			boolean found = false ;
			for ( int ii=0 ; ii<expected.length ; ii++ ) {
				final boolean b = actualEvents==expected[ii];
				System.err.println(b);
				if ( b ) {
					found = true ;
					break ;
				}
			}
			if ( !found ) {
				Assert.fail(); 
			}
		}
		{
			long [] expected = new long [] {
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/output_plans.xml.gz" ), 
					// 1884178769 mvn console, eclipse single
					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/2/output_plans.xml.gz" )
					// eclipse in context
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/3/output_plans.xml.gz" ) 
					} ;

			boolean found = false ;
			for ( int ii=0 ; ii<expected.length ; ii++ ) {
				final boolean b = actualPlans==expected[ii];
				System.err.println(b);
				if ( b ) {
					found = true ;
					break ;
				}
			}
			if ( !found ) {
				Assert.fail(); 
			}
		}
//		{
//			long [] expected = new long [] {
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/1/jill.out" ), 
//					// 5202049 eclipse single
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/2/jill.out" ),
//					// 
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/3/jill.out" ) 
//					// 3808301801 eclipse in context
//					} ;
//
//			long actual = CRCChecksum.getCRCFromFile(  "scenarios/campbells-creek/jill.out" ) ;
//			System.err.println("actual(jill)="+actual) ;
//
//			boolean found = false ;
//			for ( int ii=0 ; ii<expected.length ; ii++ ) {
//				final boolean b = actual==expected[ii];
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
//			long expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" ) ;
//			long actualCRC = CRCChecksum.getCRCFromFile( "scenarios/campbells-creek/jill.out" ) ;
//			Assert.assertEquals (expectedCRC, actualCRC); 
//		}
	}

}
