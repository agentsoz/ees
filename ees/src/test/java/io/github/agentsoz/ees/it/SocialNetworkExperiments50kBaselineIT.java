
package io.github.agentsoz.ees.it;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
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
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dsingh, Joel Robertson
 *
 */
public class SocialNetworkExperiments50kBaselineIT {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(SocialNetworkExperiments50kBaselineIT.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testSocialNetwork50kBaseline() {

        utils.getOutputDirectory(); // creates a clean one so need to call this first
        String[] args = {
                "--config", "scenarios/surf-coast-shire/typical-summer-weekday-50k/social_network_experiments_baseline.xml",
        };
        Run.main(args);

//        final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
//        final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
//        TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,5.);
//        TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,5.);
//        TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 5.);
//        TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);
    }

}

