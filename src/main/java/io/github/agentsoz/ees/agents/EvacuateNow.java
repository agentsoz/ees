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

import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.ees.ActionList;
import io.github.agentsoz.ees.matsim.MATSimEvacModel;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Location;

import java.io.PrintStream;
import java.util.Map;

public class EvacuateNow extends Plan {

	private PrintStream writer = null;
	private Resident resident;

	public EvacuateNow(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		resident = (Resident)agent;
		this.writer = ((Resident)agent).writer;
		body = steps;
	}

	public boolean context() {
		return true;
	}

	PlanStep[] steps = {
			new PlanStep() {
				public void step() {
					String bdiAction = ActionList.DRIVETO;
					Location shelterLocation = resident.getShelterLocation();
					double[] coords = shelterLocation.getCoordinates();
					double evacTime = resident.getTime() + 5.0; // five secs from now
					Object[] params = new Object[4];
					params[0] = bdiAction;
					params[1] = coords;
					params[2] = evacTime;
					params[3] = (resident.getFailedAttempts() > 0) ? MATSimEvacModel.EvacRoutingMode.carGlobalInformation : MATSimEvacModel.EvacRoutingMode.carFreespeed;
					// (could use EvacRoutingMode.carFreespeed.name() if you like that better. kai, dec'17)
					writer.println(resident.logPrefix() + "will start driving to shelter in "+ shelterLocation + " at time " + evacTime);
					post(new EnvironmentAction(
							Integer.toString(resident.getId()),
							bdiAction, params));
					}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					// Must suspend the agent when waiting for external stimuli
					resident.suspend(true);
					// All done, when we return from the above call
				}
			},
			// NOTE:
			// Since Jill does not support multiple intentions yet, any repeated postings of the
			// RespondToFireAlert goal (if the agent gets a blocked percept and retries) will end up being treated as
			// sub-goals of this plan, i.e., recursive sub-goals.
			// Therefore any plan steps below this point will be executed AFTER returning from the recursive call,
			// and is probably not what we want/expect from the execution engine. For now, avoid adding anything
			// beyond this point.
			// dsingh, 5/dec/17
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
		// TODO Auto-generated method stub

	}
}
