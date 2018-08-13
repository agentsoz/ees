package io.github.agentsoz.ees.agents.bushfire;

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

import io.github.agentsoz.jill.lang.AgentInfo;

@AgentInfo(hasGoals={
        "io.github.agentsoz.abmjill.genact.EnvironmentAction",
        "io.github.agentsoz.ees.agents.bushfire.GoalInitialResponse",
        "io.github.agentsoz.ees.agents.bushfire.GoalActNow"})
public class VisitorDaytime extends BushfireAgent {

    private Prefix prefix = new VisitorDaytime.Prefix();

    public VisitorDaytime(String id) {
        super(id);
    }

    @Override
    public double getProbHomeAfterDependents() {
        return 0;
    }

    @Override
    void triggerResponse(MemoryEventValue breach) {
        log("triggerResponse("+breach+") not implemented yet");
    }

    class Prefix{
        public String toString() {
            return String.format("Time %05.0f VisitorDaytime %-3s : ", getTime(), getId());
        }
    }

    @Override
    String logPrefix() {
        return prefix.toString();
    }

}
