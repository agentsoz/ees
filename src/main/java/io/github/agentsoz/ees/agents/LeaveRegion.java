package io.github.agentsoz.ees.agents;

/*-
 * #%L
 * Emergency Evacuation Simulator
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



import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.util.evac.ActionList;
import io.github.agentsoz.bdimatsim.MATSimModel.EvacRoutingMode;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.io.PrintStream;
import java.util.Map;

public class LeaveRegion extends Plan {

    private PrintStream writer = null;
    private Tourist tourist;

    public LeaveRegion(Agent agent, Goal goal, String name) {
        super(agent, goal, name);
        tourist = (Tourist)agent;
        this.writer = ((Tourist)agent).writer;
        body = steps;
    }

    public boolean context() {
        return true;
    }

    PlanStep[] steps = {
            new PlanStep() {
                public void step() {
                    String bdiAction = ActionList.DRIVETO;
                    Location freewayEntraceLocation = tourist.getFreewayEntraceLocation();
                    double[] coords = freewayEntraceLocation.getCoordinates();
                    double evacTime = tourist.getTime() + 5.0; // five secs from now
                    Object[] params = new Object[4];
                    params[0] = bdiAction;
                    params[1] = coords;
                    params[2] = evacTime;
                    params[3] = (tourist.getFailedAttempts() > 0) ? EvacRoutingMode.carGlobalInformation : EvacRoutingMode.carFreespeed;
                    // (could use EvacRoutingMode.carFreespeed.name() if you like that better. kai, dec'17)
                    writer.println(tourist.logPrefix() + "will start driving to freeway "+ freewayEntraceLocation + " at time " + evacTime);
                    post(new EnvironmentAction(
                            Integer.toString(tourist.getId()),
                            bdiAction, params));
                }
            },
            // Now wait till it is finished
            new PlanStep() {
                public void step() {
                    // Must suspend the agent when waiting for external stimuli
                    tourist.suspend(true);
                    // All done, when we return from the above call
                }
            },
            // NOTE:
            // Since Jill does not support multiple intentions yet, any repeated postings of the
            // RespondToFireAlert goal (if the agent gets a blocked percept and retries) will end up being treated as
            // sub-goals of this plan, i.e., recursive sub-goals.
            // Therefore any plan steps below this point will be executed AFTER returning from the recursive call,
            // and is probably not what we want/expect from the execution engine. For now, avoid adding anything
            // beyond this point.
            // dsingh, 5/dec/17
    };

    @Override
    public void setPlanVariables(Map<String, Object> vars) {
        // TODO Auto-generated method stub

    }
}
