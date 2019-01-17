package io.github.agentsoz.ees.matsim;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Replanner;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.nonmatsim.PAAgentManager;
import io.github.agentsoz.util.Disruption;
import io.github.agentsoz.util.EmergencyMessage;
import io.github.agentsoz.util.evac.PerceptList;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;

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

/**
 * @author Dhi Singh
 */
public final class MATSimEvacModel implements ABMServerInterface, QueryPerceptInterface, DataClient {

    private static final Logger log = LoggerFactory.getLogger(MATSimEvacModel.class);
    private final MATSimModel matsimModel;
    private final Map<String, DataClient> dataListeners = createDataListeners();
    private EvacConfig evacConfig = null;

    private Shape2XyWriter fireWriter = null;
    private Shape2XyWriter emberWriter = null;
    private DisruptionWriter disruptionWriter = null;


    private static final String eMaxDistanceForFireVisual = "maxDistanceForFireVisual";
    private static final String eMaxDistanceForSmokeVisual = "maxDistanceForSmokeVisual";
    private static final String eFireAvoidanceBufferForVehicles = "fireAvoidanceBufferForVehicles";
    private static final String eFireAvoidanceBufferForEmergencyVehicles = "fireAvoidanceBufferForEmergencyVehicles";

    // Defaults

    private double optMaxDistanceForFireVisual = 1000;
    private double optMaxDistanceForSmokeVisual = 3000;
    private double optFireAvoidanceBufferForVehicles = 10000;
    private double optFireAvoidanceBufferForEmergencyVehicles = 1000;

    public MATSimEvacModel(Map<String, String> opts, DataServer server) {
        matsimModel = new MATSimModel(opts, server);
        registerDataServer(server);
        this.fireWriter = new Shape2XyWriter( matsimModel.getConfig(), "fire" ) ;
        this.emberWriter = new Shape2XyWriter( matsimModel.getConfig(), "ember" ) ;
        this.disruptionWriter = new DisruptionWriter( matsimModel.getConfig() ) ;
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
                default:
                    log.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }
    }

    private EvacConfig initialiseEvacConfig(Config config) {
        evacConfig = ConfigUtils.addOrGetModule(config, EvacConfig.class);
        evacConfig.setSetup(EvacConfig.Setup.standard);
        evacConfig.setCongestionEvaluationInterval(matsimModel.getOptCongestionEvaluationInterval());
        evacConfig.setCongestionToleranceThreshold(matsimModel.getOptCongestionToleranceThreshold());
        evacConfig.setCongestionReactionProbability(matsimModel.getOptCongestionReactionProbability());
        return evacConfig;
    }

    @Override
    public void takeControl(AgentDataContainer agentDataContainer) {
        matsimModel.takeControl(agentDataContainer);
    }

    @Override
    public Object queryPercept(String agentID, String perceptID, Object args) {
        return matsimModel.queryPercept(agentID, perceptID, args);
    }

    public final void registerDataServer( DataServer server ) {
        server.subscribe(this, PerceptList.FIRE_DATA);
        server.subscribe(this, PerceptList.EMBERS_DATA);
        server.subscribe(this, PerceptList.DISRUPTION);
        server.subscribe(this, PerceptList.EMERGENCY_MESSAGE);
    }

    @Override
    public void receiveData(double time, String dataType, Object data) {
        double now = time;
        switch( dataType ) {
            case PerceptList.FIRE_DATA:
            case PerceptList.EMBERS_DATA:
            case PerceptList.DISRUPTION:
            case PerceptList.EMERGENCY_MESSAGE:
                dataListeners.get(dataType).receiveData(now, dataType, data);
                break;
            default:
                matsimModel.receiveData(time, dataType, data);
        }
    }

    /**
     * Creates a listener for each type of message we expect from the DataServer
     * @return
     */
    private Map<String, DataClient> createDataListeners() {
        Map<String, DataClient> listeners = new  HashMap<>();

        listeners.put(PerceptList.FIRE_DATA, (DataClient<Geometry>) (time, dataType, data) -> {
            processFireData(data, time, matsimModel.getPenaltyFactorsOfLinks(), matsimModel.getScenario(),
                    matsimModel.getPenaltyFactorsOfLinksForEmergencyVehicles(), fireWriter);
        });

        listeners.put(PerceptList.EMBERS_DATA, (DataClient<Geometry>) (time, dataType, data) -> {
            processEmbersData(data, time, matsimModel.getScenario(), emberWriter);
        });

        listeners.put(PerceptList.DISRUPTION, (DataClient<Map<Double,Disruption>>) (time, dataType, data) -> {
            processDisruptionData(data, time, matsimModel.getScenario(), disruptionWriter);
        });

        listeners.put(PerceptList.EMERGENCY_MESSAGE, (DataClient<Map<Double,EmergencyMessage>>) (time, dataType, data) -> {
            processEmergencyMessageData(data, time, matsimModel.getScenario());
        });

        return listeners;
    }

    private void processEmbersData(Geometry data, double now, Scenario scenario, Shape2XyWriter emberWriter) {
        log.debug("received embers data: {}", data);
        Geometry embers = data;
        if (embers == null) {
            return;
        }
        Geometry embersBuffer = embers.buffer(optMaxDistanceForSmokeVisual);
        List<Id<Person>> personsMatched = getPersonsWithin(scenario, embersBuffer);
        if (!personsMatched.isEmpty()) {
            log.info("Embers/smoke seen at time {} by {} persons ... use DEBUG to see full list",
                    now, personsMatched.size());
            log.debug("Embers/smoke seen by {} persons: {} ", personsMatched.size(), Arrays.toString(personsMatched.toArray()));
        }
        // package the messages up to send to the BDI side
        for (Id<Person> personId : personsMatched) {
            PAAgent agent = this.getAgentManager().getAgent(personId.toString());
            if (agent != null) { // only do this if this is a BDI-like agent
                PerceptContent pc = new PerceptContent(PerceptList.FIELD_OF_VIEW, PerceptList.SIGHTED_EMBERS);
                getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.FIELD_OF_VIEW, pc);
            }
        }
        emberWriter.write( now, embers);
    }

    private void processDisruptionData( Map<Double,Disruption> data, double now, Scenario scenario, DisruptionWriter disruptionWriter ) {
        log.info("receiving disruption data at time={}", now); ;
        log.info( "{}", new Gson().toJson(data) ) ;

        Map<Double,Disruption> timeMapOfDisruptions = data;

        for (Disruption dd : timeMapOfDisruptions.values()) {

            double speedInMpS = 0;
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
                    double startTime = matsimModel.convertTimeToSeconds(dd.getStartHHMM());
                    if (startTime < now) {
                        startTime = now;
                    }
                    matsimModel.addNetworkChangeEvent(speedInMpS, link, startTime);
                    disruptionWriter.write(startTime, link.getId(), link.getCoord(), speedInMpS);
                }
                {
                    double startTime = matsimModel.convertTimeToSeconds(dd.getEndHHMM());
                    if (startTime < now) {
                        startTime = now;
                    }
                    matsimModel.addNetworkChangeEvent(prevSpeed, link, startTime);
                    disruptionWriter.write(startTime, link.getId(), link.getCoord(), prevSpeed);
                }
            }
        }
    }

    private void processEmergencyMessageData(Map<Double,EmergencyMessage> data, double now, Scenario scenario) {
        log.info("receiving emergency message data at time={}", now);
        log.info( "{}{}", new Gson().toJson(data).substring(0,Math.min(new Gson().toJson(data).length(),200)),
                "... use DEBUG to see full coordinates list") ;
        log.debug( "{}", new Gson().toJson(data)) ;


        Map<Double,EmergencyMessage> timeMapOfEmergencyMessages = data;

        // FIXME: Assumes incoming is WSG84 format. See https://github.com/agentsoz/bdi-abm-integration/issues/34
        CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, scenario.getConfig().global().getCoordinateSystem());

        // The key is time, value is a message
        for (EmergencyMessage msg : timeMapOfEmergencyMessages.values()) {
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
                    PerceptContent pc = new PerceptContent(PerceptList.EMERGENCY_MESSAGE, msg.getType() + "," + msg.getContent());
                    getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.EMERGENCY_MESSAGE, pc);
                }
            }

        }
    }

    private void processFireData(Geometry data, double now, Map<Id<Link>, Double> penaltyFactorsOfLinks,
                                 Scenario scenario, Map<Id<Link>, Double> penaltyFactorsOfLinksForEmergencyVehicles,
                                 Shape2XyWriter fireWriter) {

        log.debug("received fire data: {}", data);
        Geometry fire = data;

        {
            Geometry buffer = fire.buffer(optMaxDistanceForFireVisual);
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
                    PerceptContent pc = new PerceptContent(PerceptList.FIELD_OF_VIEW, PerceptList.SIGHTED_FIRE);
                    getAgentManager().getAgentDataContainerV2().putPercept(agent.getAgentID(), PerceptList.FIELD_OF_VIEW, pc);
                }
            }
        }
//		https://stackoverflow.com/questions/38404095/how-to-calculate-the-distance-in-meters-between-a-geographic-point-and-a-given-p
        {
            final double bufferWidth = optFireAvoidanceBufferForVehicles;
            Geometry buffer = fire.buffer(bufferWidth);
            penaltyFactorsOfLinks.clear();
//		Utils.penaltyMethod1(fire, buffer, penaltyFactorsOfLinks, scenario );
            Utils.penaltyMethod2(fire, buffer, bufferWidth, penaltyFactorsOfLinks, scenario);
            // I think that penaltyMethod2 looks nicer than method1.  kai, dec'17
            // yy could make this settable, but for the time being this pedestrian approach
            // seems sufficient.  kai, jan'18
        }
        {
            final double bufferWidth = optFireAvoidanceBufferForEmergencyVehicles;
            Geometry buffer = fire.buffer(bufferWidth);
            penaltyFactorsOfLinksForEmergencyVehicles.clear();
            Utils.penaltyMethod2(fire, buffer, bufferWidth, penaltyFactorsOfLinksForEmergencyVehicles, scenario);
        }
        fireWriter.write( now, fire);
    }

    private List<Id<Person>> getPersonsWithin(Scenario scenario, Geometry shape) {
        List<Id<Person>> personsWithin = new ArrayList<>();
        for(Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            Gbl.assertNotNull( matsimModel.getMobsimDataProvider() );
            MobsimAgent agent = matsimModel.getMobsimDataProvider().getAgent(personId);
            if (agent == null) {
                log.error("MobsimAgent {} not found, should never happen!!", personId);
                continue;
            }
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


    public void init(List<String> bdiAgentIDs) {
        matsimModel.init(bdiAgentIDs);
        Controler controller = matsimModel.getControler();
        // infrastructure at QSim level (separating line not fully logical)
        controller.addOverridingQSimModule( new AbstractQSimModule() {
            @Override protected void configureQSim() {
                this.bind( AgentFactory.class ).to( EvacAgent.Factory.class ) ;
                this.bind(Replanner.class).in( Singleton.class ) ;
                this.bind( MATSimModel.class ).toInstance( matsimModel );
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
        matsimModel.start();
    }
}