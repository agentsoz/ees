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

package io.github.agentsoz.ees.matsim;

/*
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 *  A Travel Cost Calculator that uses the travel times as travel disutility.
 *
 * @author cdobler
 */
public final class EvacTravelDisutility implements TravelDisutility {
	public static final String IN_FIRE_AREA = "inFireArea";

	private static final Logger log = LoggerFactory.getLogger(EvacTravelDisutility.class) ;

	private final Map<Id<Link>, Double> linksInFireArea;
	
	private final TravelTime travelTime;

	private EvacTravelDisutility(final TravelTime travelTime, Map<Id<Link>, Double> linksInFireArea) {
//		((ch.qos.logback.classic.Logger) log).setLevel(Level.DEBUG);
		this.linksInFireArea = linksInFireArea;
		Gbl.assertNotNull(travelTime);
		this.travelTime = travelTime;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double factor = 1. ;
		Double result = linksInFireArea.get(link.getId());;
		if ( result != null ) {
			factor = result ;
			log.debug("found link {} in fire area, set factor={}" , link.getId(), factor );
		}
		return factor * this.travelTime.getLinkTravelTime(link, time, person, vehicle);
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		double factor = 1. ;
		Double result = linksInFireArea.get(link.getId());;
		if ( result != null ) {
			log.debug("found link in fire area:" + link.getId() );
			factor = result ;
		}
		return factor * this.travelTime.getLinkTravelTime(link, Time.MIDNIGHT, null, null);
	}
	
	public static final class Factory implements TravelDisutilityFactory {
		private final Map<Id<Link>, Double> linksInFireArea;
		
		public Factory(Map<Id<Link>, Double> linksInFireArea ) {
			this.linksInFireArea = linksInFireArea ;
		}
		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
			return new EvacTravelDisutility(timeCalculator, linksInFireArea);
		}
	}
	
}
