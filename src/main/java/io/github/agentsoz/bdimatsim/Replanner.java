package io.github.agentsoz.bdimatsim;

import javax.inject.Inject;

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

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelDisutilityUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeUtils;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditRoutes;
import org.matsim.withinday.utils.EditTrips;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Replanner {
	// note that this is no longer meant to be extended for customization.  The "action recipes" now go directly into the classes
	// that implement BDIActionHandler.  kai, nov'17
	
	private static final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.bushfiretute.BushfireMain");

	private QSim qsim ;
	
	private EditRoutes editRoutes;
	private EditTrips editTrips ;
	private EditPlans editPlans ;

	private TripRouter tripRouter;

	private Scenario scenario;

	@Inject Replanner(QSim qSim2) {
		setQSim( qSim2 ) ;
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
	public EditRoutes editRoutes() {
		return editRoutes;
	}

	public EditTrips editTrips() {
		return editTrips;
	}

	public EditPlans editPlans() {
		return editPlans;
	}

	static enum Congestion { freespeed, currentCongestion } 
	static enum AllowedLinks { forCivilians, forEmergencyServices, all }
	private void setQSim(QSim qSim2) {
		this.qsim = qSim2 ;
		this.scenario = qSim2.getScenario() ;
		// the following is where the router is set up.  Something that uses, e.g., congested travel time, needs more infrastructure.
		// Currently, this constructor is ultimately called from within createMobsim.  This is a good place since all necessary infrastructure should
		// be available then.  It would have to be passed to here from there (or the router constructed there and passed to here). kai, mar'15
		TravelTime travelTime = TravelTimeUtils.createFreeSpeedTravelTime() ;
		TravelDisutility travelDisutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility( scenario.getConfig().planCalcScore() ) ;

		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults() ;
		builder.setTravelTime(travelTime);
		builder.setTravelDisutility(travelDisutility);
		Provider<TripRouter> provider = builder.build( scenario ) ;
		tripRouter = provider.get() ;

		//		LeastCostPathCalculator pathCalculator = new DijkstraFactory().createPathCalculator( scenario.getNetwork(), travelDisutility, travelTime) ;
		LeastCostPathCalculator pathCalculator = new FastAStarLandmarksFactory().createPathCalculator( scenario.getNetwork(), travelDisutility, travelTime) ;
		this.editRoutes = new EditRoutes(scenario.getNetwork(), pathCalculator, scenario.getPopulation().getFactory() ) ;
		this.editTrips = new EditTrips(tripRouter, qsim.getScenario() ) ;
		this.editPlans = new EditPlans(qsim, tripRouter, editTrips, scenario.getPopulation().getFactory() ) ;
	}

}
