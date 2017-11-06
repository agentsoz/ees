package io.github.agentsoz.bushfire.matsimjill.agents;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bushfire.datamodels.Location;
import io.github.agentsoz.bushfire.matsimjill.SimpleConfig;

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

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AgentInfo(hasGoals={"io.github.agentsoz.bushfire.matsimjill.agents.RespondToFireAlert", "io.github.agentsoz.abmjill.genact.EnvironmentAction"})
public class Resident extends Agent implements io.github.agentsoz.bdiabm.Agent {

	private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.bushfire");

	public static final String BDI_PERCEPT_FIRE_ALERT = "FireAlert";
	public static final String BDI_ACTION_DRIVETO = "drive to";
	
	public PrintStream writer = null;
	
	private Location shelterLocation = null;


	public Resident(String str) {
		super(str);
	}
	
	public Location getShelterLocation() {
	  return shelterLocation;
	}

	/**
	 * Called by the Jill model when starting a new agent.
	 * There is no separate initialisation call prior to this, so all
	 * agent initialisation should be done here (using params).
	 */
	@Override
	public void start(PrintStream writer, String[] params) {
		this.writer = writer;
        shelterLocation = SimpleConfig.getRandomEvacLocation();
	}
	
	/**
	 * Called by the Jill model when terminating
	 */
	@Override
	public void finish() {
		logger.trace("Resident"+getId()+" is terminating");
	}

	/** 
	 * Called by the Jill model with the status of a BDI percept
	 * for this agent, coming from the ABM environment. 
	 */
	@Override
	public void handlePercept(String perceptID, Object parameters) {
		if (perceptID.equals(BDI_PERCEPT_FIRE_ALERT)) {
			// Received a fire alert so act now
			writer.println("Resident "+getId()+" received fire alert at time "+parameters);
			post(new RespondToFireAlert("RespondToFireAlert"));
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
		logger.debug("Agent"+getId()+" received action update: "+content);
		if (content.getAction_type().equals(BDI_ACTION_DRIVETO)) {
			if (content.getState()==State.PASSED) {
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
		logger.warn("Resident"+getId()+" using a stub for io.github.agentsoz.bdiabm.Agent.init(...)");
	}

	/**
	 * BDI-ABM agent start function; Not used by Jill. 
	 * Use {@link #start(PrintStream, String[])} instead
	 * to perform agent startup.
	 */
	@Override
	public void start() {
		logger.warn("Resident"+getId()+" using a stub for io.github.agentsoz.bdiabm.Agent.start()");
	}

	/**
	 * BDI-ABM agent kill function; Not used by Jill. 
	 * Use {@link #finish()} instead
	 * to perform agent termination.
	 */
	@Override
	public void kill() {
		logger.warn("Resident"+getId()+" using a stub for io.github.agentsoz.bdiabm.Agent.kill()");
	}
}
