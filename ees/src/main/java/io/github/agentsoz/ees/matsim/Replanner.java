package io.github.agentsoz.ees.matsim;

import io.github.agentsoz.ees.Constants;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditRoutes;
import org.matsim.withinday.utils.EditTrips;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

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

public final class Replanner {
	// note that this is no longer meant to be extended for customization.  The "action recipes" now go directly into the classes
	// that implement BDIActionHandler.  kai, nov'17
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Replanner.class) ;
	private final Map<String, TravelTime> travelTimes;
	
	private EditRoutes editRoutes;
	private EditTrips editTrips ;
	private EditPlans editPlans ;
	
	@Inject
	Replanner(QSim qSim2, TripRouter tripRouter, Map<String,TravelTime> travelTimes ) {
		Scenario scenario = qSim2.getScenario();
		this.travelTimes = travelTimes ;
		{
			TravelTime travelTime = TravelTimeUtils.createFreeSpeedTravelTime();
			TravelDisutility travelDisutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility(scenario.getConfig().scoring());
			LeastCostPathCalculator pathCalculator = new AStarLandmarksFactory(1).createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
			this.editRoutes = new EditRoutes(scenario.getNetwork(), pathCalculator, scenario.getPopulation().getFactory());
		}
		this.editTrips = new EditTrips(tripRouter, qSim2.getScenario(), null, TimeInterpretation.create(scenario.getConfig())) ;
		this.editPlans = new EditPlans(qSim2, tripRouter, editTrips, scenario.getPopulation().getFactory() ) ;
	}

	@Deprecated // yyyy but I don't have an easy replacement yet
	// (maybe just editTrips.replanCurrentTrip(...)?)
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
	@Deprecated // I don't think that this is really needed for the bushfire applications.  kai, nov'17
	public EditRoutes editRoutes(Constants.EvacRoutingMode evacRoutingMode) {
		switch( evacRoutingMode ) {
			case carFreespeed:
				return editRoutes ;
			default:
				throw new RuntimeException("not implemented.  See how editRoutes is constructed.  " +
												   "Should, however, also not be needed; try using editTrips or " +
												   "editPlans instaed.") ;
		}
	}

	public EditTrips editTrips() {
		return editTrips;
	}

	public EditPlans editPlans() {
		return editPlans;
	}
	
	final void addNetworkChangeEvent(NetworkChangeEvent changeEvent) {
		
		TravelTime globalTTime = this.travelTimes.get(Constants.EvacRoutingMode.carGlobalInformation.name());
		if ( globalTTime instanceof WithinDayTravelTime ) {
			((WithinDayTravelTime) globalTTime).addNetworkChangeEvent(changeEvent);
		}
	}
	
	enum Congestion { freespeed, currentCongestion }
	enum AllowedLinks { forCivilians, forEmergencyServices, all }
	
}
