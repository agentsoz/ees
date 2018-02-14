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

import io.github.agentsoz.bdimatsim.MATSimModel.EvacRoutingMode;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelDisutilityUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeUtils;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditRoutes;
import org.matsim.withinday.utils.EditTrips;

import java.util.Map;

import static io.github.agentsoz.bdimatsim.MATSimModel.EvacRoutingMode.*;

public final class Replanner {
	// note that this is no longer meant to be extended for customization.  The "action recipes" now go directly into the classes
	// that implement BDIActionHandler.  kai, nov'17
	
	private static final Logger logger = Logger.getLogger(Replanner.class) ;
	private final Map<String, TravelTime> travelTimes;
	
	private QSim qsim ;
	
	private EditRoutes editRoutes;
	private EditTrips editTrips ;
	private EditPlans editPlans ;

	private Scenario scenario;

	@Inject
	Replanner(QSim qSim2, TripRouter tripRouter, Map<String,TravelTime> travelTimes ) {
		this.qsim = qSim2;
		this.scenario = qSim2.getScenario() ;
		this.travelTimes = travelTimes ;
		{
			TravelTime travelTime = TravelTimeUtils.createFreeSpeedTravelTime();
			TravelDisutility travelDisutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
			LeastCostPathCalculator pathCalculator = new FastAStarLandmarksFactory().createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
			this.editRoutes = new EditRoutes(scenario.getNetwork(), pathCalculator, scenario.getPopulation().getFactory());
		}
		this.editTrips = new EditTrips(tripRouter, qsim.getScenario() ) ;
		this.editPlans = new EditPlans(qsim, tripRouter, editTrips, scenario.getPopulation().getFactory() ) ;
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
	public EditRoutes editRoutes(EvacRoutingMode evacRoutingMode) {
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
	
	public void addNetworkChangeEvent(NetworkChangeEvent changeEvent) {
		
		TravelTime globalTTime = this.travelTimes.get(EvacRoutingMode.carGlobalInformation);
		if ( globalTTime instanceof WithinDayTravelTime ) {
			double now = this.qsim.getSimTimer().getTimeOfDay() ;
			for ( Link link : changeEvent.getLinks() ) {
				// in the router, we only care about speed changes:
				final NetworkChangeEvent.ChangeValue freespeedChange = changeEvent.getFreespeedChange();
				if (freespeedChange != null ) {
					double speed;
					switch ( freespeedChange.getType() ) {
						case ABSOLUTE_IN_SI_UNITS:
							speed = freespeedChange.getValue() ;
							break;
						case FACTOR:
							speed = link.getFreespeed(now) * freespeedChange.getValue() ;
							break;
						case OFFSET_IN_SI_UNITS:
							speed = link.getFreespeed(now) + freespeedChange.getValue() ;
							break;
						default:
							throw new RuntimeException(Gbl.NOT_IMPLEMENTED) ;
					}
					((WithinDayTravelTime) globalTTime).changeSpeedMetersPerSecond(link, speed);
				}
			}
		}
	}
	
	enum Congestion { freespeed, currentCongestion }
	enum AllowedLinks { forCivilians, forEmergencyServices, all }
	
}
