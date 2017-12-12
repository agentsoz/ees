/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.utils.EditTrips;

class EvacAgent implements MobsimDriverAgent, HasPerson, PlanAgent, HasModifiablePlan {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(EvacAgent.class);

	private final BasicPlanAgentImpl basicAgentDelegate ;
	private final PlanBasedDriverAgentImpl driverAgentDelegate ;

	private final TripRouter tripRouter  ;
	private final EditTrips editTrips ;
	private final Network network;
	private final MobsimTimer simTimer;
	private final EventsManager eventsManager;
	
	private boolean planWasModified = false ;
	private double expectedLinkLeaveTime;
	
	EvacAgent(final Plan selectedPlan, final Netsim simulation, TripRouter tripRouter) {
		this.tripRouter = tripRouter;
		this.basicAgentDelegate = new BasicPlanAgentImpl(selectedPlan, simulation.getScenario(), simulation.getEventsManager(),
				simulation.getSimTimer() ) ;
		this.driverAgentDelegate = new PlanBasedDriverAgentImpl(basicAgentDelegate) ;
		this.editTrips = new EditTrips(tripRouter, simulation.getScenario() ) ;
		this.network = simulation.getScenario().getNetwork() ;
		this.simTimer = simulation.getSimTimer() ;
		this.eventsManager = simulation.getEventsManager() ;

		// deliberately does NOT keep a back pointer to the whole Netsim; this should also be removed in the constructor call.
	}

	@Override
	public final void endLegAndComputeNextState(double now) {
		basicAgentDelegate.endLegAndComputeNextState(now);
	}

	@Override
	public final void setStateToAbort(double now) {
		basicAgentDelegate.setStateToAbort(now);
	}

	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		basicAgentDelegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public final void endActivityAndComputeNextState(double now) {
		// yyyyyy planWasModified does not work all the way through; possibly confusion between static method calls and polymorphic
		// programming???

		//		final PlanElement nextPlanElement = basicAgentDelegate.getNextPlanElement();
		// yyyyyy this seems to be getting the unmodified plan but I don't know why. kai, nov'17
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(this) ;
		Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex(this) ;
		if ( index+1 < plan.getPlanElements().size() ) {
			// (otherwise it will fail, but we leave this to the delegate)
			
			PlanElement nextPlanElement = plan.getPlanElements().get(index+1) ;
			log.trace( "next plan element=" + nextPlanElement );
			if ( nextPlanElement instanceof Leg ) {
				if ( 
						//planWasModified || 
						((Leg)nextPlanElement).getRoute()==null ) {
					log.trace("leg has no route; recomputing next trip") ;
					Activity act = (Activity) basicAgentDelegate.getCurrentPlanElement() ;
					if ( !tripRouter.getStageActivityTypes().isStageActivity(act.getType()) ) {
						// (= we just ended a "real" activity)

						Trip trip = TripStructureUtils.findTripStartingAtActivity(act, this.getModifiablePlan(), 
								tripRouter.getStageActivityTypes() ) ;
						String mainMode = tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements()) ;
						log.debug("identified main mode=" + mainMode ) ;
						editTrips.replanFutureTrip(trip, this.getModifiablePlan(), mainMode, now ) ;
						
						Trip newTrip = TripStructureUtils.findTripStartingAtActivity(act, this.getModifiablePlan(), 
								tripRouter.getStageActivityTypes() ) ;

						// something in the following is a null pointer ... which is triggered even
						// when the log level is above debug.  kai, dec'17
//						if ( ! ( trip.toString().equals(newTrip.toString()) ) ) {
//							log.trace( "prevTrip:\t" + trip.toString() );
//							log.trace("prevRoute:\t" + trip.getLegsOnly().get(0).getRoute().toString()) ;
//							log.trace("newRoute:\t" + newTrip.getLegsOnly().get(0).getRoute().toString()) ;
//							log.trace( "newTrip:\t" + newTrip.toString() );
//							log.trace("");
//						}
						
					}
				}
			}
		}
		basicAgentDelegate.endActivityAndComputeNextState(now);
	}

	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		return basicAgentDelegate.getPlannedVehicleId();
	}

	@Override
	public final String getMode() {
		return basicAgentDelegate.getMode();
	}

	@Override
	public final Double getExpectedTravelTime() {
		return basicAgentDelegate.getExpectedTravelTime();
	}

	@Override
	public final Double getExpectedTravelDistance() {
		return basicAgentDelegate.getExpectedTravelDistance();
	}

	@Override
	public String toString() {
		return basicAgentDelegate.toString();
	}

	@Override
	public final PlanElement getCurrentPlanElement() {
		return basicAgentDelegate.getCurrentPlanElement();
	}

	@Override
	public final PlanElement getNextPlanElement() {
		return basicAgentDelegate.getNextPlanElement();
	}

	@Override
	public final Plan getCurrentPlan() {
		return basicAgentDelegate.getCurrentPlan();
	}

	@Override
	public final Id<Person> getId() {
		return basicAgentDelegate.getId();
	}

	@Override
	public final Person getPerson() {
		return basicAgentDelegate.getPerson();
	}

	@Override
	public final MobsimVehicle getVehicle() {
		return basicAgentDelegate.getVehicle();
	}

	@Override
	public final void setVehicle(MobsimVehicle vehicle) {
		basicAgentDelegate.setVehicle(vehicle);
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		return basicAgentDelegate.getCurrentLinkId();
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return basicAgentDelegate.getDestinationLinkId();
	}

	@Override
	public final double getActivityEndTime() {
		return basicAgentDelegate.getActivityEndTime();
	}

	@Override
	public final State getState() {
		return basicAgentDelegate.getState();
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		driverAgentDelegate.notifyMoveOverNode(newLinkId);
		Link link = network.getLinks().get( newLinkId ) ;
		double now = simTimer.getTimeOfDay() ;
		this.expectedLinkLeaveTime = link.getLength()/link.getFreespeed(now) ;
	}
	final double getExpectedLinkLeaveTime() {
		return this.expectedLinkLeaveTime ;
	}

	@Override
	public final Id<Link> chooseNextLinkId() {
		return driverAgentDelegate.chooseNextLinkId();
	}

	@Override
	public final boolean isWantingToArriveOnCurrentLink() {
		boolean retVal = driverAgentDelegate.isWantingToArriveOnCurrentLink();
		
		if (retVal==false) {
			Link nextLink = this.network.getLinks().get( this.chooseNextLinkId() ) ;
			final double now = this.simTimer.getTimeOfDay();
			if (nextLink.getFreespeed(now) < 0.1) {
				this.eventsManager.processEvent(
						new NextLinkBlockedEvent(
								now, this.getVehicle().getId(), this.getId(), this.getCurrentLinkId(),
														nextLink.getId() ) );
			}
		}
		
		return retVal ;
	}

	//	final Leg getCurrentLeg() {
	//		return basicAgentDelegate.getCurrentLeg() ;
	//	}
	//	final int getCurrentLinkIndex() {
	//		return basicAgentDelegate.getCurrentLinkIndex() ;
	//	}
	//	final int getCurrentPlanElementIndex() {
	//		return basicAgentDelegate.getCurrentPlanElementIndex() ;
	//	}
	@Override public final Plan getModifiablePlan() {
		this.planWasModified=true ;
		return basicAgentDelegate.getModifiablePlan() ;
	}
	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.basicAgentDelegate.getCurrentFacility();
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.basicAgentDelegate.getDestinationFacility();
	}

	@Override
	public final PlanElement getPreviousPlanElement() {
		return this.basicAgentDelegate.getPreviousPlanElement();
	}

	@Override
	public void resetCaches() {
		this.basicAgentDelegate.resetCaches();
		this.driverAgentDelegate.resetCaches() ;
	}

	@Override
	public int getCurrentLinkIndex() {
		return this.basicAgentDelegate.getCurrentLinkIndex() ;
	}

}
