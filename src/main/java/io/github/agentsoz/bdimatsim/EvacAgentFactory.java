/* *********************************************************************** *
 * project: org.matsim.*
 * TransitAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package io.github.agentsoz.bdimatsim;

import javax.inject.Inject;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.router.TripRouter;


public class EvacAgentFactory implements AgentFactory {

	private final Netsim simulation;
	@Inject TripRouter tripRouter ;

	@Inject
	public EvacAgentFactory(final Netsim simulation) {
		this.simulation = simulation;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(final Person p) {
//		MobsimDriverPassengerAgent agent = TransitAgent.createTransitAgent(p, this.simulation);
		return new EvacAgent( p.getSelectedPlan(), this.simulation, tripRouter );
	}

}
