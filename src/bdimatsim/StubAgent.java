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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;
final class StubAgent implements MobsimDriverAgent{

	private Id<Link> currentLinkId;
	private Id<Person> agentId;
	private MobsimVehicle vehicle;
	
	StubAgent(Id<Link> link, Id<Person> agent, MobsimVehicle vehicle){
		this.vehicle = vehicle;
		currentLinkId = link;
		agentId = agent;
	}
	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return State.ACTIVITY;
	}

	@Override
	public double getActivityEndTime() {
		// TODO Auto-generated method stub
		return 3600*24;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStateToAbort(double now) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		return 0.0;
	}

	@Override
	public String getMode() {
		// TODO Auto-generated method stub
		return TransportMode.car;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		// TODO Auto-generated method stub
		return currentLinkId;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		// TODO Auto-generated method stub
		return currentLinkId;
	}

	@Override
	public Id<Person> getId() {
		// TODO Auto-generated method stub
		return agentId;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		// TODO Auto-generated method stub
		return currentLinkId;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MobsimVehicle getVehicle() {
		// TODO Auto-generated method stub
		return vehicle;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		// TODO Auto-generated method stub
		return vehicle.getId();
	}
	@Override
	public Double getExpectedTravelDistance() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
