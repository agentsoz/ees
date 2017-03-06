package io.github.agentsoz.bdimatsim.app;

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

import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;

import io.github.agentsoz.bdimatsim.MATSimActionHandler;
import io.github.agentsoz.bdimatsim.MATSimPerceptHandler;
import io.github.agentsoz.bdimatsim.Replanner;

public class StubPlugin implements MATSimApplicationInterface {

	@Override
	public void registerNewBDIActions(MATSimActionHandler withHandler) {
	}

	@Override
	public void registerNewBDIPercepts(MATSimPerceptHandler withHandler) {
	}

	@Override
	public void notifyBeforeCreatingBDICounterparts(List<Id<Person>> bdiAgentsIds) {
	}

	@Override
	public void notifyAfterCreatingBDICounterparts(List<Id<Person>> bdiAgentsIDs) {
	}

	@Override
	public Replanner getReplanner(ActivityEndRescheduler activityEndRescheduler) {
		return null;
	}

}
