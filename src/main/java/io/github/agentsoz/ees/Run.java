package io.github.agentsoz.ees;

/*-
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdimatsim.EvacAgentTracker;
import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Utils;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Global;
import io.github.agentsoz.util.Time;
import io.github.agentsoz.util.evac.PerceptList;
import org.matsim.api.core.v01.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Emergency Evacuation Simulator (EES) main program.
 * Uses input config v2. For legacy config use {@link Main}.
 * @author Dhirendra Singh
 */
public class Run {

    private static final Logger log = LoggerFactory.getLogger(Run.class);
    public static final String DATASERVER = "ees";
    private final Map<String, DataSource> controllers = createDataProducers();
    private final Map<String, DataClient> dataListeners = createDataListeners();
    private Object adc_from_bdi = null;
    private Object adc_from_abm = null;


    public static void main(String[] args) {
        Config cfg = new Config();
        Map<String,String> opts = cfg.parse(args);
        cfg.loadFromFile(opts.get(Config.OPT_CONFIG));
        Run sim = new Run();
        sim.start(cfg);
    }

    private void start(Config cfg) {
        parse(cfg.getModelConfig(""));

        log.info("Starting the data server");
        // initialise the data server bus for passing data around using a publish/subscribe or pull mechanism
        DataServer dataServer = DataServer.getInstance(DATASERVER);
        dataServer.setTime(hhMmToS(cfg.getGlobalConfig(Config.eGlobalStartHhMm)));

        // initialise the fire model and register it as an active data source
        {
            log.info("Starting fire model");
            PhoenixFireModule model = new PhoenixFireModule(cfg.getModelConfig(Config.eModelFire), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        {
            log.info("Starting smoke model");
            PhoenixGridModel model = new PhoenixGridModel(cfg.getModelConfig(Config.eModelFire), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        // initialise the disruptions model and register it as an active data source
        {
            log.info("Starting disruptions model");
            DisruptionModel model = new DisruptionModel(cfg.getModelConfig(Config.eModelDisruption), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        // initialise the messaging model and register it as an active data source
        {
            log.info("Starting messaging model");
            MessagingModel model = new MessagingModel(cfg.getModelConfig(Config.eModelMessaging), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        // initialise the MATSim model and register it as an active data source
        log.info("Creating MATSim model");
        MATSimModel matsimModel = new MATSimModel(cfg.getModelConfig(Config.eModelMatsim), dataServer);
        EvacConfig evacConfig = matsimModel.initialiseEvacConfig(matsimModel.loadAndPrepareConfig());
        Scenario scenario = matsimModel.loadAndPrepareScenario() ;

        // get BDI agents map from the MATSim population file
        log.info("Reading BDI agents from MATSim population file");
        Map<String, List<String[]>> bdiMap = Utils.getAgentsFromMATSimPlansFile(scenario);
        JillBDIModel.removeNonBdiAgentsFrom(bdiMap);
        List<String> bdiAgentIDs = new ArrayList<>(bdiMap.keySet());

        // initialise the diffusion model and register it as an active data source
        {
            log.info("Starting information diffusion model");
            DiffusionModel model = new DiffusionModel(cfg.getModelConfig(Config.eModelDiffusion), dataServer, bdiAgentIDs);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }

        // initialise the Jill model, register it as an active data source, and start it
        log.info("Starting Jill BDI model");
        JillBDIModel jillmodel = new JillBDIModel(cfg.getModelConfig(Config.eModelBdi), dataServer, (QueryPerceptInterface)matsimModel, bdiMap);
        jillmodel.init(matsimModel.getAgentManager().getAgentDataContainer(), null, null, bdiAgentIDs.toArray( new String[bdiAgentIDs.size()] ));
        jillmodel.start();

        // --- initialize and start MATSim
        log.info("Starting MATSim model");
        matsimModel.init(bdiAgentIDs);
        {
            // yyyy try to replace this by injection. because otherwise it again needs to be added "late enough", which we
            // wanted to get rid of.  kai, dec'17
            EvacAgentTracker tracker = new EvacAgentTracker(evacConfig, matsimModel.getScenario().getNetwork(), matsimModel.getEvents());
            matsimModel.getEvents().addHandler(tracker);
        }

        // start the main simulation loop
        log.info("Starting the simulation loop");
        while (true) {
            //TODO: dataServer.publish(PerceptList.TAKE_CONTROL_BDI, adc_from_abm);
            dataServer.publish(PerceptList.TAKE_CONTROL_BDI, matsimModel.getAgentManager().getAgentDataContainer());
            if( matsimModel.isFinished() ) {
                break;
            }
            //TODO: dataServer.publish(PerceptList.TAKE_CONTROL_ABM, adc_from_bdi);
            dataServer.publish(PerceptList.TAKE_CONTROL_ABM, matsimModel.getAgentManager().getAgentDataContainer());
            dataServer.stepTime();
        }

        // finish up
        log.info("Finishing up");
        jillmodel.finish();
        matsimModel.finish() ;
        DataServer.cleanup() ;
        log.info("All done");
    }

    private void parse(Map<String, String> opts) {
        if (opts == null) {
            return;
        }
        for (String opt : opts.keySet()) {
            log.info("Found option: {}={}", opt, opts.get(opt));
            switch(opt) {
                case Config.eGlobalRandomSeed:
                    Global.setRandomSeed(Long.parseLong(opts.get(opt)));
                    break;
                default:
                    log.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }
    }


    private static double hhMmToS(String HHMM) {
        String[] tokens = HHMM.split(":");
        int[] hhmm = new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])};
        double secs = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
                + Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
        return secs;
    }

    private Map<String, DataSource> createDataProducers() {
        Map<String, DataSource> producers = new  HashMap<>();

        // Asks BDI to take control and sends it the last agent data container from the ABM
        producers.put(PerceptList.TAKE_CONTROL_BDI, (DataSource<Object>) (time, dataType) -> {
            Object adc = adc_from_abm;
            adc_from_abm = null;
            return adc;
        });

        // Asks the ABM to take control and sends it the last agent data container from BDI
        producers.put(PerceptList.TAKE_CONTROL_ABM, (DataSource<Object>) (time, dataType) -> {
            Object adc = adc_from_bdi;
            adc_from_bdi = null;
            return adc;
        });

        return producers;
    }

    private Map<String, DataClient> createDataListeners() {
        Map<String, DataClient> listeners = new  HashMap<>();

        // Saves the incoming agent data container from BDI
        listeners.put(PerceptList.AGENT_DATA_CONTAINER_FROM_BDI, (DataClient<Object>) (time, dataType, data) -> {
            adc_from_bdi = data;
        });

        // Saves the incoming agent data container from the ABM
        listeners.put(PerceptList.AGENT_DATA_CONTAINER_FROM_ABM, (DataClient<Object>) (time, dataType, data) -> {
            adc_from_abm = data;
        });
        return listeners;
    }

}
