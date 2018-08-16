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

import io.github.agentsoz.bdimatsim.EvacAgentTracker;
import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.bdimatsim.Utils;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.util.Time;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainNew {

    public static void main(String[] args) {
        Config cfg = new Config();
        Map<String,String> opts = cfg.parse(args);
        cfg.loadFromFile(opts.get(Config.OPT_CONFIG));

        MainNew sim = new MainNew();
        sim.start(cfg);
    }

    private void start(Config cfg) {
        // Create and initialise the data server used for passing
        // several different types of data around the application
        // using a publish/subscribe or pull mechanism
        DataServer dataServer = DataServer.getServer("EES");

        // initialise the fire model and register it as an active data source
        {
            PhoenixFireModule model = new PhoenixFireModule(cfg.getModelConfig(Config.eModelFire), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        // initialise the disruptions model and register it as an active data source
        {
            DisruptionModel model = new DisruptionModel(cfg.getModelConfig(Config.eModelDisruption), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        // initialise the messaging model and register it as an active data source
        {
            MessagingModel model = new MessagingModel(cfg.getModelConfig(Config.eModelMessaging), dataServer);
            model.setTimestepUnit(Time.TimestepUnit.SECONDS);
            model.start();
        }
        // initialise the MATSim model and register it as an active data source
        {
            MATSimModel model = new MATSimModel(cfg.getModelConfig(Config.eModelMatsim), dataServer);
        }
        /*

        List<SafeLineMonitor> monitors = registerSafeLineMonitors(SimpleConfig.getSafeLines(), matsimModel);

        // --- do some things for which you need access to the matsim config:
        org.matsim.core.config.Config config = matsimModel.loadAndPrepareConfig() ;
        setSimStartTimesRelativeToAlert(dataServer, config, -10*60 );
        EvacConfig evacConfig = ConfigUtils.addOrGetModule(config, EvacConfig.class);
        evacConfig.setSetup(setup);
        evacConfig.setCongestionEvaluationInterval(SimpleConfig.getCongestionEvaluationInterval());
        evacConfig.setCongestionToleranceThreshold(SimpleConfig.getCongestionToleranceThreshold());
        evacConfig.setCongestionReactionProbability(SimpleConfig.getCongestionReactionProbability());

        // --- do some things for which you need a handle to the matsim scenario:
        Scenario scenario = matsimModel.loadAndPrepareScenario() ;

        // --- initialize and start jill (need the bdiAgentIDs, for which we need the material from before)
        List<String> bdiAgentIDs = null;
        Map<String, List<String[]>> bdiMap = null;
        if (!SimpleConfig.isLoadBDIAgentsFromMATSimPlansFile()) {
            bdiAgentIDs = Utils.getBDIAgentIDs( scenario );
        } else {
            bdiMap = Utils.getAgentsFromMATSimPlansFile(scenario);
            removeNonBdiAgentsFrom(bdiMap);

            bdiAgentIDs = new ArrayList<>(bdiMap.keySet());
            if (bdiMap != null) {
                for (int i = 0; jillInitArgs != null && i < jillInitArgs.length; i++) {
                    if ("--config".equals(jillInitArgs[i]) && i < (jillInitArgs.length-1)) {
                        String agentsArg = (!bdiMap.isEmpty()) ?
                                buildJillAgentsArgsFromAgentMap(bdiMap) :
                                "agents:[{classname:io.github.agentsoz.ees.agents.bushfire.BushfireAgent, args:null, count:0}]";
                        jillInitArgs[i + 1] = jillInitArgs[i + 1].replaceAll("agents:\\[]", agentsArg);
                    }
                }
            }
        }
        // --- initialize and start the Jill model
        JillBDIModel jillmodel = initialiseAndStartJillModel(dataServer, matsimModel, bdiAgentIDs, bdiMap);


        // --- initialize and start matsim (maybe rename?):
        matsimModel.init(bdiAgentIDs);

        EvacAgentTracker tracker = new EvacAgentTracker(evacConfig, matsimModel.getScenario().getNetwork(), matsimModel.getEvents() ) ;
        matsimModel.getEvents().addHandler( tracker );
        // yyyy try to replace this by injection. because otherwise it again needs to be added "late enough", which we
        // wanted to get rid of.  kai, dec'17

        while ( true ) {

            jillmodel.takeControl( matsimModel.getAgentManager().getAgentDataContainer() );
            if( matsimModel.isFinished() ) {
                break;
            }
            //matsimModel.takeControl(matsimModel.getAgentManager().getAgentDataContainer());
            matsimModel.runUntil((long)dataServer.getTime(), matsimModel.getAgentManager().getAgentDataContainer());

            // increment time
            dataServer.stepTime();

            // firemodel is updated elsewhere
        }

        // Write safe line statistics to file
        writeSafeLineMonitors(monitors, safelineOutputFilePattern);

        // All done, so terminate now
        jillmodel.finish();
        matsimModel.finish() ;
        //		System.exit(0);
        // get rid of System.exit(...) so that tests run through ...
        DataServer.cleanup() ;
        */

    }

}
