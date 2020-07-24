package io.github.agentsoz.ees.matsim;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2020 by its authors. See AUTHORS file.
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;

class Shape2XyWriter {
	private static final Logger log = LoggerFactory.getLogger( Shape2XyWriter.class);
	
	private final Config config;
	private final String name;
	private PrintStream writer;
	Shape2XyWriter( Config config1, final String name ) {
		config = config1 ;
		this.name = name;
	}
	void write(double now, Geometry fire) {
		if ( writer == null) {
			final String filename = config.controler().getOutputDirectory() + "/output_" + name + "Coords.txt.gz";
			log.info("writing " + name + " data to " + filename);
			try {
				writer = IOUtils.getPrintStream(new File(filename).toURI().toURL());
				writer.println("time\tx\ty");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		// can't do this earlier since the output directories are not yet prepared by the controler.  kai, dec'17
		
		Envelope env = new Envelope();
		env.expandToInclude(fire.getEnvelopeInternal());
		double gridsize = 200.;
		boolean atLeastOnePixel = false;
		StringBuilder str = new StringBuilder();
		for (double yy = env.getMinY(); yy <= env.getMaxY(); yy += gridsize) {
			for (double xx = env.getMinX(); xx <= env.getMaxX(); xx += gridsize) {
				if (fire.contains(new GeometryFactory().createPoint(new Coordinate(xx, yy)))) {
					str.append(now);
					str.append("\t");
					str.append(xx);
					str.append("\t");
					str.append(yy);
					str.append("\n");
//					log.debug(str);
					atLeastOnePixel = true;
				}
			}
		}
		if(atLeastOnePixel) {
			writer.print(str);
		} else {
			final String strn = now + "\t" + fire.getCentroid().getX() + "\t" + fire.getCentroid().getY();
			writer.println(strn);
		}
	}
	
	public void close() {
		if ( writer != null ) {
			writer.close();
		}
	}
}
