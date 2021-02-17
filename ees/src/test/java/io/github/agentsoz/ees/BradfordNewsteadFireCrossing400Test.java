
package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2021 by its authors. See AUTHORS file.
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
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dsingh
 *
 */

/**
 * Tests fire avoidance behaviour. Running at reduced capacity of 10% ie 40 agents.
 */
public class BradfordNewsteadFireCrossing400Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = LoggerFactory.getLogger(BradfordNewsteadFireCrossing400Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void test() {

		utils.getOutputDirectory(); // creates a clean one so need to call this first
		String[] args = {
				"--config", "scenarios/mount-alexander-shire/bradford-newstead-fire-crossing-400/ees.xml",
		};
		Run.main(args);

		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		// TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, true);
		// If the full events comparison fails (possibly due to multi-threading differences on travis/other),
		// then use the checks below, adjusting slack as needed,
		// but ideally keeping it below 10 secs; dhi 28/may/19
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,1.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 1.);
	}

}
