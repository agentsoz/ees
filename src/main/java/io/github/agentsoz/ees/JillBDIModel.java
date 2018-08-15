package io.github.agentsoz.ees;

import java.util.*;

import io.github.agentsoz.jill.core.GlobalState;
import io.github.agentsoz.util.evac.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.github.agentsoz.util.Time;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.util.Global;

public class JillBDIModel extends JillModel implements DataClient {

	private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.ees");

	private DataServer dataServer;
	
	// Records the simulation step at which the fire alert was received
	private double fireAlertTime = -1;
	private boolean fireAlertsScheduled = false;

	// Jill initialisation args
	private String[] initArgs = null;
	
	// Map of MATSim agent IDs to jill agent IDs
	private Map<String,String> mapMATsimToJillIds;
    // Reverse map of Jill agent IDs to MATSim agent IDs (for convinience)
    private Map<String,String> mapJillToMATsimIds;
	
	// Map<Time,Agent> of scheduled fire alerts 
	private PriorityQueue<TimedAlert> alerts;

	private int evacPeak = 0;
	private int[] evacStartHHMM = {0,0};

	public JillBDIModel(String[] initArgs) {
		mapMATsimToJillIds = new LinkedHashMap<String,String>();
		mapJillToMATsimIds = new LinkedHashMap<String,String>();
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
		dataServer.subscribe(this, PerceptList.FIRE_ALERT);

		// Initialise the Jill model
		// params[] contains the list of agent names to create
		if (super.init(agentDataContainer, agentList, abmServer, initArgs)) {
			// Initialise the alerts
			int capacity = (params.length<1) ? 1 : params.length;
			alerts = new PriorityQueue<TimedAlert>(capacity, new Comparator<TimedAlert>() {
				@Override
				public int compare(TimedAlert o1, TimedAlert o2) {
					double t1 = o1.getTime();
					double t2 = o2.getTime();
					if (t1 > t2) {
						return 1;
					} else if (t1 < t2) {
						return -1;
					} else {
						return 0;
					}
				}
			});
			// Set the BDI query percept interface that the agents can use
			for (int i=0; i<params.length; i++) {
				getAgent(i).setQueryPerceptInterface(this.getQueryPerceptInterface());
			}
			// Now create the given map to jill agent ids
			for (int i=0; i<params.length; i++) {
				String jillID = String.valueOf(((Agent)getAgent(i)).getId());
				mapMATsimToJillIds.put((String)params[i], jillID);
				mapJillToMATsimIds.put(jillID, (String)params[i]);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean dataUpdate(double time, String dataType, Object data) {
		switch (dataType) {
		case PerceptList.FIRE_ALERT:
			fireAlertTime = time;
			return true;
		};
		return false;
	}

	public void setEvacuationTiming(int[] hhmm, int peak) {
		evacStartHHMM = hhmm;
		evacPeak = peak;
	}

	@Override
	// send percepts to individual agents
	public void takeControl(AgentDataContainer adc) {
		// Schedul the fire alerts
		if (!fireAlertsScheduled && fireAlertTime != -1) {
			int[] hhmm = evacStartHHMM;
			int peak = evacPeak;
			logger.info(String.format("Scheduling evacuation starting at %2d:%2d and peaking %d mins after start", hhmm[0], hhmm[1], peak));
			scheduleFireAlertsToResidents(hhmm, peak);
			fireAlertsScheduled = true;
		}
		double timeInSecs = dataServer.getTime();
		// Send fire alerts to all agents scheduled for this time step (or earlier)
		while (!alerts.isEmpty() && alerts.peek().getTime() <= timeInSecs) {
			TimedAlert alert = alerts.poll();
			String matsimAgentId = alert.getAgent();
			adc.getOrCreate(matsimAgentId).getPerceptContainer().put(PerceptList.FIRE_ALERT, new Double(timeInSecs));
		}
		translateToJillIds(adc);
		adc.getOrCreate(PerceptList.BROADCAST).getPerceptContainer().put(PerceptList.TIME, timeInSecs);
		super.takeControl(adc);
        translateToMATSimIds(adc);
	}
	
  private void translateToJillIds(AgentDataContainer adc) {
    // FIXME: The incoming IDs need to be changed to Jill IDs before the next call
  }

  private void translateToMATSimIds(AgentDataContainer adc) {
    // FIXME: The outgoing IDs need to be converted to MATSim IDs before returning
  }

  /**
	 * Schedules fire alerts to be send to residents starting at time 
	 * {@link SimpleConfig#getEvacStartHHMM()} and with a normal peak at
	 * {@link SimpleConfig#getEvacPeakMins()} minutes after start.
	 * 
	 * Ideally this decision should be made by the agents, i.e., they should 
	 * all receive a fire alert at {@link SimpleConfig#getEvacStartHHMM()} and
	 * then they should individually decide how to react to that alert.
	 * Here we are forcing a pattern of vehicles leaving, by spreading the
	 * alerts over a normal distribution.
	 */
	public void scheduleFireAlertsToResidents(int[] hhmm, int mins3Sigma) {
		double minsSigma = mins3Sigma/3.0;
		double minsMean = mins3Sigma;
		double evacStartInSeconds = Time.convertTime(hhmm[0], Time.TimestepUnit.HOURS, Time.TimestepUnit.SECONDS) 
				+ Time.convertTime(hhmm[1], Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS);
		for (String matsimAgentId: mapMATsimToJillIds.keySet()) {
			double minsOffset = Global.getRandom().nextGaussian() * minsSigma + minsMean;
			double secsOffset = Math.round(Time.convertTime(minsOffset, Time.TimestepUnit.MINUTES, Time.TimestepUnit.SECONDS));
			double evacTimeInSeconds = evacStartInSeconds + secsOffset;
			alerts.add(new TimedAlert(evacTimeInSeconds, matsimAgentId));
		}
	}

	public void initialiseAgentsWithArgs(Map<String, List<String>> map) {
		if(map == null) return;
		for (String id : map.keySet()) {
			try {
				Integer idx = Integer.valueOf(id); // agent id must be an integer
				List<String> args = map.get(id);
				if (args != null && !args.isEmpty()) {
					String[] vargs = args.toArray(new String[args.size()]);
					getAgent(Integer.valueOf(id)).init(vargs);
				}
			} catch (NumberFormatException e) {
				logger.warn("Jill agent IDs can only be integers; found `"+id+"`. " + e.getMessage());
			}
		}
	}

	private class TimedAlert{
		private double time;
		private String agent;

		private TimedAlert(double time, String agent) {
			this.time = time;
			this.agent = agent;
		}
		
		private double getTime() {
			return time;
		}

		private String getAgent() {
			return agent;
		}
	}
}
