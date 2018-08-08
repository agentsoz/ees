/**
 *
 */
package io.github.agentsoz.ees;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh, Joel Robertson
 *
 */
public class DailyPlansInterruptedTest {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = Logger.getLogger(DailyPlansInterruptedTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    @Ignore
    public void testDailyPlansInterrupted() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/plan-interrupted/scenario_main.xml",
                "--logfile", utils.getOutputDirectory()+"../scenario.log",
                "--loglevel", "INFO",
                "--seed", "12345",
                "--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
                "--matsim-output-directory", utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[]," +
                "logLevel: WARN," +
                "logFile: \""+utils.getOutputDirectory()+"../jill.log\"," +
                "programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\"," +
                "randomSeed: 12345" + // jill random seed
                //",numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "180:0.33:1.0",
                "--x-load-bdi-agents-from-matsim-plans-file", "true", // Load agents from MATSim plans file
        };

        Main.main(args);

    }

}

