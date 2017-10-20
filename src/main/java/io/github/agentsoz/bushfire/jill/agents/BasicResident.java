package io.github.agentsoz.bushfire.jill.agents;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
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

import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

import io.github.agentsoz.abmjack.shared.ActionManager;
import io.github.agentsoz.abmjack.shared.GlobalTime;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.ActionContent.State;
import io.github.agentsoz.bushfire.bdi.IBdiConnector;
import io.github.agentsoz.bushfire.jill.goals.EvacHouse;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.Global;

/**
 * 
 * @author Sewwandi Perera
 *
 */
@AgentInfo(hasGoals = { "agentsoz.bushfire.jill.goals.EvacMessage",
		"agentsoz.bushfire.jill.goals.EvacHouse",
		"agentsoz.abmjill.genact.EnvironmentAction" })
public class BasicResident extends Agent implements
		io.github.agentsoz.bdiabm.Agent {

	final Logger logger = LoggerFactory.getLogger("");
	public ActionManager actionManager;
	public boolean kidsNeedPickup;
	public boolean relsNeedPickup;
	public String id;
	public String homeRegion;
	public double[] currentDestination;
	public double[] currentStartLocation;
	public double[] startLocation;
	public IBdiConnector bdiConnector;
	public List<Coordinate> evacRoute;
	public List<String> waypointNames;
	public int evacDelay;

	public BasicResident(String name) {
		super(name);

	}

	public void init(ActionManager am, IBdiConnector bdiConnector,
			boolean getKids, boolean getRels) {
		this.actionManager = am;
		this.bdiConnector = bdiConnector;
		this.kidsNeedPickup = getKids;
		this.relsNeedPickup = getRels;
		logger.debug("Agent " + id + " initialised; {} ;",
				(getKids ? "has kids; " : "no kids; ")
						+ (getRels ? "has relatives" : "no relatives"));
		currentDestination = new double[2];
		evacDelay = Global.getRandom().nextInt(1800);
	}

	public void setDestination(double[] dest) {
		currentStartLocation = currentDestination;
		currentDestination = dest;
	}

	public ActionManager getActionManager() {
		return actionManager;
	}

	public double[] getStartLocation() {
		return startLocation;
	}

	public void updateActionState(String actionID, State state, Object[] params) {
		logger.info("============================== post updateActionState");
		// TODO:postEvent(updateaction_p.postEvent(actionID, state, params));
	}

	public long getCurrentTime() {
		return GlobalTime.time.getTime();
	}

	public void fireAlert(List<Coordinate> evacRoute, List<String> waypointNames) {
		this.evacRoute = evacRoute;
		this.waypointNames = waypointNames;
		post(new EvacHouse("Evac House"));
	}

	@Override
	public void init(String[] args) {

	}

	@Override
	public void start() {

	}

	@Override
	public void handlePercept(String perceptID, Object parameters) {

	}

	@Override
	public void packageAction(String actionID, Object[] parameters) {

	}

	@Override
	public void updateAction(String actionID, ActionContent content) {

	}

	@Override
	public void kill() {
		super.finish();
	}

}
