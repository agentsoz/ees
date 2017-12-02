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

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.vehicles.Vehicle;

/**
 * @author kainagel
 *
 */
public class EvacQSimModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(EvacQSimModule.class) ;
	
	@Inject Config config ;

	private final MATSimModel matSimModel;
	private EventsManager eventsManager;
	
	public EvacQSimModule(MATSimModel matSimModel, EventsManager eventsManager) {
//		log.setLevel(Level.DEBUG);
		this.matSimModel = matSimModel;
		this.eventsManager = eventsManager;
	}

	@Override
	protected void configure() {
//		bind(MATSimModel.class).toInstance(this.matSimModel);
		bind(Mobsim.class).toProvider(QSimProvider.class);
		if ( config.qsim().isUseLanes() ) {
			throw new RuntimeException("not yet implemented for this setup") ;
		}
		ConfigurableQNetworkFactory qNetworkFactory = new ConfigurableQNetworkFactory( eventsManager, matSimModel.getScenario() ) ;
		qNetworkFactory.setTurnAcceptanceLogic(new TurnAcceptanceLogic(){
			TurnAcceptanceLogic delegate = new DefaultTurnAcceptanceLogic() ;
			@Override
			public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh, QNetwork qNetwork) {
				
				AcceptTurn accept = delegate.isAcceptingTurn(currentLink, currentLane, nextLinkId, veh, qNetwork);
				
				QLinkI nextQLink = qNetwork.getNetsimLink(nextLinkId);
				double now = matSimModel.getTime();
				double speed = nextQLink.getLink().getFreespeed(now);
				if ( speed < 0.1 ) { // m/s
					accept = AcceptTurn.WAIT ;
					Id<Person> driverId = veh.getDriver().getId() ;
					Id<Link> currentLinkId = veh.getCurrentLink().getId() ;
					Id<Vehicle> vehicleId = veh.getId() ;
					Id<Link> blockedLinkId = nextLinkId ;
					eventsManager.processEvent(new NextLinkBlockedEvent( now, vehicleId,
																			   driverId, currentLinkId, blockedLinkId) );
				}
				log.debug("time=" + matSimModel.getTime() + ";\t fromLink=" + currentLink.getId() +
								  ";\ttoLink=" + nextLinkId + ";\tanswer=" + accept.name() ) ;
				return accept ;
			}
		});
		bind(QNetworkFactory.class).toInstance( qNetworkFactory ) ;
	}

	@SuppressWarnings("static-method")
	@Provides
	Collection<AbstractQSimPlugin> provideQSimPlugins(Config config1) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config1));
		plugins.add(new ActivityEnginePlugin(config1));
		plugins.add(new QNetsimEnginePlugin(config1));
		if (config1.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config1));
		}
		if (config1.transit().isUseTransit()) {
			plugins.add(new TransitEnginePlugin(config1));
		}
		plugins.add(new TeleportationPlugin(config1));
//		plugins.add(new PopulationPlugin(config1));
		plugins.add(new EvacPopulationPlugin(config1));
		plugins.add(new AbstractQSimPlugin(config1) {
			@Override public Collection<? extends Module> modules() {
				Collection<Module> result = new ArrayList<>();
				result.add(new com.google.inject.AbstractModule() {
					@Override protected void configure() {
						bind(Replanner.class).in( Singleton.class );
						bind(MATSimModel.class).toInstance( matSimModel ) ;
					}
				});
				return result;
			}
		}) ;
		return plugins;
	}
}
