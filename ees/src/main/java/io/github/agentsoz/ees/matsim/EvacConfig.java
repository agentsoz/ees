package io.github.agentsoz.ees.matsim;

/*
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

import org.matsim.core.config.ReflectiveConfigGroup;

public class EvacConfig extends ReflectiveConfigGroup{
	public static final String NAME="evac" ;
	public static final String SETUP_INDICATOR="--setup" ;

	private Setup setup = Setup.standard ;
	private double congestionEvaluationInterval; // secs between BDI agent evaluatiing if it is in congestion
	private double congestionToleranceThreshold; // as a proportion of the congestionEvaluationInterval
	private double congestionReactionProbability; // likelihood that agent will react to congestion


	public EvacConfig() {
		super(NAME);
	}
	
	public static enum Setup { standard, blockage, tertiaryRoadsCorrection }
	
	public void setSetup( Setup setup ) {
		this.setup = setup ;
	}
	public Setup getSetup() {
		return this.setup ;
	}

	public double getCongestionEvaluationInterval() {
		return congestionEvaluationInterval;
	}

	public void setCongestionEvaluationInterval(double congestionEvaluationInterval) {
		this.congestionEvaluationInterval = congestionEvaluationInterval;
	}

	public double getCongestionToleranceThreshold() {
		return congestionToleranceThreshold;
	}

	public void setCongestionToleranceThreshold(double congestionToleranceThreshold) {
		this.congestionToleranceThreshold = congestionToleranceThreshold;
	}

	public double getCongestionReactionProbability() {
		return congestionReactionProbability;
	}

	public void setCongestionReactionProbability(double congestionReactionProbability) {
		this.congestionReactionProbability = congestionReactionProbability;
	}
}
