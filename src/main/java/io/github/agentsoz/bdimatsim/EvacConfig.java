package io.github.agentsoz.bdimatsim;

import org.matsim.core.config.ReflectiveConfigGroup;

public class EvacConfig extends ReflectiveConfigGroup{
	public static final String NAME="evac" ;
	private Setup setup;
	
	public EvacConfig() {
		super(NAME);
	}
	
	public static enum Setup { standard, blockage, fireArea }
	
	public void setSetup( Setup setup ) {
		this.setup = setup ;
	}
	public Setup getSetup() {
		return this.setup ;
	}
	
}
