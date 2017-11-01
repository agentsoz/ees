package io.github.agentsoz.bushfire.jill.goals;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2016 by its authors. See AUTHORS file.
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

import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.GoalInfo;

/**
 * 
 * @author Sewwandi Perera
 *
 */
@GoalInfo(hasPlans = { "agentsoz.bushfire.jill.plans.CheckPlan" })
public class CheckplanGoal extends Goal {

	private String regionName;

	public CheckplanGoal(String str) {
		super(str);
	}

	public CheckplanGoal(String str, String region) {
		super(str);
		this.regionName = region;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regingName) {
		this.regionName = regingName;
	}
}
