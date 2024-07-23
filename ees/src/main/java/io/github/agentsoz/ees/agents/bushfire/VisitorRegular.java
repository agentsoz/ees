package io.github.agentsoz.ees.agents.bushfire;

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
