package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
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
import java.util.*;

import static io.github.agentsoz.ees.Constants.EmergencyMessage.*;
import static org.matsim.core.utils.misc.Time.writeTime;

/**
 * ArchetypeAgent Class
 *
 * author: dsingh
 */
@AgentInfo(hasGoals={
        "io.github.agentsoz.ees.agents.archetype.GoalFullResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalInitialResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalFinalResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalGoto",
        "io.github.agentsoz.abmjill.genact.EnvironmentAction",
})
public class ArchetypeAgent extends Agent implements io.github.agentsoz.bdiabm.Agent {


    //===============================================================================
    //region Class constants
    //===============================================================================

    private final Logger logger = LoggerFactory.getLogger(ArchetypeAgent.class);

    private final int reactionTimeInSecs = 30;

    enum State {
        responseThresholdInitialReached,
        responseThresholdFinalReached,
        //
        status,
        receivedMessages,
        isStuck,
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
        Id("Id"),
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
        LagTimeInMinsForInitialResponse("LagTimeInMinsForInitialResponse"),
        LagTimeInMinsForFinalResponse("LagTimeInMinsForFinalResponse"),
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
        WillReceiveMessages("WillReceiveMessages")
        ;

        private final String commonName;

        Beliefname(String name){
            this.commonName = name;
        }

        public String getCommonName() {
            return commonName;
        }
    };

    enum StatusValue {
       to,
       at,
    }

    //===============================================================================
    //endregion
    //===============================================================================

    //===============================================================================
    //region Class variables
    //===============================================================================

    private PrintStream writer = null;
    private double time = -1;
    private ArchetypeAgent.Prefix prefix = new ArchetypeAgent.Prefix();
    private Random rand = new Random(0);

    private Location dependentsLocation;
    private Location evacLocation;
    private Location invacLocation;
    private Location homeLocation;
    private Location workLocation;
    private Location[] stuckLocation;

    private double responseThresholdInitial = 1.0;
    private double responseThresholdFinal = 1.0;

    private double futureValueOfFireDangerIndexRating = 0.0;
    private double futureValueOfVisibleFire = 0.0;
    private double futureValueOfVisibleSmoke = 0.0;
    private double futureValueOfSmokeImmersion = 0.0;
    private double futureValueOfVisibleEmbers = 0.0;
    private double futureValueOfVisibleResponders = 0.0;
    private double futureValueOfMessageRespondersAttending = 0.0;
    private double futureValueOfMessageAdvice = 0.0;
    private double futureValueOfMessageWatchAndAct = 0.0;
    private double futureValueOfMessageEmergencyWarning = 0.0;
    private double futureValueOfMessageEvacuateNow = 0.0;
    private double futureValueOfMessageSocial = 0.0;
    //
    private double anxietyFromSituation = 0.0;
    private double anxietyFromEmergencyMessages = 0.0;
    private double anxietyFromSocialMessages = 0.0;

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
                slocation = slocation
                        .replaceAll("\\[", "")
                        .replaceAll("\\]", "");
                String[] tokens = slocation.split(",");
                if (tokens.length>=3) {
                    String name = tokens[0];
                    double[] coords = new double[]{Double.parseDouble(tokens[1].trim()), Double.parseDouble(tokens[2].trim())};
                    return new Location(name, coords[0], coords[1]);
                }
                double[] coords = new double[]{Double.parseDouble(tokens[0].trim()), Double.parseDouble(tokens[1].trim())};
                return new Location("", coords[0], coords[1]);
            } catch (Exception e) {
                logger.error("Could not parse location: " + slocation);
            }
        }
        return null;
    }

    double getDrivingDistanceTo(Location location) throws AgentNotFoundException {
        double dist = (location == null || Boolean.valueOf(getBelief(ArchetypeAgent.State.isStuck.name()))) ? -1 :
                (double)getQueryPerceptInterface().queryPercept(
                    String.valueOf(getId()),
                    Constants.REQUEST_DRIVING_DISTANCE_TO,
                    location.getCoordinates());
        return dist;
    }

    /**
     * Prepares a goal to drive to a given activity
     * @param activity the name of the destination activity
     * @param location the location of the activity
     * @param routingMode the routing mode to use
     * @param replanningActivityDurationInMins add replanning activity of given duration prior to drive (or not if =< 0)
     * @return
     */
    Goal prepareDrivingGoal(Constants.EvacActivity activity,
                            Location location,
                            Constants.EvacRoutingMode routingMode,
                            int replanningActivityDurationInMins) {
        Object[] params = new Object[7];
        params[0] = Constants.DRIVETO;
        params[1] = location.getCoordinates();
        params[2] = getTime() + 5.0; // five secs from now;
        params[3] = routingMode;
        params[4] = activity.toString();
        params[5] = (replanningActivityDurationInMins>0); // add replan activity to mark location/time of replanning
        params[6] = (replanningActivityDurationInMins>0) ? rand.nextInt(replanningActivityDurationInMins*60) : 0;
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.DRIVETO, params);
        addActiveEnvironmentAction(action); // will be reset by updateAction()
        return action;
    }

    private void evaluateSituation() {
        try {
            // get values of anxiety and the thresholds
            double anxiety = anxietyFromSituation
                    + anxietyFromEmergencyMessages
                    + anxietyFromSocialMessages;
            if (anxiety > 0) {
                // and whether we already know if the thresholds are reached
                boolean initialThresholdAlreadyReached = Boolean.valueOf(getBelief(State.responseThresholdInitialReached.name()));
                boolean finalThresholdAlreadyReached = Boolean.valueOf(getBelief(State.responseThresholdFinalReached.name()));
                boolean initialThresholdJustReached = !initialThresholdAlreadyReached && anxiety >= responseThresholdInitial;
                boolean finalThresholdJustReached = !finalThresholdAlreadyReached && anxiety >= responseThresholdFinal;

                // if either threshold was just reached, then react now
                if (initialThresholdJustReached || finalThresholdJustReached) {
                    boolean bothThresholdsJustReached = initialThresholdJustReached && finalThresholdJustReached;

                    if (bothThresholdsJustReached) {
                        believe(State.responseThresholdInitialReached.name(),
                                Boolean.toString(initialThresholdJustReached || initialThresholdAlreadyReached));
                        believe(State.responseThresholdFinalReached.name(),
                                Boolean.toString(finalThresholdJustReached || finalThresholdAlreadyReached));
                        post(new GoalFullResponse(GoalFullResponse.class.getSimpleName()));
                    } else if (initialThresholdJustReached) {
                        believe(State.responseThresholdInitialReached.name(),
                                Boolean.toString(initialThresholdJustReached || initialThresholdAlreadyReached));
                        post(new GoalInitialResponse(GoalInitialResponse.class.getSimpleName()));
                    } else if (finalThresholdJustReached) {
                        believe(State.responseThresholdFinalReached.name(),
                                Boolean.toString(finalThresholdJustReached || finalThresholdAlreadyReached));
                        post(new GoalFinalResponse(GoalFinalResponse.class.getSimpleName()));
                    }
                }
            }
        } catch (Exception e) {}
    }

    public Location[] getCurrentLocation() {
        if (Boolean.valueOf(getBelief(ArchetypeAgent.State.isStuck.name()))) {
            return stuckLocation;
        } else {
            try {
                return (Location[]) getQueryPerceptInterface().queryPercept(String.valueOf(getId()), Constants.REQUEST_LOCATION, null);
            } catch (AgentNotFoundException e) {
                handleAgentNotFoundException("disconnected with physical agent");
                return stuckLocation;
            }
        }
    }

    public String getCurrentStatus() {
        String status = getBelief(State.status.name());
        if (status == null) {
            status = ArchetypeAgent.StatusValue.at.name() + ":" + Constants.EvacActivity.UnknownPlace.name();
        }
        return status;
    }

    public String getReceivedMessages() {
        String val = getBelief(State.receivedMessages.name());
        if (val == null) {
            val = "na";
        }
        return val;
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
        String type = ((Map<String,String>)parameters).get("actType");
        record("started driving to " + type);
        believe(State.status.name(), ArchetypeAgent.StatusValue.to.name() + ":" + type);


    }

    private void handleActivityStarted(Object parameters) {
        String type = ((Map<String,String>)parameters).get("actType");
        record("started activity " + type);
        believe(State.status.name(), ArchetypeAgent.StatusValue.at.name() + ":" + type);

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
        record("saw " + view);
        if (Constants.SIGHTED_EMBERS.equalsIgnoreCase(view.toString())) {
            anxietyFromSituation += futureValueOfVisibleEmbers;
            futureValueOfVisibleEmbers = 0.0;
            record("believes anxietyFromSituation=" + anxietyFromSituation);
            record("believes futureValueOfVisibleEmbers=" + futureValueOfVisibleEmbers);
        } else if (Constants.SIGHTED_FIRE.equalsIgnoreCase(view.toString())) {
            anxietyFromSituation += futureValueOfVisibleFire;
            futureValueOfVisibleFire = 0.0;
            record("believes anxietyFromSituation=" + anxietyFromSituation);
            record("believes futureValueOfVisibleFire=" + futureValueOfVisibleFire);
        } else {
            logger.error("{} ignoring field of view percept: {}", logPrefix(), view);
            return;
        }
    }

    private void handleEmergencyMessage(Object parameters) {
        try{
            boolean willReceiveMessages = Boolean.valueOf(getBelief(Beliefname.WillReceiveMessages.name()));
            if(!willReceiveMessages) {
                record("did not get message " + parameters);
                believe(State.receivedMessages.name(), Boolean.toString(false));
                return;
            }
            record("got message " + parameters);
            believe(State.receivedMessages.name(), Boolean.toString(true));

            double effect = 0.0;
            String[] args = ((String)parameters).split(",");
            if (Advice.getCommonName().equals(args[0])) {
                effect = futureValueOfMessageAdvice;
                futureValueOfMessageAdvice = 0.0;
                record("believes futureValueOfMessageAdvice=" + futureValueOfMessageAdvice);
            } else if (WatchAndAct.getCommonName().equals(args[0])) {
                effect = futureValueOfMessageWatchAndAct;
                futureValueOfMessageWatchAndAct = 0.0;
                record("believes futureValueOfMessageWatchAndAct=" + futureValueOfMessageWatchAndAct);
            } else if (EmergencyWarning.getCommonName().equals(args[0])) {
                effect = futureValueOfMessageEmergencyWarning;
                futureValueOfMessageEmergencyWarning = 0.0;
                record("believes futureValueOfMessageEmergencyWarning=" + futureValueOfMessageEmergencyWarning);
            } else if (EvacuateNow.getCommonName().equals(args[0])) {
                effect = futureValueOfMessageEvacuateNow;
                futureValueOfMessageEvacuateNow = 0.0;
                record("believes futureValueOfMessageEvacuateNow=" + futureValueOfMessageEvacuateNow);
                if (args.length >= 4) { // we have three more args being name,x,y
                    String loc = args[1] + "," + args[2] + "," + args[3];
                    evacLocation = parseLocation(loc);
                }
            }
            anxietyFromEmergencyMessages += effect;
            record("believes anxietyFromEmergencyMessages=" + anxietyFromEmergencyMessages);
       } catch (Exception e) {
            logger.warn(logPrefix() + "failed to parse " + parameters);
        }
    }

    private void handleSocialNetworkMessage(Object parameters) {
    }

    private void handleStuck(Object parameters) {
        believe(State.isStuck.name(), Boolean.toString(true));
        believe(State.status.name(), ArchetypeAgent.StatusValue.at.name() + ":" + Constants.EvacActivity.StuckPlace.name());
        stuckLocation = new Location[2];
        if(parameters != null) {
            Object[] args = (Object[]) parameters;
            stuckLocation[0] = new Location((String) args[0], (double) args[1], (double) args[2]);
            stuckLocation[1] = new Location((String) args[3], (double) args[4], (double) args[5]);
        } else {
            stuckLocation[0] = new Location("unknown:unknown", -1, -1);
            stuckLocation[1] = new Location("unknown:unknown", -1, -1);
        }
    }

    void handleAgentNotFoundException(String errorMessage) {
        believe(State.isStuck.name(), Boolean.toString(true));
        believe(State.status.name(), ArchetypeAgent.StatusValue.at.name() + ":" + Constants.EvacActivity.StuckPlace.name());
        out("disconnected with physical agent: " + errorMessage);
        stuckLocation = new Location[2];
        stuckLocation[0] = new Location("unknown:unknown", -1, -1);
        stuckLocation[1] = new Location("unknown:unknown", -1, -1);
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
    ActionContent.State lastBdiActionState;

    private void setLastBdiActionAndState(EnvironmentAction action, ActionContent.State state) {
        this.lastBdiAction = action;
        this.lastBdiActionState = state;
    }

    public ActionContent.State getLastBdiActionState() {
        return lastBdiActionState;
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
                synchronized (beliefs) {
                    Iterator<Belief> it = beliefs.iterator();
                    while(it.hasNext()) {
                        Belief belief = it.next();
                        try {
                            removeBelief(belief);
                        } catch (BeliefBaseException e) {
                            logger.error("Could not remove belief:  " + belief, e);
                        }
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
        String b;
        //
        b = getBelief(Beliefname.LagTimeInMinsForInitialResponse.name());
        believe(Beliefname.LagTimeInMinsForInitialResponse.name(), b == null ? "1.0" : b);
        b = getBelief(Beliefname.LagTimeInMinsForFinalResponse.name());
        believe(Beliefname.LagTimeInMinsForFinalResponse.name(), b == null ? "1.0" : b);
        //
        b = getBelief(Beliefname.Archetype.name());
        believe(Beliefname.Archetype.name(), b == null ? "Some.Archetype" : b);
        //
        believe(State.responseThresholdInitialReached.name(), null);
        believe(State.responseThresholdFinalReached.name(), null);
        //
        b = getBelief(Beliefname.WillReceiveMessages.name());
        believe(Beliefname.WillReceiveMessages.name(), b == null ? "true" : b);
        believe(State.receivedMessages.name(), null);
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

    public Location getDependentsLocation() {
        return dependentsLocation;
    }

    public Location getEvacLocation() {
        return evacLocation;
    }

    public Location getInvacLocation() {
        return invacLocation;
    }

    public Location getHomeLocation() {
        return homeLocation;
    }

    public Location getWorkLocation() {
        return workLocation;
    }

    //===============================================================================
    //endregion
    //===============================================================================


    //===============================================================================
    //region Arguments parsing
    //===============================================================================

    protected void parseArgs(String[] args) {

        Set discard = new HashSet();
        discard.add(Beliefname.Age.getCommonName());
        discard.add(Beliefname.AgentId.getCommonName());
        discard.add(Beliefname.ArchetypeAge.getCommonName());
        discard.add(Beliefname.ArchetypeHousehold.getCommonName());
        discard.add(Beliefname.AgentType.getCommonName());
        discard.add(Beliefname.Address.getCommonName());
        discard.add(Beliefname.AddressCoordinates.getCommonName());
        discard.add(Beliefname.Gender.getCommonName());
        discard.add(Beliefname.HouseholdId.getCommonName());
        discard.add(Beliefname.Id.getCommonName());
        discard.add(Beliefname.PrimaryFamilyType.getCommonName());
        discard.add(Beliefname.Sa1.getCommonName());
        discard.add(Beliefname.Sa2.getCommonName());

        Map<String, Beliefname> commonNames = new HashMap();
        for (Beliefname beliefname : Beliefname.values()) {
            commonNames.put(beliefname.getCommonName(), beliefname);
        }

        for (int i = 0; args != null && i < args.length; i++) {
            String key = args[i];
            String value = null;
            if (i + 1 < args.length) {
                i++;
                value = args[i];
            }
            if (discard.contains(key)) {
                continue; // discard key/values we don't care about
            }
            
            if (Beliefname.ResponseThresholdInitial.getCommonName().equals(key)) {
                responseThresholdInitial = Double.parseDouble(value);

            } else if (Beliefname.ResponseThresholdFinal.getCommonName().equals(key)) {
                responseThresholdFinal = Double.parseDouble(value);

            } else if (Beliefname.HasDependentsAtLocation.getCommonName().equals(key)) {
                dependentsLocation = parseLocation(value);

            } else if (Beliefname.LocationEvacuationPreference.getCommonName().equals(key)) {
                evacLocation = parseLocation(value);

            } else if (Beliefname.LocationInvacPreference.getCommonName().equals(key)) {
                invacLocation = parseLocation(value);

            } else if (Beliefname.LocationHome.getCommonName().equals(key)) {
                homeLocation = parseLocation(value);

            } else if (Beliefname.LocationWork.getCommonName().equals(key)) {
                workLocation = parseLocation(value);

            } else if (Beliefname.ImpactFromFireDangerIndexRating.getCommonName().equals(key)) {
                futureValueOfFireDangerIndexRating = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromImmersionInSmoke.getCommonName().equals(key)) {
                futureValueOfSmokeImmersion = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromMessageAdvice.getCommonName().equals(key)) {
                futureValueOfMessageAdvice = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromMessageEmergencyWarning.getCommonName().equals(key)) {
                futureValueOfMessageEmergencyWarning = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromMessageEvacuateNow.getCommonName().equals(key)) {
                futureValueOfMessageEvacuateNow = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromMessageRespondersAttending.getCommonName().equals(key)) {
                futureValueOfMessageRespondersAttending = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromMessageWatchAndAct.getCommonName().equals(key)) {
                futureValueOfMessageWatchAndAct = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromSocialMessage.getCommonName().equals(key)) {
                futureValueOfMessageSocial = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromVisibleEmbers.getCommonName().equals(key)) {
                futureValueOfVisibleEmbers = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromVisibleFire.getCommonName().equals(key)) {
                futureValueOfVisibleFire = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromVisibleResponders.getCommonName().equals(key)) {
                futureValueOfVisibleResponders = Double.parseDouble(value);

            } else if (Beliefname.ImpactFromVisibleSmoke.getCommonName().equals(key)) {
                futureValueOfVisibleSmoke = Double.parseDouble(value);

            } else if (commonNames.containsKey(key)) {
                // store all the other known key/values
                believe(commonNames.get(key).name(), value);

            } else {
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
        String beliefname = Beliefname.ResponseThresholdInitial.getCommonName();
        record("believes " + beliefname + "=" + responseThresholdInitial + " #" + beliefname);
        beliefname = Beliefname.ResponseThresholdFinal.getCommonName();
        record("believes " + beliefname + "=" + responseThresholdFinal + " #" + beliefname);
        beliefname = Beliefname.HasDependentsAtLocation.getCommonName();
        record("believes " + beliefname + "=" + dependentsLocation + " #" + beliefname);
        beliefname = Beliefname.LocationEvacuationPreference.getCommonName();
        record("believes " + beliefname + "=" + evacLocation + " #" + beliefname);
        beliefname = Beliefname.LocationInvacPreference.getCommonName();
        record("believes " + beliefname + "=" + invacLocation + " #" + beliefname);
        beliefname = Beliefname.LocationHome.getCommonName();
        record("believes " + beliefname + "=" + homeLocation + " #" + beliefname);
        beliefname = Beliefname.LocationWork.getCommonName();
        record("believes " + beliefname + "=" + workLocation + " #" + beliefname);

        record("believes futureValueOfFireDangerIndexRating=" + futureValueOfFireDangerIndexRating);
        record("believes futureValueOfSmokeImmersion=" + futureValueOfSmokeImmersion);
        record("believes futureValueOfMessageAdvice=" + futureValueOfMessageAdvice);
        record("believes futureValueOfMessageEmergencyWarning=" + futureValueOfMessageEmergencyWarning);
        record("believes futureValueOfMessageEvacuateNow=" + futureValueOfMessageEvacuateNow);
        record("believes futureValueOfMessageRespondersAttending=" + futureValueOfMessageRespondersAttending);
        record("believes futureValueOfMessageWatchAndAct=" + futureValueOfMessageWatchAndAct);
        record("believes futureValueOfMessageSocial=" + futureValueOfMessageSocial);
        record("believes futureValueOfVisibleEmbers=" + futureValueOfVisibleEmbers);
        record("believes futureValueOfVisibleFire=" + futureValueOfVisibleFire);
        record("believes futureValueOfVisibleResponders=" + futureValueOfVisibleResponders);
        record("believes futureValueOfVisibleSmoke=" + futureValueOfVisibleSmoke);
        record("believes anxietyFromSituation=" + anxietyFromSituation);

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
                        Constants.ACTIVITY_ENDED,
                        Constants.STUCK});
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
            case Constants.STUCK:
                handleStuck(parameters);
                break;
            default:
                logger.warn("{} received unknown percept '{}'", logPrefix(), perceptID);
                break;
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
            EnvironmentAction lastAction = removeActiveEnvironmentAction(content.getAction_type());
            setLastBdiActionAndState(lastAction, actionState);
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

    protected String getTimeString() {
        return writeTime(getTime(), "HH:mm:ss");
    }
    class Prefix{
        public String toString() {
            return String.format("%05.0f|%s|%s|%s|", getTime(), getTimeString(), ArchetypeAgent.class.getSimpleName(), getId());
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
