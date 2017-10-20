package io.github.agentsoz.bushfire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import aos.jack.jak.agent.Agent;
import io.github.agentsoz.abmjack.JACKModel;
import io.github.agentsoz.abmjack.shared.ActionManager;
import io.github.agentsoz.abmjack.shared.GlobalTime;

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
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdimatsim.moduleInterface.data.SimpleMessage;
import io.github.agentsoz.bushfire.datamodels.Region;
import io.github.agentsoz.bushfire.datamodels.RegionSchedule;
import io.github.agentsoz.bushfire.jack.agents.BasicResident;
import io.github.agentsoz.bushfire.jack.agents.EvacController;
import io.github.agentsoz.bushfire.shared.ActionID;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.util.Global;

/**
 * The BushfireApplication class handles passing percepts and actions to and
 * from the resident agents, passing simulation setup data (locations, routes
 * etc) and agent plan choices to the visualiser, broadcasting alerts to the
 * agents, and managing the EvacController and relief schedule data.
 **/
public class BushfireApplication extends JACKModel implements DataClient,
		DataSource, ActionManager {

	final Logger logger = LoggerFactory.getLogger("");
	private BdiConnector bdiConnector;
	protected EvacController controller;
	protected HashMap<String, Object[]> agentsDriveToActions = new LinkedHashMap<String, Object[]>();
	protected DataServer dataServer;

	/**
	 * Used only when byPassController is enabled
	 */
	static ArrayList<String> regionsToEvacuate = new ArrayList<String>();

	/**
	 * Used only when byPassController is enabled
	 */
	static boolean startedEvac = false;

	@Override
	public void takeControl(AgentDataContainer adc) {
		Object[] agentsInDriveToList = agentsDriveToActions.keySet().toArray();

		for (Object agentId : agentsInDriveToList) {
			Object[] parameters = agentsDriveToActions.get((String) agentId);

			if ((Integer) parameters[3] == 1) {
				agentsDriveToActions.remove(agentId);
				startDriving((String) agentId, parameters);
			} else {
				parameters[3] = (Integer) parameters[3] - 1;
				agentsDriveToActions.remove(agentId);
				agentsDriveToActions.put((String) agentId, parameters);
			}
		}

		this.setAgentsDepartureEvents(adc);
		GlobalTime.increaseNewTime();
		super.takeControl(adc);
	}

	protected void setAgentsDepartureEvents(AgentDataContainer adc) {
		// on receiving the fire alert, insert a global percept into the data
		// container
		if (!regionsToEvacuate.isEmpty()) {
			adc.getOrCreate("global").getPerceptContainer()
					.put(FireModule.FIREALERT, regionsToEvacuate.clone());
			logger.debug("added fire alert percept");
			regionsToEvacuate.clear();
		}
	}

	@Override
	public void setup(ABMServerInterface abmServer) {

		dataServer = DataServer.getServer("Bushfire");
		dataServer.subscribe(this, DataTypes.FIRE_ALERT);
		dataServer.subscribe(this, DataTypes.EVAC_BROADCAST);
		dataServer.subscribe(this, DataTypes.MATSIM_AGENT_UPDATES);
		dataServer.registerSource("time", this);

		bdiConnector = new BdiConnector(this);
		controller = new EvacController("controller", bdiConnector);
		publishGeography();
	}

	// Implemented here so we can cleanly close our log files.
	@Override
	public void finish() {
		logger.info("shut down");
		EvacuationReport.close();
		super.finish();
	}

	void promptControllerInput() {
		SimpleMessage msg = new SimpleMessage();
		msg.name = DataTypes.UI_PROMPT;
		String prompt = "Specify input for EvacController agent?";
		String[] options = new String[] { "Yes", "No" };
		msg.params = new Object[] { prompt, options };
		dataServer.publish(DataTypes.UI_PROMPT, msg);
	}

	@Override
	public void packageAction(String agentID, String actionID,
			Object[] parameters) {

		Region region = getResidetsHomeRegion(agentID);

		// BDI agent has completed all of its goals - publish update
		if ((String) parameters[0] == "done" && dataServer != null) {
			SimpleMessage message = new SimpleMessage();
			message.name = "done";
			message.params = new Object[] { agentID, "done" };
			dataServer.publish(DataTypes.BDI_AGENT_UPDATES, message);
			logger.debug("agent {} made it to relief centre", agentID);
			region.agentEvacuated();
			return;
		}

		// translate abstract destination names to actual network coordinates
		// before packaging
		if ((String) parameters[0] == ActionID.DRIVETO) {
			if ((Integer) parameters[3] == 0) {
				startDriving(agentID, parameters);
			} else {
				logger.debug("agent {} will drive to {} after {} seconds",
						agentID, (String) parameters[2],
						Integer.toString((int) parameters[3]));
				agentsDriveToActions.put(agentID, parameters);
			}
		}
	}

	protected void startDriving(String agentID, Object[] parameters) {
		Region region = getResidetsHomeRegion(agentID);

		double[] destination = (double[]) parameters[1];
		String destinationName = (String) parameters[2];

		logger.debug("agent {} driving to {}, coordinates: {}", agentID,
				destinationName, destination);
		region.agentStart();

		callSuperPackageAction(agentID, ActionID.DRIVETO, parameters);
	}

	protected void callSuperPackageAction(String agentID, String action,
			Object[] parameters) {
		super.packageAction(agentID, action, parameters);

		if (dataServer != null) {
			// publish new agent actions
			SimpleMessage message = new SimpleMessage();
			message.name = "updateAgentBDI";
			message.params = new Object[] { agentID, parameters[0],
					parameters[1], parameters[2] };
			dataServer.publish(DataTypes.BDI_AGENT_UPDATES, message);
		}
	}

	protected Region getResidetsHomeRegion(String agentID) {
		BasicResident resident = ((BasicResident) agents.get(agentID));
		return Config.getRegion(resident.homeRegion);
	}

	/**
	 * The visualiser module will store this data, and send it to the visualiser
	 * when the visualiser requests it TODO: Should go under visualiser module
	 */
	void publishGeography() {

		logger.debug("publishing LOCATION, ROUTE, REGION, and IMAGE");

		for (String location : Config.getLocations()) {
			dataServer
					.publish(DataTypes.LOCATION, Config.getLocation(location));
		}
		for (String route : Config.getRoutes()) {
			dataServer.publish(DataTypes.ROUTE, Config.getRoute(route));
		}
		for (String region : Config.getRegionsByName()) {
			dataServer.publish(DataTypes.REGION, Config.getRegion(region));
		}
		dataServer.publish(DataTypes.IMAGE, Config.getImage());
	}

	@Override
	public Agent createAgent(String agentID, Object[] initData) {
		return new BasicResident(agentID, this, bdiConnector, getKids(""),
				getRels(""));
	}

	public void publishSchedule(RegionSchedule rs) {
		dataServer.publish(DataTypes.EVAC_SCHEDULE, rs);
	}

	@SuppressWarnings({"unchecked" })
	@Override
	public void handlePercept(Agent agent, String perceptID, Object parameters) {

		if (Config.getBypassController()) {

			BasicResident resident = (BasicResident) agent;
			if (perceptID.equals(FireModule.FIREALERT)
					&& ((ArrayList<String>) parameters)
							.contains(resident.homeRegion)) {
				List<Coordinate> waypoints = new ArrayList<Coordinate>();
				List<String> waypointNames = new ArrayList<String>();

				String randomShelter = Config.getRandomEvacPoint();

				waypointNames.add(Config.getReliefCentre(randomShelter)
						.getLocation());
				double[] coords = Config.getLocation(waypointNames.get(0))
						.getCoordinates();
				waypoints.add(new Coordinate(coords[0], coords[1]));
				logger.debug("agent {} received fire alert", resident.id);
				resident.fireAlert(waypoints, waypointNames);
			}

		} else {
			// Evac controller triggers residents byhimself. Therefore, it is
			// not needed to trigger each residents here.
		}
	}

	// update agent action state within the JACK program
	@Override
	public void updateAction(Agent agent, String actionID, State state,
			Object[] parameters) {
		((BasicResident) agent).updateActionState(actionID, state, parameters);
	}

	// listens for fire alerts and sets received flag to true
	@Override
	public boolean dataUpdate(double time, String dataType, Object data) {

		switch (dataType) {

		case DataTypes.FIRE_ALERT: {

			logger.debug("received fire alert");

			if (Config.getBypassController()) {

				logger.debug("bypassing controller, broadcasting evac message");
				regionsToEvacuate = new ArrayList<String>(
						Config.getRegionsByName());
				logger.debug("number of regions to evacuate is {}",
						regionsToEvacuate.size());
				return true;
			}
			logger.debug("telling controller to start fire response");
			controller.startFireResponse();
			return true;
		}
		case DataTypes.EVAC_BROADCAST: {

			logger.debug("received evacuation broadcast");
			startedEvac = true;
			return true;
		}
		// establish starting location and home region for each agent
		case DataTypes.MATSIM_AGENT_UPDATES: {
			logger.debug("received matsim agent updates");
			for (SimpleMessage msg : (SimpleMessage[]) data) {

				String id = (String) msg.params[0];
				BasicResident agent = (BasicResident) agents.get(id);

				if (agent != null && agent.getStartLocation() == null) {

					double lat = (double) msg.params[1];
					double lon = (double) msg.params[2];
					logger.debug("agent {} start location is {},{}", id, lon,
							lat);

					agent.startLocation = new double[] { lat, lon };
					Region r = Config.getRegion(lat, lon);

					if (r != null) {

						agent.homeRegion = r.getName();
						r.agents.add(agent);
						logger.debug("agent {} is in region {}", id,
								agent.homeRegion);
						dataServer.publish(DataTypes.REGION_ASSIGNMENT,
								new Object[] { agent.id, agent.homeRegion });
					} else {
						logger.warn("Agent {} was not assigned to any region",
								agent.id);
					}
				}
			}
		}
		}

		return false;
	}

	public boolean getKids(String name) {
		return Global.getRandom().nextDouble() < Config.getProportionWithKids();
	}

	public boolean getRels(String name) {
		return Global.getRandom().nextDouble() < Config.getProportionWithRelatives();
	}

	@Override
	public Object getNewData(double time, Object parameters) {

		// for (RegionSchedule rs : schedule.values()) {
		//
		// if (!rs.haveEvacuated && time > rs.getEvacTime() && startedEvac) {
		//
		// logger.info("Evacuating region " + rs.getRegion());
		// rs.haveEvacuated = true;
		// regionsToEvacuate.add(rs.getRegion());
		// }
		// }
		return null;
	}

	@Override
	public double getSimTime() {

		return dataServer.getTime();
	}
}