package io.github.agentsoz.bushfire.matsimjill;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result;

public final class TestUtils {
	private TestUtils(){}  // do not instantiate

	public static void compareEventsWithSlack(SortedMap<Id<Person>, List<Double>> arrivalsExpected,
			SortedMap<Id<Person>, List<Double>> arrivalsActual, double slack) {
		Assert.assertEquals( arrivalsExpected.size(), arrivalsActual.size() ) ;
		Iterator<Entry<Id<Person>, List<Double>>> itActual = arrivalsActual.entrySet().iterator() ;
		Iterator<Entry<Id<Person>, List<Double>>> itExpected = arrivalsExpected.entrySet().iterator() ;
		double differencesSum = 0. ;
		long differencesCnt = 0 ;
		while ( itActual.hasNext() ) {
			Entry<Id<Person>, List<Double>> actual = itActual.next() ;
			Entry<Id<Person>, List<Double>> expected = itExpected.next() ;
			Assert.assertEquals( expected.getKey(), actual.getKey() );
			Assert.assertEquals( expected.getValue().size(), actual.getValue().size() ) ;
			Iterator<Double> itExpectedArrivals = expected.getValue().iterator() ; 
			Iterator<Double> itActualArrivals = actual.getValue().iterator() ;
			while ( itExpectedArrivals.hasNext() ) {
				double expectedArrival = itExpectedArrivals.next() ;
				double actualArrival = itActualArrivals.next() ;
				if ( expectedArrival != actualArrival ) {
					final double difference = actualArrival-expectedArrival;
					differencesSum += difference ;
					differencesCnt ++ ;
					System.err.println( "personId=" + expected.getKey()
					+ ";\texpectedTime=" + expectedArrival
					+ ";\tactualTime=" + actualArrival
					+ ";\tdifference=" + difference );
				}
				Assert.assertEquals(expectedArrival, actualArrival,slack);
			}
		}
		if ( differencesCnt > 0 ) {
			System.err.println( "differencesSum=" + differencesSum + ";\tdifferencesAv=" + differencesSum/differencesCnt );
		}
	}

	public static SortedMap<Id<Person>, List<Double>> collectArrivals(final String filename) {
		SortedMap<Id<Person>,List<Double>> actualArrivals = new TreeMap<>() ;
		EventsManager events = new EventsManagerImpl() ;
		events.addHandler(new PersonArrivalEventHandler(){
			@Override public void handleEvent(PersonArrivalEvent event) {
				Id<Person> personId = event.getPersonId() ;
				if ( !actualArrivals.containsKey(personId) ) {
					actualArrivals.put(personId, new ArrayList<Double>() ) ;
				}
				List<Double> list = actualArrivals.get( personId ) ;
				list.add( event.getTime() ) ;
			}
		});
		new MatsimEventsReader(events).readFile(filename);
		return actualArrivals ;
	}

	public static SortedMap<Id<Person>, List<Double>> collectDepartures(final String filename) {
		SortedMap<Id<Person>,List<Double>> actualDepartures = new TreeMap<>() ;
		EventsManager events = new EventsManagerImpl() ;
		events.addHandler(new PersonDepartureEventHandler(){
			@Override public void handleEvent(PersonDepartureEvent event) {
				Id<Person> personId = event.getPersonId() ;
				if ( !actualDepartures.containsKey(personId) ) {
					actualDepartures.put(personId, new ArrayList<Double>() ) ;
				}
				List<Double> list = actualDepartures.get( personId ) ;
				list.add( event.getTime() ) ;
			}
		});
		new MatsimEventsReader(events).readFile(filename);
		return actualDepartures ;
	}

	public static SortedMap<Id<Person>, List<Double>> collectEnterTraffic(final String filename) {
		SortedMap<Id<Person>,List<Double>> actualDepartures = new TreeMap<>() ;
		EventsManager events = new EventsManagerImpl() ;
		events.addHandler(new VehicleEntersTrafficEventHandler(){
			@Override public void handleEvent(VehicleEntersTrafficEvent event) {
				Id<Person> personId = event.getPersonId() ;
				if ( !actualDepartures.containsKey(personId) ) {
					actualDepartures.put(personId, new ArrayList<Double>() ) ;
				}
				List<Double> list = actualDepartures.get( personId ) ;
				list.add( event.getTime() ) ;
			}
		});
		new MatsimEventsReader(events).readFile(filename);
		return actualDepartures ;
	}

	public static List<Long> checkSeveralFiles(final String cmpFileName, String baseDir ) {
		List<Long> expecteds = new ArrayList<>() ;
	
		try {
			Files.walkFileTree(new File(baseDir).toPath(), new SimpleFileVisitor<Path>() {
				@Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					final String filename = dir + "/" + cmpFileName;
					if ( Files.exists( new File( filename).toPath() ) ) {
						System.err.println( "checking against " + filename );
						long crc = CRCChecksum.getCRCFromFile( filename ) ; 
						expecteds.add(crc) ;
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	
		return expecteds ;
	}

	public static void checkSeveral(List<Long> expecteds, long actualEvents) {
		long [] expectedsArray = new long[expecteds.size()] ;
		for ( int ii=0 ; ii<expecteds.size() ; ii++ ) {
			expectedsArray[ii] = expecteds.get(ii) ;
		}
		TestUtils.checkSeveral( expectedsArray, actualEvents ) ;
	}

	public static void checkSeveral(long[] expectedEvents, long actualEvents) {
		boolean found = false ;
		for ( int ii=0 ; ii<expectedEvents.length ; ii++ ) {
			final boolean b = actualEvents==expectedEvents[ii];
			System.err.println("checking if " + actualEvents + "==" + expectedEvents[ii] + " ? " + b);
			if ( b ) {
				found = true ;
				break ;
			}
		}
		if ( !found ) {
			Assert.fail(); 
		}
	}

	static void comparingArrivals(final String primaryExpectedEventsFilename, String actualEventsFilename) {
		System.err.println("Comparing arrivals:");
		SortedMap<Id<Person>, List<Double>> arrivalsExpected = 
				collectArrivals(primaryExpectedEventsFilename) ;
		SortedMap<Id<Person>, List<Double>> arrivalsActual = 
				collectArrivals(actualEventsFilename) ;
		compareEventsWithSlack(arrivalsExpected, arrivalsActual, 20.);
		System.err.println("Arrivals: Comparison with slack: passed.");
		System.err.println() ;
	
		Assert.assertEquals(arrivalsExpected, arrivalsActual);
		System.err.println("Arrivals: Exact comparison: passed.");
	}

	static void comparingEnterTraffic(final String primaryExpectedEventsFilename, String actualEventsFilename) {
		System.err.println("Comparing enterTraffic:");
		SortedMap<Id<Person>, List<Double>> enterTrafficExpected = 
				collectEnterTraffic(primaryExpectedEventsFilename) ;
		SortedMap<Id<Person>, List<Double>> enterTrafficActual = 
				collectEnterTraffic(actualEventsFilename) ;
		compareEventsWithSlack(enterTrafficExpected, enterTrafficActual, 20.);
		System.err.println("EnterTraffic: Comparison with slack: passed.");
		System.err.println() ;
	}

	static void comparingDepartures(final String primaryExpectedEventsFilename, String actualEventsFilename) {
		System.err.println("Comparing departures:");
		SortedMap<Id<Person>, List<Double>> departuresExpected = 
				collectDepartures(primaryExpectedEventsFilename) ;
		SortedMap<Id<Person>, List<Double>> departuresActual = 
				collectDepartures(actualEventsFilename) ;
		compareEventsWithSlack(departuresExpected, departuresActual, 20.);
		System.err.println("Departures: Comparison with slack: passed.");
		System.err.println() ;
	}

	static void compareFullEvents(final String primaryExpectedEventsFilename, String actualEventsFilename) {
		Result result = EventsFileComparator.compare(primaryExpectedEventsFilename, actualEventsFilename) ;
		System.err.println("Full events file: comparison: result=" + result.name() );
		Assert.assertEquals(Result.FILES_ARE_EQUAL, result);
	}

}
