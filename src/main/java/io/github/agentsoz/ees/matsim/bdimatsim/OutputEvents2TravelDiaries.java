package io.github.agentsoz.bdimatsim;

/*-
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.travelsummary.events2traveldiaries.EventsToTravelDiaries;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.slf4j.LoggerFactory;

import java.io.IOException;
@Singleton
public class OutputEvents2TravelDiaries implements IterationStartsListener, IterationEndsListener {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(MATSimModel.class);

	EventsToTravelDiaries delegate;
	@Inject EventsManager eventsManager;
	@Inject Scenario scenario;

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() != scenario.getConfig().controler().getLastIteration())
			return;
		delegate = new EventsToTravelDiaries(scenario);
		eventsManager.addHandler(delegate);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() != scenario.getConfig().controler().getLastIteration())
			return;
		try {
			delegate.writeSimulationResultsToTabSeparated(scenario.getConfig().controler().getOutputDirectory(),"output_");
		} catch (IOException e) {
			log.error("Writing of traveldiaries failed.");
			e.printStackTrace();
		}
	}
}
