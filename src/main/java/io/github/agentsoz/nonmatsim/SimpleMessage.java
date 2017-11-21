package io.github.agentsoz.nonmatsim;

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

public class SimpleMessage {
	public static enum State{
		INITIAL,RUNNING,PASSED,FAILED,CANCELLED,STOPPED
	}
	public int ID;
	public String agentID;
	public String name;
	public Object[] params;
	public Object[] response;
	public State state;
	
	@Override
	public boolean equals(Object other){
		if(other instanceof SimpleMessage){
			return ID == ((SimpleMessage)other).ID;
		}
		return super.equals(other);
	}
	
	@Override
	public String toString() {
		String paramsS = "";
		for (Object object : params) {
			paramsS += object.toString() + ",";
		}
		
				
		return "agentID["+agentID+"] name["+name+"] state["+state+"] params[" + paramsS + "]";
	}
}