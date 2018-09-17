package io.github.agentsoz.ees.agents;

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




import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.ees.SimpleConfig;
import io.github.agentsoz.ees.agents.RespondToFire;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

@AgentInfo(hasGoals={"io.github.agentsoz.ees.agents.RespondToFire", "io.github.agentsoz.abmjill.genact.EnvironmentAction"})
public class Tourist extends  Agent implements io.github.agentsoz.bdiabm.Agent {

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    PrintStream writer = null;
    private Location FreewayEntraceLocation = null;
    private double CongestionEvaluationInterval = 600;
    private double CongestionToleranceThreshold = 0.5;


    private io.github.agentsoz.ees.agents.Tourist.Prefix prefix = new io.github.agentsoz.ees.agents.Tourist.Prefix();
    private int failedAttempts;
    private double time = -1;
    private QueryPerceptInterface queryInterface;




    public Tourist(String id) {
        super(id);
    }

    Location getFreewayEntraceLocation() {
        return FreewayEntraceLocation;
    }

    Double getCongestionEvaluationInterval() {return CongestionEvaluationInterval;}
    Double getCongestionToleranceThreshold() {return CongestionToleranceThreshold;}




    /**
     * Called by the Jill model when starting a new agent.
     * There is no separate initialisation call prior to this, so all
     * agent initialisation should be done here (using params).
     */
    @Override
    public void start(PrintStream writer, String[] params) {
        this.writer = writer;
        parseArgs(params);
        SimpleConfig.setCongestionEvaluationInterval(CongestionEvaluationInterval);
        SimpleConfig.setCongestionToleranceThreshold(CongestionToleranceThreshold);
    }

    private void parseArgs(String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {

                 if("--WayHome".equals(args[i])) {
                    String[] sCoords = args[i+1].split(",");
                    FreewayEntraceLocation = new Location(sCoords[0], Double.parseDouble(sCoords[1]), Double.parseDouble(sCoords[2]));

                }

                if("--CongestionBehavior".equals(args[i])){
                    CongestionEvaluationInterval = Double.parseDouble(args[i+1]);
                    CongestionToleranceThreshold = Double.parseDouble(args[i+2]);
                }
            }
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
            // Agent just arrived at freeway entrance
            writer.println(prefix + "arrived at entrance to " + getFreewayEntraceLocation());
        } else if (perceptID.equals(PerceptList.BLOCKED)) {
            // Something went wrong while driving and the road is blocked
            failedAttempts++;
            writer.println(prefix + "is blocked (" + parameters + ")");
            if (failedAttempts < MAX_FAILED_ATTEMPTS) {
                writer.println(prefix
                        + "will try to evacuate again (attempt "+(failedAttempts+1)
                        + " of " + MAX_FAILED_ATTEMPTS
                        + ")");
                post(new RespondToFire("RespondToFire"));
            } else {
                writer.println(prefix + "is giving up after " + failedAttempts + " failed attempts to evacuate");
            }
        } else if (perceptID.equals(PerceptList.FIRE_ALERT)) {
            // Received a fire alert so act now
            writer.println(prefix + "received fire alert");
            post(new RespondToFire("RespondToFire"));
        }
    }

    /**
     * Called by the Jill model when this agent posts a new BDI action
     * to the ABM environment
     */
    @Override
    public void packageAction(String actionID, Object[] parameters) {
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

    int getFailedAttempts() {
        return failedAttempts;
    }
    
    public String logPrefix() {
        return prefix.toString();
    }

    class Prefix{
        public String toString() {
            return String.format("Time %05.0f Tourist %-4s : ", getTime(), getId());
        }
    }
}
