package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.agentsoz.abmjill.JillModel;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.agents.archetype.ArchetypeAgent;
import io.github.agentsoz.ees.util.Utils;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPOutputStream;


public class JillBDIModel extends JillModel implements DataClient {

	private final Logger logger = LoggerFactory.getLogger(JillBDIModel.class);

	// Jill options
	static final String OPT_JILL_CONFIG = "--config";
	static final String OPT_JILL_PLAN_SELECTION_POLICY = "--plan-selection-policy";
	// Model options in EES config XML
	private static String eBDIAgentType = "BDIAgentType";
	static final String eEvacPeakMins = "evacPeakMins";
	static final String eConfig = "jillconfig";
	static final String ePlanSelectionPolicy = "jPlanSelectionPolicy";
	static final String eAgents = "jAgents";
	static final String eLogLevel = "jLogLevel";
	static final String eLogFile = "jLogFile";
	static final String eOutFile = "jOutFile";
	static final String eNumThreads = "jNumThreads";
	static final String eMetricsFile = "bdiMetricsFile";
	static final String eMetricsFrequencyInSecs = "bdiMetricsFrequencyInSeconds";
	// Model option defaults
	private String oRandomSeed = null;
	private String oPlanSelectionPolicy = null;
	private String oAgents = null;
	private String oLogLevel = null;
	private String oLogFile = null;
	private String oOutFile = null;
	private String oNumThreads = null;
	private String oMetricsFile = null;
	private int oMetricsFrequencyInSecs = 900; // 15mins
	private int metricCountdown = 0;
	private int lastTime = -1;


	private DataServer dataServer;

	// Records the simulation step at which the fire alert was received
	private double fireAlertTime = -1;
	private boolean fireAlertPerceptsScheduled = false;

	// Jill initialisation args
	private String[] initArgs = null;
	private Map<Integer, List<String[]>> agentsInitMap = null;

	// Map of MATSim agent IDs to jill agent IDs
	private Map<String,String> mapMATsimToJillIds;
    // Reverse map of Jill agent IDs to MATSim agent IDs (for convinience)
    private Map<String,String> mapJillToMATsimIds;
    // Map of agent metrics over time
	private SimulationMetrics metrics;

	// Map<Time,Agent> of scheduled fire alertPercepts
	private PriorityQueue<TimedAlert> alertPercepts;

	Map<Double, DiffusedContent> msgMap;
	HashMap<String,Set<String>> informedAgents;

	private int evacPeak = 0;
	private int[] evacStartHHMM = {0,0};

	private final Map<String, DataClient> dataListeners = createDataListeners();

	private Object sequenceLock;

	public JillBDIModel(String[] initArgs) {
		super();
		msgMap = new TreeMap<>();
		informedAgents = new HashMap<>();
		mapMATsimToJillIds = new LinkedHashMap<String,String>();
		mapJillToMATsimIds = new LinkedHashMap<String,String>();
		this.initArgs = initArgs;
	}

	public JillBDIModel(Map<String, String> opts, DataServer dataServer, QueryPerceptInterface qpi, Map<Integer, List<String[]>> agentsInitMap) {
		this(null);
		parse(opts);
		this.dataServer = dataServer;
		this.agentsInitMap = agentsInitMap;
		this.setQueryPerceptInterface(qpi);
		initArgs = buildJillConfig(agentsInitMap);
		metrics = new SimulationMetrics(opts.get(Config.eGlobalCoordinateSystem));
	}

	private String[] buildJillConfig(Map<Integer, List<String[]>> agentsInitMap) {
		List<String> args = new ArrayList<>();
		if (oPlanSelectionPolicy != null && !oPlanSelectionPolicy.isEmpty()) {
			args.add(OPT_JILL_PLAN_SELECTION_POLICY);
			args.add(oPlanSelectionPolicy);
		}

		args.add(OPT_JILL_CONFIG);
		StringBuilder cfg = new StringBuilder();
		cfg.append(cfg.toString().isEmpty() ? "" : ",");
		cfg.append("agents:");
		if (oAgents != null && !oAgents.isEmpty()) {
			cfg.append(oAgents);
		} else {
			cfg.append(buildJillAgentsArgsFromAgentMap(agentsInitMap));
		}
		if (oRandomSeed != null && !oRandomSeed.isEmpty()) {
			cfg.append(cfg.toString().isEmpty() ? "" : ",");
			cfg.append("randomSeed:");
			cfg.append(oRandomSeed);
		}
		if (oNumThreads != null && !oNumThreads.isEmpty()) {
			cfg.append(cfg.toString().isEmpty() ? "" : ",");
			cfg.append("numThreads:");
			cfg.append(oNumThreads);
		}
		if (oLogLevel != null && !oLogLevel.isEmpty()) {
			cfg.append(cfg.toString().isEmpty() ? "" : ",");
			cfg.append("logLevel:");
			cfg.append(oLogLevel);
		}
		if (oLogFile != null && !oLogFile.isEmpty()) {
			cfg.append(cfg.toString().isEmpty() ? "" : ",");
			cfg.append("logFile:");
			cfg.append(oLogFile.startsWith("\"") ? "" : "\"");
			cfg.append(oLogFile);
			cfg.append(oLogFile.endsWith("\"") ? "" : "\"");
		}
		if (oOutFile != null && !oOutFile.isEmpty()) {
			cfg.append(cfg.toString().isEmpty() ? "" : ",");
			cfg.append("programOutputFile:");
			cfg.append(oOutFile.startsWith("\"") ? "" : "\"");
			cfg.append(oOutFile);
			cfg.append(oOutFile.endsWith("\"") ? "" : "\"");
		}
		cfg.insert(0,"{");
		cfg.append("}");
		args.add(cfg.toString());
		return args.toArray(new String[args.size()]);
	}

	private void parse(Map<String, String> opts) {
		if (opts == null) {
			return;
		}
		for (String opt : opts.keySet()) {
			logger.info("Found option: {}={}", opt, opts.get(opt));
			switch(opt) {
				case eConfig:
					initArgs = opts.get(opt).split("\\|");
					break;
				case eEvacPeakMins:
					evacPeak = Integer.parseInt(opts.get(opt));
					break;
				case Config.eGlobalStartHhMm:
					String[] tokens = opts.get(opt).split(":");
					evacStartHHMM = new int[]{Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1])};
					break;
				case Config.eGlobalRandomSeed:
					oRandomSeed = opts.get(opt);
					break;
				case ePlanSelectionPolicy:
					oPlanSelectionPolicy = opts.get(opt);
					break;
				case eAgents:
					oAgents = opts.get(opt);
					break;
				case eLogLevel:
					oLogLevel = opts.get(opt);
					break;
				case eLogFile:
					oLogFile = opts.get(opt);
					break;
				case eOutFile:
					oOutFile = opts.get(opt);
					break;
				case eNumThreads:
					oNumThreads = opts.get(opt);
					break;
				case eMetricsFile:
					oMetricsFile = opts.get(opt);
					break;
				case eMetricsFrequencyInSecs:
					oMetricsFrequencyInSecs = Integer.parseInt(opts.get(opt));
					break;
				default:
					logger.warn("Ignoring option: " + opt + "=" + opts.get(opt));
			}
		}
	}

	/**
	 * Replaces an empty jill agents config arg ie {@code agents:[]} with the appropriate
	 * config calculated using the given agents map. The result is an updated agents
	 * config arg like
	 * <pre>
	 * agents:[{classname:package.agentclass1, args:[...], count:n},...]
	 * </pre>
	 *
	 * @param jillargs
	 * @param map map of agent id to list of its init args
	 */
	private static void updateJillConfigFromAgentsMap(String[] jillargs, Map<Integer, List<String[]>> map) {
		if (map != null) {
			for (int i = 0; jillargs != null && i < jillargs.length; i++) {
				if (JillBDIModel.OPT_JILL_CONFIG.equals(jillargs[i]) && i < (jillargs.length-1)) {
					String agentsArg = (!map.isEmpty()) ?
							JillBDIModel.buildJillAgentsArgsFromAgentMap(map) :
							"agents:[{classname:io.github.agentsoz.ees.agents.bushfire.BushfireAgent, args:null, count:0}]";
					jillargs[i + 1] = jillargs[i + 1].replaceAll("agents:\\[]", agentsArg);
				}
			}
		}
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
    static String buildJillAgentsArgsFromAgentMap(Map<Integer, List<String[]>> map) {
        if (map == null) {
            return null;
        }

		// Count instances of each agent type
		Map<String,Integer> counts = new LinkedHashMap<>();

		for (String id : Utils.getAsSortedStringArray(map.keySet())) {
			List<String[]> values = map.get(Integer.valueOf(id));
            for (String[] val : values) {
                if (eBDIAgentType.equals(val[0])) {
                    String type = val[1];
                    int count = counts.containsKey(type) ? counts.get(type) : new Integer(0);
                    counts.put(type, count + 1);
                }
            }
        }

        StringBuilder arg = new StringBuilder();
        arg.append("[");
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

	/**
     * Filter out all but the BDI agents
     * @param map input map to filter from
     */
    public static void removeNonBdiAgentsFrom(Map<Integer, List<String[]>> map) {
        Iterator<Map.Entry<Integer, List<String[]>>> it = map.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Integer, List<String[]>> entry = it.next();
            boolean found = false;
            for (String[] val : entry.getValue()) {
                if (eBDIAgentType.equals(val[0])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                it.remove();
            }
        }
    }

	static Map<Integer, List<String>> getFlattenedArgsFromAgentsInitMap(Map<Integer, List<String[]>> agentsInitMap) {
        Map<Integer,List<String>> args = new HashMap<>();
        for (Integer id : agentsInitMap.keySet()) {
            List<String[]> values = agentsInitMap.get(id);
            List<String> flatlist = new ArrayList<>();
            for (String[] arr : values) {
                for (String val : arr) {
                    flatlist.add(val);
                }
            }
            args.put(id, flatlist);
        }
        return args;
    }

	public void registerDataServer(DataServer dataServer) {
		this.dataServer = dataServer;
	}

	@Override
	public void init(Object[] params)
	{
		dataServer.subscribe(this, Constants.TAKE_CONTROL_BDI);
		dataServer.subscribe(this, Constants.FIRE_ALERT);
		dataServer.subscribe(this, Constants.DIFFUSION);
		dataServer.subscribe(this, Constants.SOCIAL_NETWORK_MSG);

		logger.info("Initialising jill with args: " + Arrays.toString(initArgs));
		// Initialise the Jill model
		// params[] contains the list of agent names to create
		super.init(initArgs);
		// Initialise the alertPercepts
		int capacity = (params.length<1) ? 1 : params.length;
		alertPercepts = new PriorityQueue<TimedAlert>(capacity, new Comparator<TimedAlert>() {
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
		// Initialise agents with per-given args if available
		if (agentsInitMap != null) {
			Map<Integer, List<String>> args = JillBDIModel.getFlattenedArgsFromAgentsInitMap(agentsInitMap);
			initialiseAgentsWithArgs(args);
		}
		// Now create the given map to jill agent ids
		for (int i=0; i<params.length; i++) {
			String jillID = String.valueOf(((Agent)getAgent(i)).getId());
			mapMATsimToJillIds.put((String)params[i], jillID);
			mapJillToMATsimIds.put(jillID, (String)params[i]);
		}
	}

	@Override
	public void receiveData(double time, String dataType, Object data) {
		switch (dataType) {
			case Constants.TAKE_CONTROL_BDI:
			case Constants.FIRE_ALERT:
			case Constants.DIFFUSION:
			case Constants.SOCIAL_NETWORK_MSG:
				dataListeners.get(dataType).receiveData(time, dataType, data);
				break;
			default:
				throw new RuntimeException("Unknown data type received: " + dataType);
		}
	}

	public void setEvacuationTiming(int[] hhmm, int peak) {
		evacStartHHMM = hhmm;
		evacPeak = peak;
	}

	private void sendSocialNetworkMessagesToAgents(AgentDataContainer adc) {
//		for (Double msgTime : msgMap.keySet()) {
//			DiffusedContent content = msgMap.get(msgTime);
//			Map<String, String[]> msgs = content.getcontentSpreadMap();
//			for (String msg : msgs.keySet()) {
//				Set<String> messagedAgents = informedAgents.containsKey(msg) ? informedAgents.get(msg) : new HashSet<>();
//				String[] agents = msgs.get(msg);
//				for (String agent : agents) {
//					if (!messagedAgents.contains(agent)) {
//						String id = String.valueOf(agent);
//						adc.getOrCreate(id).getPerceptContainer().put(PerceptList.SOCIAL_NETWORK_MSG, msg);
//						messagedAgents.add(agent);
//						informedAgents.put(msg,messagedAgents);
//					}
//				}
//				logger.info("Total "+messagedAgents.size()+" agents received this message from social network:" + msg);
//				logger.info("Agents receiving message from social network are: {}",
//						Arrays.toString(messagedAgents.toArray()));
//
//			}
//		}
//		msgMap.clear();
	}

	private void translateToJillIds(AgentDataContainer adc) {
    // FIXME: The incoming IDs need to be changed to Jill IDs before the next call
  }

  private void translateToMATSimIds(AgentDataContainer adc) {
    // FIXME: The outgoing IDs need to be converted to MATSim IDs before returning
  }

	public void initialiseAgentsWithArgs(Map<Integer, List<String>> map) {
		if(map == null) return;
		for (Integer id : map.keySet()) {
			try {
				List<String> args = map.get(id);
				if (args != null && !args.isEmpty()) {
					String[] vargs = args.toArray(new String[args.size()]);
					getAgent(id).init(vargs);
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

	/**
	 * Creates a listener for each type of message we expect from the DataServer
	 * @return
	 */
	private Map<String, DataClient> createDataListeners() {
		Map<String, DataClient> listeners = new  HashMap<>();

		listeners.put(Constants.TAKE_CONTROL_BDI, (DataClient<io.github.agentsoz.bdiabm.v2.AgentDataContainer>) (time, dataType, data) -> {
			//takeControl(data);
			synchronized (getSequenceLock()) {
				getAgentDataContainer().clear();
				takeControl(time, data);
				recordMetrics(time);
				dataServer.publish(Constants.AGENT_DATA_CONTAINER_FROM_BDI, getAgentDataContainer());
			}
		});

		listeners.put(Constants.FIRE_ALERT, (DataClient<Double>) (time, dataType, data) -> {
			fireAlertTime = time;
		});

		listeners.put(Constants.DIFFUSION, (DataClient<Map<Double, DiffusedContent>>) (time, dataType, data) -> {
			msgMap = data;
		});

		listeners.put(Constants.SOCIAL_NETWORK_MSG, (DataClient<String[]>) (time, dataType, data) -> {
			logger.warn("Ignoring received data of type {}", dataType);
		});

		return listeners;
	}

	private void recordMetrics(double time) {
		int now = (int)time;
		if (oMetricsFile == null) {
			return;
		}
		lastTime = (lastTime == -1) ? now : lastTime;
		metricCountdown -= now - lastTime;
		lastTime = now;
		if (metricCountdown <= 0) {
			metricCountdown = oMetricsFrequencyInSecs;
			Map<Integer,MetricData> metric = metrics.getTimeData();

			// record metrics
			Map<Integer,AgentMetricData> agentsMetrics = new LinkedHashMap();
			Map<String,LinkMetricData> linksMetrics = new LinkedHashMap<>();
			for(String sid : mapJillToMATsimIds.keySet()) {
				int id = Integer.valueOf(sid);
				Agent thisAgent = (Agent)getAgent(Integer.valueOf(id));
				if (thisAgent instanceof ArchetypeAgent) {
					ArchetypeAgent agent = (ArchetypeAgent) getAgent(Integer.valueOf(id));
					String type = thisAgent.getClass().getSimpleName();
					Location[] fromTo = agent.getCurrentLocation();
					String linkId = fromTo[0].getName().split(":")[0];
					String status = agent.getCurrentStatus();
					String receivedMessages = agent.getReceivedMessages();
					agentsMetrics.put(id, new AgentMetricData(id, type, status, linkId, fromTo[0], fromTo[1], receivedMessages));

					String statusType = status.split(":")[0];
					LinkMetricData linkMetricData = linksMetrics.containsKey(linkId) ?
							linksMetrics.get(linkId) :
							new LinkMetricData(linkId, fromTo[0], fromTo[1]);
					switch (statusType) {
						case "at":
							linkMetricData.setAgentsInActivities(linkMetricData.getAgentsInActivities()+1);
							break;
						case "to":
							linkMetricData.setAgentsDriving(linkMetricData.getAgentsDriving()+1);
							break;
						default:
							// ignore the rest
							break;
					}
					linksMetrics.put(linkId, linkMetricData);
				}
			}
			// store combined metrics
			if (!agentsMetrics.isEmpty() || !linksMetrics.isEmpty()) {
				metric.put(now, new MetricData(agentsMetrics, linksMetrics));
			}
			// save to file
			writeMetrics(oMetricsFile);
		}
	}

	private void writeMetrics(String str) {
		try {
			Writer writer = str.endsWith(".gz") ?
					new BufferedWriter(
							new OutputStreamWriter(
									new GZIPOutputStream(
											new FileOutputStream(new File(str))), "UTF-8")) :
					Files.newBufferedWriter(Paths.get(str));
			Gson gson = new GsonBuilder()
					.setPrettyPrinting()
					.create();
			gson.toJson(metrics, writer);
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void useSequenceLock(Object sequenceLock) {
		this.sequenceLock = sequenceLock;
	}

	protected Object getSequenceLock() {
		return sequenceLock;
	}

	@Override
	public void finish() {
		super.finish();
		if (oMetricsFile != null) {
			// record metrics one last time at the end of the simulation
			metricCountdown = 0;
			recordMetrics(lastTime);
		}
	}

	class AgentMetricData {
		final private int agentId;
		//final private String agentType;
		final private String linkId;
		//final private Location nodeFrom;
		//final private Location nodeTo;
		final private String status;
		final private String messagesReceived;

		public AgentMetricData(int agentId,
							   String type,
							   String status,
							   String linkId,
							   Location nodeFrom,
							   Location nodeTo,
							   String messagesReceived) {
			this.agentId = agentId;
			//this.agentType = type; // TODO: disabled for SCS exercise on 14.Dec.21
			this.status = status;
			this.linkId = linkId;
			//this.nodeFrom = nodeFrom; // TODO: disabled for SCS exercise on 14.Dec.21
			//this.nodeTo = nodeTo; // TODO: disabled for SCS exercise on 14.Dec.21
			this.messagesReceived = messagesReceived;

		}
	}

	class LinkMetricData {
		final private String linkId;
		final private Location from;
		final private Location to;
		private int agentsDriving;
		private int agentsInActivities;

		public LinkMetricData(String linkId, Location from, Location to) {
			this.linkId = linkId;
			this.from = from;
			this.to = to;
		}

		public int getAgentsDriving() {
			return agentsDriving;
		}

		public void setAgentsDriving(int agentsDriving) {
			this.agentsDriving = agentsDriving;
		}

		public int getAgentsInActivities() {
			return agentsInActivities;
		}

		public void setAgentsInActivities(int agentsInActivities) {
			this.agentsInActivities = agentsInActivities;
		}
	}

	class MetricData {
		private final Map<Integer,AgentMetricData> agents;
		private final Map<String,LinkMetricData> links;

		public MetricData(Map<Integer,AgentMetricData> agents, Map<String,LinkMetricData> links) {
			this.agents = agents;
			this.links = links;
		}
	}

	class SimulationMetrics {
		private final String crs;
		Map<Integer,MetricData> timeData;

		public SimulationMetrics(String crs){
			this.crs = crs;
			timeData = new LinkedHashMap<>();
		}

		public Map<Integer, MetricData> getTimeData() {
			return timeData;
		}

		public void setTimeData(Map<Integer, MetricData> timeData) {
			this.timeData = timeData;
		}
	}
}
