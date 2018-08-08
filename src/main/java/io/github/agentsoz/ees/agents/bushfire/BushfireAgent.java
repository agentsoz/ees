package io.github.agentsoz.ees.agents.bushfire;


/*
 * #%L
 * Jill Cognitive Agents Platform
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

@AgentInfo(hasGoals={"io.github.agentsoz.ees.agents.bushfire.GoalDoNothing"})
public class BushfireAgent extends  Agent implements io.github.agentsoz.bdiabm.Agent {

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");
    PrintStream writer = null;
    private QueryPerceptInterface queryInterface;
    private double time = -1;
    private BushfireAgent.Prefix prefix = new BushfireAgent.Prefix();

    public BushfireAgent(String id) {
        super(id);
    }

    /**
     * Called by the Jill model when starting a new agent.
     * There is no separate initialisation call prior to this, so all
     * agent initialisation should be done here (using params).
     */
    @Override
    public void start(PrintStream writer, String[] params) {
        this.writer = writer;
        parseArgs(params);
    }

    private void parseArgs(String[] args) {
        if (args != null) {
            logger.warn("{} ignoring received args: {}", prefix, args);
        }
    }


    /**
     * Called by the Jill model when terminating
     */
    @Override
    public void finish() {
        logger.trace("{} is terminating", prefix);
    }

    /**
     * Called by the Jill model with the status of a BDI percept
     * for this agent, coming from the ABM environment.
     */
    @Override
    public void handlePercept(String perceptID, Object parameters) {
        if (perceptID.equals(PerceptList.TIME)) {
            if (parameters instanceof Double) {
                time = (double) parameters;
            }
        } else if (perceptID.equals(PerceptList.ARRIVED)) {
            // Agent just arrived
            writer.println(prefix + "arrived at " + parameters);
        } else if (perceptID.equals(PerceptList.BLOCKED)) {
            // Something went wrong while driving and the road is blocked
            writer.println(prefix + "is blocked (" + parameters + ")");
        } else if (perceptID.equals(PerceptList.FIRE_ALERT)) {
            // Received a fire alert so act now
            writer.println(prefix + "received fire alert");
        }
    }

    /**
     * Called by the Jill model when this agent posts a new BDI action
     * to the ABM environment
     */
    @Override
    public void packageAction(String actionID, Object[] parameters) {
        logger.warn("{} ignoring action {}", prefix, actionID);
    }

    /**
     * Called by the Jill model with the status of a BDI action previously
     * posted by this agent to the ABM environment.
     */
    @Override
    public void updateAction(String actionID, ActionContent content) {
        logger.debug("{} received action update: {}", prefix, content);
        if (content.getAction_type().equals(ActionList.DRIVETO)) {
            if (content.getState()== ActionContent.State.PASSED) {
                // Wake up the agent that was waiting for external action to finish
                // FIXME: BDI actions put agent in suspend, which won't work for multiple intention stacks
                suspend(false);
            } else if (content.getState()== ActionContent.State.FAILED) {
                // Wake up the agent that was waiting for external action to finish
                // FIXME: BDI actions put agent in suspend, which won't work for multiple intention stacks
                suspend(false);
            }
        }
    }

    /**
     * BDI-ABM agent init function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform any agent specific initialisation.
     */
    @Override
    public void init(String[] args) {
        parseArgs(args);
    }

    /**
     * BDI-ABM agent start function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform agent startup.
     */
    @Override
    public void start() {
        logger.warn("{} using a stub for io.github.agentsoz.bdiabm.Agent.start()", prefix);
    }

    /**
     * BDI-ABM agent kill function; Not used by Jill.
     * Use {@link #finish()} instead
     * to perform agent termination.
     */

    @Override
    public void kill() {
        logger.warn("{} using a stub for io.github.agentsoz.bdiabm.Agent.kill()", prefix);
    }

    @Override
    public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
        this.queryInterface = queryInterface;
    }

    @Override
    public QueryPerceptInterface getQueryPerceptInterface() {
        return queryInterface;
    }


    double getTime() {
        return time;
    }

    class Prefix{
        public String toString() {
            return String.format("Time %05.0f BushfireAgent %-4s : ", getTime(), getId());
        }
    }
}
