package io.github.agentsoz.ees;

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
