package io.github.agentsoz.ees;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
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

import java.util.Map;
import java.util.TreeMap;

public class EmergencyMessage {

    // FIXME: superceded by Constants.EmergencyMessage, however used still for loading object from JSON; Dhi Aug/19
    public enum EmergencyMessageType {
        ADVICE(0.2),
        WATCH_AND_ACT(0.4),
        EMERGENCY_WARNING(0.5),
        EVACUATE_NOW(0.6);

        private final double value;

        private EmergencyMessageType(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    private EmergencyMessageType type;

    private String content;
    private String broadcastHHMM;
    private TreeMap<String,Double[][]> broadcastZones;

    public EmergencyMessage(EmergencyMessageType type,
                            String content,
                            String broadcastHHMM,
                            TreeMap<String,Double[][]> broadcastZones) {
        this.type = type;
        this.content = content;
        this.broadcastHHMM = broadcastHHMM;
        this.broadcastZones = broadcastZones;
    }

    public EmergencyMessageType getType() {
        return type;
    }

    public String getBroadcastHHMM() {
        return broadcastHHMM;
    }

    public TreeMap<String,Double[][]> getBroadcastZones() {
        return broadcastZones;
    }

    public String getContent() { return content; }

    public void setBroadcastZones(TreeMap<String, Double[][]> broadcastZones) {
        this.broadcastZones = broadcastZones;
    }

    @Override
    public String toString() {
        return (new Gson()).toJson(this);
    }
}
