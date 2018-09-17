
package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.util.TestUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dsingh, Joel Robertson
 *
 */
public class SurfCoastShirePopulationSubgroupsSampleTest {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(SurfCoastShirePopulationSubgroupsSampleTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testSurfCoastShirePopulationSubgroupsSample() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/population-subgroups-sample/ees.xml",
        };
        Run.main(args);
    }

    @Test
    @Ignore
    public void testSurfCoastShirePopulationSubgroupsSampleLegacyConfig() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/population-subgroups-sample/scenario_main.xml",
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
                "--x-messages-file", "scenarios/surf-coast-shire/population-subgroups-sample/scenario_messages.json", // specifies when to send evac now msg
                "--x-zones-file", "scenarios/surf-coast-shire/population-subgroups-sample/Anglesea_SA1s_WSG84.json", // map from zone (SA1) ids to shapes
        };

        Main.main(args);

    }

}

