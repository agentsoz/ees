package io.github.agentsoz.ees.matsim;

/*
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
