package io.github.agentsoz.ees.agents.bushfire;

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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Location;

import java.util.Map;


public class PlanGotoEvacPlace extends Plan {

	BushfireAgent agent = null;
	private Location destination = null;
	boolean startedDriving = false;

	public PlanGotoEvacPlace(Agent agent, Goal goal, String name) {
		super(agent, goal, name);
		this.agent = (BushfireAgent)agent;
		body = steps;
	}

	public boolean context() {
		boolean applicable = true;
		agent.memorise(BushfireAgent.MemoryEventType.DECIDED.name(), BushfireAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
				+ ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + applicable);
		return applicable;
	}

	PlanStep[] steps = {
			() -> {
				agent.memorise(BushfireAgent.MemoryEventType.ACTIONED.name(), getGoal() + "|" + this.getClass().getSimpleName());
				destination = agent.getLocations().get(agent.LOCATION_EVAC_PREFERRED);
				startedDriving = agent.startDrivingTo(destination, Constants.EvacRoutingMode.carFreespeed);
			},
			() -> {
				if (startedDriving) {
					// Step subsequent to post must suspend agent when waiting for external stimuli
					// Will be reset by updateAction()
					agent.suspend(true);
					// Do not add any checks here since the above call is non-blocking
					// Suspend will happen once this step is finished
				}
			},
			() -> {
				// Try a second time
				startedDriving = false;
				if (agent.getLastDriveActionStatus() != ActionContent.State.DROPPED && agent.getTravelDistanceTo(destination)  > 0.0) {
					startedDriving = agent.startDrivingTo(destination, Constants.EvacRoutingMode.carGlobalInformation);
				}
			},
			() -> {
				if (startedDriving) {
					// Step subsequent to post must suspend agent when waiting for external stimuli
					// Will be reset by updateAction()
					agent.suspend(true);
					// Do not add any checks here since the above call is non-blocking
					// Suspend will happen once this step is finished
				}
			},
			() -> {
				// Try a third time
				startedDriving = false;
				if (agent.getLastDriveActionStatus() != ActionContent.State.DROPPED && agent.getTravelDistanceTo(destination)  > 0.0) {
					startedDriving = agent.startDrivingTo(destination, Constants.EvacRoutingMode.carGlobalInformation);
				}
			},
			() -> {
				if (startedDriving) {
					// Step subsequent to post must suspend agent when waiting for external stimuli
					// Will be reset by updateAction()
					agent.suspend(true);
					// Do not add any checks here since the above call is non-blocking
					// Suspend will happen once this step is finished
				}
			},
			() -> {
				// Hopefully we are there, else we will give up now after three tries
				agent.getTravelDistanceTo(destination);
				agent.memorise(BushfireAgent.MemoryEventType.BELIEVED.name(),
						BushfireAgent.MemoryEventValue.DISTANCE_TO_LOCATION.name()
								+ ":"+ destination
								+ ":" + String.format("%.0f", agent.getTravelDistanceTo(destination)) + "m");
			},
	};

	@Override
	public void setPlanVariables(Map<String, Object> vars) {
	}
}
