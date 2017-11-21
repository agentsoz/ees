package io.github.agentsoz.nonmatsim;

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

public interface BDIActionHandler {
	
	public boolean handle(String agentID, String actionID, Object[] actionArgs); 
	// yyyy we have a tendency to pass "global" infrastructure (here: MATSim Model) rather through the constructor.  Reasons:
	// * The constructor is not part of the interface, so it is easier to change in specific implementations.
	// * People start putting something like "this.model = model" into the handle method in order to remember the global
	// object elsewhere in the handler class.  Then, however, it is more transparent to have it in the constructor right away.
	// kai, nov'17

}
