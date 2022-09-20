package io.github.agentsoz.ees.it;

import io.github.agentsoz.ees.Run;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthEastQldNetworkChangeEventsTestIT {




        // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

        private static final Logger log = LoggerFactory.getLogger(SouthEastQldNetworkChangeEventsTestIT.class) ;

        @Rule
        public MatsimTestUtils utils = new MatsimTestUtils() ;

        @Test
        public void test() {

            utils.getOutputDirectory(); // creates a clean one so need to call this first
            String[] args = {
                    "--config", "scenarios/qld/south_east_qld/0.baseline-network-change-events-test.xml",
            };
            Run.main(args);

        }

}
