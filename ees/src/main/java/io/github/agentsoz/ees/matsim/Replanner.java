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
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.*;
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
 * Copyright (C) 2014 - 2021 by its authors. See AUTHORS file.
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
			TravelDisutility travelDisutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
			LeastCostPathCalculator pathCalculator = new FastAStarLandmarksFactory(1).createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
			this.editRoutes = new EditRoutes(scenario.getNetwork(), pathCalculator, scenario.getPopulation().getFactory());
		}
		this.editTrips = new EditTrips(tripRouter, qSim2.getScenario(), null ) ;
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
