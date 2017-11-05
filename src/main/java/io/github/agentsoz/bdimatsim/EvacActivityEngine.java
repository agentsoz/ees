package io.github.agentsoz.bdimatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import javax.inject.Inject;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

final class EvacActivityEngine implements MobsimEngine, ActivityHandler {
	private final ActivityEngine delegate ;
	private EventsManager eventsManager;

	@Inject
	public EvacActivityEngine(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
		delegate = new ActivityEngine( this.eventsManager ) ;
	}

	/**
	 * 
	 * @see org.matsim.core.mobsim.qsim.ActivityEngine#onPrepareSim()
	 */
	@Override
	public void onPrepareSim() {
		delegate.onPrepareSim();
	}

	/**
	 * @param time
	 * @see org.matsim.core.mobsim.qsim.ActivityEngine#doSimStep(double)
	 */
	@Override
	public void doSimStep(double time) {
		delegate.doSimStep(time);
		
		throw new RuntimeException("not feasible in a meaningful way") ;
		
//		beforeFirstSimStep = false;
//		while (activityEndsList.peek() != null) {
//			if (activityEndsList.peek().activityEndTime <= time) {
//				MobsimAgent agent = activityEndsList.poll().agent;
//				unregisterAgentAtActivityLocation(agent);
//				agent.endActivityAndComputeNextState(time);
//				internalInterface.arrangeNextAgentState(agent);
//			} else {
//				return;
//			}
//		}

	}

	/**
	 * 
	 * @see org.matsim.core.mobsim.qsim.ActivityEngine#afterSim()
	 */
	@Override
	public void afterSim() {
		delegate.afterSim();
	}

	/**
	 * @param internalInterface
	 * @see org.matsim.core.mobsim.qsim.ActivityEngine#setInternalInterface(org.matsim.core.mobsim.qsim.InternalInterface)
	 */
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		delegate.setInternalInterface(internalInterface);
	}

	/**
	 * @param agent
	 * @return
	 * @see org.matsim.core.mobsim.qsim.ActivityEngine#handleActivity(org.matsim.core.mobsim.framework.MobsimAgent)
	 */
	@Override
	public boolean handleActivity(MobsimAgent agent) {
		return delegate.handleActivity(agent);
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		delegate.rescheduleActivityEnd(agent);
	}
}
