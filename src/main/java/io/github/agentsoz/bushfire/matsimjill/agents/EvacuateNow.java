package io.github.agentsoz.bushfire.matsimjill.agents;

import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.bdimatsim.MATSimActionList;
import io.github.agentsoz.bdimatsim.MATSimModel.EvacRoutingMode;
import io.github.agentsoz.bushfire.datamodels.Location;
import io.github.agentsoz.dataInterface.DataServer;

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
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.io.PrintStream;
import java.util.HashMap;

public class EvacuateNow extends Plan { 

	private PrintStream writer = null;
	private Location shelterLocation = null;
	
	public EvacuateNow(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.writer = ((Resident)agent).writer;
		body = steps;
	}

	public boolean context() {
		return true;
	}
	
	PlanStep[] steps = {
			// Post the test action
			new PlanStep() {
				public void step() {
					String bdiAction = MATSimActionList.DRIVETO;
					shelterLocation = ((Resident)getAgent()).getShelterLocation();
					double [] coords = shelterLocation.getCoordinates() ;
					double evacTime = DataServer.getServer("Bushfire").getTime() + 5.0; // five secs from now
					Object[] params = {bdiAction, coords, evacTime, EvacRoutingMode.carFreespeed};
					writer.println("Resident "+getAgent().getId()+": will start evacuating to shelter in "+shelterLocation + " at time " + evacTime);
					post(new EnvironmentAction(
							Integer.toString(((Resident)getAgent()).getId()),
							bdiAction, params));
					}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					// Must suspend the agent when waiting for external stimuli
					writer.println("Resident " + getAgent().getId() + ": is driving to shelter in " + shelterLocation);
					((Resident) getAgent()).suspend(true);
				}
			},
			// All done
			new PlanStep() {
				public void step() {
					if (((Resident)getAgent()).getFailedAttempts() == 0) {
						writer.println("Resident "+getAgent().getId()+": has reached shelter in "+ shelterLocation);
					} else {
						// Failed
						String bdiAction = MATSimActionList.DRIVETO;
						shelterLocation = ((Resident)getAgent()).getShelterLocation();
						double[] coords = shelterLocation.getCoordinates();
						double evacTime = DataServer.getServer("Bushfire").getTime() + 5.0; // five secs from now
						Object[] params = {bdiAction, coords, evacTime, EvacRoutingMode.carGlobalInformation};
						writer.println("Resident "+getAgent().getId()+": will replan to shelter in "+shelterLocation + " at time " + evacTime);
						post(new EnvironmentAction(
								Integer.toString(((Resident)getAgent()).getId()),
								bdiAction, params));
					}
				}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					if (((Resident)getAgent()).getFailedAttempts() == 0) {
						return;
					}
					// Must suspend the agent when waiting for external stimuli
					writer.println("Resident " + getAgent().getId() + ": is driving to shelter in " + shelterLocation);
					((Resident) getAgent()).suspend(true);
				}
			},
			// All done
			new PlanStep() {
				public void step() {
					if (((Resident) getAgent()).getFailedAttempts() == 0) {
						return;
					}
					writer.println("Resident " + getAgent().getId() + ": has reached shelter in " + shelterLocation);
				}
			}
	};

	@Override
	public void setPlanVariables(HashMap<String, Object> vars) {
		// TODO Auto-generated method stub
		
	}
}
