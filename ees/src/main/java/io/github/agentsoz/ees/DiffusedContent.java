package io.github.agentsoz.ees;

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

import java.util.HashMap;

public class DiffusedContent {

    // contenttype, active agents IDs
    HashMap<String,String[]> oneStepSpreadMap;

    public DiffusedContent()
    {
        this.oneStepSpreadMap = new HashMap<String,String[]>();
    }

    public int getTotalDiffusionContents() {
        return oneStepSpreadMap.size();
    }

    public int getAdoptedAgentCountForContent(String c) {
        if (this.oneStepSpreadMap.containsKey(c)) {

            return this.oneStepSpreadMap.get(c).length;
        }
        else{
            return 0; // no agent has adopted the content
        }
    }
    public void setContentSpreadMap(HashMap<String,String[]> currentSpreadMap) {

        this.oneStepSpreadMap = currentSpreadMap;
    }

    public HashMap<String,String[]> getcontentSpreadMap() {
        return this.oneStepSpreadMap;
    }


}
