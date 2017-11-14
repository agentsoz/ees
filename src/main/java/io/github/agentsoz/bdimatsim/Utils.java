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

import io.github.agentsoz.bdimatsim.moduleInterface.data.SimpleMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.GeometryUtils;

import com.vividsolutions.jts.geom.LineString;


/**
 * Extends the original withinday controler listener so that 
 * the updated engine can be initialised here
 * @author QingyuChen
 *
 */
public final class Utils {
	private Utils(){} // do not instantiate. kai, mar'15

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Utils.class);
	
	static List<Id<Person>> getBDIAgentIDs( Scenario scenario ) {
		// this goes through all matsim agents, ignores the stub agents, and returns as many of those agent ids as the
		// bdi module wants as bdi agents (the remaining agents will, I guess, remain normal matsim agents).  kai, mar'15
		
		ArrayList<Id<Person>> bDIagentIDs = new ArrayList<>();

		Id<Person> stubId = Id.createPersonId("StubAgent");
		for ( Id<Person> id : scenario.getPopulation().getPersons().keySet() ) {
			if(id.compareTo(stubId) == 0){
				continue;
			}
			bDIagentIDs.add(id);
		}
		return bDIagentIDs;
	}

	static void initialiseVisualisedAgents(MATSimModel matSimModel){
		Map<Id<Link>,? extends Link> links = matSimModel.getScenario().getNetwork().getLinks();
		for(MobsimAgent agent: matSimModel.getMobsimAgentMap().values()){
			SimpleMessage m = new SimpleMessage();
			m.name = "initAgent";
			//m.params = new Object[]{agent.getId().toString(),links.get(agent.getCurrentLinkId()).getFromNode().getId().toString(),(((MATSimReplannableAgent)agent).taxi == true?"T":"N")};
			m.params = new Object[]{//agent.getId().toString(),links.get(agent.getCurrentLinkId()).getFromNode().getId().toString(),"T"};
					agent.getId().toString(),
					links.get(agent.getCurrentLinkId()).getFromNode().getCoord().getX(),
					links.get(agent.getCurrentLinkId()).getFromNode().getCoord().getY()
			};
			matSimModel.addExternalEvent("initAgent",m);
		}
		//interfaceV.sendInitialAgentData(new ArrayList<MobsimAgent>());
	}

	final static double[]  computeBoundingBox(Collection<? extends Link> links) {
		double[] boundingBox0 = {Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY };
		for ( Link link : links ) {
			final double xx = link.getCoord().getX();
			final double yy = link.getCoord().getY();
	
			boundingBox0[0] = Math.min( boundingBox0[0],  xx ) ; 
			boundingBox0[1] = Math.min( boundingBox0[1],  yy ) ; 
			boundingBox0[2] = Math.max( boundingBox0[2],  xx ) ; 
			boundingBox0[3] = Math.max( boundingBox0[3],  yy ) ; 
		}
		return boundingBox0 ;
	}

	final static double[] generateLinkCoordsAsArray(Collection<? extends Link> links) {
		double[] array = new double[links.size()*2];
		int ii = -1 ;
		for ( Link link : links ) {
			ii++ ;
	
			final double xx = link.getCoord().getX();
			final double yy = link.getCoord().getY();
	
			array[ii*2] = xx ;
			array[ii*2+1] = yy;
		}
		return array ;
	}

	final static String[] createFlatLinkIDs(Collection<Id<Link>> allLinkIDs) {
		String[] flatLinkIDList = new String[allLinkIDs.size()];
		int j = 0;
		for(Id<Link> i :allLinkIDs){
			flatLinkIDList[j] = i.toString();
			j ++;
		}
		return flatLinkIDList;
	}

	 final static String[] getPersonIDsAsArray( List<Id<Person>> personIds ){
		List<Id<Person>> list = personIds ;
		String[] agentIDArray = new String[list.size()];
		int j = 0;
		for(Id<Person> id: list){
			agentIDArray[j] = id.toString();
			j ++;
		}
		return agentIDArray;
	}
	 
	 public static List<Link> findIntersectingLinks(Link link, Network network) {
		 return GeometryUtils.findIntersectingLinks(link, network) ;
	 }
	 public static List<Link> findIntersectingLinks(LineString lineString, Network network) {
		 return GeometryUtils.findIntersectingLinks(lineString, network) ;
	 }
	 public static Link getNearestLink(Network network, Coord coord) {
		 return NetworkUtils.getNearestLink(network, coord) ;
	 }
	 public static Link getNearestLinkExactly( Network network, Coord coord ) {
		 return NetworkUtils.getNearestLinkExactly(network, coord) ;
	 }
	 public static Node getNearestNode(Network network, Coord coord) {
		 return NetworkUtils.getNearestNode(network, coord) ;
	 }
	 public static Collection<Node> getNearestNodes(Network network, Coord coord, double distance) {
		 return NetworkUtils.getNearestNodes(network, coord, distance) ;
	 }
	 
	 
	 
	 
}