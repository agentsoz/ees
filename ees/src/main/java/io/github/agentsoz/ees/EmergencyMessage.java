package io.github.agentsoz.ees;

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
