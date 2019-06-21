package io.github.agentsoz.ees.util;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Extends the original withinday controler listener so that 
 * the updated engine can be initialised here
 * @author QingyuChen
 *
 */
public final class Utils {
	private Utils(){} // do not instantiate. kai, mar'15

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(Utils.class);

    /**
     * Returns a map of agent IDs to specified attributes
     * Input should be of form given below:
     * <pre>{@code
     * <person id="2">
     *   <attributes>
     *     <attribute name="key" class="java.lang.String" >value</attribute>
     *   </attributes>
     *   <plan selected="yes">
     *     ...
     *   </plan>
     * </person>
     * }</pre>
     *
     * @param scenario MATSim scenario reference
     * @return the map or else an empty map if no BDI agent types were found
     */
    public static Map<String,List<String[]>> getAgentsFromMATSimPlansFile(Scenario scenario ) {
        Map<String,List<String[]>> map = new LinkedHashMap<>();
        for (Person person : scenario.getPopulation().getPersons().values()) {

			// Get all person attributes
			// Below seems the only ugly way to get attributes list out
			String args = person.getAttributes().toString();
			List<String[]> initArgs = new ArrayList<>();
			Pattern logEntry = Pattern.compile("\\{\\s*key\\s*=\\s*(.*?)\\s*;\\s*object\\s*=\\s*(.*?)\\s*\\}");
			Matcher matchPattern = logEntry.matcher(args);
			while(matchPattern.find()) {
				String key = matchPattern.group(1);
				String val = matchPattern.group(2);
				initArgs.add(new String[]{key,val});
			}

			// Also get all activities and locations
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					String type = ((Activity)element).getType();
					String xy = String.format("%f,%f", ((Activity)element).getCoord().getX(), ((Activity)element).getCoord().getY());
					initArgs.add(new String[]{type,xy});
				}
				element.toString();
			}

			map.put(person.getId().toString(), initArgs);
        }

        return map;
    }
	public static Map<String,List<String[]>> getAgentsFromMATSimPlansFile(String matsimConfigFile ) {
		Config config = ConfigUtils.loadConfig(matsimConfigFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return getAgentsFromMATSimPlansFile(scenario);

	}

	/**
	 * Deprecated. Use {@link #getAgentsFromMATSimPlansFile} instead.
	 * Example usage is in {@code examples/bushfire/scenarios/mount-alexander-shire/maldon-100-with-emergency-vehicles}
	 * @param scenario the MATSim scenario
	 * @return the list of BDI agents' IDs
	 */
	@Deprecated
    public static List<String> getBDIAgentIDs( Scenario scenario ) {
		// this goes through all matsim agents, ignores the stub agents, and returns as many of those agent ids as the
		// bdi module wants as bdi agents (the remaining agents will, I guess, remain normal matsim agents).  kai, mar'15

		List<String> bDIagentIDs = new ArrayList<>();

		Id<Person> stubId = Id.createPersonId("StubAgent");
		for ( Id<Person> id : scenario.getPopulation().getPersons().keySet() ) {
			if(id.compareTo(stubId) == 0){
				continue;
			}
			bDIagentIDs.add(id.toString());
		}
		
		// assume in popgen you did something like
		//     person.getAttributes().putAttribute("isBdiAgent", true/false ) ;
		// you could now retrieve it via
		//     person.getAttributes().getAttribute("isBdiAgent") ;
		// kai, dec'17
		
		return bDIagentIDs;
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

	public static void populationWithBDIAttributeExample() {
		{
			Config config = ConfigUtils.createConfig() ;

			Scenario scenario = ScenarioUtils.createScenario( config ) ;

			Population pop = scenario.getPopulation() ;
			PopulationFactory pf = pop.getFactory() ;

			for ( int ii=0 ; ii<10 ; ii++ ) {
				Person person = pf.createPerson( Id.createPersonId(ii)) ;
				person.getAttributes().putAttribute("isBDIAgent", true ) ;
				pop.addPerson(person);
			}

			PopulationUtils.writePopulation(pop, "pop.xml");
		}

		{
			Config config = ConfigUtils.createConfig() ;

			config.plans().setInputFile("pop.xml");

			Scenario scenario = ScenarioUtils.loadScenario( config ) ;

			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				final String attribute = (String) person.getAttributes().getAttribute("isBDIAgent");
				System.out.println( "id=" + person.getId() + "; isBdi=" + attribute ) ; 

			}


		}


	}
	
	public static void penaltyMethod2(Geometry fire, Geometry buffer, double bufferWidth,
									  Map<Id<Link>, Double> penaltyFactorsOfLinks, Scenario scenario) {
		// Starting thoughts:
		// * Could make everything very expensive in buffer + fire, they find the fastest path out.  This may, however,
		// be through the fire.
		// * Could make everything very expensive in fire, and factor of 10 less expensive in buffer.  Then they would
		// find fastest way out of fire, then fastest way out of buffer.
		// * Problem is that fastest way out of buffer may entail getting or remaining close to the fire.
		//
		// What about
		//
		// deltaHeight = (bufferWidth-distanceToFireToNode)^2 - (bufferWidth-distanceToFireFromNode)^2,
		//
		// e.g. fromNode close to fire = large second term, toNode close to outside = small first term,
		// deltaHeight negative (since we go downhill).
		//
		//  This should look like the Drakensberg, i.e.
		// * high and flat in the fire
		// * steep outside the fire but close ("distance ..." small)
		// * increasingly more flat farther away
		//
		// (Actually, the "Drakensberg" is quite misleading, since this is the shape of the 1st derivative.  The
		// actual slope thus is
		// * very high in the fire
		// * quickly becoming less steep outside the fire (which is to make "away and back in" cheaper than
		//    "in and back out")
		//
		// Now use deltaHeight, when positive, as penalty factor (times travel time).  This should penalize "wiggling"
		// close to the fire more than wiggling farther away.
		//
		// In the fire the penalty factor then is bufferWidth^2.
		//
		// Links where only one node is in the fire need to be treated as in the fire, otherwise we erode the
		// sharp edge.
		
		// yy in the below formulation, factor could be "zero".  Hedge against that (maybe
		// even in utility code).
		// yy Am just doing inLinks.  I think that that should be enough ...
		// yy Should be able to extract the "height" function ...
		
		for ( Node node : scenario.getNetwork().getNodes().values() ) {
			Point point = GeometryUtils.createGeotoolsPoint(node.getCoord());
			if (fire.contains(point)) {
				log.debug("node {} is IN fire area ", node.getId());
				// in links
				for (Link link : node.getInLinks().values()) {
					penaltyFactorsOfLinks.put( link.getId(), bufferWidth*bufferWidth) ;
				}
			} else if ( buffer.contains(point) ) {
				log.debug("node {} is IN buffer", node.getId());
				// in links
				for (Link link : node.getInLinks().values()) {
					Point fromPoint = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
					if (fire.contains(fromPoint)) { // coming from fire
						penaltyFactorsOfLinks.put(link.getId(), bufferWidth*bufferWidth); // treat as "in fire".
						// (yyyy probably too drastic; will avoid long links leading out of the fire)
					} else if (buffer.contains(fromPoint)) {
						final double heightAtFromNode = Math.pow(bufferWidth-fromPoint.distance(fire),2.) ;
						final double heightAtToNode = Math.pow( bufferWidth - point.distance(fire),2.) ;
						if ( heightAtToNode>heightAtFromNode) {
							penaltyFactorsOfLinks.put( link.getId(), heightAtToNode-heightAtFromNode ) ;
						}
					} else { // coming from out
						final double heightAtToNode = Math.pow( bufferWidth - point.distance(fire),2.) ;
						penaltyFactorsOfLinks.put(link.getId(), heightAtToNode ) ;
						// (height out will always be zero, and so height in will always be larger)
					}
				}
			}
		}
	}
	
	static void penaltyMethod1(Geometry fire, Geometry buffer,
							   Map<Id<Link>, Double> penaltyFactorsOfLinks, Scenario scenario) {
		// risk increasing are essentially forbidden:
		final double buffer2fire = 1000. ;
		final double out2buffer = 500 ;
		final double out2fire = out2buffer + buffer2fire ;
		final double buffer2bufferGettingCloser = 500. ;
		
		// risk reducing is ok, we just want to keep them short:
		final double fire2buffer = 20. ;
		final double fire2out = 10. ;
		final double buffer2out = 5. ;
		final double buffer2bufferGettingAway = 5. ;
		
		// in fire essentially forbidden:
		final double fire2fire = 1000. ;
		
		
		for ( Node node : scenario.getNetwork().getNodes().values() ) {
			Point point = GeometryUtils.createGeotoolsPoint(node.getCoord());
			if (fire.contains(point)) {
				log.debug("node {} is IN fire area " + node.getId());
				// outlinks:
				for (Link link : node.getOutLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());
					if (fire.contains(point2)) {
						penaltyFactorsOfLinks.put(link.getId(), fire2fire ); // fire --> fire
					} else if (buffer.contains(point2)) {
						penaltyFactorsOfLinks.put(link.getId(), fire2buffer ); // fire --> buffer
					} else {
						penaltyFactorsOfLinks.put(link.getId(), fire2out ); // fire --> out
					}
				}
				// in links
				for (Link link : node.getInLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());
					if (fire.contains(point2)) {
						penaltyFactorsOfLinks.put(link.getId(), fire2fire ); // fire <-- fire
					} else if (buffer.contains(point2)) {
						penaltyFactorsOfLinks.put(link.getId(), buffer2fire ); // fire <-- buffer
					} else {
						penaltyFactorsOfLinks.put(link.getId(), out2fire ); // fire <-- out
					}
				}
			} else if ( buffer.contains(point) ) {
				log.debug("node {} is IN buffer", node.getId());
				// outlinks:
				for (Link link : node.getOutLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getToNode().getCoord());
					if (fire.contains(point2)) {
						penaltyFactorsOfLinks.put(link.getId(), buffer2fire ); // buffer --> fire
					} else if (buffer.contains(point2)) {// buffer --> buffer
						final double distanceFromNode = point.distance(fire) ;
						final double distanceToNode = point2.distance(fire) ;
						if ( distanceToNode > distanceFromNode ) {
							penaltyFactorsOfLinks.put(link.getId(), buffer2bufferGettingAway);
						} else {
							penaltyFactorsOfLinks.put(link.getId(), buffer2bufferGettingCloser);
						}
					} else {
						penaltyFactorsOfLinks.put(link.getId(), buffer2out ); // buffer --> out
					}
				}
				// in links
				for (Link link : node.getInLinks().values()) {
					Point point2 = GeometryUtils.createGeotoolsPoint(link.getFromNode().getCoord());
					if (fire.contains(point2)) {
						penaltyFactorsOfLinks.put(link.getId(), fire2buffer); // buffer <-- fire
					} else if (buffer.contains(point2)) { // buffer <-- buffer
						final double distanceToNode = point.distance(fire) ;
						final double distanceFromNode = point2.distance(fire) ;
						if ( distanceToNode > distanceFromNode ) {
							penaltyFactorsOfLinks.put(link.getId(), buffer2bufferGettingAway);
						} else {
							penaltyFactorsOfLinks.put(link.getId(), buffer2bufferGettingCloser);
						}
					} else {
						penaltyFactorsOfLinks.put(link.getId(), out2buffer); // buffer <-- out
					}
				}
			}
		}
		// yyyy above is start, but one will need the full "potential" approach from Gregor. Otherwise, you cut a link
		// in two and get a different answer.  Which should not be. kai, dec'17
	}
}