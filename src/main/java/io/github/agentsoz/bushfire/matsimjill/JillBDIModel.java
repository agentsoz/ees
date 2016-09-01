package io.github.agentsoz.bushfire.matsimjill;

import java.util.HashMap;

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

import io.github.agentsoz.abmjill.JillModel;
import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.bdiabm.data.AgentStateList;
import io.github.agentsoz.bushfire.DataTypes;
import io.github.agentsoz.bushfire.FireModule;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;

public class JillBDIModel extends JillModel implements DataClient {

	private DataServer dataServer;
	
	// Records the simulation step at which the fire alert was received
	private double fireAlertTime = -1;
	private boolean fireAlertSent = false;

	// Jill initialisation args
	private String[] initArgs = null;
	
	// Map of bushfire agent names to jill agent names
	private HashMap<String,String> agents;
	
	public JillBDIModel(String[] initArgs) {
		agents = new HashMap<String,String>();
		this.initArgs = initArgs;
	}
	public void registerDataServer(DataServer dataServer) {
		this.dataServer = dataServer;
	}

	@Override
	public boolean init(AgentDataContainer agentDataContainer,
			AgentStateList agentList, 
			ABMServerInterface abmServer,
			Object[] params) 
	{
		dataServer.subscribe(this, DataTypes.FIRE_ALERT);

		// Initialise the Jill model
		// params[] contains the list of agent names to create
		if (super.init(agentDataContainer, agentList, abmServer, initArgs)) {
			// Now create the given map to jill agent names
			// By default, jill agents are called a0, a1, ...
			for (int i=0; i<params.length; i++) {
				agents.put((String)params[i], "a"+i);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean dataUpdate(double time, String dataType, Object data) {
		switch (dataType) {
		case DataTypes.FIRE_ALERT:
			fireAlertTime = time;
			return true;
		};
		return false;
	}


	@Override
	// send percepts to individual agents
	public void takeControl(AgentDataContainer adc) {
		// Send the fire alert if it was just received
		if (!fireAlertSent && fireAlertTime != -1) {
			adc.getOrCreate("global").getPerceptContainer().put(FireModule.FIREALERT, null);
			fireAlertSent = true;
		}
		super.takeControl(adc);
	}
}
