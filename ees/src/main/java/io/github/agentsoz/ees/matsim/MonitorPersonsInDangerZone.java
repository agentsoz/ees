package io.github.agentsoz.ees.matsim;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2020 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.nonmatsim.PAAgent;
import io.github.agentsoz.nonmatsim.PAAgentManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class MonitorPersonsInDangerZone implements LinkEnterEventHandler {

    private static final Logger log = LoggerFactory.getLogger(MonitorPersonsInDangerZone.class);

    private PAAgentManager agentManager;

    private Set<Id<Link>> linksInFireBuffer = new HashSet<>();
    private Set<Id<Link>> linksInEmbersBuffer = new HashSet<>();

    public MonitorPersonsInDangerZone(PAAgentManager agentManager) {
        this.agentManager = agentManager;
    }

    public void setFireZone(Set<Id<Link>> linksWithin) {
        linksInFireBuffer = linksWithin;
    }

    public void setEmbersZone(Set<Id<Link>> linksWithin) {
        linksInEmbersBuffer = linksWithin;
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        PAAgent agent = agentManager.getAgent(linkEnterEvent.getVehicleId().toString());
        if (agent != null) { // only do this if this is a BDI-like agent
            if(linksInFireBuffer.contains(linkEnterEvent.getLinkId())) {
                PerceptContent pc = new PerceptContent(Constants.FIELD_OF_VIEW, Constants.SIGHTED_FIRE);
                agentManager.getAgentDataContainerV2().putPercept(agent.getAgentID(), Constants.FIELD_OF_VIEW, pc);
            }
            if(linksInEmbersBuffer.contains(linkEnterEvent.getLinkId())) {
                PerceptContent pc = new PerceptContent(Constants.FIELD_OF_VIEW, Constants.SIGHTED_EMBERS);
                agentManager.getAgentDataContainerV2().putPercept(agent.getAgentID(), Constants.FIELD_OF_VIEW, pc);
            }
        }
    }
}
