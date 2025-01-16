
package io.github.agentsoz.ees.it;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import io.github.agentsoz.ees.Run;
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
public class SurfCoastShirePopulationSubgroupsIT {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(SurfCoastShirePopulationSubgroupsIT.class);

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

        //Main.main(args);

    }

}

