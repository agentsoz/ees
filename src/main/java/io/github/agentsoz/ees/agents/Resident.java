package io.github.agentsoz.ees.agents;

import java.io.PrintStream;

import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.util.EmergencyMessage;
import io.github.agentsoz.util.Global;
import io.github.agentsoz.util.evac.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.ees.SimpleConfig;

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

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;

@AgentInfo(hasGoals={"io.github.agentsoz.ees.agents.RespondToFireAlert", "io.github.agentsoz.abmjill.genact.EnvironmentAction"})
public class Resident extends Agent implements io.github.agentsoz.bdiabm.Agent{

	private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

	private static final int MAX_FAILED_ATTEMPTS = 5;
	PrintStream writer = null;

	private Location shelterLocation = null;

	private Prefix prefix = new Prefix();
	private int failedAttempts;

	private double time = -1;
	private QueryPerceptInterface queryInterface;

	private boolean isEvacuating = false;
	private Object postedBlockageInfoToSocialNetwork = null;
	private final double messagePostingProbability = 0.2; // FIXME: read this from agent config

	public Resident(String str) {
		super(str);
	}

	Location getShelterLocation() {
	  return shelterLocation;
	}

	/**
	 * Called by the Jill model when starting a new agent.
	 * There is no separate initialisation call prior to this, so all
	 * agent initialisation should be done here (using params).
	 */
	@Override
	public void start(PrintStream writer, String[] params) {
		parseArgs(params);
		this.writer = writer;
        shelterLocation = SimpleConfig.getRandomEvacLocation();


	}

	private void parseArgs(String[] args) {
		if (args != null && args.length != 0) {
			logger.debug("{} ignoring received args: {}", prefix, args);
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
			// Agent just arrived at the shelter
			writer.println(prefix + "arrived at shelter in " + getShelterLocation());
		} else if (perceptID.equals(PerceptList.BLOCKED) || perceptID.equals(PerceptList.CONGESTION)) {
			// Something went wrong while driving and the road is blocked or congested
			String status = perceptID.equals(PerceptList.BLOCKED) ? "is blocked" : "is in traffic congestion";
			failedAttempts++;
			writer.println(prefix + status + " at link " + parameters);

			if (postedBlockageInfoToSocialNetwork == null && perceptID.equals(PerceptList.BLOCKED)) {
				// if this is the first time we got blocked then communicate with social network with some probability
				// Note, might have heard about it from the social network already
				decideAndPostOnSocialMedia(PerceptList.BLOCKED + ":" + parameters);
			}

			Location[] coords = (Location[])getQueryPerceptInterface().queryPercept(String.valueOf(getId()), PerceptList.REQUEST_LOCATION, parameters);
			writer.println(prefix + "is currently between locations " + coords[0] + " and " + coords[1]);
			double distanceToSafePlace = (double)getQueryPerceptInterface().queryPercept(String.valueOf(getId()),
					PerceptList.REQUEST_DRIVING_DISTANCE_TO, getShelterLocation().getCoordinates());
			writer.println(prefix + "estimated driving distance to shelter accounting for detour is " + String.format("%.1f",distanceToSafePlace/1000) + "km");
			if (failedAttempts < MAX_FAILED_ATTEMPTS) {
				writer.println(prefix
						+ "will reevaluate route to destination (attempt "+(failedAttempts+1)
						+ " of " + MAX_FAILED_ATTEMPTS
						+ ")");
				post(new RespondToFireAlert("RespondToFireAlert"));
			} else {
				writer.println(prefix + "is giving up after " + failedAttempts + " failed attempts to evacuate");
			}
		} else if (perceptID.equals(PerceptList.FIRE_ALERT)) {
			// Received a fire alert so act now
			writer.println(prefix + "received fire alert");
			post(new RespondToFireAlert("RespondToFireAlert"));
		} else if (perceptID.equals(PerceptList.EMERGENCY_MESSAGE)) {
			String[] tokens = ((String) parameters).split(",");
			EmergencyMessage.EmergencyMessageType type = EmergencyMessage.EmergencyMessageType.valueOf(tokens[0]);
			writer.println(prefix + "received emergency message " + type);
			if (type==EmergencyMessage.EmergencyMessageType.EVACUATE_NOW) {
				if (isEvacuating) {
					writer.println(prefix + "will ignore "+type+" message as already evacuating");
				} else {
					isEvacuating = true;
					post(new RespondToFireAlert("RespondToFireAlert"));
				}
			}
		} else if (perceptID.equals(PerceptList.SOCIAL_NETWORK_MSG)) {
			// Ignore social network messages that this agent itself posted earlier
			if (postedBlockageInfoToSocialNetwork == null || !postedBlockageInfoToSocialNetwork.equals(parameters)) {
				writer.println(prefix + "received social network message: " + parameters);
				// FIXME: drop current driveto action and then replan to destination using congestion router
			}
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
			State actionState = content.getState();
			if (actionState==State.PASSED || actionState==State.FAILED || actionState==State.DROPPED) {
				// Wake up the agent that was waiting for external action to finish
				// FIXME: BDI actions put agent in suspend, which won't work for multiple intention stacks
				suspend(false);
			}
		}
	}

	private void decideAndPostOnSocialMedia(String content) {
		if (Global.getRandom().nextDouble() < messagePostingProbability) {
			writer.println(prefix + "will share disruption info on social network");
			String[] msg = {content, String.valueOf(getId())};
			postedBlockageInfoToSocialNetwork = msg[0];
			DataServer.getServer("Bushfire").publish(PerceptList.SOCIAL_NETWORK_MSG, msg);
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


	int getFailedAttempts() {
		return failedAttempts;
	}

	double getTime() {
		return time;
	}

	public String logPrefix() {
		return prefix.toString();
	}

	class Prefix{
		public String toString() {
			return String.format("Time %05.0f Resident %-4s : ", getTime(), getId());
		}
	}
}
