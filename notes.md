### How jill tells MATSim what to do

From Dhirendra:

1. During init, JillBDIModel.scheduleFireAlertsToResidents() is called to schedule the time (clock tick) at which each agent will be told to evacuate. This function distributes the times normally over some period around a given peak.
2. Then in JILLBDIModel.takeControl(), at the appropriate times, the FIREALERT message is pushed into the AgentDataContainer (the big data structure that gets passed around between BDI-MATSim.
3. This eventually filters down to each agent via io.github.agentsoz.bushfire.matsimjill.agents.Resident.handlePercept(), where each agent then posts a goal (io.github.agentsoz.bushfire.matsimjill.agents.RespondToFireAlert) to handle this percept.
4. Jill then eventually achieves this RespondToFireAlert goal for each agent by calling the io.github.agentsoz.bushfire.matsimjill.agents.EvacuateNow plan.
5. EvacuateNow executes an EnvironmentAction("drive to").
6. That eventually gets passed to MATSimAgentManager.updateActions() which calls initiateNewAction() that finally calls the agents ActioNHandler to process that action (agent.getActionHandler().processAction()).
