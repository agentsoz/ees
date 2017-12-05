/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyTimeDependentTravelCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.agentsoz.bdimatsim.MATSimModel.FIRE_DATA_MSG;

/**
 *  A Travel Cost Calculator that uses the travel times as travel disutility.
 *
 * @author cdobler
 */
public final class EvacTravelDisutility implements TravelDisutility {
	public static final String IN_FIRE_AREA = "inFireArea";

	private static final Logger log = Logger.getLogger(EvacTravelDisutility.class);

	private final Set<Id<Link>> linksInFireArea;
	
	private final TravelTime travelTime;

	private EvacTravelDisutility(final TravelTime travelTime, Set<Id<Link>> linksInFireArea) {
		this.linksInFireArea = linksInFireArea;
		Gbl.assertNotNull(travelTime);
		this.travelTime = travelTime;
	}
	
	static boolean determineLinksInFireArea(String dataType, Object data, Config config, Set<Id<Link>> linksInFireArea, Scenario scenario) {
		if (FIRE_DATA_MSG.equals(dataType)) {
			CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(
					TransformationFactory.WGS84, config.global().getCoordinateSystem());
			final String json = new Gson().toJson(data);
			log.error("Received '{}', must do something with it\n{}"+
					dataType +json);
			log.error("class =" + data.getClass() ) ;

//			GeoJSONReader reader = new GeoJSONReader();
//			Geometry geometry = reader.read(json);
			// unfortunately does not work since the incoming data is not typed accordingly. kai, dec'17

			Map<Double, Double[][]> map = (Map<Double, Double[][]>) data;
			List<Polygon> polygons = new ArrayList<>() ;
			// the map key is time; we just take the superset of all polygons
			for ( Double[][] pairs : map.values() ) {
				List<Coord> coords = new ArrayList<>() ;
				for ( int ii=0 ; ii<pairs.length ; ii++ ) {
					Coord coordOrig = new Coord( pairs[ii][0], pairs[ii][1]) ;
					Coord coordTransformed = transform.transform(coordOrig) ;
					coords.add( coordTransformed ) ;
				}
				Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
				polygons.add(polygon) ;
			}
			
			linksInFireArea.clear();

			for ( Node node : scenario.getNetwork().getNodes().values() ) {
				Point point = GeometryUtils.createGeotoolsPoint( node.getCoord() ) ;
				for ( Polygon polygon : polygons ) {
					if (polygon.contains(point)) {
						log.warn("node {} is IN fire area"+ node.getId());
						for ( Link link : node.getOutLinks().values() ) {
							linksInFireArea.add( link.getId() ) ;
						}
						for ( Link link : node.getInLinks().values() ) {
							linksInFireArea.add( link.getId() ) ;
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double factor = 1. ;
		if ( linksInFireArea.contains( link.getId() ) ) {
			log.debug("found link in fire area:" + link.getId() );
			factor = 10. ;
		}
		return factor * this.travelTime.getLinkTravelTime(link, time, person, vehicle);
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		double factor = 1. ;
		if ( ((Customizable)link).getCustomAttributes().containsKey(IN_FIRE_AREA) ) {
			factor = 10. ;
		}
		return factor * this.travelTime.getLinkTravelTime(link, Time.UNDEFINED_TIME, null, null);
	}
	
	public static final class Factory implements TravelDisutilityFactory {
		private final Set<Id<Link>> linksInFireArea;
		
		public Factory(Set<Id<Link>> linksInFireArea ) {
			this.linksInFireArea = linksInFireArea ;
		}
		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
			return new EvacTravelDisutility(timeCalculator, linksInFireArea);
		}
	}
	
}