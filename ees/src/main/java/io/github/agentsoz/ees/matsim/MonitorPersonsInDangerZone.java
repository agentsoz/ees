package io.github.agentsoz.ees.matsim;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
    private Set<Id<Link>> linksInCycloneBuffer = new HashSet<>();
    private Set<Id<Link>> linksInFloodBuffer = new HashSet<>();

    public MonitorPersonsInDangerZone(PAAgentManager agentManager) {
        this.agentManager = agentManager;
    }

    public void setFireZone(Set<Id<Link>> linksWithin) {
        linksInFireBuffer = linksWithin;
    }

    public void setEmbersZone(Set<Id<Link>> linksWithin) {
        linksInEmbersBuffer = linksWithin;
    }

    public void setCycloneZone(Set<Id<Link>> linksWithin) {
        linksInCycloneBuffer = linksWithin;
    }
    public void setFloodZone(Set<Id<Link>> linksWithin) {
        linksInFloodBuffer = linksWithin;
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
