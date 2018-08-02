/**
 * 
 */
package io.github.agentsoz.ees;

import io.github.agentsoz.util.TestUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dsingh
 *
 */
@Ignore
public class AttributesInPopulationTest {
	// have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17
	
	Logger log = Logger.getLogger( AttributesInPopulationTest.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testAttribsInPopulation() {
		final String filename = utils.getOutputDirectory() + "pop.xml.gz";
		
		// Step 1: Generate and write a person with attributes (could be done by other means, e.g. in an editor):
		{
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			Population pop = scenario.getPopulation();
			PopulationFactory pf = pop.getFactory();
			
			for (int ii = 0; ii < 10; ii++) {
				Person person = pf.createPerson(Id.createPersonId(ii));
				if (ii % 2 == 0) {
					person.getAttributes().putAttribute("BDIType", "Resident");
				}
				pop.addPerson(person);
			}
			
			PopulationUtils.writePopulation(pop, filename);
		}
		// ===
		
		// Step 2: extract this information (e.g. in the Main method of the bushfire module):
		{
			Config config = ConfigUtils.createConfig();
			config.plans().setInputFile(filename);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
			// the code from here ...
			for (Person person : scenario.getPopulation().getPersons().values()) {
				String bdiType = (String) person.getAttributes().getAttribute("BDIType");
				if (bdiType!=null && bdiType.equals("Resident")) {
					// instantiate a a resident
				} else {
					// otherwise not
				}
			}
			// ... to here is really all that needs to be inserted into the bushfire Main.
			
		}
	}
		
}
