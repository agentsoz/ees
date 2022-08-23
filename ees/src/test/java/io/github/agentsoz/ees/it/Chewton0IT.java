package io.github.agentsoz.ees.it;

import io.github.agentsoz.ees.Run;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chewton0IT {


    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(SouthEastQld0IT.class) ;

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void test() {

        utils.getOutputDirectory(); // creates a clean one so need to call this first
        String[] args = {
                "--config", "scenarios/mount-alexander-shire/chewton/0.baseline.xml",
        };
        Run.main(args);

    }
}
