package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */

/**
 * Tests staged evacuation through messaging to zones (Surf Coast Shire).
 * This is a 12k population, running at a ~4% sample with capacity on roads reduced accordingly.
 * Congestion rerouting is enabled.
 */
public class Anglesea12kTestStagedSample {
    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    Logger log = Logger.getLogger(Anglesea12kTestStagedSample.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testAnglesea12kStagedSample() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/anglesea-12k-sample/scenario_main_sample.xml",
                "--logfile", utils.getOutputDirectory() + "../scenario.log",
                "--loglevel", "INFO",
                //	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
                "--seed", "12345",
                "--safeline-output-file-pattern", utils.getOutputDirectory() + "../safeline.%d%.out",
                MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:50}]," +
                "logLevel: WARN," +
                "logFile: \""+utils.getOutputDirectory()+"../jill.log\"," +
                "programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\"," +
                "randomSeed: 12345," + // jill random seed
                "numThreads: 1" + // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "180:0.33:0.2", // if delay for 3min section is >1min, then 20% will replan
                "--sendFireAlertOnFireStart", "false", // disable fire alert from fire model, instead will use messaging
                "--x-messages-file", "scenarios/surf-coast-shire/anglesea-12k-sample/scenario_messages_staged.json", // specifies when to send evac now msg
                "--x-zones-file", "scenarios/surf-coast-shire/anglesea-12k-sample/Anglesea_SA1s_WSG84.json", // map from zone (SA1) ids to shapes
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

        TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,10.);
        TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,10.);
        TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 10.);
        TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

    }
}

