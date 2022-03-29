package io.github.agentsoz.ees.agents;

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
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Location;

import java.io.PrintStream;
import java.util.Map;

public class ProtectInhabitedAreaUnderThreat extends Plan {

	private PrintStream writer = null;
	private Responder responder;

	public ProtectInhabitedAreaUnderThreat(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		responder = (Responder)agent;
		this.writer = ((Responder)agent).writer;
		body = steps;
	}

	public boolean context() {
		return true;
	}
	
	PlanStep[] steps = {
			new PlanStep() {
				public void step() {
					String bdiAction = Constants.DRIVETO;
					Location threatenedLocation = responder.getLocationUnderFireThreat();
					double[] coords = threatenedLocation.getCoordinates();
					double evacTime = responder.getTime() + 5.0; // five secs from now
					Object[] params = new Object[4];
					params[0] = bdiAction;
					params[1] = coords;
					params[2] = evacTime;
					params[3] = Constants.EvacRoutingMode.emergencyVehicle;
					writer.println(responder.logPrefix() + "will start driving to "+ threatenedLocation + " under fire threat at time " + evacTime);
					subgoal(new EnvironmentAction(
							Integer.toString(responder.getId()),
							bdiAction, params));
					}
			},
			// Now wait till it is finished
			new PlanStep() {
				public void step() {
					// Must suspend the agent when waiting for external stimuli
					responder.suspend(true);
					// All done, when we return from the above call
				}
			},
			// NOTE by DS on 23/jan/18:
			// Since Jill does not support multiple intentions yet, any new goal postings done at this
			// point in the code will end up being treated as sub-goals of this plan.
			// Any plan steps after such a posting will be executed AFTER returning from the recursive call,
			// and may not be what is desired.
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
		// Intentionally left empty
		
	}
}
