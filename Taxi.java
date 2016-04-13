/* Taxi data structure 
   Physically represents a taxi that would be needed to accomodate trips.
   Saves an itinerary for each taxi, such that all taxis combined service all trips that were generated
*/

import java.util.*;
import java.io.*;

public class Taxi implements Comparable<Taxi> {
	private int trips; // # of trips this taxi makes in a day
	private TripNode first; // first trip of the day
	private TripNode last; // last trip of the day
	private Station oStation; // origin station for this taxi
	private Station cStation; // current station at which taxi is located
	private Double dTime; // depart time of the first trip of the taxi
	private Double timeAvailable; // time taxi is available after dropping off last passenger

	private double emptyMiles; // empty miles burden this taxi endures

	private class TripNode {
		private Trip trip; // destination pixel of node
		private TripNode next; // link to the node that comes next
	}

	/* Initialize an empty taxi */
	public Taxi() {
		trips = 0;
		first = null;
		last = null;
		oStation = null;
		cStation = null;
		dTime = Double.POSITIVE_INFINITY;
		timeAvailable = Double.NEGATIVE_INFINITY;
		emptyMiles = 0;
	}

	/* Initialize a taxi with one trip */
	public Taxi(Trip trip, String countyname, List<Station> stations) {
		trips = 1;
		TripNode newTrip = new TripNode();
		newTrip.trip = trip;
		newTrip.next = null;

		first = newTrip;
		last = newTrip;

		oStation = null;
		cStation = null;

		dTime = trip.dTime();
		timeAvailable = trip.aTime();
		emptyMiles = 0;

	}

	/* Update taxi everywhere both at departing station and arriving station */
	public List<Station> updateEverywhere(List<Station> stations) {
		boolean success = true;

		// add departure from oStation if intracounty
		if (oStation != null) {
			Station station = stations.get(stations.indexOf(oStation));
			success = station.addDeparture(this);
			stations.set(stations.indexOf(station), station);
		}

		if (!success) {
			System.out.println("update departure failure");
			return null;
		}

		// add arrival to cStation if intracounty
		if (cStation != null) {
			Station station = stations.get(stations.indexOf(cStation));
			success = station.addArrival(this);
			stations.set(stations.indexOf(station), station);
		}

		if (!success) {
			System.out.println("update arrival failure");
			return null;
		}

		return stations;
	}

	/* Remove taxi from both departing and arriving station */
	public List<Station> removeAllTraces(List<Station> stations) {
		boolean success = true;

		// remove departure from oStation if intracounty
		if (oStation != null) {
			Station station = stations.get(stations.indexOf(oStation));
			success = station.removeDeparture(this);
			stations.set(stations.indexOf(station), station);
		}

		if (!success) {
			System.out.println("remove departure failure");
			// return null;
		}

		// remove arrival to cStation if intracounty
		if (cStation != null) {
			Station station = stations.get(stations.indexOf(cStation));
			success = station.removeArrival(this);
			stations.set(stations.indexOf(station), station);
			if (!success) 
				System.out.println("remove arrival failure");
		}

		if (!success) {
			System.out.println("remove arrival failure");
			// return null;
		}

		return stations;
	}

	/* Function that adds the trips of another taxi to this one */
	public boolean addTrips(Taxi that) {
		trips += that.trips; // update total # of trips
		emptyMiles += that.emptyMiles; // update total # of empty miles

		int totalNodes = totalTripNodes() + that.totalTripNodes();

		// if other taxi comes before this one
		if (that.timeAvailable <= dTime) {
			
			// update dTime
			dTime = that.dTime;

			// add taxi's trips to the beginning of this taxi's list of trips
			that.last.next = first;
			first = that.first;

			int count = 0;
			int nodes = 0;
			TripNode index = first;
			while (index != null) {
				count++;
				nodes += index.trip.nodes();
				index = index.next;
			}

			if (trips != count) 
				System.out.println("something wrong with adding trips in addTrips - 1");
			if (nodes != totalTripNodes()) 
				System.out.println("something wrong with nodes in addTrips - 1");
			if (nodes != totalNodes) 
				System.out.println("something wrong with adding nodes in addTrips - 1");

			// update oStation
			oStation = that.oStation;

			// if this is the first trip being added
			if (last == null) {
				last = that.last;
				timeAvailable = that.timeAvailable;
				cStation = that.cStation;
			}

			return true;
		}

		// if this taxi comes before other one
		if (that.dTime >= timeAvailable) {
			
			// update arrival time
			timeAvailable = that.timeAvailable;

			// add taxi's trips to the end of this taxi's trips
			if (last != null) {
				last.next = that.first;
			}
			last = that.last;
			

			int count = 0;
			int nodes = 0;
			TripNode index = first;
			while (index != null) {
				count++;
				nodes += index.trip.nodes();
				index = index.next;
			}

			if (trips != count) 
				System.out.println("something wrong with adding trips in addTrips - 2");
			if (nodes != totalTripNodes()) 
				System.out.println("something wrong with nodes in addTrips - 2");
			if (nodes != totalNodes) 
				System.out.println("something wrong with adding nodes in addTrips - 2");

			// update cStation
			cStation = that.cStation;

			return true;
		}
		System.out.println("add trips failure");
		return false;
	}

	/* Returns a taxi with the given time as the timeAvailable 
		NOTE: should only be called on empty taxis and for creating subsets
		of larger groups of arrival taxis */
	public void arrivalComparison(double time) {
		timeAvailable = time;
	}

	/* Returns a taxi with the given time as the dTime 
		NOTE: should only be called on empty taxis and for creating subsets
		of larger groups of departure taxis */
	public void departureComparison(double time) {
		dTime = time;
	}

	public int numTrips() {
		return trips;
	}

	/* Returns the total number of trip nodes covered by this taxi */
	public int totalTripNodes() {
		int nodes = 0;
		TripNode curr = first;
		// System.out.println("in total trip nodes");
		while (curr != null) {
			nodes += curr.trip.nodes();
			curr = curr.next;
		}
		return nodes;
	}

	public double totalTripMiles() {
		double miles = 0;

		TripNode curr = first;

		while (curr != null) {
			miles += curr.trip.vehMiles();
			curr = curr.next;
		}

		return miles;
	}

	public Station oStation() {
		return oStation;
	}

	public Station cStation() {
		return cStation;
	}
	public double dTime() {
		return dTime;
	}

	public Double timeAvailable() {
		return timeAvailable;
	}

	public double emptyMiles() {
		return emptyMiles;
	}

	public boolean isEmpty() {
		return trips == 0;
	}

	public Trip currentTrip() {
		return last.trip;
	}

	public Trip firstTrip() {
		return first.trip;
	}

	/* Helper function for the first round of optimization.
		Checks that taxis are compatible to be combined, before combining.
		If not compatible, returns false */
	public boolean combineInitialTripNodes(Taxi that) {
		
		// first check that combining nodes of trips won't exceed the maximum of 3
		if ((currentTrip().nodes() + that.currentTrip().nodes()) > 3) return false;

		// make sure adding this departure doesnt make the delay exceed 5 min
		if (((Math.abs(dTime - that.dTime) + currentTrip().delay()) > 300) || ((Math.abs(dTime - that.dTime)+that.currentTrip().delay()) > 300)) return false;

		// check that time to pick up passengers from both trips is <= difference in departTImes
		if ((new Station(currentTrip().oPixel(), true).distanceTo(new Station(that.currentTrip().oPixel(), true))* 3600 / 30) > Math.abs(dTime - that.dTime)) 
			return false;

		// check that the total riders <= 6
		if ((currentTrip().totalRiders() + that.currentTrip().totalRiders()) > 6) return false;

		return currentTrip().combineNodes(that.currentTrip());
	}

	public boolean equals(Object obj) {
		// make sure the other object is a Trip
		if (!(obj instanceof Taxi)) return false;
		Taxi that = (Taxi) obj;

		return (compareTo(that) == 0);
	}

	private int compareTrips(Taxi that) {
		int BEFORE = -1;
		int EQUAL = 0;
		int AFTER = 1;

		if (trips > that.trips) return AFTER;
		if (trips < that.trips) return BEFORE;

		if (first != null && that.first != null) {
			TripNode curr = first;
			TripNode tCurr = that.first;

			while (curr != null && tCurr != null) {
				if (!curr.trip.equals(tCurr.trip)) {
					Trip trip = curr.trip;
					Trip tTrip = tCurr.trip;

					if (trip.nodes().compareTo(tTrip.nodes()) != 0) return trip.nodes().compareTo(tTrip.nodes());
					if (trip.dTime().compareTo(tTrip.dTime()) != 0) return trip.dTime().compareTo(tTrip.dTime());
					if (trip.aTime().compareTo(tTrip.aTime()) != 0) return trip.aTime().compareTo(tTrip.aTime());
					if (trip.vehMiles().compareTo(tTrip.vehMiles()) == 0) return trip.vehMiles().compareTo(tTrip.vehMiles());
					if (((Integer)trip.totalRiders()).compareTo(tTrip.totalRiders()) != 0) 
						return ((Integer)trip.totalRiders()).compareTo(tTrip.totalRiders());

					// IF NECESSARY WE CAN CHECK EACH INDIVID NODE OF TRIP
					// COMPARE PIXEL DISTANCE AND NUM RIDERS
				}

				curr = curr.next;
				tCurr = tCurr.next;
			}

		}

		// if both sets of trips are emtpy or if all the trips are the same
		return EQUAL;

	}

	/* sets Taxi's natural ordering to be based on increasing dTime, followed by increasing time available */
	@Override public int compareTo(Taxi that) {
		final int BEFORE = -1;
    	final int EQUAL = 0;
    	final int AFTER = 1;

		// if (this.equals(that)) return EQUAL;
		if (dTime.compareTo(that.dTime) == 0) {
			if (timeAvailable.compareTo(that.timeAvailable) == 0) {
				
				// if both are departing from the same station and everything else is the same
				if (oStation != null && that.oStation != null && oStation.equals(that.oStation)) {

					// if they happen to be traveling the same distance, make sure they are not the same taxi
					if (((Double)oStation.distanceTo(last.trip.currentPixel())).compareTo(oStation.distanceTo(that.last.trip.currentPixel())) == 0) {
						return compareTrips(that);
					}
					else return ((Double)oStation.distanceTo(last.trip.currentPixel())).compareTo(oStation.distanceTo(that.last.trip.currentPixel()));
				}

				// if both are arriving to the same station and everything else is the same, sort by distance
				if (cStation != null && that.cStation != null && cStation.equals(that.cStation)) {

					// if they happen to be traveling the same distance, make sure they are not the same taxi
					if (((Double)cStation.distanceTo(first.trip.oPixel())).compareTo(cStation.distanceTo(first.trip.oPixel())) == 0) {
						return compareTrips(that);
					}
					return ((Double)cStation.distanceTo(first.trip.oPixel())).compareTo(cStation.distanceTo(that.first.trip.oPixel()));
				}

			}
			else return timeAvailable.compareTo(that.timeAvailable);				
		}
		else return dTime.compareTo(that.dTime);

		return EQUAL;
		
	}

	/* creates a compareTo method to compare arrival taxis - puts preference on arrival time instead of departure time */
	public int compareByArrivals(Taxi that) {

		if (timeAvailable.compareTo(that.timeAvailable) == 0) {
			return compareTo(that);			
		}
		else return timeAvailable.compareTo(that.timeAvailable);

	}

	/* set the taxi's base station */
	public void setBase(Station station) {
		oStation = station;
	}

	/* Send the taxi, empty, to given station */
	public void sendTo(Station station) {
		// update cStation
		cStation = station;

		// update empty vehicle miles
		if (last == null) System.out.println("last is null");
		else if (last.trip == null) System.out.println("trip is null");

		emptyMiles += station.distanceTo(last.trip.currentPixel());

		// update timeAvailable
		timeAvailable += (station.distanceTo(last.trip.currentPixel()) * 3600 / 30);
	}

	/* set the taxi's current station */
	public void setCurrent(Station station) {
		cStation = station;
	}
}