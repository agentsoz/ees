package io.github.agentsoz.ees.matsim;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2021 by its authors. See AUTHORS file.
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.PriorityQueue;
import java.util.Queue;

class DisruptionWriter {
	private static final Logger log = LoggerFactory.getLogger(DisruptionWriter.class);
	
	private final Config config;
	private PrintStream writer;
	
	private Queue<String> records = new PriorityQueue<>() ;
	
	DisruptionWriter(Config config1 ) {
		config = config1 ;
	}
	void write(double time, Id<Link> linkId, Coord coord, double speed ) {
		if (writer == null) {
			final String filename = config.controler().getOutputDirectory() + "/output_disruptionCoords.txt.gz";
			log.warn("writing disruption data to " + filename);
			try {
				writer = IOUtils.getPrintStream(new File(filename).toURI().toURL());
				writer.println("time\tlinkId\tx\ty\tnewSpeed");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		// can't do this earlier since the output directories are not yet prepared by the controler.  kai, dec'17
		// Since we are memorizing the records towards the very end, could as well just open and close the writer there.
		// kai, feb'18
		
		final String str = Time.writeTime(time) + "\t" + linkId + "\t" + coord.getX() + "\t" + coord.getY() + "\t" + speed ;
		// need time in hh:mm:ss since it is to be string-sorted based on that, and otherwise would need numerical sorting.
		// kai, feb'18
		log.info( "disruption data: " + str ) ;
		records.add( str ) ;
		// memorizing the entries towards the very end, since the "begin" and "end" disruption events come together, but I
		// want to have the output sorted by time. kai, feb'18
		
	}
	
	public void finish(double now) {
		if ( writer != null ) {
			while ( records.peek()!=null ) {
				String record = records.poll();
				String timeStr = record.substring(0, 8);
				log.info( "timeStr=" + timeStr ) ;
				String nowAsStr = Time.writeTime(now) ;
				if ( Time.parseTime(timeStr) > now ) {
					record.replace(timeStr,nowAsStr) ;
					// (clear disruptions at simulation end rather than at provided time.  Makes the VIA time range more meaningful.
					// Also, I need to clear them since otherwise the time-dep display does not work.  :-(  kai, feb'18)
				}
				writer.println( record );
			}
			writer.close();
		}
	}
}
