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
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.util.Time;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MainNew {

    private static final Logger log = LoggerFactory.getLogger(MainNew.class);

    public static void main(String[] args) {
        Config cfg = new Config();
        Map<String,String> opts = cfg.parse(args);
        cfg.loadFromFile(opts.get(Config.OPT_CONFIG));

        MainNew sim = new MainNew();
        sim.start(cfg);
    }

    private void start(Config cfg) {
        // initialise the data server bus for passing data around using a publish/subscribe or pull mechanism
        DataServer dataServer = DataServer.getServer("EES");
        dataServer.setTime(hhMmToS(cfg.getGlobalConfig(Config.eGlobalStartHhMm)));

        // initialise the fire model and register it as an active data source
        {
            log.info("Starting fire model");
            PhoenixFireModule model = new PhoenixFireModule(cfg.getModelConfig(Config.eModelFire), dataServer);
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
        log.info("Starting MATSim model");
        MATSimModel matsimModel = new MATSimModel(cfg.getModelConfig(Config.eModelMatsim), dataServer);
        log.info("Loading MATSim config");
        org.matsim.core.config.Config config = matsimModel.loadAndPrepareConfig() ;
        EvacConfig evacConfig = ConfigUtils.addOrGetModule(config, EvacConfig.class);
        evacConfig.setSetup(EvacConfig.Setup.standard);
        evacConfig.setCongestionEvaluationInterval(matsimModel.getOptCongestionEvaluationInterval());
        evacConfig.setCongestionToleranceThreshold(matsimModel.getOptCongestionToleranceThreshold());
        evacConfig.setCongestionReactionProbability(matsimModel.getOptCongestionReactionProbability());
        log.info("Loading MATSim scenario");
        Scenario scenario = matsimModel.loadAndPrepareScenario() ;

        //List<SafeLineMonitor> monitors = registerSafeLineMonitors(SimpleConfig.getSafeLines(), matsimModel);
        // Write safe line statistics to file
        //writeSafeLineMonitors(monitors, safelineOutputFilePattern);

        // initialise the Jill model and register it as an active data source
        // FIXME: move jill stuff to jill model
        log.info("Starting Jill BDI model");
        JillBDIModel jillmodel = null;
        List<String> bdiAgentIDs = null;
        String[] jillInitArgs = cfg.getModelConfig(Config.eModelBdi).get("jillconfig").split("\\|");
        {
            Map<String, List<String[]>> bdiMap = null;
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
            jillmodel = initialiseAndStartJillModel(jillInitArgs, dataServer, matsimModel, bdiAgentIDs, bdiMap);
        }

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


        // All done, so terminate now
        jillmodel.finish();
        matsimModel.finish() ;
        //		System.exit(0);
        // get rid of System.exit(...) so that tests run through ...
        DataServer.cleanup() ;

        log.info("Finishing");
    }

    /**
     * Returns something like:
     * <pre>
     * agents:[
     *  {classname:io.github.agentsoz.ees.agents.Resident,
     *   args:null,
     *   count:10
     *  },
     *  {classname:io.github.agentsoz.ees.agents.Responder,
     *   args:[--respondToUTM, \"Tarrengower Prison,237100,5903400\"],
     *   count:3
     *  }
     * ]
     * </pre>
     *
     * @param map
     * @return
     */
    private String buildJillAgentsArgsFromAgentMap(Map<String, List<String[]>> map) {
        if (map == null) {
            return null;
        }
        // Count instances of each agent type
        Map<String,Integer> counts = new TreeMap<>();
        for (List<String[]> values: map.values()) {
            for (String[] val : values) {
                if (SimpleConfig.getBdiAgentTagInMATSimPopulationFile().equals(val[0])) {
                    String type = val[1];
                    int count = counts.containsKey(type) ? counts.get(type) : new Integer(0);
                    counts.put(type, count + 1);
                }
            }

        }

        StringBuilder arg = new StringBuilder();
        arg.append("agents:[");
        if (map != null) {
            Iterator<String> it = counts.keySet().iterator();
            while(it.hasNext()) {
                String key = it.next();
                arg.append("{");
                arg.append("classname:"); arg.append(key); arg.append(",");
                arg.append("args:[],"); // empty class args; per-instance args done later
                arg.append("count:"); arg.append(counts.get(key));
                arg.append("}");
                if (it.hasNext()) arg.append(",");
            }
        }
        arg.append("]");
        return arg.toString();
    }

    private JillBDIModel initialiseAndStartJillModel(String[] jillInitArgs, DataServer dataServer, MATSimModel matsimModel, List<String> bdiAgentIDs, Map<String, List<String[]>> bdiMap) {
        // Create the Jill BDI model
        JillBDIModel jillmodel = new JillBDIModel(jillInitArgs);
        // Set the evacuation timing
        jillmodel.setEvacuationTiming(SimpleConfig.getEvacStartHHMM(), SimpleConfig.getEvacPeakMins());
        // Set the query percept interface to use
        jillmodel.setQueryPerceptInterface((QueryPerceptInterface) matsimModel);
        // register the server to use for transferring data between models
        jillmodel.registerDataServer(dataServer);
        // initialise and start
        jillmodel.init(matsimModel.getAgentManager().getAgentDataContainer(),
                null, // agentList is not used
                matsimModel,
                bdiAgentIDs.toArray( new String[bdiAgentIDs.size()] ));
        Map<String,List<String>> args = new HashMap<>();
        if (bdiMap != null) {
            for (String id : bdiMap.keySet()) {
                List<String[]> values = bdiMap.get(id);
                List<String> flatlist = new ArrayList<>();
                for (String[] arr : values) {
                    for (String val : arr) {
                        flatlist.add(val);
                    }
                }
                args.put(id, flatlist);
            }
            jillmodel.initialiseAgentsWithArgs(args);
        }
        jillmodel.start();
        return jillmodel;
    }

    /**
     * Filter out all but the BDI agents
     *
     * @param map
     */
    private void removeNonBdiAgentsFrom(Map<String, List<String[]>> map) {
        Iterator<Map.Entry<String, List<String[]>>> it = map.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, List<String[]>> entry = it.next();
            String id = entry.getKey();
            boolean found = false;
            for (String[] val : entry.getValue()) {
                if (SimpleConfig.getBdiAgentTagInMATSimPopulationFile().equals(val[0])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                it.remove();
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


}
