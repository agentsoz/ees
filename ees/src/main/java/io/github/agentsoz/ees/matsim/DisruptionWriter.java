package io.github.agentsoz.ees.matsim;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2024 EES code contributors.
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
			final String filename = config.controller().getOutputDirectory() + "/output_disruptionCoords.txt.gz";
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
