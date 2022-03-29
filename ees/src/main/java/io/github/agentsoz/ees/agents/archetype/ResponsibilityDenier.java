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


import io.github.agentsoz.jill.lang.AgentInfo;

@AgentInfo(hasGoals={
        "io.github.agentsoz.ees.agents.archetype.GoalFullResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalInitialResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalFinalResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalGoto",
        "io.github.agentsoz.abmjill.genact.EnvironmentAction",
})
public class ResponsibilityDenier extends ArchetypeAgent {

    private Prefix prefix = new ResponsibilityDenier.Prefix();

    public ResponsibilityDenier(String id) {
        super(id);
    }

    class Prefix{
        public String toString() {
            return String.format("%05.0f|%s|%s|%s|", getTime(), getTimeString(), ResponsibilityDenier.class.getSimpleName(), getId());
        }
    }

    @Override
    public String logPrefix() {
        return prefix.toString();
    }


}
