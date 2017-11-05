/**
 * 
 */
package io.github.agentsoz.bdimatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

/**
 * @author kainagel
 *
 */
public class EvacActivityEnginePlugin extends AbstractQSimPlugin {

	public EvacActivityEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			public void configure() {
				bind(EvacActivityEngine.class).asEagerSingleton();
			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends ActivityHandler>> activityHandlers() {
		Collection<Class<? extends ActivityHandler>> result = new ArrayList<>();
		result.add(EvacActivityEngine.class);
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
		result.add(EvacActivityEngine.class);
		return result;
	}

}
