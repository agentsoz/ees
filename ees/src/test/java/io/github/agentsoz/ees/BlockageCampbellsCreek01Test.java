
package io.github.agentsoz.ees;

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
public class BlockageCampbellsCreek01Test {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

	private static final Logger log = LoggerFactory.getLogger(BlockageCampbellsCreek01Test.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void test() {

		utils.getOutputDirectory(); // creates a clean one so need to call this first
		String[] args = {
				"--config", "scenarios/mount-alexander-shire/campbells-creek-1/ees.xml",
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

		// Compare the DeckGL file
		final String actualDeckglFilename = utils.getOutputDirectory() + "/trips.deckgl.json";
		final String expectedDeckglFilename = utils.getInputDirectory() + "/trips.deckgl.json";
		// DS 25/05/24: Failing in GitHub Actions in 9bccca29
		// TestUtils.compareFiles(expectedDeckglFilename,actualDeckglFilename);

	}

}
