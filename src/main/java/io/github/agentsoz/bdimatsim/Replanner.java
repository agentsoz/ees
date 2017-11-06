package io.github.agentsoz.bdimatsim;

import javax.inject.Provider;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.StageActivityTypes;
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

/**
 * Design thoughts:<ul>
 * <li> The {@link Replanner} gives access to the MATSim API, which is provided at different hierarchy levels: editRoutes, editTrips,
 * editPlans.  It is (as of now) also the place where the routers for the different modes (CAR_FREESPEED, CAR_CONGESTED, ...)
 * need to be set up.  
 * <li> It might in the end be easiest to just inject it!!
 * </ul>
 * 
 * @author kainagel and others
 */
public final class Replanner {
	private static final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.bushfiretute.BushfireMain");

	protected final MATSimModel model;
	protected final QSim qsim ;

	private final EditRoutes editRoutes;
	private final EditTrips editTrips ;
	private final EditPlans editPlans ;

	final TripRouter tripRouter;
	final StageActivityTypes stageActivities ;

	public Replanner(MATSimModel model, QSim qsim)
	{
		this.model = model;
		this.qsim = qsim ;

		// the following is where the router is set up.  Something that uses, e.g., congested travel time, needs more infrastructure.
		// Currently, this constructor is ultimately called from within createMobsim.  This is a good place since all necessary infrastructure should
		// be available then.  It would have to be passed to here from there (or the router constructed there and passed to here). kai, mar'15
		TravelTime travelTime = TravelTimeUtils.createFreeSpeedTravelTime() ;
		TravelDisutility travelDisutility = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility( model.getScenario().getConfig().planCalcScore() ) ;
		{
			TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults() ;
			builder.setTravelTime(travelTime);
			builder.setTravelDisutility(travelDisutility);
			Provider<TripRouter> provider = builder.build( model.getScenario() ) ;
			tripRouter = provider.get() ;
			stageActivities = tripRouter.getStageActivityTypes() ;
		}
		//		LeastCostPathCalculator pathCalculator = new DijkstraFactory().createPathCalculator( model.getScenario().getNetwork(), travelDisutility, travelTime) ;
		LeastCostPathCalculator pathCalculator = new FastAStarLandmarksFactory().createPathCalculator( model.getScenario().getNetwork(), travelDisutility, travelTime) ;
		this.editRoutes = new EditRoutes(model.getScenario().getNetwork(), pathCalculator, model.getScenario().getPopulation().getFactory() ) ;
		this.editTrips = new EditTrips(tripRouter, qsim.getScenario() ) ;
		this.editPlans = new EditPlans(qsim, tripRouter, editTrips, model.getScenario().getPopulation().getFactory() ) ;
	}

	public EditRoutes getEditRoutes() {
		return editRoutes;
	}

	public EditTrips getEditTrips() {
		return editTrips;
	}

	public EditPlans getEditPlans() {
		return editPlans;
	}

	static enum Congestion { freespeed, currentCongestion } 
	static enum AllowedLinks { forCivilians, forEmergencyServices, all }

}
