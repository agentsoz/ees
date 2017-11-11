package io.github.agentsoz.bdimatsim.app;

public interface BDIActionHandler {
	
	public boolean handle(String agentID, String actionID, Object[] actionArgs); 
	// yyyy we have a tendency to pass "global" infrastructure (here: MATSim Model) rather through the constructor.  Reasons:
	// * The constructor is not part of the interface, so it is easier to change in specific implementations.
	// * People start putting something like "this.model = model" into the handle method in order to remember the global
	// object elsewhere in the handler class.  Then, however, it is more transparent to have it in the constructor right away.
	// kai, nov'17

}
