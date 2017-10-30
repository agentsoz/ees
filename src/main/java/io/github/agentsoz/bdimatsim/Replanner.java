package io.github.agentsoz.bdimatsim;

import java.util.Collection;
import java.util.Iterator;

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
import java.util.ListIterator;

import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelDisutilityUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.withinday.utils.EditRoutes;
import org.matsim.withinday.utils.EditTrips;
import org.matsim.withinday.utils.ReplanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replanner {
	protected static final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.bushfiretute.BushfireMain");

	protected final MATSimModel model;
	protected final QSim qsim ;
	protected final EditRoutes editRoutes;
	protected final EditTrips editTrips ;

	private final TripRouter tripRouter;
	private final StageActivityTypes stageActivities ;

	protected Replanner(MATSimModel model, QSim qsim)
	{
		this.model = model;
		this.qsim = qsim ;

		// the following is where the router is set up.  Something that uses, e.g., congested travel time, needs more infrastructure.
		// Currently, this constructor is ultimately called from within createMobsim.  This is a good place since all necessary infrastructure should
		// be available then.  It would have to be passed to here from there (or the router constructed there and passed to here). kai, mar'15
		TravelTime travelTime = TravelTimeUtils.createFreeSpeedTravelTime() ;
		TravelDisutility travelDisutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility( model.getScenario().getConfig().planCalcScore() ) ;

		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults() ;
		builder.setTravelTime(travelTime);
		builder.setTravelDisutility(travelDisutility);
		Provider<TripRouter> provider = builder.build( model.getScenario() ) ;
		tripRouter = provider.get() ;
		stageActivities = tripRouter.getStageActivityTypes() ;

		//		LeastCostPathCalculator pathCalculator = new DijkstraFactory().createPathCalculator( model.getScenario().getNetwork(), travelDisutility, travelTime) ;
		LeastCostPathCalculator pathCalculator = new FastAStarLandmarksFactory().createPathCalculator( model.getScenario().getNetwork(), travelDisutility, travelTime) ;
		this.editRoutes = new EditRoutes(model.getScenario().getNetwork(), pathCalculator, model.getScenario().getPopulation().getFactory() ) ;
		this.editTrips = new EditTrips(tripRouter) ;
	}

	protected final void reRouteCurrentLeg( MobsimAgent agent, double now ) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		PlanElement pe = plan.getPlanElements().get( WithinDayAgentUtils.getCurrentPlanElementIndex(agent)) ;
		if ( !(pe instanceof Leg) ) {
			return ;
		}
		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent) ;
		this.editRoutes.replanCurrentLegRoute((Leg)pe, ((HasPerson)agent).getPerson(), currentLinkIndex, now ) ;
		WithinDayAgentUtils.resetCaches(agent);
	}

	protected final void attachNewActivityAtEndOfPlan(Id<Link> newActivityLinkId, Id<Person> agentId ) {
		// called at least once

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
		lastAct.setEndTime(endTime);

		// now the real work begins. This changes the activity (i.e. the destination of the current leg) and then
		// re-splices the plan

		Activity newAct = this.model.getScenario().getPopulation().getFactory().createActivityFromLinkId("work", newActivityLinkId ) ;

//		Leg newLeg = this.model.getScenario().getPopulation().getFactory().createLeg(TransportMode.car);
//		// new Route for current Leg.
//		newLeg.setDepartureTime(endTime);
//		editRoutes.relocateFutureLegRoute(newLeg, lastAct.getLinkId(), newActivityLinkId,((HasPerson)agent).getPerson());

		newAct.setEndTime( Double.POSITIVE_INFINITY ) ;

//		planElements.add(newLeg);
		planElements.add(newAct);
		
		List<PlanElement> sublist = planElements.subList(planElements.size()-2, planElements.size()-1 ) ;
		// (does it work without the leg in between?)
		
		Trip trip = TripStructureUtils.getTrips(sublist, stageActivities).get(0) ;
		editTrips.replanFutureTrip(trip, plan, TransportMode.car) ;

		WithinDayAgentUtils.resetCaches(agent);
		// this is necessary since the simulation may have cached some info from the plan at other places.
		// (May note be necessary in this particular situation since there is nothing to cache when an agent is at an activity.) kai, feb'14

		this.qsim.rescheduleActivityEnd(agent);
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

	static enum Congestion { freespeed, currentCongestion } 
	static enum AllowedLinks { forCivilians, forEmergencyServices, all }
	public final class IntelligentPlan {
		private MobsimAgent agent;
		private final Person person ;
		private List<PlanElement> planElements;
		private Plan plan;
		IntelligentPlan( MobsimAgent agent ) {
			this.agent = agent ;
			this.plan = WithinDayAgentUtils.getModifiablePlan(agent);
			this.planElements = plan.getPlanElements();
			if ( agent instanceof HasPerson ) {
				this.person = ((HasPerson) agent).getPerson() ;
			} else {
				this.person = null ;
			}
		}
		public void setRouting(Congestion routing) {
		}
		public void setAllowedLinks(AllowedLinks allowedLinks) {
		}
		// ---
		public boolean addActivityAtEnd(Activity e) {
			return planElements.add(e) ;
		}
		public boolean removeActivity(Activity o) {
			return planElements.remove(o) ;
		}
		public Activity setActivity(int index, Activity newAct) {
			return setActivity( index, newAct, null, null ) ;
		}
		public Activity setActivity(int index, Activity newAct, String requestedUpstreamMode, String downstreamMode ) {
			// make sure we have indeed an activity position:
			if ( planElements.get(index) instanceof Activity ) {
				throw new ReplanningException("trying to replace a non-activity in the plan by an activity; this is not possible") ;
			}

			Activity origAct = (Activity) planElements.get(index) ;

			// make sure that this was not a stage activity:
			if( stageActivities.isStageActivity(origAct.getType()) ){
				throw new ReplanningException("trying to replace a helper activity (stage activity) by a real activity; this is not possible") ;
			}

			final Integer currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

			// make sure we are not yet beyond the to-be-replanned activity:
			if ( index < currentIndex ) {
				throw new ReplanningException("trying to replace an activity that lies in the past; this is not possible") ;
			}

			// set the new activity:
			planElements.set(index, newAct) ;

			if ( locationsAreSame( origAct, newAct ) ) {
				return origAct ;  // signature of method, from Java Collections
				// yyyy if newAct has a different duration/endTime, we should still replan the following trip
			}
			// else:
			logger.trace("location has changed; need to replan both trips") ;
			
			// trip before (if any):
			if ( index > 0 ) {
				Trip tripBeforeAct = findTripBeforeAct(origAct);
				Gbl.assertNotNull( tripBeforeAct );  // there could also just be a sequence of activities?!

				final PlanElement currentPlanElement = ((PlanAgent)agent).getCurrentPlanElement();
				final List<PlanElement> currentTripElements = tripBeforeAct.getTripElements();
				final String currentMode = tripRouter.getMainModeIdentifier().identifyMainMode( currentTripElements ) ;

				// find out if trip before has already been started:
				int tripElementsIndex = currentTripElements.indexOf( currentPlanElement ) ;
				if ( tripElementsIndex==-1) {
					// trip has not yet started; this is the easy case
					if ( requestedUpstreamMode != null ) {
						editTrips.replanFutureTrip(tripBeforeAct, plan, requestedUpstreamMode);
					} else {
						editTrips.replanFutureTrip(tripBeforeAct, plan, currentMode);
					}
				} else {
					// trip has already started
					checkIfSameMode(requestedUpstreamMode, currentMode);
					editTrips.replanCurrentTrip(agent, qsim.getSimTimer().getTimeOfDay(), qsim.getScenario() );
				}
			}
			// trip after (if any):
			if ( index < planElements.size()-1 ) {
				Trip tripAfterAct = findTripAfterAct(origAct);
				Gbl.assertIf( tripAfterAct!=null ); // there could also just be a sequence of activities?!
				if ( downstreamMode==null ) {
					editTrips.replanFutureTrip(tripAfterAct, plan, tripRouter.getMainModeIdentifier().identifyMainMode( tripAfterAct.getTripElements() ));
				} else {
					editTrips.replanFutureTrip(tripAfterAct, plan, downstreamMode);
				}
			}
			WithinDayAgentUtils.resetCaches(agent);
			qsim.rescheduleActivityEnd(agent);
			return origAct ;
		}
		private void checkIfSameMode(String upstreamMode, final String currentMode) {
			if ( upstreamMode!=null && !upstreamMode.equals(currentMode) ) {
				throw new ReplanningException( "cannot change mode in trip that has already started.  Don't set the mode in the request, "
						+ "or somehow make the agent to abort the current trip first." ) ;
			}
		}
		private Trip findTripAfterAct(Activity act) {
			List<Trip> trips = TripStructureUtils.getTrips(planElements, tripRouter.getStageActivityTypes() ) ;
			Trip tripAfterAct = null ; 
			for ( Trip trip : trips ) {
				if ( act.equals( trip.getOriginActivity() ) ) {
					tripAfterAct = trip ;
				}
			}
			return tripAfterAct;
		}
		private Trip findTripBeforeAct(Activity act) {
			List<Trip> trips = TripStructureUtils.getTrips(planElements, tripRouter.getStageActivityTypes() ) ;
			Trip tripBeforeAct = null ;
			for ( Trip trip : trips ) {
				if ( act.equals( trip.getDestinationActivity() ) ) {
					tripBeforeAct = trip ;
				}
			}
			return tripBeforeAct;
		}
		private boolean locationsAreSame(Activity origAct, Activity newAct) {
			if ( origAct.getFacilityId() != null && newAct.getFacilityId() != null ) {
				return origAct.getFacilityId().equals( newAct.getFacilityId() ) ;
			}
			if ( origAct.getCoord() != null && newAct.getCoord() != null ) {
				return origAct.getCoord().equals( newAct.getCoord() ) ;
			}
			return false ;
		}
		public void insertActivity(int index, Activity activity, String upstreamMode, String downstreamMode ) {
			planElements.add( index, activity ) ;
			int currentPlanElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ;
			Facility<?> hereFacility = asFacility(activity);
			{
				// find activity before:
				Activity prevAct = null ;
				for ( int ii=0 ; ii<currentPlanElementsIndex ; ii++ ) {
					if ( planElements.get(ii) instanceof Activity ) {
						Activity act = (Activity) planElements.get(ii) ;
						if ( !stageActivities.isStageActivity( act.getType() ) ) {
							prevAct = act ;
						}
					}
				}
				if ( prevAct != null ) {
					// yyyyyy trip may already have started !!!!
					double departureTime = qsim.getSimTimer().getTimeOfDay() ;  // yyyyyy it might be earlier or later!
					tripRouter.calcRoute(upstreamMode, asFacility(prevAct), hereFacility, departureTime, person) ;
				}
			}
			{
				// find activity after:
				Activity nextAct = null ;
				for ( int ii=planElements.size()-1 ; ii>currentPlanElementsIndex; ii-- ) {
					if ( planElements.get(ii) instanceof Activity ) {
						Activity act = (Activity) planElements.get(ii) ;
						if ( !stageActivities.isStageActivity( act.getType() ) ) {
							nextAct = act ;
						}
					}
				}
				if ( nextAct != null ) {
					double departureTime = qsim.getSimTimer().getTimeOfDay() ;  // yyyyyy it might be earlier or later!
					tripRouter.calcRoute(downstreamMode, hereFacility, asFacility(nextAct), departureTime, person) ;
				}
			}			
		}
		private Facility<?> asFacility(Activity activity) {
			Facility<?> hereFacility = new ActivityWrapperFacility( activity ) ;
			if ( activity.getFacilityId()!=null ) {
				ActivityFacility facility = qsim.getScenario().getActivityFacilities().getFacilities().get( activity.getFacilityId() ) ;
				if ( facility != null ) {
					hereFacility = facility ;
				}
			}
			return hereFacility;
		}
		public PlanElement removeActivity(int index) {
			return planElements.remove(index) ;
		}
		public int indexOf(Object o) {
			return planElements.indexOf(o) ;
		}
		public int indexOfNextActivityWithType( String type ) {
			for ( int index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ; index < planElements.size() ; index++ ) {
				PlanElement pe = planElements.get(index) ;
				if ( pe instanceof Activity ) {
					if ( ((Activity)pe).getType().equals(type) ) {
						return index ;
					}
				}
			}
			return -1 ;
		}
		public List<PlanElement> subList(int fromIndex, int toIndex) {
			return WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().subList( fromIndex, toIndex ) ;
		}

	}

}
