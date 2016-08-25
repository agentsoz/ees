package io.github.agentsoz.bushfire.matsimjill.agents;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionContent.State;

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
import io.github.agentsoz.jill.util.Log;

import java.io.PrintStream;

@AgentInfo(hasGoals={"io.github.agentsoz.bushfire.matsimjill.agents.RespondToFireAlert", "io.github.agentsoz.abmjill.genact.EnvironmentAction"})
public class Resident extends Agent implements io.github.agentsoz.bdiabm.Agent {

	public static final String BDI_PERCEPT_FIRE_ALERT = "FireAlert";
	public static final String BDI_ACTION_DRIVETO = "drive to";
	
	public PrintStream writer = null;
	

	public Resident(String str) {
		super(str);
	}

	@Override
	public void start(PrintStream writer, String[] params) {
		this.writer = writer;
	}
	
	@Override
	public void finish() {
	}

	@Override
	public void init(String[] args) {
		Log.warn("Resident using a stub for io.github.agentsoz.bdiabm.Agent.init(...)");
	}

	@Override
	public void start() {
		Log.warn("Resident using a stub for io.github.agentsoz.bdiabm.Agent.start()");
	}

	@Override
	public void handlePercept(String perceptID, Object parameters) {
		if (perceptID.equals(BDI_PERCEPT_FIRE_ALERT)) {
			// Received a fire alert so act now
			writer.println("Resident"+getId()+" received fire alert");
			post(new RespondToFireAlert("RespondToFireAlert"));
		}
	}

	@Override
	public void packageAction(String actionID, Object[] parameters) {
		Log.warn("Resident using a stub for io.github.agentsoz.bdiabm.Agent.packageAction(...)");
	}

	@Override
	public void updateAction(String actionID, ActionContent content) {
		Log.debug("Agent"+getId()+" received action update: "+content);
		if (content.getAction_type().equals(BDI_ACTION_DRIVETO)) {
			if (content.getState()==State.PASSED) {
				// Wake up the agent that was waiting for external action to finish
				suspend(false);
			}
		}
	}

	@Override
	public void kill() {
		// TODO Auto-generated method stub
	}
}
