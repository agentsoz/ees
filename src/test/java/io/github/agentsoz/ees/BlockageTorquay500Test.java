
package io.github.agentsoz.ees;

        import io.github.agentsoz.bdimatsim.EvacConfig;
        import org.apache.log4j.Logger;
        import org.junit.Ignore;
        import org.junit.Rule;
        import org.junit.Test;
        import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh, Joel Robertson
 *
 */
public class BlockageTorquay500Test {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = Logger.getLogger(BlockageTorquay500Test.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
//    @Ignore
    public void testTorquay500() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/torquay-500/scenario_main.xml",
                "--logfile", "scenarios/surf-coast-shire/torquay-500/scenario.log",
                "--loglevel", "INFO",
                //	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
                "--seed", "12345",
                "--safeline-output-file-pattern", "scenarios/surf-coast-shire/torquay-500/safeline.%d%.out",
                "--matsim-output-directory", utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:500}]," +
                "logLevel: WARN," +
                "logFile: \"scenarios/surf-coast-shire/torquay-500/jill.log\"," +
                "programOutputFile: \"scenarios/surf-coast-shire/torquay-500/jill.out\"," +
                "randomSeed: 12345" + // jill random seed
                //"numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
                "}",
//                "--x-blocked-link", "10520",
                "--x-disruptions-file", "scenarios/surf-coast-shire/torquay-500/scenario_disruptions.json",
        };

        Main.main(args);
    }
}

//	FIXME:Diff checks need to be enabled once expected behaviour is established-- Joel Jan '18
// final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
//		long actualCRCevents = CRCChecksum.getCRCFromFile(actualEventsFilename) ;
//		log.info( "actual(events)=" + actualCRCevents ); ;
//		long actualCRCplans = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
//		log.info( "actual(plans)=" + actualCRCplans ); ;
//		long actualCRCjill = CRCChecksum.getCRCFromFile( "scenarios/mount-alexander-shire/campbells-creek-1/jill.out" ) ;
//		log.info( "actual(jill)=" + actualCRCjill ) ;
//
//		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
//
//		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,1.);
//		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,1.);
//		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 1.);
//		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, true);
//
//		//		{
////			long [] expectedCRC = {
////					CRCChecksum.getCRCFromFile(primaryExpectedEventsFilename)
////			};
////			TestUtils.checkSeveral(expectedCRC, actualCRCevents);
////		}
//
//		{
//			long [] expectedCRC = {
//					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" )
//			} ;
//			TestUtils.checkSeveral(expectedCRC, actualCRCplans);
//		}
////		{
////			long [] expectedCRC = {
////					CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" )
////			} ;
////			TestUtils.checkSeveral(expectedCRC, actualCRCjill);
////		}
//	}
//
//}

