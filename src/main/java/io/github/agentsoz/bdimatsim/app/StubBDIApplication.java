package io.github.agentsoz.bdimatsim.app;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;

import io.github.agentsoz.bdimatsim.MATSimActionHandler;
import io.github.agentsoz.bdimatsim.MatsimPerceptHandler;

public class StubBDIApplication implements BDIApplicationInterface {

	@Override
	public void registerNewBDIActions(MATSimActionHandler withHandler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerNewBDIPercepts(MatsimPerceptHandler withHandler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyBeforeCreatingBDICounterparts(List<Id<Person>> bdiAgentsIds) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyAfterCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {
		// TODO Auto-generated method stub

	}

}
