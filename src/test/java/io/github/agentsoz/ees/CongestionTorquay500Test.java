/**
 *
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh, Joel Robertson
 *
 */
public class CongestionTorquay500Test {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = Logger.getLogger(CongestionTorquay500Test.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test

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
                "randomSeed: 12345," + // jill random seed
                "numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "120:0.1"
        };

        Main.main(args);

//        final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
//        long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
//        System.err.println("actual(events)="+actualEventsCRC) ;
//
//        long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
//        System.err.println("actual(plans)="+actualPlansCRC) ;
//
//        // ---
//
//        final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
//
//        // ---
//
//        TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,10.);
//        TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,10.);
//        TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 10.);
//        TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);
    }
}

