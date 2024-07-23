package io.github.agentsoz.ees.matsim;

/*
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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.nonmatsim.BDIActionHandler;
import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.EventData;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.util.Global;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EvacDrivetoActionHandlerV2 implements BDIActionHandler {
	private static final Logger log = LoggerFactory.getLogger(EvacDrivetoActionHandlerV2.class ) ;

	private final MATSimModel model;
	public EvacDrivetoActionHandlerV2(MATSimModel model ) {
//		log.setLevel(Level.DEBUG);
		this.model = model;
	}
	@Override
	public ActionContent.State handle(String agentID, String actionID, Object[] args) {
		log.debug("------------------------------------------------------------------------------------------");
		{
			// dsingh 16/may/18 - towards #12
			if (args.length < 4) {
				log.error("agent " + agentID + " DRIVETO handler has " +args.length + " args (>=4 expected); will continue without handling this event");
				return ActionContent.State.RUNNING; // FIXME: seems incorrect to return RUNNING
			}
		}
		// assertions:
		Gbl.assertIf( args.length >= 4 ) ;
		Gbl.assertIf( args[1] instanceof double[] ) ;
		double[] coords = (double[]) args[1];
		Coord coord = new Coord( coords[0], coords[1] ) ;
		Gbl.assertIf( args[3] instanceof Constants.EvacRoutingMode) ; // could have some default
		
		log.debug("replanning; time step=" + model.getTime()
		+ "; agentId=" + agentID + "; routing mode " + args[3] ) ;

		// preparations:

		MobsimAgent mobsimAgent = model.getMobsimAgentFromIdString(agentID) ;
		if (mobsimAgent == null) {
			log.error("MobsimAgent " + agentID +
					" no longer exists (was likely removed from QSim); will ignore " + actionID +
					" request");
			return ActionContent.State.FAILED;
		}

		final Link nearestLink = NetworkUtils.getNearestLink(model.getScenario().getNetwork(), coord );
		Gbl.assertNotNull(nearestLink);
		Id<Link> newLinkId = nearestLink.getId();
		//  could give just coordinates to matsim, but for time being need the linkId in the percept anyways
		
		// new departure time:
		if ( model.getReplanner().editPlans().isAtRealActivity(mobsimAgent) ) {
			model.getReplanner().editPlans().rescheduleCurrentActivityEndtime(mobsimAgent, (double)args[2]);
		}
		
		// routingMode:
		String routingMode = null ; // could have some default
		switch (((Constants.EvacRoutingMode) args[3])) {
			case carFreespeed:
				routingMode = Constants.EvacRoutingMode.carFreespeed.name() ;
				break;
			case carGlobalInformation:
				routingMode = Constants.EvacRoutingMode.carGlobalInformation.name() ;
				break;
			case emergencyVehicle:
				routingMode = Constants.EvacRoutingMode.emergencyVehicle.name();
				break;
			default:
				throw new RuntimeException("not implemented" ) ;
		}
		
		// flush everything beyond current:
		printPlan("before flush: ", mobsimAgent);
		model.getReplanner().editPlans().flushEverythingBeyondCurrent(mobsimAgent) ;
		printPlan("after flush: " , mobsimAgent) ;

		boolean addReplanActivity = (args.length >=  6 && args[5] instanceof Boolean) ? (Boolean)args[5] : false;
		int replanTime = (args.length >=  7 && args[6] instanceof Integer) ? (int)args[6] : 0;

		// add small replan activity so we know where/when the replanning occurred
		Activity rnewAct = null;
		if (addReplanActivity) {
			String ractivity = "Replan";
			final Link link = model.getScenario().getNetwork().getLinks().get( mobsimAgent.getCurrentLinkId() );
			rnewAct = model.getReplanner().editPlans().createFinalActivity(ractivity, link.getId());
			rnewAct.setStartTime(model.getTime());
			rnewAct.setEndTime(model.getTime() + replanTime);
			model.getReplanner().editPlans().addActivityAtEnd(mobsimAgent, rnewAct, routingMode);
			printPlan("after adding act: ", mobsimAgent);
		}

		// new evac destination
		String activity = (args.length >=  5 && args[4] instanceof String) ? (String)args[4] : "DriveTo";
		Activity newAct = model.getReplanner().editPlans().createFinalActivity( activity, newLinkId ) ;
		model.getReplanner().editPlans().addActivityAtEnd(mobsimAgent, newAct, routingMode) ;
		printPlan("after adding act: " , mobsimAgent ) ;

		// add an empty leg between the replan and evac activities
		if (addReplanActivity) {
			model.getReplanner().editTrips().insertEmptyTrip(
					WithinDayAgentUtils.getModifiablePlan(mobsimAgent),
					rnewAct, newAct, routingMode);
		}

		// Record that this agent is driving
		model.getAgentManager().getAgentsPerformingBdiDriveTo().put(agentID, newLinkId.toString());

		log.debug("------------------------------------------------------------------------------------------"); ;
		return ActionContent.State.RUNNING;
	}
	
	private void printPlan(String str ,MobsimAgent agent1) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent1) ;
		log.debug(str + plan ); ;
		for ( PlanElement pe : plan.getPlanElements() ) {
			log.debug("    " + pe ); ;
		}
	}
}
