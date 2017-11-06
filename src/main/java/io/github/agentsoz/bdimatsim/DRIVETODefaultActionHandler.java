package io.github.agentsoz.bdimatsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.SearchableNetwork;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

final class DRIVETODefaultActionHandler implements BDIActionHandler {
	@Override
	public boolean handle(String agentID, String actionID, Object[] args, MATSimModel model) {
		// Get nearest link ID and calls the Replanner to map to MATSim.
		Id<Link> newLinkId;
		if (args[1] instanceof double[]) {
			double[] coords = (double[]) args[1];
			newLinkId = ((SearchableNetwork) model.getScenario()
					.getNetwork()).getNearestLinkExactly(
					new Coord(coords[0], coords[1])).getId();
		} else {
			throw new RuntimeException("Destination coordinates are not given");
		}

		model.getReplanner().attachNewActivityAtEndOfPlan(newLinkId, Id.createPersonId(agentID), model);

		// Now register a event handler for when the agent arrives at the destination
		MATSimAgent agent = model.getBDIAgent(agentID);
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(), 
				MonitoredEventType.ArrivedAtDestination, 
				newLinkId,
				new BDIPerceptHandler() {
					@Override
					public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent, MATSimModel model) {
						MATSimAgent agent = model.getBDIAgent(agentId);
						Object[] params = { linkId.toString() };
						agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
						agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
						return true;
					}
				});
		return true;
	}
}