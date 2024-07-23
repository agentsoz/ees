package io.github.agentsoz.ees.agents;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
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
