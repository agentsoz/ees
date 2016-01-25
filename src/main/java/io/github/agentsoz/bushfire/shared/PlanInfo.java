package io.github.agentsoz.bushfire.shared;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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
import java.util.List;

/* This class carries information about plans and plan instances.
 * Currently used by the visualiser.
 * Raw types are used because JACK cannot compile with generics.
 */

public class PlanInfo {

	// Class for a single setting, if the plan allows manual control.
	// Includes available options and a default.
	static public class Setting {

		public String name;
		public String desc;
		public String[] options;
		public int defaultSetting;

		public Setting(String name) {
			this.name = name;
		}
	}

	public String planClass;
	public String name;
	public String desc;
	public List settings = new ArrayList();
	public String[] bdiTrace;

	public PlanInfo(String planClass, String name) {

		this.planClass = planClass;
		this.name = name;
	}
}