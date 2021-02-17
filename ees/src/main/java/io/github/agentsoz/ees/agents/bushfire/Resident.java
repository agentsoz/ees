package io.github.agentsoz.ees.agents.bushfire;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2021 by its authors. See AUTHORS file.
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
        "io.github.agentsoz.abmjill.genact.EnvironmentAction",
        "io.github.agentsoz.ees.agents.bushfire.GoalInitialResponse",
        "io.github.agentsoz.ees.agents.bushfire.GoalActNow",
        "io.github.agentsoz.ees.agents.bushfire.GoalGoHome",
        "io.github.agentsoz.ees.agents.bushfire.GoalGotoDependents",
        "io.github.agentsoz.ees.agents.bushfire.GoalGotoInvacPlace",
        "io.github.agentsoz.ees.agents.bushfire.GoalGotoEvacPlace"})
public class Resident extends BushfireAgent {

    private Prefix prefix = new Resident.Prefix();

    public Resident(String id) {
        super(id);
    }

    class Prefix{
        public String toString() {
            return String.format("Time %05.0f Resident %-9s : ", getTime(), getId());
        }
    }

    @Override
    public String logPrefix() {
        return prefix.toString();
    }


}
