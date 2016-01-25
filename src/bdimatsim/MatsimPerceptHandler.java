package bdimatsim;

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
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * @author Edmund Kemsley
 * Processes percept/s from TaxiPerceptList by getting information from MATSim
 * and then returning as an Object array
 */

final class MatsimPerceptHandler {
	private final MATSimModel matSimModel;
	
	MatsimPerceptHandler(MATSimModel matSimModel) {
		this.matSimModel = matSimModel ;
	}
	
	final  Object[] processPercept(String agentID, String perceptID) {
		if (perceptID == MatsimPerceptList.ARRIVED){
			/*
			 * Returns all passed actions held in the MatsimAgent object as an array
			 * Designed to handle multiple arrivals in one percept
			 */
			MatsimAgent agent = this.matSimModel.getBDIAgent( Id.createPersonId(agentID));
			ArrayList<Id<Link>> passedActions = agent.getpassedDriveToActions();
			if (passedActions.isEmpty()){
				return null;
			}
			String[] array=new String[passedActions.size()];
			Iterator<Id<Link>> it = passedActions.iterator();
			int i=0;
			while (it.hasNext()){
				Id<Link> action = it.next();
				array[i++] = action.toString();
			}
			agent.clearPassedDriveToActions();
			return array;
		}
		if (perceptID == MatsimPerceptList.REQUESTLOCATION){
			/*
			 * At times BDI will need the current position of agent
			 * Returns the current link coordinates
			 */
			MobsimAgent agent = this.matSimModel.getMobsimAgentMap().get( Id.createPersonId(agentID));
			//Link currentLink = agentManager.getMatSimModel().getBdiMATSimWithinDayControllerSample().getWithinDayControlerListener().getScenario().getNetwork().getLinks().get(agent.getCurrentLinkId());
			Link currentLink = this.matSimModel.getScenario().getNetwork().getLinks().get( agent.getCurrentLinkId() ) ;
			
			return new Object[] {currentLink.getCoord().getX(),currentLink.getCoord().getY()};
		}
		else{
			return null;
		}
	}
}
