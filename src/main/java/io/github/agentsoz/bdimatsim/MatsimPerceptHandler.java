package io.github.agentsoz.bdimatsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

/**
 * @author Edmund Kemsley, Dhirendra Singh
 */

public final class MatsimPerceptHandler {

	private final AgentActivityEventHandler eventHandler;

	/**
	 * Constructor
	 * 
	 * @param matSimModel
	 */
	protected MatsimPerceptHandler(final MATSimModel matSimModel) {
		this.eventHandler = matSimModel.getEventHandler();
	}

	/**
	 * For a given agent, registers a {@link BDIPerceptHandler} to be called 
	 * whenever an event of type {@link MonitoredEventType} is triggered
	 * for {@code linkId}. 
	 * @param agentId
	 * @param linkId
	 * @param event
	 * @param handler
	 * @return
	 */
	public int registerBDIPerceptHandler(Id<Person> agentId, MonitoredEventType event,Id<Link> linkId, BDIPerceptHandler handler) {
		return eventHandler.registerMonitor(agentId, event, linkId, handler);
	}

	/**
	 * Process percepts
	 * 
	 * @param agentIDg
	 *            ID of the agent
	 * @param perceptID
	 *            Percept IDs are defined in {@link MatsimPerceptList}. eg:
	 *            ARRIVED, REQUESTLOCATION.
	 * @return The response
	 */
	@Deprecated
	final Object[] processPercept(String agentID, String perceptID) {
		System.err.println("processPercept(String agentID, String perceptID) should never be called (deprecated)!");
		return null;
	}
	
}
