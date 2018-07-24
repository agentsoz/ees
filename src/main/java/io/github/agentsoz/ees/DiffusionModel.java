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

import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


public class DiffusionModel implements DataSource, DataClient {



    private DataServer dataServer;
    private SocialNetworkManager snManager;
    private TreeMap<Double, DiffusedContent> allStepsInfoSpreadMap;
    private double lastUpdateTimeInMinutes = -1;
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

    public DiffusionModel(String configFile) {
        this.snManager = new SocialNetworkManager(configFile);
        this.allStepsInfoSpreadMap = new TreeMap<Double, DiffusedContent>();
    }

    public void init(List<String> idList) {
        for (String id : idList) {
            this.snManager.createSocialAgent(id); //populate agentmap
        }
        this.snManager.genNetworkAndDiffModels(); // setup configs, gen network and diffusion models
        this.snManager.printSNModelconfigs();
        //subscribe to BDI data updates
        this.dataServer.subscribe(this, PerceptList.BDI_STATE_UPDATES);
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
        double currentTime = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
        SortedMap<Double, DiffusedContent> periodicInfoSpread = allStepsInfoSpreadMap.subMap(lastUpdateTimeInMinutes, currentTime);
        lastUpdateTimeInMinutes = currentTime;
        //Double nextTime = timestep + SNConfig.getDiffturn(); //allStepsInfoSpreadMap.higherKey(currentTime);
        Double nextTime = timestep; // FIXME: should be timestep + SNConfig.getDiffturn()
        if (nextTime != null) {
            dataServer.registerTimedUpdate(PerceptList.DIFFUSION, this, nextTime);
            // step the model before begin called again
            stepDiffusionProcess();
        }
        return periodicInfoSpread;
    }


    @Override
    public boolean dataUpdate(double time, String dataType, Object data) { // data package from the BDI side

        switch (dataType) {

            case PerceptList.BDI_STATE_UPDATES: { // update social states based on BDI reasoning

                logger.debug("SNModel: received BDI state updates");
                ICModel icModel = (ICModel) this.snManager.getDiffModel();
                icModel.updateSocialStatesFromBDIPercepts(data);
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
