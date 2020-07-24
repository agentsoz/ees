package io.github.agentsoz.ees.matsim;

/*
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2020 by its authors. See AUTHORS file.
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

import com.google.common.collect.ObjectArrays;
import com.google.gson.Gson;
import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Replanner;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.Disruption;
import io.github.agentsoz.ees.EmergencyMessage;
import io.github.agentsoz.ees.matsim.router.ExampleRoutingAlgorithmFactory;
import io.github.agentsoz.ees.util.Utils;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.nonmatsim.PAAgentManager;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Stream;

import static io.github.agentsoz.bdimatsim.MATSimModel.convertTimeToSeconds;

/**
 * @author Dhi Singh
 */
public final class MATSimEvacModel implements ABMServerInterface, QueryPerceptInterface, DataClient {

    private static final Logger log = LoggerFactory.getLogger(MATSimEvacModel.class);
    private final MATSimModel matsimModel;
    private final Map<String, DataClient> dataListeners = createDataListeners();
    private EvacConfig evacConfig = null;

    private Shape2XyWriter fireWriter;
    private Shape2XyWriter emberWriter;
    private DisruptionWriter disruptionWriter;

    private final Map<Id<Link>,Double> penaltyFactorsOfLinks = new HashMap<>() ;
    private final Map<Id<Link>,Double> penaltyFactorsOfLinksForEmergencyVehicles = new HashMap<>() ;

    private static final String eMaxDistanceForFireVisual = "maxDistanceForFireVisual";
    private static final String eMaxDistanceForSmokeVisual = "maxDistanceForSmokeVisual";
    private static final String eFireAvoidanceBufferForVehicles = "fireAvoidanceBufferForVehicles";
    private static final String eFireAvoidanceBufferForEmergencyVehicles = "fireAvoidanceBufferForEmergencyVehicles";
    private static final String eRoutingAlgorithmType = "routingAlgorithmType";

    public enum EvacuationRoutingAlgorithmType {MATSimDefault, ExampleRoutingAlgorithm}

    MonitorPersonsInDangerZone monitorPersonsEnteringDangerZones;

    // Defaults

    private double optMaxDistanceForFireVisual = 1000;
    private double optMaxDistanceForSmokeVisual = 3000;
    private double optFireAvoidanceBufferForVehicles = 10000;
    private double optFireAvoidanceBufferForEmergencyVehicles = 1000;
    private EvacuationRoutingAlgorithmType optRoutingAlgorithmType =
            EvacuationRoutingAlgorithmType.MATSimDefault;

    public MATSimEvacModel(Map<String, String> opts, DataServer server) {
        matsimModel = new MATSimModel(opts, server);
        registerDataServer(server);
        this.fireWriter = new Shape2XyWriter( matsimModel.getConfig(), "fire" ) ;
        this.emberWriter = new Shape2XyWriter( matsimModel.getConfig(), "ember" ) ;
        this.disruptionWriter = new DisruptionWriter( matsimModel.getConfig() ) ;
        this.monitorPersonsEnteringDangerZones = new MonitorPersonsInDangerZone(getAgentManager());

        if (opts == null) {
            return;
        }
        for (String opt : opts.keySet()) {
            log.info("Found option: {}={}", opt, opts.get(opt));
            switch(opt) {
                case eMaxDistanceForFireVisual:
                    optMaxDistanceForFireVisual = Double.parseDouble(opts.get(opt));
                    break;
                case eMaxDistanceForSmokeVisual:
                    optMaxDistanceForSmokeVisual = Double.parseDouble(opts.get(opt));
                    break;
                case eFireAvoidanceBufferForVehicles:
                    optFireAvoidanceBufferForVehicles = Double.parseDouble(opts.get(opt));
                    break;
                case eFireAvoidanceBufferForEmergencyVehicles:
                    optFireAvoidanceBufferForVehicles = Double.parseDouble(opts.get(opt));
                    break;
                case eRoutingAlgorithmType:
                    optRoutingAlgorithmType =
                            EvacuationRoutingAlgorithmType.valueOf(opts.get(opt));
                    break;
                default:
                    log.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }
    }

    private void initialiseEvacConfig(Config config) {
        evacConfig = ConfigUtils.addOrGetModule(config, EvacConfig.class);
        evacConfig.setSetup(EvacConfig.Setup.standard);
        evacConfig.setCongestionEvaluationInterval(matsimModel.getOptCongestionEvaluationInterval());
        evacConfig.setCongestionToleranceThreshold(matsimModel.getOptCongestionToleranceThreshold());
        evacConfig.setCongestionReactionProbability(matsimModel.getOptCongestionReactionProbability());
    }

    @Override
    public AgentDataContainer takeControl(double time, AgentDataContainer agentDataContainer) {
        return takeControl(time, agentDataContainer);
    }

    @Override
    public Object queryPercept(String agentID, String perceptID, Object args) {
        return matsimModel.queryPercept(agentID, perceptID, args);
    }

    private void registerDataServer( DataServer server ) {
        server.subscribe(this, Constants.FIRE_DATA);
        server.subscribe(this, Constants.EMBERS_DATA);
        server.subscribe(this, Constants.DISRUPTION);
        server.subscribe(this, Constants.EMERGENCY_MESSAGE);
    }

    @Override
    public void receiveData(double time, String dataType, Object data) {
        switch( dataType ) {
            case Constants.FIRE_DATA:
            case Constants.EMBERS_DATA:
            case Constants.DISRUPTION:
            case Constants.EMERGENCY_MESSAGE:
                dataListeners.get(dataType).receiveData(time, dataType, data);
                break;
            default:
                matsimModel.receiveData(time, dataType, data);
        }
    }

    /**
     * Creates a listener for each type of message we expect from the DataServer
     * @return the map of listeners
     */
    private Map<String, DataClient> createDataListeners() {
        Map<String, DataClient> listeners = new  HashMap<>();

        listeners.put(Constants.FIRE_DATA, (DataClient<Geometry>) (time, dataType, data)
                -> processFireData(data, time, penaltyFactorsOfLinks, matsimModel.getScenario(),
                penaltyFactorsOfLinksForEmergencyVehicles, fireWriter));

        listeners.put(Constants.EMBERS_DATA, (DataClient<Geometry>) (time, dataType, data)
                -> processEmbersData(data, time, matsimModel.getScenario(), emberWriter));

        listeners.put(Constants.DISRUPTION, (DataClient<Map<Double,Disruption>>) (time, dataType, data)
                -> processDisruptionData(data, time, matsimModel.getScenario(), disruptionWriter));

        listeners.put(Constants.EMERGENCY_MESSAGE, (DataClient<Map<Double,EmergencyMessage>>) (time, dataType, data)
                -> processEmergencyMessageData(data, time, matsimModel.getScenario()));

        return listeners;
    }

    private void processEmbersData(Geometry data, double now, Scenario scenario, Shape2XyWriter emberWriter) {
        log.debug("received embers data: {}", data);
        Geometry buffer = data.buffer(optMaxDistanceForSmokeVisual);
        monitorPersonsEnteringDangerZones.setEmbersZone(getLinksWithin(scenario, buffer));
        List<Id<Person>> personsMatched = getPersonsWithin(scenario, buffer);
        if (!personsMatched.isEmpty()) {
            log.info("Embers/smoke seen at time {} by {} persons ... use DEBUG to see full list",
                    now, personsMatched.size());
            log.debug("Embers/smoke seen by {} persons: {} ", personsMatched.size(), Arrays.toString(personsMatched.toArray()));
        }
        // package the messages up to send to the BDI side
        for (Id<Person> personId : personsMatched) {
            PAAgent agent = this.getAgentManager().getAgent(personId.toString());
            if (agent != null) { // only do this if this is a BDI-like agent
                PerceptContent pc = new PerceptContent(Constants.FIELD_OF_VIEW, Constants.SIGHTED_EMBERS);
                getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), Constants.FIELD_OF_VIEW, pc);
            }
        }
        emberWriter.write( now, data);
    }

    private void processDisruptionData(Map<Double, Disruption> data, double now, Scenario scenario, DisruptionWriter disruptionWriter ) {
        log.info("receiving disruption data at time={}", now);
        log.info( "{}", new Gson().toJson(data) ) ;

        for (Disruption dd : data.values()) {

            double speedInMpS;
            switch (dd.getEffectiveSpeedUnit()) {
                case "kmph":
                case "KMPH":
                    speedInMpS = dd.getEffectiveSpeed() / 3.6;
                    break;
                default:
                    throw new RuntimeException("unimplemented speed unit");
            }

            String linkIds = Arrays.toString(dd.getImpactLinks())
                    .replaceAll("\\[", "")
                    .replaceAll("\\]", "")
                    .replaceAll(",", " ");
            List<Link> links = NetworkUtils.getLinks(scenario.getNetwork(),NetworkUtils.getLinkIds(linkIds));
            for (Link link : links) {
                double prevSpeed = link.getFreespeed(now);
                log.info("Updating freespeed on link {} from {} to {} due to disruption",
                        link.getId(), prevSpeed, speedInMpS);
                {
                    double startTime = convertTimeToSeconds(dd.getStartHHMM());
                    if (startTime < now) {
                        startTime = now;
                    }
                    matsimModel.addNetworkChangeEvent(speedInMpS, link, startTime);
                    disruptionWriter.write(startTime, link.getId(), link.getCoord(), speedInMpS);
                }
                {
                    double startTime = convertTimeToSeconds(dd.getEndHHMM());
                    if (startTime < now) {
                        startTime = now;
                    }
                    matsimModel.addNetworkChangeEvent(prevSpeed, link, startTime);
                    disruptionWriter.write(startTime, link.getId(), link.getCoord(), prevSpeed);
                }
            }
        }
    }

    private void processEmergencyMessageData(Map<Double, EmergencyMessage> data, double now, Scenario scenario) {
        log.info("receiving emergency message data at time={}", now);
        log.info( "{}{}", new Gson().toJson(data).substring(0,Math.min(new Gson().toJson(data).length(),200)),
                "... use DEBUG to see full coordinates list") ;
        log.debug( "{}", new Gson().toJson(data)) ;


        // FIXME: Assumes incoming is WSG84 format. See https://github.com/agentsoz/bdi-abm-integration/issues/34
        CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, scenario.getConfig().global().getCoordinateSystem());

        // The key is time, value is a message
        for (EmergencyMessage msg : data.values()) {
            // For each zone in this message
            List<Id<Person>> personsInZones = new ArrayList<>();

            for (String zoneId : msg.getBroadcastZones().keySet()) {
                Double[][] pairs = msg.getBroadcastZones().get(zoneId);
                List<Coord> coords = new ArrayList<>() ;
                for (Double[] pair : pairs) {
                    coords.add(transform.transform(new Coord(pair[0], pair[1])));
                }
                // Create a polygon for this zone
                Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
                // And find everyone inside it
                List<Id<Person>> personsMatched = getPersonsWithin(scenario, polygon);
                personsInZones.addAll(personsMatched);
                log.info("Zone {} has persons: {} ", zoneId, Arrays.toString(personsMatched.toArray()));
            }
            log.info("Message " + msg.getType() + " will be sent to total " + personsInZones.size() + " persons in zones " + msg.getBroadcastZones().keySet());
            // package the messages up to send to the BDI side
            for (Id<Person> personId : personsInZones) {
                PAAgent agent = this.getAgentManager().getAgent(personId.toString());
                if (agent != null) { // only do this if this is a BDI-like agent
                    PerceptContent pc = new PerceptContent(Constants.EMERGENCY_MESSAGE, msg.getType() + "," + msg.getContent());
                    getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), Constants.EMERGENCY_MESSAGE, pc);
                }
            }

        }
    }

    private void processFireData(Geometry data, double now, Map<Id<Link>, Double> penaltyFactorsOfLinks,
                                 Scenario scenario, Map<Id<Link>, Double> penaltyFactorsOfLinksForEmergencyVehicles,
                                 Shape2XyWriter fireWriter) {

        log.debug("received fire data: {}", data);

        {
            Geometry buffer = data.buffer(optMaxDistanceForFireVisual);
            monitorPersonsEnteringDangerZones.setFireZone(getLinksWithin(scenario, buffer));
            List<Id<Person>> personsMatched = getPersonsWithin(scenario, buffer);
            if (!personsMatched.isEmpty()) {
                log.info("Fire seen at time {} by {} persons ... use DEBUG to see full list",
                        now, personsMatched.size());
                log.debug("Fire seen by {} persons: {} ", personsMatched.size(), Arrays.toString(personsMatched.toArray()));
            }
            // package the messages up to send to the BDI side
            for (Id<Person> personId : personsMatched) {
                PAAgent agent = this.getAgentManager().getAgent(personId.toString());
                if (agent != null) { // only do this if this is a BDI-like agent
                    PerceptContent pc = new PerceptContent(Constants.FIELD_OF_VIEW, Constants.SIGHTED_FIRE);
                    getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), Constants.FIELD_OF_VIEW, pc);
                }
            }
        }
		//https://stackoverflow.com/questions/38404095/how-to-calculate-the-distance-in-meters-between-a-geographic-point-and-a-given-p
        {
            final double bufferWidth = optFireAvoidanceBufferForVehicles;
            Geometry buffer = data.buffer(bufferWidth);
            penaltyFactorsOfLinks.clear();
            Utils.penaltyMethod2(data, buffer, bufferWidth, penaltyFactorsOfLinks, scenario);
            // I think that penaltyMethod2 looks nicer than method1.  kai, dec'17
            // yy could make this settable, but for the time being this pedestrian approach
            // seems sufficient.  kai, jan'18
        }
        {
            final double bufferWidth = optFireAvoidanceBufferForEmergencyVehicles;
            Geometry buffer = data.buffer(bufferWidth);
            penaltyFactorsOfLinksForEmergencyVehicles.clear();
            Utils.penaltyMethod2(data, buffer, bufferWidth, penaltyFactorsOfLinksForEmergencyVehicles, scenario);
        }
        fireWriter.write( now, data);
    }

    private List<Id<Person>> getPersonsWithin(Scenario scenario, Geometry shape) {
        List<Id<Person>> personsWithin = new ArrayList<>();
        for(Id<Person> personId : matsimModel.getMobsimDataProvider().getAgents().keySet()) {
            Gbl.assertNotNull( matsimModel.getMobsimDataProvider() );
            MobsimAgent agent = matsimModel.getMobsimDataProvider().getAgent(personId);
            final Id<Link> linkId = agent.getCurrentLinkId();
            final Link link = scenario.getNetwork().getLinks().get(linkId);
            Point fromPoint = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
            if (shape.contains(fromPoint)) { // coming from polygon area
                // this agent is in (or has potentially just exited) the messaging area
                personsWithin.add(personId);
            }
        }
        return personsWithin;
    }

    /**
     * Gets all links that have a fromNode within the given shape
     * @param scenario
     * @param shape
     * @return
     */
    private Set<Id<Link>> getLinksWithin(Scenario scenario, Geometry shape) {
        Set<Id<Link>> links = new HashSet<>();
        for(Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
            final Link link = scenario.getNetwork().getLinks().get(linkId);
            Point fromPoint = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
            if (shape.contains(fromPoint)) {
                links.add(linkId);
            }
        }
        return links;
    }


    public void init(Object[] args) {
        String[] acts = Stream.of(Constants.EvacActivity.values()).map(Constants.EvacActivity::name).toArray(String[]::new);
        matsimModel.init(ObjectArrays.concat(args, new Object[]{Arrays.asList(acts)}, Object.class));
        List<String> bdiAgentIDs = (List<String>)args[0];
        initialiseControllerForEvac(matsimModel.getControler());
        for(String agentId: bdiAgentIDs) {
            PAAgent paAgent = getAgentManager().getAgent( agentId );

            // replace the default action handlers with evacuation specific ones
            paAgent.getActionHandler().registerBDIAction(
                    Constants.DRIVETO, new EvacDrivetoActionHandlerV2(matsimModel));
            paAgent.getActionHandler().registerBDIAction(
                    Constants.REPLAN_CURRENT_DRIVETO, new ReplanDriveToDefaultActionHandlerV2(matsimModel));
        }

    }

    private void initialiseControllerForEvac(Controler controller) {
        controller.getEvents().addHandler(monitorPersonsEnteringDangerZones);
        // infrastructure at QSim level (separating line not fully logical)
        controller.addOverridingQSimModule( new AbstractQSimModule() {
            @Override protected void configureQSim() {
                this.bind( AgentFactory.class ).to( EvacAgent.Factory.class ) ;
                this.bind(Replanner.class).in( Singleton.class ) ;
                this.bind( MATSimModel.class ).toInstance( matsimModel );
            }
        } );
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // Attach our evacuation-based routing algorithm selector
                bindEvacuationRoutingAlgorithm();
                // Set up the evacuation routers
                setupEmergencyVehicleRouting();
                setupCarGlobalInformationRouting();
                setupCarFreespeedRouting();
            }

            private void bindEvacuationRoutingAlgorithm() {
                if (optRoutingAlgorithmType == EvacuationRoutingAlgorithmType.ExampleRoutingAlgorithm) {
                    this.bind(LeastCostPathCalculatorFactory.class).to(ExampleRoutingAlgorithmFactory.class);
                }
            }

            private void setupCarFreespeedRouting() {
                // memorize the routing mode:
                String routingMode = Constants.EvacRoutingMode.carFreespeed.name() ;

                addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car,routingMode)) ;
                // (above line means that when "routingMode" is requested, a "network" routing will be provided, and the route
                // will be executed as "car" in the mobsim).

                addTravelTimeBinding(routingMode).to(FreeSpeedTravelTime.class);
                // (this defines which travel time this routing mode should use.  Here: free speed))

                TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyFactorsOfLinks);
                addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
                // (this defines which travel disutility this routing mode should use.  Here: a specific evac travel disutility, which takes
                // penalty factors as input.  The penalty factors are filled from fire data; if there is no fire data, they remain empty)
            }

            private void setupCarGlobalInformationRouting() {
                final String routingMode = Constants.EvacRoutingMode.carGlobalInformation.name();

                addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode)) ;

                // congested travel time:
                bind(WithinDayTravelTime.class).in(Singleton.class);
                addEventHandlerBinding().to(WithinDayTravelTime.class);
                addMobsimListenerBinding().to(WithinDayTravelTime.class);
                addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;

                // travel disutility includes the fire penalty. If no data arrives, it makes no difference.
                TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyFactorsOfLinks);
                // (yyyyyy the now following cases are just there because of the different tests.  Solve by config,
                addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
            }

            private void setupEmergencyVehicleRouting() {
                final String routingMode = Constants.EvacRoutingMode.emergencyVehicle.name();

                addRoutingModuleBinding(routingMode).toProvider(new NetworkRoutingProvider(TransportMode.car, routingMode)) ;

                // congested travel time:
                bind(WithinDayTravelTime.class).in(Singleton.class);
                addEventHandlerBinding().to(WithinDayTravelTime.class);
                addMobsimListenerBinding().to(WithinDayTravelTime.class);
                addTravelTimeBinding(routingMode).to(WithinDayTravelTime.class) ;

                // travel disutility includes the fire penalty:
                TravelDisutilityFactory disutilityFactory = new EvacTravelDisutility.Factory(penaltyFactorsOfLinksForEmergencyVehicles);
                // yyyyyy This uses the same disutility as the evacuees.  May not be what we want.
                // But what do we want?  kai, dec'17/jan'18

//					TravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
                addTravelDisutilityFactoryBinding(routingMode).toInstance(disutilityFactory);
            }
        } );
    }

    public Scenario getScenario() {
        return matsimModel.getScenario();
    }

    public EventsManager getEvents() {
        return matsimModel.getEvents();
    }

    public void setAgentDataContainer(io.github.agentsoz.bdiabm.v2.AgentDataContainer adc_from_abm) {
        matsimModel.setAgentDataContainer(adc_from_abm);
    }

    @Override
    public AgentDataContainer getAgentDataContainer() {
        return matsimModel.getAgentDataContainer();
    }

    public void useSequenceLock(Object sequenceLock) {
        matsimModel.useSequenceLock(sequenceLock);
    }

    public boolean isFinished() {
        return matsimModel.isFinished();
    }

    public void finish() {
        if ( fireWriter!=null ) {
            fireWriter.close();
        }
        if ( emberWriter !=null ) {
            emberWriter.close();
        }
        if ( disruptionWriter!=null ) {
            disruptionWriter.finish(matsimModel.getTime());
        }
        matsimModel.finish();
    }

    public Config loadAndPrepareConfig() {
        Config config = matsimModel.loadAndPrepareConfig();
        initialiseEvacConfig(config);
        return config;
    }

    public Scenario loadAndPrepareScenario() {
        matsimModel.loadAndPrepareScenario();
        // make sure links don't have speed infinity (results in problems with the router; yy instead repair input files?):
        for ( Link link : matsimModel.getScenario().getNetwork().getLinks().values() ) {
            final double veryLargeSpeed = 9999999999.;
            if ( link.getFreespeed() > veryLargeSpeed ) {
                link.setFreespeed(veryLargeSpeed);
            }
            if ( evacConfig.getSetup()== EvacConfig.Setup.tertiaryRoadsCorrection ) {
                // yyyy correction for much too high speed value on tertiary roads. kai, dec'17
                if (link.getFreespeed() == 27.77777777777778 && link.getCapacity() == 600.) {
                    link.setFreespeed(50. / 3.6);
                }
            }
        }
        return matsimModel.getScenario();
    }

    public PAAgentManager getAgentManager() {
        return matsimModel.getAgentManager();
    }

    public EvacConfig getEvacConfig() {
        return evacConfig;
    }

    public void start() {
        // FIXME: Eventually remove the following hack to reset the log level for EditTrips
        // EditTrips incorrectly forces a particular log level in its constructor and orerwrites anything coming from log4j.xml,
        // so here we reset it back to what was in log4j.xml
        org.apache.log4j.Level level = org.apache.log4j.Logger.getLogger(org.matsim.withinday.utils.EditTrips.class).getLevel();
        matsimModel.start();
        org.apache.log4j.Logger.getLogger(org.matsim.withinday.utils.EditTrips.class).setLevel(level);
    }
}
