package io.github.agentsoz.ees.matsim.router;

/*-
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example router implementation.
 * author: dsingh
 */
public class ExampleRoutingAlgorithm implements LeastCostPathCalculator {

    private static final Logger log = LoggerFactory.getLogger(ExampleRoutingAlgorithm.class);

    private final TravelDisutility costFunction;
    private final TravelTime timeFunction;

    public ExampleRoutingAlgorithm(Network network, TravelDisutility travelDisutility, TravelTime travelTime) {
        this.costFunction = travelDisutility;
        this.timeFunction = travelTime;
    }

    @Override
    public Path calcLeastCostPath(Node node, Node node1, double v, Person person, Vehicle vehicle) {
        final String err = "Class io.github.agentsoz.ees.matsim.router.ExampleRoutingAlgorithm is not for real use!";
        log.error(err);
        throw new RuntimeException(err);
    }
}
