package io.github.agentsoz.ees.matsim;

/*
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
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReplanDriveToDefaultActionHandlerV2 implements BDIActionHandler {
	private static final Logger log = LoggerFactory.getLogger(ReplanDriveToDefaultActionHandlerV2.class ) ;

	private final MATSimModel model;
	public ReplanDriveToDefaultActionHandlerV2(MATSimModel model ) {
		this.model = model;
	}
	@Override
	public ActionContent.State handle(String agentID, String actionID, Object[] args) {
		// assertions:
		MobsimAgent mobsimAgent = model.getMobsimAgentFromIdString(agentID) ;
		if (mobsimAgent == null) {
			log.error("MobsimAgent " + agentID +
					" no longer exists (was likely removed from QSim); will ignore " + actionID +
					" request");
			return ActionContent.State.FAILED;
		}

		Gbl.assertIf( args.length >= 1 );
		Gbl.assertIf( args[0] instanceof Constants.EvacRoutingMode) ; // could have some default
		Constants.EvacRoutingMode routingMode = (Constants.EvacRoutingMode)args[0];
		if (WithinDayAgentUtils.isOnReplannableCarLeg(mobsimAgent)) {
			model.getReplanner().editTrips().replanCurrentTrip(mobsimAgent, 0.0, routingMode.name());
		}
		return ActionContent.State.PASSED;
	}
}
