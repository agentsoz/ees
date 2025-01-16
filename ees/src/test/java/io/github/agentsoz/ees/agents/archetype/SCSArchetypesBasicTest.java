
package io.github.agentsoz.ees.agents.archetype;

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
import io.github.agentsoz.util.TestUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dhi Singh
 *
 */
public class SCSArchetypesBasicTest {

    private static final Logger log = LoggerFactory.getLogger(SCSArchetypesBasicTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    @Ignore // FIXME: fails intermittently on GitHub CI server, 26/Apr/21 Dhi
    public void test() {

        utils.getOutputDirectory(); // creates a clean one so need to call this first
        String[] args = {
                "--config", "scenarios/surf-coast-shire/archetypes-basic/ees.xml",
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

