package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
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
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.matsim.EvacAgentTracker;
import io.github.agentsoz.ees.matsim.EvacConfig;
import io.github.agentsoz.ees.matsim.MATSimEvacModel;
import io.github.agentsoz.ees.util.Utils;
import io.github.agentsoz.util.Global;
import io.github.agentsoz.util.Time;
import org.matsim.api.core.v01.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Emergency Evacuation Simulator (EES) main program.
 * @author Dhirendra Singh
 */
public class Run implements DataClient {

    private static final Logger log = LoggerFactory.getLogger(Run.class);
    public static final String DATASERVER = "ees";
    private final Map<String, DataClient> dataListeners = createDataListeners();
    private AgentDataContainer adc_from_bdi = new AgentDataContainer();
    private AgentDataContainer adc_from_abm = new AgentDataContainer();
    private final Object sequenceLock = new Object();

    // Models
    DataServer dataServer = null;
    private DiffusionModel diffusionModel = null;

    //  Defaults
    private int optTimestep = 1; // in seconds


    public static void main(String[] args) {
        Thread.currentThread().setName("ees");

        // Read the config
        Config cfg = new Config();
        Map<String,String> opts = cfg.parse(args);
        cfg.loadFromFile(opts.get(Config.OPT_CONFIG));

        // Get BDI agents map from the MATSim population file
        log.info("Reading BDI agents from MATSim population file");
        Map<String, List<String[]>> bdiMap = Utils.getAgentsFromMATSimPlansFile(cfg.getModelConfig(Config.eModelMatsim).get("configXml"));
        JillBDIModel.removeNonBdiAgentsFrom(bdiMap);

        // Run it
        new Run()
        .withModel(DataServer.getInstance(DATASERVER))
        .withModel(new DiffusionModel(cfg.getModelConfig(Config.eModelDiffusion), DataServer.getInstance(DATASERVER), new ArrayList<>(bdiMap.keySet())))
        .start(cfg, bdiMap);
    }

    public void start(Config cfg, Map<String, List<String[]>> bdiMap) {
        parse(cfg.getModelConfig(""));

        // Get the BDI agent IDs from the  map
        List<String> bdiAgentIDs = new ArrayList<>(bdiMap.keySet());

        log.info("Starting the data server");
        // initialise the data server bus for passing data around using a publish/subscribe or pull mechanism
        if (dataServer == null) {
            dataServer = DataServer.getInstance(DATASERVER);
        }
        dataServer.setTime(hhMmToS(cfg.getGlobalConfig(Config.eGlobalStartHhMm)));
        dataServer.setTimeStep(optTimestep);
        dataServer.subscribe(this, Constants.AGENT_DATA_CONTAINER_FROM_BDI);

        // initialise the fire model and register it as an active data source
        {
            log.info("Starting phoenix fire model (ISO)");
            PhoenixFireModule model = new PhoenixFireModule(cfg.getModelConfig(Config.eModelFire), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        {
            log.info("Starting phoenix fire model (GRID)");
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
        MATSimEvacModel matsimEvacModel = new MATSimEvacModel(cfg.getModelConfig(Config.eModelMatsim), dataServer);
        matsimEvacModel.loadAndPrepareConfig();
        EvacConfig evacConfig = matsimEvacModel.getEvacConfig();
        Scenario scenario = matsimEvacModel.loadAndPrepareScenario() ;

        // initialise the diffusion model and register it as an active data source
        log.info("Starting information diffusion model");
        if (diffusionModel == null) {
            diffusionModel = new DiffusionModel(cfg.getModelConfig(Config.eModelDiffusion), dataServer, bdiAgentIDs);
        }
        diffusionModel.setTimestepUnit(Time.TimestepUnit.SECONDS);
        diffusionModel.start();

        // initialise the Jill model, register it as an active data source, and start it
        log.info("Starting Jill BDI model");
        JillBDIModel jillmodel = new JillBDIModel(cfg.getModelConfig(Config.eModelBdi), dataServer, (QueryPerceptInterface)matsimEvacModel, bdiMap);
        jillmodel.setAgentDataContainer(adc_from_bdi);
        jillmodel.init(bdiAgentIDs.toArray( new String[bdiAgentIDs.size()] ));
        jillmodel.start();

        // --- initialize and start MATSim
        log.info("Starting MATSim model");
        matsimEvacModel.setAgentDataContainer(adc_from_abm);
        matsimEvacModel.init(new Object[]{bdiAgentIDs});
        matsimEvacModel.start();
        {
            // yyyy try to replace this by injection. because otherwise it again needs to be added "late enough", which we
            // wanted to get rid of.  kai, dec'17
            EvacAgentTracker tracker = new EvacAgentTracker(evacConfig, matsimEvacModel.getScenario().getNetwork(), matsimEvacModel.getEvents());
            matsimEvacModel.getEvents().addHandler(tracker);
        }

        // start the main simulation loop
        log.info("Starting the simulation loop");
        jillmodel.useSequenceLock(sequenceLock);
        matsimEvacModel.useSequenceLock(sequenceLock);

        while (true) {
            // Wait till both models are done before incrementing time
            synchronized (sequenceLock) {
                dataServer.stepTime();
            }
            // Wait till both models are done before checking for termination condition
            synchronized (sequenceLock) {
                if (matsimEvacModel.isFinished()) {
                    break;
                }
            }
            // ABM to take control; the ABM thread should synchronize on adc_from_abm
            dataServer.publish(Constants.TAKE_CONTROL_ABM, adc_from_bdi);
            // BDI to take control; the BDI thread should synchronize on adc_from_bdi
            dataServer.publish(Constants.TAKE_CONTROL_BDI, adc_from_abm);
        }

        // finish up
        log.info("Finishing up");
        jillmodel.finish();
        matsimEvacModel.finish() ;
        diffusionModel.finish();
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
                case Config.eGlobalTimeStep:
                    optTimestep = Integer.parseInt(opts.get(opt));
                    break;
                default:
                    log.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }
    }

    /**
     * Convert HHMM string to seconds
     * @param HHMM time in HHMM format
     * @return time in seconds
     */
    private static double hhMmToS(String HHMM) {
        String[] tokens = HHMM.split(":");
        int[] hhmm = new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])};
        double secs = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
                + Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
        return secs;
    }

    /**
     * Allows models to be overriden; interim solution for #37
     * @param model the model to override with
     * @return the run object
     */
    public Run withModel(Object model) {
        if (model != null) {
            if (model instanceof DataServer) {
                this.dataServer = (DataServer) model;

            } else if (model instanceof DiffusionModel) {
                this.diffusionModel = (DiffusionModel) model;

            } else {
                throw new RuntimeException(
                        "Not all models can be overriden in this way. " +
                                " A cleaner mechanism is under development, " +
                                "see https://github.com/agentsoz/ees/issues/37"
                );
            }
        }
        return this;
    }

    private Map<String, DataClient> createDataListeners() {
        Map<String, DataClient> listeners = new  HashMap<>();

        // Saves the incoming agent data container from BDI
        listeners.put(Constants.AGENT_DATA_CONTAINER_FROM_BDI, (DataClient<AgentDataContainer>) (time, dataType, data) -> {
            adc_from_bdi = data;
        });

        // Saves the incoming agent data container from the ABM
        listeners.put(Constants.AGENT_DATA_CONTAINER_FROM_ABM, (DataClient<AgentDataContainer>) (time, dataType, data) -> {
            adc_from_abm = data;
        });
        return listeners;
    }

    @Override
    public void receiveData(double time, String dataType, Object data) {
        switch (dataType) {
            case Constants.AGENT_DATA_CONTAINER_FROM_BDI:
            case Constants.AGENT_DATA_CONTAINER_FROM_ABM:
                dataListeners.get(dataType).receiveData(time, dataType, data);
                break;
            default:
                throw new RuntimeException("Unknown data type received: " + dataType);
        }
    }


}
