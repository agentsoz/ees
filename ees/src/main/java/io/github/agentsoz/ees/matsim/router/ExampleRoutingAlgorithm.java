package io.github.agentsoz.ees.matsim.router;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
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
