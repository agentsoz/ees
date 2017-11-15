package io.github.agentsoz.bdimatsim;

import java.util.List;

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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType;
import io.github.agentsoz.bdimatsim.app.BDIActionHandler;
import io.github.agentsoz.bdimatsim.app.BDIPerceptHandler;

final class DRIVETODefaultActionHandler implements BDIActionHandler {
	private final MATSimModel model;
	public DRIVETODefaultActionHandler(MATSimModel model ) {
		this.model = model;
	}
	@Override
	public boolean handle(String agentID, String actionID, Object[] args) {
		// Get nearest link ID and calls the Replanner to map to MATSim.
		Id<Link> newLinkId;
		if (args[1] instanceof double[]) {
			double[] coords = (double[]) args[1];
			newLinkId = ((SearchableNetwork) model.getScenario()
					.getNetwork()).getNearestLinkExactly(
					new Coord(coords[0], coords[1])).getId();
		} else {
			throw new RuntimeException("Destination coordinates are not given");
		}

		DRIVETODefaultActionHandler.attachNewActivityAtEndOfPlan(newLinkId, Id.createPersonId(agentID), model);

		// Now register a event handler for when the agent arrives at the destination
		AgentWithPerceptsAndActions agent = model.getAgentManager().getAgent( agentID );
		agent.getPerceptHandler().registerBDIPerceptHandler(
				agent.getAgentID(), 
				MonitoredEventType.ArrivedAtDestination, 
				newLinkId,
				new BDIPerceptHandler() {
					@Override
					public boolean handle(Id<Person> agentId, Id<Link> linkId, MonitoredEventType monitoredEvent) {
						AgentWithPerceptsAndActions agent = model.getAgentManager().getAgent( agentId );
						Object[] params = { linkId.toString() };
						agent.getActionContainer().register(MATSimActionList.DRIVETO, params);
						agent.getActionContainer().get(MATSimActionList.DRIVETO).setState(ActionContent.State.PASSED);
						agent.getPerceptContainer().put(MATSimPerceptList.ARRIVED, params);
						return true;
					}
				});
		return true;
	}

	protected static final void attachNewActivityAtEndOfPlan(Id<Link> newActivityLinkId, Id<Person> agentId, MATSimModel model ) {
			// called at least once
	
			// yyyy if the current activity is not already the last activity of the agent, this method may not behave as expected. kai, feb'14
	
			//changing the end time of the activity can be replaced by the forceEndActivity method
	
			double now = model.getTime() ; 
	
			MobsimAgent agent = model.getMobsimDataProvider().getAgents().get(agentId);
	
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
	
			List<PlanElement> planElements = plan.getPlanElements() ;
	
			int planElementsIndex = planElements.size()-1;
			// seems that under normal circumstances in this pgm, size returns 1 und idx is thus 0.
	
			Activity lastAct = (Activity)planElements.get(planElementsIndex);
			// i.e. this would be the first activity
	
			double endTime = now;
			if(endTime <= lastAct.getStartTime() +10){
				endTime = lastAct.getStartTime() +10;
			}
			lastAct.setEndTime(endTime);
	
			// now the real work begins. This changes the activity (i.e. the destination of the current leg) and then
			// re-splices the plan
	
			Activity newAct = model.getScenario().getPopulation().getFactory().createActivityFromLinkId("work", newActivityLinkId ) ;
	
			Leg newLeg = model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
	//		// new Route for current Leg.
			newLeg.setDepartureTime(endTime);
	//		editRoutes.relocateFutureLegRoute(newLeg, lastAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson());
			// --- old version end
	
			newAct.setEndTime( Double.POSITIVE_INFINITY ) ;
	
			planElements.add(newLeg);
			planElements.add(newAct); 
			
	//		final List<Trip> trips = TripStructureUtils.getTrips(planElements, stageActivities);
			
	//		Gbl.assertIf( trips.size()>=1 );
			
			List<PlanElement> sublist = planElements.subList(planElements.size()-3, planElements.size() ) ;
			// (the sub-list is "exclusive" the right index)
			
			Trip trip = TripStructureUtils.getTrips(sublist, model.getReplanner().getEditTrips().getStageActivities()).get(0) ;
			model.getReplanner().getEditTrips().replanFutureTrip(trip, plan, TransportMode.car) ;
	
			WithinDayAgentUtils.resetCaches(agent);
			// this is necessary since the simulation may have cached some info from the plan at other places.
			// (May not be necessary in this particular situation since there is nothing to cache when an agent is at an activity.) kai, feb'14
	
			model.getReplanner().getEditPlans().rescheduleActivityEnd(agent);
			// this is the only place where the internal interface is truly needed, since it is the only place where the agent needs to be "woken up".
			// This is necessary since otherwise the simulation will not touch the agent until its previously scheduled activity end. kai, feb/14
	
		}
		/*
		protected final boolean removeActivities(Id<Person> agentId) {
			// possibly never called
	
			Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
			MobsimAgent agent = mapping.get(agentId);
	
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
	
			List<PlanElement> planElements = plan.getPlanElements() ;
	
			while(planElements.size() > 1){
				planElements.remove(planElements.size()-1);
			}
	
			Activity act = (Activity) planElements.get(0);
			act.setEndTime(3600*24);
			act.setMaximumDuration(3600*24);
	
			WithinDayAgentUtils.resetCaches(agent);
			this.qsim.rescheduleActivityEnd(agent);
			return true;
		}
		 */
		/*
		protected final boolean changeActivityEndTimeByPlanElementIndex(Id<Person> agentId, int planElementIndex, double newEndTime) {
			// probably never called
	
			Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
			MobsimAgent agent = mapping.get(agentId);
	
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
	
			List<PlanElement> planElements = plan.getPlanElements() ;
	
	
			Activity act = (Activity) planElements.get(planElementIndex);
			act.setEndTime(newEndTime);
			act.setMaximumDuration(3600*24);
	
			WithinDayAgentUtils.resetCaches(agent);
			this.qsim.rescheduleActivityEnd(agent);
			return true;
		}
		 */
}