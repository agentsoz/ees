package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.MATSimModel;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
//@Ignore
public class Anglesea12kTestDiffusionSample {
    // have tests in separate classes so that they run, at least und    er maven, in separate JVMs.  kai, nov'17

    Logger log = Logger.getLogger(Anglesea12kTestDiffusionSample.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testAnglesea12kDiffusionSample() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/anglesea-12k-sample/scenario_main_sample.xml",
                "--logfile", "scenarios/surf-coast-shire/anglesea-12k-sample/scenario.log",
                "--loglevel", "INFO",
                //	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
                "--seed", "12345",
                "--safeline-output-file-pattern", "scenarios/surf-coast-shire/anglesea-12k-sample/safeline.%d%.out",
                MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:482}]," +
                "logLevel: WARN," +
                "logFile: \"scenarios/surf-coast-shire/anglesea-12k-sample/jill.log\"," +
                "programOutputFile: \"scenarios/surf-coast-shire/anglesea-12k-sample/jill.out\"," +
                "randomSeed: 12345," + // jill random seed
                "numThreads: 1" + // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "1200:2.0", // check every 20 mins per vehicle, and trigger if time taken is double than expected
                //"--sendFireAlertOnFireStart", "false", // disable fire alert from fire model, instead will use messaging
                //"--x-messages-file", "scenarios/surf-coast-shire/anglesea-12k-sample/scenario_messages.json", // specifies when to send evac now msg
                //"--x-zones-file", "scenarios/surf-coast-shire/anglesea-12k-sample/Anglesea_SA1s_WSG84.json", // map from zone (SA1) ids to shapes
                "--x-disruptions-file", "scenarios/surf-coast-shire/anglesea-12k-sample/scenario_disruptions.json", //
                "--x-diffusion-config", "scenarios/surf-coast-shire/anglesea-12k-sample/scenario_diffusion_config.xml", //
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
//        //TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 300.);
        //TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

    }
}

