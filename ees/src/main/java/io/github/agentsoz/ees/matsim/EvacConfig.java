package io.github.agentsoz.ees.matsim;

/*
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
