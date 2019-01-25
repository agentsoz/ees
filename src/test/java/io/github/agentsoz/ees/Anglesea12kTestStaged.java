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



import io.github.agentsoz.bdimatsim.MATSimModel;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dsingh
 *
 */
@Ignore
public class Anglesea12kTestStaged {
    // have tests in separate classes so that they run, at least und    er maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(Anglesea12kTestStaged.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    @Ignore // in intellij, the annotation at the class level is not sufficient.  kai, nov'18
    public void testAnglesea12kStaged() {

        String[] args = {
                "--config", "scenarios/surf-coast-shire/anglesea-12k/scenario_main.xml",
                "--logfile", "scenarios/surf-coast-shire/anglesea-12k/scenario.log",
                "--loglevel", "INFO",
                //	                "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
                "--seed", "12345",
                "--safeline-output-file-pattern", "scenarios/surf-coast-shire/anglesea-12k/safeline.%d%.out",
                MATSimModel.MATSIM_OUTPUT_DIRECTORY_CONFIG_INDICATOR, utils.getOutputDirectory(),
                "--jillconfig", "--config={" +
                "agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:12052}]," +
                "logLevel: WARN," +
                "logFile: \"scenarios/surf-coast-shire/anglesea-12k/jill.log\"," +
                "programOutputFile: \"scenarios/surf-coast-shire/anglesea-12k/jill.out\"," +
                "randomSeed: 12345," + // jill random seed
                "numThreads: 1" + // run jill in single-threaded mode so logs are deterministic
                "}",
                "--x-congestion-config", "100000:100000", // virtually disallow congestion re-routing (painfully slow otherwise!)
                "--sendFireAlertOnFireStart", "false", // disable fire alert from fire model, instead will use messaging
                "--x-messages-file", "scenarios/surf-coast-shire/anglesea-12k/scenario_messages_staged.json", // specifies when to send evac now msg
                "--x-zones-file", "scenarios/surf-coast-shire/anglesea-12k/Anglesea_SA1s_WSG84.json", // map from zone (SA1) ids to shapes
        };

        //Main.main(args);

        final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
        long actualEventsCRC = CRCChecksum.getCRCFromFile( actualEventsFilename ) ;
        System.err.println("actual(events)="+actualEventsCRC) ;

        long actualPlansCRC = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_plans.xml.gz" ) ;
        System.err.println("actual(plans)="+actualPlansCRC) ;

        // ---

        final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";

        // ---

        //TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,300.);
        //TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 300.);
        //TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

    }
}

