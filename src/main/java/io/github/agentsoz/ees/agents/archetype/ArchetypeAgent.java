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
import io.github.agentsoz.util.Global;
import io.github.agentsoz.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.matsim.core.utils.misc.Time.parseTime;
import static org.matsim.core.utils.misc.Time.writeTime;

/**
 * ArchetypeAgent Class
 *
 * author: dsingh
 */
@AgentInfo(hasGoals={
        "io.github.agentsoz.abmjill.genact.EnvironmentAction",
        "io.github.agentsoz.ees.agents.archetype.GotoLocationEvacuation"
})
public class ArchetypeAgent extends Agent implements io.github.agentsoz.bdiabm.Agent {


    //===============================================================================
    //region Class constants
    //===============================================================================

    private final Logger logger = LoggerFactory.getLogger(ArchetypeAgent.class);

    private final int reactionTimeInSecs = 30;

    enum State {
        anxietyFromSituation,
        anxietyFromEmergencyMessages,
        anxietyFromSocialMessages,
        //
        futureValueOfFireDangerIndexRating,
        futureValueOfVisibleFire,
        futureValueOfVisibleSmoke,
        futureValueOfSmokeImmersion,
        futureValueOfVisibleEmbers,
        futureValueOfVisibleResponders,
        futureValueOfMessageRespondersAttending,
        futureValueOfMessageAdvice,
        futureValueOfMessageWatchAndAct,
        futureValueOfMessageEmergencyWarning,
        futureValueOfMessageEvacuateNow,
        futureValueOfMessageSocial,
        //
        responseThresholdInitialReached,
        responseThresholdFinalReached,
    }
    
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
     * @param name name of this agent
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
        Object[] params = new Object[7];
        params[0] = Constants.DRIVETO;
        params[1] = location.getCoordinates();
        params[2] = getTime() + 5.0; // five secs from now;
        params[3] = routingMode;
        params[4] = activity.toString();
        params[5] = true; // add replan activity to mark location/time of replanning
        params[6] = Global.getRandom().nextInt(reactionTimeInSecs);
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.DRIVETO, params);
        addActiveEnvironmentAction(action); // will be reset by updateAction()
        return action;
    }

    private void evaluateSituation() {
        try {
            // get values of anxiety and the thresholds
            double anxiety = Double.valueOf(getBelief(State.anxietyFromSituation.name()))
                    + Double.valueOf(getBelief(State.anxietyFromEmergencyMessages.name()))
                    + Double.valueOf(getBelief(State.anxietyFromSocialMessages.name()));
            double initialResponseThreshold = Double.valueOf(getBelief(Beliefname.ResponseThresholdInitial.name()));
            double finalResponseThreshold = Double.valueOf(getBelief(Beliefname.ResponseThresholdInitial.name()));

            // and whether we already know if the thresholds are reached
            boolean initialThresholdReached = Boolean.valueOf(getBelief(State.responseThresholdInitialReached.name()));
            boolean finalThresholdReached = Boolean.valueOf(getBelief(State.responseThresholdFinalReached.name()));

            // if either threshold was just reached, then react now
            if ((!initialThresholdReached && (anxiety >= initialResponseThreshold)) ||
                    (!finalThresholdReached &&  (anxiety >= finalResponseThreshold)))
            {
                if (!initialThresholdReached && (anxiety >= initialResponseThreshold)) {
                    initialThresholdReached = true;
                    believe(State.responseThresholdInitialReached.name(), Boolean.toString(initialThresholdReached));
                    post(new GotoLocationEvacuation(GotoLocationEvacuation.class.getSimpleName()));
                }
                if (!finalThresholdReached && (anxiety >= finalResponseThreshold)) {
                    finalThresholdReached = true;
                    believe(State.responseThresholdFinalReached.name(), Boolean.toString(finalThresholdReached));
                }
            }
        } catch (Exception e) {}
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
        record("stopped driving");
    }

    private void handleDeparted(Object parameters) {
        record("started driving to " + ((Map<String,String>)parameters).get("actType"));
    }

    private void handleActivityStarted(Object parameters) {
        record("started activity " + ((Map<String,String>)parameters).get("actType"));
    }

    private void handleActivityEnded(Object parameters) {
        record("finished activity " + ((Map<String,String>)parameters).get("actType"));
    }

    private void handleBlocked(Object parameters) {
        record("is blocked and will replan");
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.REPLAN_CURRENT_DRIVETO,
                new Object[] {Constants.EvacRoutingMode.carGlobalInformation});
        addActiveEnvironmentAction(action); // will be reset by updateAction()
        post(action);
    }

    private void handleCongestion(Object parameters) {
        record("is in congestion and will replan");
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.REPLAN_CURRENT_DRIVETO,
                new Object[] {Constants.EvacRoutingMode.carGlobalInformation});
        addActiveEnvironmentAction(action); // will be reset by updateAction()
        post(action);
    }

    private void handleFieldOfView(Object view) {
        if (view == null) {
            return;
        }
        record("saw " + view);
        if (Constants.SIGHTED_EMBERS.equalsIgnoreCase(view.toString())) {
            double effect = Double.valueOf(getBelief(State.futureValueOfVisibleEmbers.name()));
            double barometer = Double.valueOf(getBelief(State.anxietyFromSituation.name()));
            believe(State.anxietyFromSituation.name(), Double.toString(barometer+effect));
            believe(State.futureValueOfVisibleEmbers.name(), "0.0");

        } else if (Constants.SIGHTED_FIRE.equalsIgnoreCase(view.toString())) {
            double effect = Double.valueOf(getBelief(State.futureValueOfVisibleFire.name()));
            double barometer = Double.valueOf(getBelief(State.anxietyFromSituation.name()));
            believe(State.anxietyFromSituation.name(), Double.toString(barometer+effect));
            believe(State.futureValueOfVisibleFire.name(), "0.0");
        } else {
            logger.error("{} ignoring field of view percept: {}", logPrefix(), view);
            return;
        }
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
     * Sets a belief for this agent. If the belief already exists it will be overwritten.
     * @param key the belief name
     * @param value the value of the belief
     */
    void believe(String key, String value) {
        try {
            removeIfExists(key, value);
            addBelief(beliefSetName, key, value);
            record("believes " + key + "=" + value);
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
     * Removes the belief if it already exists.
     * @param key
     * @param value
     */
    private void removeIfExists(String key, String value) {
        if (key == null) {
            return;
        }
        String query = beliefSetName + ".key=" + key;
        try {
            if (eval(query)) {
                Set<Belief> beliefs = getLastResults();
                for (Belief belief : beliefs) {
                    try {
                        removeBelief(belief);
                    } catch (BeliefBaseException e){
                        logger.error("Could not remove belief:  " + belief, e);
                    }
                }
            }
        } catch (BeliefBaseException e) {
            logger.error("Could not evaluate belief query:  " + query, e);
        }
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

    private void initialiseBeliefs() {
        believe(State.anxietyFromSituation.name(), "0.0");
        believe(State.anxietyFromSocialMessages.name(), "0.0");
        believe(State.anxietyFromEmergencyMessages.name(), "0.0");
        //
        believe(State.futureValueOfFireDangerIndexRating.name(), getBelief(Beliefname.ImpactFromFireDangerIndexRating.name()));
        believe(State.futureValueOfSmokeImmersion.name(), getBelief(Beliefname.ImpactFromImmersionInSmoke.name()));
        believe(State.futureValueOfMessageAdvice.name(), getBelief(Beliefname.ImpactFromMessageAdvice.name()));
        believe(State.futureValueOfMessageEmergencyWarning.name(), getBelief(Beliefname.ImpactFromMessageEmergencyWarning.name()));
        believe(State.futureValueOfMessageEvacuateNow.name(), getBelief(Beliefname.ImpactFromMessageEvacuateNow.name()));
        believe(State.futureValueOfMessageRespondersAttending.name(), getBelief(Beliefname.ImpactFromMessageRespondersAttending.name()));
        believe(State.futureValueOfMessageWatchAndAct.name(), getBelief(Beliefname.ImpactFromMessageWatchAndAct.name()));
        believe(State.futureValueOfMessageSocial.name(), getBelief(Beliefname.ImpactFromSocialMessage.name()));
        believe(State.futureValueOfVisibleEmbers.name(), getBelief(Beliefname.ImpactFromVisibleEmbers.name()));
        believe(State.futureValueOfVisibleResponders.name(), getBelief(Beliefname.ImpactFromVisibleResponders.name()));
        believe(State.futureValueOfVisibleFire.name(), getBelief(Beliefname.ImpactFromVisibleFire.name()));
        believe(State.futureValueOfVisibleSmoke.name(), getBelief(Beliefname.ImpactFromVisibleSmoke.name()));
        //
        believe(State.responseThresholdInitialReached.name(), null);
        believe(State.responseThresholdFinalReached.name(), null);

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
            record("believes " + beliefname.name() + "=" + value + " #" + beliefname.getCommonName());
        }

        // Initialise behaviour attributes from initial beliefs
        initialiseBeliefs();

        // register to perceive certain events
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.PERCEIVE,
                new Object[] {
                        Constants.BLOCKED,
                        Constants.CONGESTION,
                        Constants.ARRIVED,
                        Constants.DEPARTED,
                        Constants.ACTIVITY_STARTED,
                        Constants.ACTIVITY_ENDED});
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
            case Constants.DEPARTED:
                handleDeparted(parameters);
                break;
            case Constants.ACTIVITY_STARTED:
                handleActivityStarted(parameters);
                break;
            case Constants.ACTIVITY_ENDED:
                handleActivityEnded(parameters);
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

        // evaluate the situation always
        evaluateSituation();
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
            return String.format("%8s|%s|%-5s|", getTimeString(), ArchetypeAgent.class.getSimpleName(), getId());
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

    void record(String msg) {
        if (msg != null) {
            out(msg + " @@");
        }
    }

    //===============================================================================
    //endregion
    //===============================================================================

}
