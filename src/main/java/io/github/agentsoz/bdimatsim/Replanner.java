package io.github.agentsoz.bdimatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelDisutilityUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeUtils;
import org.matsim.withinday.utils.EditRoutes;

public final class Replanner {

	private MATSimModel model;
	private TripRouter tripRouter;
	
	private ActivityEndRescheduler internalInterface ;
	
	Replanner(MATSimModel model, ActivityEndRescheduler activityEndRescheduler)
	{
		this.model = model;
		this.internalInterface = activityEndRescheduler ;
		
		// the following is where the router is set up.  Something that uses, e.g., congested travel time either needs more infrastructure.
		// Currently, this constructor is ultimately called from within createMobsim.  This is a good place since all necessary infrastructure should
		// be available then.  It would have to be passed to here from there (or the router constructed there and passed to here). kai, mar'15
		TravelTime travelTime = TravelTimeUtils.createFreeSpeedTravelTime() ;
		TravelDisutility travelDisutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility( model.getScenario().getConfig().planCalcScore() ) ;
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults() ;
		DefaultTripRouterFactoryImpl factory = builder.build( model.getScenario() ) ;
		tripRouter = factory.instantiateAndConfigureTripRouter(routingContext);
	}
	
	final void reRouteCurrentLeg( MobsimAgent agent, double time ) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		PlanElement pe = plan.getPlanElements().get( WithinDayAgentUtils.getCurrentPlanElementIndex(agent)) ;
		if ( !(pe instanceof Leg) ) {
			return ;
		}
		
		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent) ;
		
		EditRoutes.replanCurrentLegRoute((Leg)pe, ((HasPerson)agent).getPerson(), currentLinkIndex, time, model.getScenario().getNetwork(), tripRouter) ;
		WithinDayAgentUtils.resetCaches(agent);
	}

	final void attachNewActivityAtEndOfPlan(Id<Link> newActivityLinkId, Id<Person> agentId )
	{
		// yyyy if the current activity is not already the last activity of the agent, this method may not behave as expected. kai, feb'14
		
		//changing the end time of the activity can be replaced by the forceEndActivity method
		
		double now = model.getTime() ; 
		
		MobsimAgent agent = model.getMobsimAgentMap().get(agentId);
		
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
//		lastAct.setMaximumDuration(endTime -lastAct.getStartTime());
		lastAct.setEndTime(endTime);

		// now the real work begins. This, as an example, changes the activity (i.e. the destination of the current leg) and then
		// re-splices the plan
		
		Activity newAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("work", newActivityLinkId ) ;
		Leg newLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
		// new Route for current Leg.
		newLeg.setDepartureTime(endTime);
		EditRoutes.relocateFutureLegRoute(newLeg, lastAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson(), 
				this.model.getScenario().getNetwork(), tripRouter );

		newAct.setEndTime( Double.POSITIVE_INFINITY ) ;
		
		planElements.add(newLeg);
		planElements.add(newAct);
		
		WithinDayAgentUtils.resetCaches(agent);
		// this is necessary since the simulation may have cached some info from the plan at other places.
		// (May note be necessary in this particular situation since there is nothing to cache when an agent is at an activity.) kai, feb'14

		this.internalInterface.rescheduleActivityEnd(agent);
		// this is the only place where the internal interface is truly needed, since it is the only place where the agent needs to be "woken up".
		// This is necessary since otherwise the simulation will not touch the agent until its previously scheduled activity end. kai, feb/14
		
	}
	
	final boolean removeActivities(Id<Person> agentId)
	{
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
		this.internalInterface.rescheduleActivityEnd(agent);
		return true;
	}
	
	final boolean forceEndActivity(Id<Person> agentId, int planElementIndex, double newEndTime)
	{
		Map<Id<Person>, MobsimAgent> mapping = model.getMobsimAgentMap();
		MobsimAgent agent = mapping.get(agentId);
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		
		List<PlanElement> planElements = plan.getPlanElements() ;
		
		
		Activity act = (Activity) planElements.get(planElementIndex);
		act.setEndTime(newEndTime);
		act.setMaximumDuration(3600*24);
		
		WithinDayAgentUtils.resetCaches(agent);
		this.internalInterface.rescheduleActivityEnd(agent);
		return true;
	}
}
