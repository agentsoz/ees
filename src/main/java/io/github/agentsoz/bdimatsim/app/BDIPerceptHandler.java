package io.github.agentsoz.bdimatsim.app;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import io.github.agentsoz.bdimatsim.AgentActivityEventHandler.MonitoredEventType;

public interface BDIPerceptHandler {

	/**
	 * Handles the given {@link MonitoredEventType}.
	 * <p>
	 * Returning true will cause this handler to be removed (seful for handling
	 * some event once). Returning false will keep this handler registered such
	 * that it gets called again on subsequent events that match. 
	 * @param agentId
	 * @param linkId
	 * @param monitoredEvent
	 * @return
	 */
	public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent); 
	// yyyy we have a tendency to pass "global" infrastructure (here: MATSim Model) rather through the constructor.  Reasons:
	// * The constructor is not part of the interface, so it is easier to change in specific implementations.
	// * People start putting something like "this.model = model" into the handle method in order to remember the global
	// object elsewhere in the handler class.  Then, however, it is more transparent to have it in the constructor right away.
	// kai, nov'17

}
