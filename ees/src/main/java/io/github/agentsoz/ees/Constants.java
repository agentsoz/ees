package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


public class Constants {
    // Actions
    public static final String DRIVETO = io.github.agentsoz.util.ActionList.DRIVETO;
    public static final String REPLAN_CURRENT_DRIVETO = io.github.agentsoz.util.ActionList.REPLAN_CURRENT_DRIVETO;
    public static final String PERCEIVE = io.github.agentsoz.util.ActionList.PERCEIVE;

    // Percepts
    public static final String ARRIVED = io.github.agentsoz.util.PerceptList.ARRIVED;
    public static final String ACTIVITY_STARTED = io.github.agentsoz.util.PerceptList.ACTIVITY_STARTED;
    public static final String ACTIVITY_ENDED = io.github.agentsoz.util.PerceptList.ACTIVITY_ENDED;
    public static final String BLOCKED = io.github.agentsoz.util.PerceptList.BLOCKED;
    public static final String BROADCAST = io.github.agentsoz.util.PerceptList.BROADCAST;
    public static final String CONGESTION = io.github.agentsoz.util.PerceptList.CONGESTION;
    public static final String DEPARTED = io.github.agentsoz.util.PerceptList.DEPARTED;
    public static final String DISRUPTION = "disruption";
    public static final String EMERGENCY_MESSAGE = "emergency_message";
    public static final String FIELD_OF_VIEW = "field_of-view";
    public static final String FIRE = "fire";
    public static final String EMBERS_DATA = "embers_data";
    public static final String FIRE_ALERT = "fire_alert";
    public static final String FIRE_DATA  = "fire_data";
    public static final String CYCLONE_DATA  = "cylcone_data";
    public static final String FLOOD_DATA  = "flood_data";
    public static final String SIGHTED_EMBERS= "embers";
    public static final String SIGHTED_FIRE= "fire";
    public static final String STUCK =io.github.agentsoz.util.PerceptList.STUCK;
    public static final String TIME = io.github.agentsoz.util.PerceptList.TIME;

    // BDI Query Percept  strings
    public static final String REQUEST_LOCATION = io.github.agentsoz.util.PerceptList.REQUEST_LOCATION;
    public static final String REQUEST_DRIVING_DISTANCE_TO = io.github.agentsoz.util.PerceptList.REQUEST_DRIVING_DISTANCE_TO;

    //Diffusion model percepts
    public static final String DIFFUSION = "diffusion";
    public static final String BDI_STATE_UPDATES    = "bdi_state_updates";
    public static final String SOCIAL_NETWORK_MSG = "social_network_message";
    public static final String SOCIAL_NETWORK_BROADCAST_MSG = "social_network_broadcast_message";

    // Used to control the simulation run and data passing between BDI and ABM models
    public static final String TAKE_CONTROL_BDI = io.github.agentsoz.util.PerceptList.TAKE_CONTROL_BDI;
    public static final String TAKE_CONTROL_ABM = io.github.agentsoz.util.PerceptList.TAKE_CONTROL_ABM;
    public static final String AGENT_DATA_CONTAINER_FROM_BDI = io.github.agentsoz.util.PerceptList.AGENT_DATA_CONTAINER_FROM_BDI;
    public static final String AGENT_DATA_CONTAINER_FROM_ABM = io.github.agentsoz.util.PerceptList.AGENT_DATA_CONTAINER_FROM_ABM;

    // Evacuation-related activity names (added dynamically to MATSim)
    public enum EvacActivity {
        Home,
        EvacPlace,
        InvacPlace,
        DependentsPlace,
        StuckPlace,
        UnknownPlace,
    }

    public enum EvacRoutingMode {carFreespeed, carGlobalInformation, emergencyVehicle}

    public enum EmergencyMessage {
        Advice("ADVICE"),
        WatchAndAct("WATCH_AND_ACT"),
        EmergencyWarning("EMERGENCY_WARNING"),
        EvacuateNow("EVACUATE_NOW")
        ;

        private final String commonName;

        EmergencyMessage(String name){
            this.commonName = name;
        }

        public String getCommonName() {
            return commonName;
        }
    }
}
