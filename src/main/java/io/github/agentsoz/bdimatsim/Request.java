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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;

/**
 * @author kainagel
 *
 */
final class Request implements List<PlanElement> {
	static class Builder {
		static enum Routing { freespeed, currentCongestion } 
		static enum AllowedLinks { forCivilians, forEmergencyServices, all }
		private Routing routing = Routing.freespeed ;
		private AllowedLinks allowedLinks = AllowedLinks.all ;
		private PlanAgent planAgent;
		Builder( MobsimAgent agent ) {
			Gbl.assertIf( agent instanceof PlanAgent );
			planAgent = (PlanAgent) agent ;
		}
		public void setRouting(Routing routing) {
			this.routing = routing;
		}
		public void setAllowedLinks(AllowedLinks allowedLinks) {
			this.allowedLinks = allowedLinks;
		}
	}
	@Override public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator<PlanElement> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean add(PlanElement e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends PlanElement> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends PlanElement> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PlanElement get(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PlanElement set(int index, PlanElement element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(int index, PlanElement element) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PlanElement remove(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<PlanElement> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<PlanElement> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanElement> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
