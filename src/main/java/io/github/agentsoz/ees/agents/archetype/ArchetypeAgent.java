package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */



import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.ees.ActionList;
import io.github.agentsoz.ees.PerceptList;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Map;

/**
 * ArchetypeAgent Class
 *
 * author: dsingh
 */
@AgentInfo(hasGoals={"io.github.agentsoz.ees.agents.bushfire.GoalDoNothing"})
public abstract class ArchetypeAgent extends Agent implements io.github.agentsoz.bdiabm.Agent {


    //===============================================================================
    //region Class constants
    //===============================================================================
    private final Logger logger = LoggerFactory.getLogger(ArchetypeAgent.class);

    //===============================================================================
    //endregion
    //===============================================================================

    //===============================================================================
    //region Class variables
    //===============================================================================

    private PrintStream writer = null;
    private double time = -1;
    private ArchetypeAgent.Prefix prefix = new ArchetypeAgent.Prefix();

    //===============================================================================
    //endregion
    //===============================================================================


    //===============================================================================
    //region Class functionality
    //===============================================================================

    /**
     * Constructor.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform any agent specific initialisation.
     * @param name
     */
    public ArchetypeAgent(String name) {
        super(name);
    }


    //===============================================================================
    //endregion
    //===============================================================================


    //===============================================================================
    //region BDI percepts
    //===============================================================================

    /**
     * Handles the TIME percept
     * @param parameters
     */
    private void handleTime(Object parameters) {
        if (parameters instanceof Double) {
            time = (double) parameters;
        }
    }

    private void handleArrived(Object parameters) {
    }

    private void handleBlocked(Object parameters) {
    }

    private void handleCongestion(Object parameters) {
    }

    private void handleFieldOfView(Object parameters) {
    }

    private void handleEmergencyMessage(Object parameters) {
    }

    private void handleSocialNetworkMessage(Object parameters) {
    }


    //===============================================================================
    //endregion
    //===============================================================================

    //===============================================================================
    //region BDI actions
    //===============================================================================

    // Map of active BDI actions, one per action type supported only
    private Map<String,EnvironmentAction> activeBdiActions;

    // Last BDI action done (used for checking status of finished drive actions)
    EnvironmentAction lastBdiAction;

    private void setLastBdiAction(EnvironmentAction action) {
        this.lastBdiAction = action;
    }

    public EnvironmentAction getLastBdiAction() {
        return lastBdiAction;
    }

    private void addActiveEnvironmentAction(EnvironmentAction activeEnvironmentAction) {
        activeBdiActions.put(activeEnvironmentAction.getActionID(), activeEnvironmentAction);
    }

    private EnvironmentAction removeActiveEnvironmentAction(String actionId) {
        if (actionId != null && activeBdiActions.containsKey(actionId)) {
            return activeBdiActions.remove(actionId);
        }
        return null;
    }

    //===============================================================================
    //endregion
    //===============================================================================



    //===============================================================================
    //region BDI beliefs
    //===============================================================================

    // This agent's belief set
    private static final String beliefSetName = "mem";

    /**
     * Creates all belief sets for this BDI agent
     */
    private void createBeliefSets() {
        // Create a new belief set to store memory
        BeliefSetField[] fields = {
                new BeliefSetField("key", String.class, false),
                new BeliefSetField("value", String.class, false),
        };
        try {
            this.createBeliefSet(beliefSetName, fields); // Attach this belief set to this agent
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a belief for this agent
     * @param key the belief name
     * @param value the value of the belief
     */
    void believe(String key, String value) {
        try {
            addBelief(beliefSetName, key, value);
            out("believed " + key + "=" + value);
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }


    //===============================================================================
    //endregion
    //===============================================================================



    //===============================================================================
    //region Getters & Setters
    //===============================================================================

    public double getTime() {
        return time;
    }


    //===============================================================================
    //endregion
    //===============================================================================


    //===============================================================================
    //region Arguments parsing
    //===============================================================================

    private void parseArgs(String[] params) {
    }

    //===============================================================================
    //endregion
    //===============================================================================

    //===============================================================================
    //region Jill Agent inherited functions
    //===============================================================================

    /**
     * Called by the Jill model when starting a new agent.
     * There is no separate initialisation call prior to this, so all
     * agent initialisation should be done here (using params).
     */
    @Override
    public void start(PrintStream writer, String[] params) {
        this.writer = writer;
        parseArgs(params);
        createBeliefSets();

        // perceive congestion and blockage events always
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                ActionList.PERCEIVE,
                new Object[] {PerceptList.BLOCKED, PerceptList.CONGESTION});
        post(action);
        addActiveEnvironmentAction(action);
    }


    /**
     * Called by the Jill model when terminating
     */
    @Override
    public void finish() {
    }

    //===============================================================================
    //endregion
    //===============================================================================


    //===============================================================================
    //region BDI-ABM Agent interface functions
    //===============================================================================

    private QueryPerceptInterface queryInterface;
    private EnvironmentActionInterface envActionInterface;

    @Override
    public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
        this.queryInterface = queryInterface;
    }

    @Override
    public QueryPerceptInterface getQueryPerceptInterface() {
        return queryInterface;
    }

    @Override
    public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface) {
        this.envActionInterface = envActInterface;
    }

    @Override
    public EnvironmentActionInterface getEnvironmentActionInterface() {
        return envActionInterface;
    }

    /**
     * BDI-ABM agent init function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform any agent specific initialisation.
     */
    @Override
    public void init(String[] args) {
        throw new RuntimeException(logPrefix()
                + "unexpected call, "
                + "use start(PrintStream, String[]) instead");
    }

    /**
     * BDI-ABM agent start function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform agent startup.
     */
    @Override
    public void start() {
        throw new RuntimeException(logPrefix()
                + "unexpected call, "
                + "use start(PrintStream, String[]) instead");
    }

    /**
     * BDI-ABM agent kill function; Not used by Jill.
     * Use {@link #finish()} instead
     * to perform agent termination.
     */
    @Override
    public void kill() {
        throw new RuntimeException(logPrefix()
                + "unexpected call, "
                + "use finish() instead");
    }

    /**
     * Called by the Jill model with the status of a BDI percept
     * for this agent, coming from the ABM environment.
     */
    @Override
    public void handlePercept(String perceptID, Object parameters) {
        if (perceptID == null || perceptID.isEmpty()) {
            return;
        }
        switch(perceptID) {
            case PerceptList.TIME:
                handleTime(parameters);
                break;
            case PerceptList.ARRIVED:
                handleArrived(parameters);
                break;
            case PerceptList.BLOCKED:
                handleBlocked(parameters);
                break;
            case PerceptList.CONGESTION:
                handleCongestion(parameters);
                break;
            case PerceptList.FIELD_OF_VIEW:
                handleFieldOfView(parameters);
                break;
            case PerceptList.EMERGENCY_MESSAGE:
                handleEmergencyMessage(parameters);
                break;
            case PerceptList.SOCIAL_NETWORK_MSG:
                handleSocialNetworkMessage(parameters);
                break;
            default:
                logger.warn("{} received unknown percept '{}'", logPrefix(), perceptID);

        }
    }

    /**
     * Called by the Jill model with the status of a BDI action previously
     * posted by this agent to the ABM environment.
     */
    @Override
    public void updateAction(String actionID, ActionContent content) {
        logger.debug("{} received action update: {}", logPrefix(), content);
        ActionContent.State actionState = content.getState();
        if (actionState == ActionContent.State.PASSED ||
                actionState == ActionContent.State.FAILED ||
                actionState == ActionContent.State.DROPPED) {


            //memorise(BushfireAgent.MemoryEventType.BELIEVED.name(),
            //        BushfireAgent.MemoryEventValue.LAST_ENV_ACTION_STATE.name() + "=" + actionState.name());


            // remove the action and records it as the last action performed
            setLastBdiAction(removeActiveEnvironmentAction(content.getAction_type()));
            if (content.getAction_type().equals(ActionList.DRIVETO)) {
                // Wake up the agent that was waiting for drive action to finish
                suspend(false);
            }
        }
    }

    //===============================================================================
    //endregion
    //===============================================================================


    //===============================================================================
    //region Logging
    //===============================================================================

    class Prefix{
        public String toString() {
            return String.format("Time %05.0f BushfireAgent %-4s : ", getTime(), getId());
        }
    }

    public String logPrefix() {
        return prefix.toString();
    }

    void out(String msg) {
        writer.println(logPrefix()+msg);
    }

    //===============================================================================
    //endregion
    //===============================================================================

}
