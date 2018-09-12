/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.util.TestUtils;
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
public class Maldon100WithEmergencyVehiclesTest {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	private static final Logger log = LoggerFactory.getLogger(Maldon100WithEmergencyVehiclesTest.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testMaldon10WithEmergencyVehiclesV2() {

		utils.getOutputDirectory(); // creates a clean one so need to call this first
		String[] args = {
				"--config", "scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles/ees.xml",
		};
		// FIXME: io.github.agentsoz.ees.agents.Responder does not work as it does not react to fire/messaging
		// As a result, all three emergency responders in the test do not move. Accepting for now.
		Run.main(args);

		final String actualEventsFilename = utils.getOutputDirectory() + "/output_events.xml.gz";
		final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
		TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,10.);
		TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,10.);
		TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 10.);
		TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);

	}

}
