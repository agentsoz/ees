package io.github.agentsoz.ees;

import io.github.agentsoz.socialnetwork.ICModel;
import io.github.agentsoz.socialnetwork.SNConfig;
import io.github.agentsoz.socialnetwork.SocialNetworkManager;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.socialnetwork.util.DiffusedContent;
import io.github.agentsoz.util.Time;
import io.github.agentsoz.util.evac.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class DiffusionModel implements DataSource, DataClient {



    private DataServer dataServer;
    private SocialNetworkManager snManager;
    private TreeMap<Double, DiffusedContent> allStepsInfoSpreadMap;
    private double lastUpdateTimeInMinutes = -1;
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;

    Map<String, Set> contentFromAgents;

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

    public DiffusionModel(String configFile) {
        this.snManager = new SocialNetworkManager(configFile);
        this.allStepsInfoSpreadMap = new TreeMap<Double, DiffusedContent>();
        this.contentFromAgents = new HashMap<>();
    }

    public void init(List<String> idList) {
        for (String id : idList) {
            this.snManager.createSocialAgent(id); //populate agentmap
        }
        this.snManager.genNetworkAndDiffModels(); // setup configs, gen network and diffusion models
        this.snManager.printSNModelconfigs();
        //subscribe to BDI data updates
        this.dataServer.subscribe(this, PerceptList.SOCIAL_NETWORK_MSG);
    }

    private void stepDiffusionProcess() {
        if (snManager.processDiffusion((long) dataServer.getTime())) {
            if (snManager.getDiffModel() instanceof ICModel) {
                ICModel icModel = (ICModel) snManager.getDiffModel();
                HashMap<String, Integer[]> latestUpdate = icModel.getLatestDiffusionUpdates();
                if (!latestUpdate.isEmpty()) {
                    DiffusedContent dc = new DiffusedContent();
                    dc.setContentSpreadMap(latestUpdate);
                    this.allStepsInfoSpreadMap.put(dataServer.getTime(), dc);
                    logger.debug("put timed diffusion updates for ICModel at {}", dataServer.getTime());
                }
            }

        }

    }

    @Override
    public Object getNewData(double timestep, Object parameters) {
        //Double nextTime = timestep + SNConfig.getDiffturn(); //allStepsInfoSpreadMap.higherKey(currentTime);
        Double nextTime = timestep; // FIXME: should be timestep + SNConfig.getDiffturn()
        if (nextTime != null) {
            dataServer.registerTimedUpdate(PerceptList.DIFFUSION, this, nextTime);
            // update the model with any new messages form agents
            ICModel icModel = (ICModel) this.snManager.getDiffModel();
            if (!contentFromAgents.isEmpty()) {
                // FIXME: remove below once diffusion model handles String[] for agents
                Map<String, int[]> map = new HashMap<>();
                for (String key : contentFromAgents.keySet()) {
                    Object[] set = contentFromAgents.get(key).toArray(new String[0]);
                    int[] newSet = new int[set.length];
                    for (int i = 0; i < set.length; i++) {
                        newSet[i] = Integer.parseInt((String)set[i]);
                    }
                    map.put(key,newSet);
                }
                icModel.updateSocialStatesFromBDIPercepts(map);
            }
            // step the model before begin called again
            stepDiffusionProcess();
            // clear the contents
            contentFromAgents.clear();
        }
        double currentTime = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
        SortedMap<Double, DiffusedContent> periodicInfoSpread = allStepsInfoSpreadMap.subMap(lastUpdateTimeInMinutes, currentTime);
        lastUpdateTimeInMinutes = currentTime;

        return (periodicInfoSpread.isEmpty()) ? null : periodicInfoSpread;
    }


    @Override
    public boolean dataUpdate(double time, String dataType, Object data) { // data package from the BDI side

        switch (dataType) {

            case PerceptList.SOCIAL_NETWORK_MSG: { // update social states based on BDI reasoning

                // data from agent is of type [String,String] being [content,agentid]
                if (!(data instanceof String[]) || ((String[])data).length != 2) {
                    logger.error("received unknown data: " + data);
                    return true;
                }
                String[] content = (String[]) data;
                logger.debug("received content " + content);
                String msg = content[0];
                String agentId = content[1];
                Set<String> agents = (contentFromAgents.containsKey(msg)) ? contentFromAgents.get(msg) :
                        new HashSet<>();
                agents.add(agentId);
                contentFromAgents.put(msg,agents);
                return true;
            }

        }
        return false;
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

    /**
     * Start publishing data
     */
    public void start(int[] hhmm) {
        double startTimeInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS)
                + Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
        dataServer.registerTimedUpdate(PerceptList.DIFFUSION, this, startTimeInSeconds);
    }


    public void finish() {
        // cleaning
    }
}
