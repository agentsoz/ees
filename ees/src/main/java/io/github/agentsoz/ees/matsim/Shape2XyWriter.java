package io.github.agentsoz.ees.matsim;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 EES code contributors.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
			final String filename = config.controller().getOutputDirectory() + "/output_" + name + "Coords.txt.gz";
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
