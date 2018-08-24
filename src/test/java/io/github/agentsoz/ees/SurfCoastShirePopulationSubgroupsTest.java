/**
 *
 */
package io.github.agentsoz.ees;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dsingh, Joel Robertson
 *
 */
public class SurfCoastShirePopulationSubgroupsTest {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(SurfCoastShirePopulationSubgroupsTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();


    @Test
    public void testSurfCoastShirePopulationSubgroups() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/population-subgroups/ees.xml",
        };
        Run.main(args);
    }

    @Test
    @Ignore
    public void testSurfCoastShirePopulationSubgroupsLegacyConfig() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/population-subgroups/scenario_main.xml",
                "--logfile", utils.getOutputDirectory()+"../scenario.log",
                "--loglevel", "INFO",
                "--seed", "12345",
                "--safeline-output-file-pattern", utils.getOutputDirectory()+"../safeline.%d%.out",
                "--matsim-output-directory", utils.getOutputDirectory(),
                "--jillconfig",
                "--plan-selection-policy=FIRST=" + // to allow fallback to last option PlanDoNothing
                "--config={" +
                "agents:[],"+ // must be this string; will be replaced based on MATSim plans file
                "logLevel: WARN," +
                "logFile: \""+utils.getOutputDirectory()+"../jill.log\"," +
                "programOutputFile: \""+utils.getOutputDirectory()+"../jill.out\"," +
                "randomSeed: 12345" + // jill random seed
                ",numThreads: 1"+ // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "180:0.33:1.0",
                "--x-load-bdi-agents-from-matsim-plans-file", "true", // Load agents from MATSim plans file
                "--x-messages-file", "scenarios/surf-coast-shire/population-subgroups/scenario_messages.json", // specifies when to send evac now msg
                "--x-zones-file", "scenarios/surf-coast-shire/population-subgroups/Anglesea_SA1s_WSG84.json", // map from zone (SA1) ids to shapes
        };

        Main.main(args);

    }

}

