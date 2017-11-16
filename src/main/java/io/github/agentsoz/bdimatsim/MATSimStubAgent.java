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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
final class MATSimStubAgent implements MobsimDriverAgent{

	private Id<Link> currentLinkId;
	private Id<Person> agentId;
	private MobsimVehicle vehicle;
	
	MATSimStubAgent(Id<Link> link, Id<Person> agent, MobsimVehicle vehicle){
		this.vehicle = vehicle;
		currentLinkId = link;
		agentId = agent;
	}
	@Override
	public State getState() {
		return State.ACTIVITY;
	}

	@Override
	public double getActivityEndTime() {
		return 3600*24;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
	}

	@Override
	public void endLegAndComputeNextState(double now) {
	}

	@Override
	public void setStateToAbort(double now) {
	}

	@Override
	public Double getExpectedTravelTime() {
		return 0.0;
	}

	@Override
	public String getMode() {
		return TransportMode.car;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return currentLinkId;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return currentLinkId;
	}

	@Override
	public Id<Person> getId() {
		return agentId;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return currentLinkId;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
	}

	@Override
	public MobsimVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return vehicle.getId();
	}
	@Override
	public Double getExpectedTravelDistance() {
		throw new RuntimeException("not implemented") ;
	}
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		throw new RuntimeException("not implemented") ;
	}
	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		return null;
	}

}
