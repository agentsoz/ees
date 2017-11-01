/**
 * 
 */
package io.github.agentsoz.bushfire.matsimjill;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import io.github.agentsoz.bushfire.matsimjill.Main;
/**
 * @author dsingh
 *
 */
public class MainTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@SuppressWarnings("static-method")
	@Test
	public void testMaldon600() {

	  /*
--config /var/www/data/user-data/2017-10-23-ds-maldon/scenario/scenario_main.xml 
--logfile /var/www/data/user-data/2017-10-23-ds-maldon/scenario/scenario.log 
--loglevel TRACE 
--jillconfig "--config={agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:600}],logLevel: WARN,logFile: \"/var/www/data/user-data/2017-10-23-ds-maldon/scenario/jill.log\",programOutputFile: \"/var/www/data/user-data/2017-10-23-ds-maldon/scenario/jill.out\"}"
  */
      String [] args = {
          "--config",  "scenarios/maldon-2017-11-01/scenario_main.xml", 
          "--logfile", "scenarios/maldon-2017-11-01/scenario.log",
          "--loglevel", "INFO",
          "--plan-selection-policy", "FIRST", // ensures it is deterministic, as default is RANDOM
          "--seed", "12345",
          "--jillconfig", "--config={"+
              "agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:600}],"+
              "logLevel: WARN,"+
              "logFile: \"scenarios/maldon-2017-11-01/jill.log\","+
              "programOutputFile: \"scenarios/maldon-2017-11-01/jill.out\""+
              "}"};

      Main.main(args);

      
      long expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_plans.xml.gz" ) ;
      long actualCRC = CRCChecksum.getCRCFromFile( "output/output_plans.xml.gz" ) ;
      Assert.assertEquals (expectedCRC, actualCRC);

      // FIXME: still differences due to the timing of logging from different jill agents
      // that exist in different intention selector threads. dsingh, 02nov/17
      // expectedCRC = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/jill.out" ) ;
      // actualCRC = CRCChecksum.getCRCFromFile( "scenarios/maldon-2017-11-01/jill.out" ) ;
      // Assert.assertEquals (expectedCRC, actualCRC); 

	}
}
