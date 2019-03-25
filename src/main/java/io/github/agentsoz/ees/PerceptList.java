package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2019 by its authors. See AUTHORS file.
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

public class PerceptList {
    public static final String ARRIVED = io.github.agentsoz.util.PerceptList.ARRIVED;
    public static final String BLOCKED = io.github.agentsoz.util.PerceptList.BLOCKED;
    public static final String BROADCAST = io.github.agentsoz.util.PerceptList.BROADCAST;
    public static final String CONGESTION = io.github.agentsoz.util.PerceptList.CONGESTION;
    public static final String DISRUPTION = "disruption";
    public static final String EMERGENCY_MESSAGE = "emergency_message";
    public static final String FIELD_OF_VIEW = "field_of-view";
    public static final String FIRE = "fire";
    public static final String EMBERS_DATA = "embers_data";
    public static final String FIRE_ALERT = "fire_alert";
    public static final String FIRE_DATA  = "fire_data";
    public static final String SIGHTED_EMBERS= "sighted_embers";
    public static final String SIGHTED_FIRE= "sighted_fire";
    public static final String TIME = io.github.agentsoz.util.PerceptList.TIME;

    // BDI Query Percept strings
    public static final String REQUEST_LOCATION = io.github.agentsoz.util.PerceptList.REQUEST_LOCATION;
    public static final String REQUEST_DRIVING_DISTANCE_TO = io.github.agentsoz.util.PerceptList.REQUEST_DRIVING_DISTANCE_TO;

    // Used to control the simulation run and data passing between BDI and ABM models
    public static final String TAKE_CONTROL_BDI = io.github.agentsoz.util.PerceptList.TAKE_CONTROL_BDI;
    public static final String TAKE_CONTROL_ABM = io.github.agentsoz.util.PerceptList.TAKE_CONTROL_ABM;
    public static final String AGENT_DATA_CONTAINER_FROM_BDI = io.github.agentsoz.util.PerceptList.AGENT_DATA_CONTAINER_FROM_BDI;
    public static final String AGENT_DATA_CONTAINER_FROM_ABM = io.github.agentsoz.util.PerceptList.AGENT_DATA_CONTAINER_FROM_ABM;

    //diffusion model percepts
    public static final String DIFFUSION = "diffusion";
    public static final String BDI_STATE_UPDATES    = "bdi_state_updates";
    public static final String SOCIAL_NETWORK_MSG = "social_network_message";
    public static final String SOCIAL_NETWORK_BROADCAST_MSG = "social_network_broadcast_message";



}
