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

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.MobsimAgent;

import moduleInterface.data.SimpleMessage;

/**
 * @author Edmund Kemsley
 * Acts as a simple listener for Matsim agent events then passes to MatsimAgentManager
 */

final class AgentActivityEventHandler implements LinkEnterEventHandler, PersonArrivalEventHandler,
PersonDepartureEventHandler{

	private MATSimModel model;	
	private Network network;
	
	AgentActivityEventHandler(MATSimModel model)
	{
		this.model = model;
	}
	
	@Override
	public final void reset(int iteration) {
	
	}
	@Override
	public final void handleEvent (LinkEnterEvent event){
		if(network == null){
			network = model.getScenario().getNetwork();
		}
		
		//Update Visualiser
		SimpleMessage message = new SimpleMessage();
		message.name = "updateAgent";
		MobsimAgent agent = model.getMobsimAgentMap().get(event.getPersonId());
		//Sends message to visualiser containing agent ID and what lat and long to head to next
		message.params = new String[] 	{agent.getId().toString(),
										String.valueOf(((Link)network.getLinks().get(event.getLinkId())).getToNode().getCoord().getX()),
										String.valueOf(((Link)network.getLinks().get(event.getLinkId())).getToNode().getCoord().getY()),
										String.valueOf(network.getLinks().get(event.getLinkId()).getFreespeed())};
		model.addExternalEvent("moveAgent",message);
		
		//Notify Agent
		model.getAgentManager().arrivedAtNode(event.getPersonId(), event.getLinkId());
	}

	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		model.getAgentManager().departedNode(event.getPersonId(), event.getLinkId());
	}

	@Override
	public final void handleEvent(PersonArrivalEvent event) {
		model.getAgentManager().arrivedAtDestination(event.getPersonId(), event.getLinkId());
		
		//Previously commented out code. 
		/*Map<Id, PlanBasedWithinDayAgent> mapping= ((BDIMATSimWithinDayEngine) withinDayEngine).getControlerListener().getActivityReplanningMap().getPersonAgentMapping();
		Map<Id, MobsimAgent> mapping= model.getAgentMap();
		MobsimAgent a = mapping.get(event.getPersonId());
		if(a instanceof MATSimReplannableAgent){
			MATSimReplannableAgent agent = (MATSimReplannableAgent) a;
			if(agent.getState()==State.LEG)
			{
				//log.info("reset its links");

				BDIMATSimWithinDayEngine.allCachedLinkedIds.put(event.getPersonId(), null);
			}
		}*/
	}
}
