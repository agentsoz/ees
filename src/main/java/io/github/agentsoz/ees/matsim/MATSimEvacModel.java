package io.github.agentsoz.ees.matsim;

import java.util.*;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.*;
import io.github.agentsoz.abmjill.JillModel;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdimatsim.EvacConfig;
import io.github.agentsoz.bdimatsim.MATSimModel;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.util.Disruption;
import io.github.agentsoz.util.EmergencyMessage;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.util.evac.PerceptList;
import io.github.agentsoz.util.Location;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlayPauseSimulationControl;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultTurnAcceptanceLogic;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

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

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.nonmatsim.PAAgentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Dhi Singh
 */
public final class MATSimEvacModel implements ABMServerInterface, QueryPerceptInterface, DataClient {

    private final MATSimModel matsimModel;

    public MATSimEvacModel(MATSimModel model) {
        matsimModel = model;
    }

    public io.github.agentsoz.bdimatsim.EvacConfig initialiseEvacConfig(Config config) {
        io.github.agentsoz.bdimatsim.EvacConfig evacConfig = ConfigUtils.addOrGetModule(config, io.github.agentsoz.bdimatsim.EvacConfig.class);
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

    @Override
    public void receiveData(double time, String dataType, Object data) {
        matsimModel.receiveData(time, dataType, data);
    }

    public void init(List<String> bdiAgentIDs) {
        matsimModel.init(bdiAgentIDs);
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
        matsimModel.finish();
    }

    public Config loadAndPrepareConfig() {
        return matsimModel.loadAndPrepareConfig();
    }

    public Scenario loadAndPrepareScenario() {
        return matsimModel.loadAndPrepareScenario();
    }

    public PAAgentManager getAgentManager() {
        return matsimModel.getAgentManager();
    }
}