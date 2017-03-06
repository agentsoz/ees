package io.github.agentsoz.bdimatsim;

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * This class handles the config file that is passed as the first argument
 * and saves the information to be accessed by matSimModel and the AgentManager
 */
@Deprecated
public class MatsimParameterHandler{
	private static String [] NETWORKIDS;
	public static double DISTANCE_THREDSHOLD;
	public static double MAX_DURATION;
	public static int DISTANCE_CALCULATION_DURATION;
	public static int DURATION_REASONING;
	public static int BDI_DURATION;
	public static int BDI_TIME_FOR_JOB;
	public static int BDI_TIME_COUNT;
	public static int BDI_MULTIPLIER;
	public static int BDI_TOTAL_JOBS;
	public static int NUMBER_OF_BDI_AGENTS;
	
	 @SuppressWarnings("static-method")
	String[] getNETWORKIDS() {
		return NETWORKIDS;
	}

	@SuppressWarnings("static-method")
	void setNETWORKIDS(String[] nETWORKIDS) {
		NETWORKIDS = nETWORKIDS;
	}

	@SuppressWarnings("static-method")
	public int getNumberOfAgents(){
		return NUMBER_OF_BDI_AGENTS;
	}

	public Object matSimParameterQuery(String agentID) {
		if(agentID.compareTo("DURATION")==0)
		{
			return BDI_DURATION;
		}
		else if(agentID.compareTo("TIME_FOR_JOB")==0)
		{
			return BDI_TIME_FOR_JOB;
		}
		else if(agentID.compareTo("TIME_COUNT")==0)
		{
			return BDI_TIME_COUNT;
		}
		else if(agentID.compareTo("MULTIPLIER")==0)
		{
			return BDI_MULTIPLIER;
		}
		else if(agentID.compareTo("DISTANCE_THREDSHOLD")==0)
		{
		    return DISTANCE_THREDSHOLD;
		}
		else if(agentID.compareTo("MAX_DURATION")==0)
		{
			return MAX_DURATION;
		}
		else if(agentID.compareTo("DISTANCE_CALCULATION_DURATION")==0)
		{
			return DISTANCE_CALCULATION_DURATION;
		}
		else if(agentID.compareTo("DURATION_REASONING")==0)
		{
			return DURATION_REASONING;
		}
		else if(agentID.compareTo("NETWORK")==0)
		{
			return getNETWORKIDS();
		}
		return null;
	}

	public void readParameters(String file) {
		//This is terrible
		//C:\Users\QingyuChen\git\bdimatsimintegrationhonoursproject\HonoursProject\src\input\plans_hwh_2pct.xml
		BufferedReader br = null;
		 
		try {
 
			String string;
			//br = new BufferedReader(new FileReader("C:\\Users\\QingyuChen\\git\\bdimatsimintegrationhonoursproject\\HonoursProject\\src\\input\\simulation_parameters.txt"));
			br = new BufferedReader(new FileReader(file));
			
			while ((string = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(string,",");
				String parameter = st.nextToken();
				String valueSt = st.nextToken();
				//JOptionPane.showMessageDialog(null, "name: "+parameter+"  value: "+valueSt);
				int value = Integer.parseInt(valueSt);
				if(parameter.compareTo("DURATION")==0)
				{
					BDI_DURATION=value;
				}
				else if(parameter.compareTo("TIME_FOR_JOB")==0)
				{
					BDI_TIME_FOR_JOB=value;
				}
				else if(parameter.compareTo("TIME_COUNT")==0)
				{
					BDI_TIME_COUNT = value;
				}
				else if(parameter.compareTo("MULTIPLIER")==0)
				{
					BDI_MULTIPLIER = value;
				}
				else if(parameter.compareTo("DISTANCE_THREDSHOLD")==0)
				{
				    DISTANCE_THREDSHOLD=value;
				}
				else if(parameter.compareTo("MAX_DURATION")==0)
				{
					MAX_DURATION=value;
				}
				else if(parameter.compareTo("NUMBER_OF_AGENTS")==0)
				{
					NUMBER_OF_BDI_AGENTS=value;
				}
				else if(parameter.compareTo("DISTANCE_CALCULATION_DURATION")==0)
				{
					DISTANCE_CALCULATION_DURATION=value;
				}
				else
				{
					DURATION_REASONING=value;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
	}

}
