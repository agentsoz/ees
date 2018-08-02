package io.github.agentsoz.ees;

import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.*;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;



        import io.github.agentsoz.bdimatsim.EvacConfig;
        import io.github.agentsoz.bdimatsim.MATSimModel;
        import io.github.agentsoz.util.TestUtils;
        import org.apache.log4j.Logger;
        import org.junit.Rule;
        import org.junit.Test;
        import org.matsim.core.utils.misc.CRCChecksum;
        import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh, Joel Robertson
 *
 */
@Ignore
public class OtwaysBlockage50Test {
    // have tests in separate classes so that they run, at least und    er maven, in separate JVMs.  kai, nov'17

    Logger log = Logger.getLogger(OtwaysBlockage50Test.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test

    public void testBlockage50() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/otways-50/scenario_main.xml",
                "--logfile", "scenarios/surf-coast-shire/otways-50/scenario.log",
                "--loglevel", "INFO",
                //	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
                "--seed", "12345",
                "--safeline-output-file-pattern", "scenarios/surf-coast-shire/otways-50/safeline.%d%.out",
                MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:50}]," +
                "logLevel: WARN," +
                "logFile: \"scenarios/surf-coast-shire/otways-50/jill.log\"," +
                "programOutputFile: \"scenarios/surf-coast-shire/otways-50/jill.out\"," +
                "randomSeed: 12345," + // jill random seed
                "numThreads: 1" + // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-disruptions-file", "scenarios/surf-coast-shire/otways-50/scenario_disruptions.json",
//                "--x-blocked-link", "89175",
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

        TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,300.);
        TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,300.);
        TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 300.);
        TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

    }
}

