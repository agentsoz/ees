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
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.jill.core.beliefbase.Belief;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.core.utils.misc.Time.writeTime;

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

    enum Beliefname {
        Age("Age"),
        AgentId("AgentId"),
        Archetype("Archetype"),
        ArchetypeAge("Archetypes.Age"),
        ArchetypeHousehold("Archetypes.Household"),
        AgentType("BDIAgentType"),
        Address("EZI_ADD"),
        Gender("Gender"),
        AddressCoordinates("Geographical.Coordinate"),
        HasDependents("HasDependents"),
        HasDependentsAtLocation("HasDependentsAtLocation"),
        HouseholdId("HouseholdId"),
        isDriving("isDriving"),
        ImpactFromFireDangerIndexRating("ImpactFromFireDangerIndexRating"),
        ImpactFromImmersionInSmoke("ImpactFromImmersionInSmoke"),
        ImpactFromMessageAdvice("ImpactFromMessageAdvice"),
        ImpactFromMessageEmergencyWarning("ImpactFromMessageEmergencyWarning"),
        ImpactFromMessageEvacuateNow("ImpactFromMessageEvacuateNow"),
        ImpactFromMessageRespondersAttending("ImpactFromMessageRespondersAttending"),
        ImpactFromMessageWatchAndAct("ImpactFromMessageWatchAndAct"),
        ImpactFromSocialMessage("ImpactFromSocialMessage"),
        ImpactFromVisibleEmbers("ImpactFromVisibleEmbers"),
        ImpactFromVisibleFire("ImpactFromVisibleFire"),
        ImpactFromVisibleResponders("ImpactFromVisibleResponders"),
        ImpactFromVisibleSmoke("ImpactFromVisibleSmoke"),
        LocationEvacuationPreference("EvacLocationPreference"),
        LocationInvacPreference("InvacLocationPreference"),
        LocationHome("home"),
        LocationWork("work"),
        PrimaryFamilyType("PrimaryFamilyType"),
        ResponseThresholdFinal("ResponseThresholdFinal"),
        ResponseThresholdInitial("ResponseThresholdInitial"),
        Sa1("SA1_7DIGCODE"),
        Sa2("SA2_MAINCODE"),
        WillGoHomeAfterVisitingDependents("WillGoHomeAfterVisitingDependents"),
        WillGoHomeBeforeLeaving("WillGoHomeBeforeLeaving"),
        WillStay("WillStay"),
        ;

        private final String commonName;

        Beliefname(String name){
            this.commonName = name;
        }

        public String getCommonName() {
            return commonName;
        }
    };

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
        activeBdiActions = new HashMap<>();
    }


    Location parseLocation(String slocation) {
        if (slocation != null && !slocation.isEmpty()) {
            try {
                String[] tokens = slocation.split(",");
                String loc = tokens[0];
                double[] coords = new double[]{Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2])};
                return new Location(loc, coords[0], coords[1]);
            } catch (Exception e) {
                logger.error("Could not parse location: " + slocation);
            }
        }
        return null;
    }

    double getTravelDistanceTo(Location location) {
        double dist = (location == null) ? -1 :
                (double) getQueryPerceptInterface().queryPercept(
                        String.valueOf(getId()),
                        Constants.REQUEST_DRIVING_DISTANCE_TO,
                        location.getCoordinates());
        return dist;
    }

    Goal prepareDrivingGoal(Constants.EvacActivity activity, Location location, Constants.EvacRoutingMode routingMode) {
        // perceive congestion and blockage events always
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.PERCEIVE,
                new Object[] {Constants.BLOCKED, Constants.CONGESTION});
        post(action);
        addActiveEnvironmentAction(action);

        Object[] params = new Object[6];
        params[0] = Constants.DRIVETO;
        params[1] = location.getCoordinates();
        params[2] = getTime() + 5.0; // five secs from now;
        params[3] = routingMode;
        params[4] = activity.toString();
        params[5] = true; // add zero-time replan activity to mark location/time of replanning
        action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.DRIVETO, params);
        addActiveEnvironmentAction(action); // will be reset by updateAction()
        return action;
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
            setTime((double) parameters);
        }
    }

    private void handleArrived(Object parameters) {
    }

    private void handleBlocked(Object parameters) {
    }

    private void handleCongestion(Object parameters) {
    }

    private void handleFieldOfView(Object parameters) {
        post(new GotoLocationEvacuation(GotoLocationEvacuation.class.getSimpleName()));
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
        try {
            this.createBeliefSet(beliefSetName,
                    new BeliefSetField[]{
                            new BeliefSetField("key", String.class, true),
                            new BeliefSetField("value", String.class, false),
                    });
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
            out("believes " + key + "=" + value);
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get's the named belief
     * @param key the name of the belief
     * @return the value of the belief, or null if not found
     */
    String getBelief(String key) {
        if (key != null) {
            String query = beliefSetName + ".key=" + key;
            try {
                if (eval(query)) {
                    return (String) getLastResults().toArray(new Belief[0])[0].getTuple()[1];
                }
            } catch (BeliefBaseException e) {
                logger.error("Could not evaluate belief query:  " + query, e);
            }
        }
        return null;
    }


    /**
     * Queries this agent's beliefs
     * @param key the name of the belief
     * @param value the value of the belief
     * @return true if the agent hold's the belief with that value
     */
    boolean hasBelief(String key, String value) {
        if (key == null || value == null) {
            return false;
        }
        String beliefValue = getBelief(key);
        if (beliefValue != null && ("*".equals(value) || beliefValue.equals(value))) {
            return true;
        }
        return false;
    }

    //===============================================================================
    //endregion
    //===============================================================================



    //===============================================================================
    //region Class getters & setters
    //===============================================================================

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;

    }


    //===============================================================================
    //endregion
    //===============================================================================


    //===============================================================================
    //region Arguments parsing
    //===============================================================================

    private void parseArgs(String[] args) {
        for (int i = 0; args!= null && i < args.length; i++) {
            String key = args[i];
            String value = null;
            if (i + 1 < args.length) {
                i++;
                value = args[i];
            }
            boolean found = false;
            for(Beliefname beliefname : Beliefname.values()) {
                if (key.equals(beliefname.getCommonName())) {
                    believe(beliefname.name(), value);
                    found = true;
                }
            }
            if (!found) {
                String s = "Ignoring unknown key/value: " + key + "=" + value;
                out(s);
                logger.warn(s);

            }
        }
    }

    //===============================================================================
    //endregion
    //===============================================================================

    //===============================================================================
    //region Jill Agent functions
    //===============================================================================

    /**
     * Called by the Jill model when starting a new agent.
     * Prior to this {@link #init(String[])} is called with initialisation arguments
     * too.
     */
    @Override
    public void start(PrintStream writer, String[] params) {
        this.writer = writer;
        parseArgs(params);

        // Reset state beliefs
        believe(Beliefname.isDriving.name(), new Boolean(false).toString());

        // Write out my beliefs
        for(Beliefname beliefname : Beliefname.values()) {
            String value = getBelief(beliefname.toString());
            out("believes " + beliefname.name() + "=" + value + " #" + beliefname.getCommonName());
        }

        // perceive congestion and blockage events always
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.PERCEIVE,
                new Object[] {Constants.BLOCKED, Constants.CONGESTION});
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
    //region BDI-ABM Agent functions
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
     * BDI-ABM agent init function; used for initialising agent args only;
     * Use {@link #start(PrintStream, String[])}
     * to perform agent startup.
     */
    @Override
    public void init(String[] args) {
        createBeliefSets(); // create this agent's belief sets
        parseArgs(args); // and store any initial beliefs

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
            case Constants.TIME:
                handleTime(parameters);
                break;
            case Constants.ARRIVED:
                handleArrived(parameters);
                break;
            case Constants.BLOCKED:
                handleBlocked(parameters);
                break;
            case Constants.CONGESTION:
                handleCongestion(parameters);
                break;
            case Constants.FIELD_OF_VIEW:
                handleFieldOfView(parameters);
                break;
            case Constants.EMERGENCY_MESSAGE:
                handleEmergencyMessage(parameters);
                break;
            case Constants.SOCIAL_NETWORK_MSG:
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
            if (content.getAction_type().equals(Constants.DRIVETO)) {
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

    String getTimeString() {
        return writeTime(getTime(), "HH:mm:ss");
    }
    class Prefix{
        public String toString() {
            return String.format("Time %9s %s %-4s : ", getTimeString(), ArchetypeAgent.class.getSimpleName(), getId());
        }
    }

    public String logPrefix() {
        return prefix.toString();
    }

    void out(String msg) {
        if (writer != null) {
            writer.println(logPrefix() + msg);
        }
    }

    //===============================================================================
    //endregion
    //===============================================================================

}
