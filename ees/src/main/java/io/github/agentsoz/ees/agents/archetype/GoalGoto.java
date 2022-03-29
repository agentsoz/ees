package io.github.agentsoz.ees.agents.archetype;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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



import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.GoalInfo;
import io.github.agentsoz.util.Location;

@GoalInfo(hasPlans={
        "io.github.agentsoz.ees.agents.archetype.PlanGoto",
        "io.github.agentsoz.ees.agents.archetype.PlanDoNothing"
        })
public class GoalGoto extends Goal {
    private Location destination;
    private Constants.EvacActivity type;
    private double replanningActivityDurationInMins;

    public GoalGoto(String name, Location destination, Constants.EvacActivity type, double replanningActivityDurationInMins) {
        super(name);
        this.destination = destination;
        this.type = type;
        this.replanningActivityDurationInMins = replanningActivityDurationInMins;
    }

    public Location getDestination() {
        return destination;
    }

    public Constants.EvacActivity getDestinationType() {
        return type;
    }

    public double getReplanningActivityDurationInMins() {
        return replanningActivityDurationInMins;
    }
}
