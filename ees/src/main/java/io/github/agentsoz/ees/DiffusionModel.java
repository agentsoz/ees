package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.socialnetwork.ICModel;
import io.github.agentsoz.socialnetwork.SNConfig;
import io.github.agentsoz.socialnetwork.SocialNetworkManager;
import io.github.agentsoz.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class DiffusionModel implements DataSource<SortedMap<Double, DiffusedContent>>, DataClient<String[]> {

    private final Logger logger = LoggerFactory.getLogger(DiffusionModel.class);

    private static final String eConfigFile = "configFile";

    private DataServer dataServer;
    private double startTimeInSeconds = -1;
    private SocialNetworkManager snManager;
    private TreeMap<Double, DiffusedContent> allStepsInfoSpreadMap;
    private double lastUpdateTimeInMinutes = -1;
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private String configFile = null;
    private List<String> agentsIds = null;

    Map<String, Set> localContentFromAgents;
    ArrayList<String> globalContentFromAgents;

    public DiffusionModel(String configFile) {
        this.snManager = (configFile==null) ? null : new SocialNetworkManager(configFile);
        this.allStepsInfoSpreadMap = new TreeMap<>();
        this.localContentFromAgents = new HashMap<>();
        this.globalContentFromAgents =  new ArrayList<String>();
    }

    public DiffusionModel(Map<String, String> opts, DataServer dataServer, List<String> agentsIds) {
        parse(opts);
        this.snManager = (configFile==null) ? null : new SocialNetworkManager(configFile);
        this.allStepsInfoSpreadMap = new TreeMap<>();
        this.localContentFromAgents = new HashMap<>();
        this.globalContentFromAgents =  new ArrayList<String>();
        this.dataServer = dataServer;
        this.agentsIds = agentsIds;
    }

    private void parse(Map<String, String> opts) {
        if (opts == null) {
            return;
        }
        for (String opt : opts.keySet()) {
            logger.info("Found option: {}={}", opt, opts.get(opt));
            switch(opt) {
                case Config.eGlobalStartHhMm:
                    String[] tokens = opts.get(opt).split(":");
                    setStartHHMM(new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])});
                    break;
                case eConfigFile:
                    configFile = opts.get(opt);
                    break;
                default:
                    logger.warn("Ignoring option: " + opt + "=" + opts.get(opt));
            }
        }
    }

    public void setStartHHMM(int[] hhmm) {
        startTimeInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, timestepUnit)
                + Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, timestepUnit);
    }


    public void init(List<String> idList) {

        this.snManager.setupSNConfigsAndLogs(); // first, setup configs and create log
        for (String id : idList) {
            this.snManager.createSocialAgent(id); //populate agentmap
        }
        this.snManager.genNetworkAndDiffModels(); // setup configs, gen network and diffusion models
        this.snManager.printSNModelconfigs();
        //subscribe to BDI data updates
        this.dataServer.subscribe(this, Constants.SOCIAL_NETWORK_MSG);
        this.dataServer.subscribe(this, Constants.SOCIAL_NETWORK_BROADCAST_MSG);
    }

    private void stepDiffusionProcess() {
        snManager.diffuseContent(); // step the diffusion model
        if (snManager.getDiffModel() instanceof ICModel) {
            ICModel icModel = (ICModel) snManager.getDiffModel();
            icModel.recordCurrentStepSpread(dataServer.getTime());

            // FIXME: disabling below, broken since diffusion-model repo commit ced012c, dhi 21/jun/19
            //HashMap<String, String[]> latestUpdate = icModel.getLatestDiffusionUpdates();
            HashMap<String, String[]> latestUpdate = new HashMap<>();
            if (!latestUpdate.isEmpty()) {
                DiffusedContent dc = new DiffusedContent();
                dc.setContentSpreadMap(latestUpdate);
                this.allStepsInfoSpreadMap.put(dataServer.getTime(), dc);
                logger.debug("put timed diffusion updates for ICModel at {}", dataServer.getTime());
            }
        }


    }

    @Override
    public SortedMap<Double, DiffusedContent> sendData(double timestep, String dataType) {
        Double nextTime = timestep + SNConfig.getDiffturn();
        if (nextTime != null) {
            dataServer.registerTimedUpdate(Constants.DIFFUSION, this, nextTime);
            // update the model with any new messages form agents
            ICModel icModel = (ICModel) this.snManager.getDiffModel();
            if (!localContentFromAgents.isEmpty()) { // update local content
                Map<String, String[]> map = new HashMap<>();
                for (String key : localContentFromAgents.keySet()) {
                    Object[] set = localContentFromAgents.get(key).toArray(new String[0]);
                    String[] newSet = new String[set.length];
                    for (int i = 0; i < set.length; i++) {
                        newSet[i] = (String)set[i];
                    }
                    map.put(key,newSet);
                    logger.info(String.format("At time %.0f, total %d agents will spread new message: %s", timestep, newSet.length, key));
                    logger.info("Agents spreading new message are: {}", Arrays.toString(newSet));
                }
                icModel.updateSocialStatesFromLocalContent(map);
            }

            if(!globalContentFromAgents.isEmpty()) { // update global content

                logger.info("Global content received to spread: {}", globalContentFromAgents.toString());
                icModel.updateSocialStatesFromGlobalContent(globalContentFromAgents);

            }
            // step the model before begin called again
            stepDiffusionProcess();

            // clear the contents
            globalContentFromAgents.clear();
            localContentFromAgents.clear();
        }
        double currentTime = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
        SortedMap<Double, DiffusedContent> periodicInfoSpread = allStepsInfoSpreadMap.subMap(lastUpdateTimeInMinutes, currentTime);
        lastUpdateTimeInMinutes = currentTime;

        return (periodicInfoSpread.isEmpty()) ? null : periodicInfoSpread;
    }


    @Override
    public void receiveData(double time, String dataType, String[] data) { // data package from the BDI side

        switch (dataType) {
            case Constants.SOCIAL_NETWORK_MSG: // update social states based on BDI reasoning
                // data from agent is of type [String,String] being [content,agentid]
                if (!(data instanceof String[]) || ((String[]) data).length != 2) {
                    logger.error("received unknown data: " + data);
                    break;
                }
                String[] content = (String[]) data;
                logger.debug("received local content " + content);
                String msg = content[0];
                String agentId = content[1];
                Set<String> agents = (localContentFromAgents.containsKey(msg)) ? localContentFromAgents.get(msg) :
                        new HashSet<>();
                agents.add(agentId);
                localContentFromAgents.put(msg, agents);
                break;
            case Constants.SOCIAL_NETWORK_BROADCAST_MSG:
                if (!(data instanceof String[]) || ((String[]) data).length != 2) {
                    logger.error("received unknown data: " + data);
                    break;
                }
                String[] globalContent = (String[]) data;
                logger.debug("received global content " + globalContent);
                String gMsg = globalContent[0];
                if(!globalContentFromAgents.contains(gMsg)) {
                    globalContentFromAgents.add(gMsg);
                }
                break;

            default:
                throw new RuntimeException("Unknown data type received: " + dataType);
        }
    }

    /**
     * Sets the publish/subscribe data server
     * @param dataServer the server to use
     */
    void setDataServer(DataServer dataServer) {
        this.dataServer = dataServer;
    }

    /**
     * Set the time step unit for this model
     * @param unit the time step unit to use
     */
    void setTimestepUnit(Time.TimestepUnit unit) {
        timestepUnit = unit;
    }


    public void start() {
        if (snManager != null) {
            init(agentsIds);
            setTimestepUnit(Time.TimestepUnit.MINUTES);
            dataServer.registerTimedUpdate(Constants.DIFFUSION, this, Time.convertTime(startTimeInSeconds, Time.TimestepUnit.SECONDS, timestepUnit));
        } else {
            logger.warn("started but will be idle forever!!");
        }
    }

    /**
     * Start publishing data
     * @param hhmm an array of size 2 with hour and minutes representing start time
     */
    public void start(int[] hhmm) {
        double startTimeInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
                + Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
        dataServer.registerTimedUpdate(Constants.DIFFUSION, this, startTimeInSeconds);
    }


    public void finish() {
        // cleaning

        if(snManager == null) { // return if the diffusion model is not executed
            return;
        }
        if (snManager.getDiffModel() instanceof ICModel) {

            //terminate diffusion model and output diffusion data
            ICModel icModel = (ICModel) this.snManager.getDiffModel();
            icModel.finish();
            icModel.getDataCollector().writeSpreadDataToFile();
        }

    }
}
