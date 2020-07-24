package io.github.agentsoz.ees.agents.bushfire;

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


import io.github.agentsoz.ees.agents.archetype.ArchetypeAgent;
import io.github.agentsoz.jill.lang.AgentInfo;

import java.util.ArrayList;
import java.util.List;

@AgentInfo(hasGoals={
        "io.github.agentsoz.ees.agents.archetype.GoalFullResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalInitialResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalFinalResponse",
        "io.github.agentsoz.ees.agents.archetype.GoalGoto",
        "io.github.agentsoz.abmjill.genact.EnvironmentAction",
})
public class VisitorRegular extends ArchetypeAgent {

    private Prefix prefix = new VisitorRegular.Prefix();

    public VisitorRegular(String id) {
        super(id);
    }

    class Prefix{
        public String toString() {
            return String.format("%05.0f|%s|%s|%s|", getTime(), getTimeString(), VisitorRegular.class.getSimpleName(), getId());
        }
    }

    @Override
    public String logPrefix() {
        return prefix.toString();
    }

    @Override
    protected void parseArgs(String[] args) {
        List<String> baseArgs = new ArrayList<>();
        for (int i = 0; args!= null && i < args.length; i++) {
            String key = args[i];
            String value = null;
            if (i + 1 < args.length) {
                i++;
                value = args[i];
            }
            switch (key) {
                case "beach":
                case "shops":
                case "other":
                case "sharesInfoWithSocialNetwork":
                    // do something with these if needed
                    break;
                default:
                    // keep all other args for the base class
                    baseArgs.add(key);
                    if (value != null) {
                        baseArgs.add(value);
                    }
                    break;
            }
        }
        super.parseArgs(baseArgs.toArray(new String[0]));
    }

}
