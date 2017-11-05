package io.github.agentsoz.bdimatsim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;

import com.google.inject.Module;

public class EvacPopulationPlugin extends AbstractQSimPlugin {

	public EvacPopulationPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(PopulationAgentSource.class).asEagerSingleton();
				bind(AgentFactory.class).to(EvacAgentFactory.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends AgentSource>> agentSources() {
		Collection<Class<? extends AgentSource>> result = new ArrayList<>();
		result.add(PopulationAgentSource.class);
		return result;
	}
}
