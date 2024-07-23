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

import com.google.gson.Gson;

public class  Disruption {
    private String description;
    private String startHHMM;
    private String endHHMM;
    private double effectiveSpeed;
    private String effectiveSpeedUnit;
    private String[] impactedLinks;

    public Disruption(String description,
                      String startHHMM,
                      String endHHMM,
                      double effectiveSpeed,
                      String effectiveSpeedUnit,
                      String[] impactedLinks) {
        this.description = description;
        this.startHHMM = startHHMM;
        this.endHHMM = endHHMM;
        this.effectiveSpeed = effectiveSpeed;
        this.effectiveSpeedUnit = effectiveSpeedUnit;
        this.impactedLinks = impactedLinks;
    }

    public String getDescription() {
        return description;
    }

    public String getStartHHMM() {
        return startHHMM;
    }

    public String getEndHHMM() {
        return endHHMM;
    }

    public double getEffectiveSpeed() {
        return effectiveSpeed;
    }

    public String getEffectiveSpeedUnit() {
        return effectiveSpeedUnit;
    }

    public String[] getImpactLinks() {
        return impactedLinks;
    }

    @Override
    public String toString() {
        return (new Gson()).toJson(this);
    }
}
