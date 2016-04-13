/* Station data structure 
   Saves the relevant information that a station would need to know
*/

import java.util.*;

public class Station {
	double cx; // xPixel around which station is formed
	double cy; // yPixel around which station is formed
	TreeSet<Taxi> dTrips; // list of departures from station sorted by departure time
	TreeSet<Taxi> aTrips;
	int intercountyDnodes; // number of intercounty nodes accounted for by a foreign pixel
	int intercountyAnodes;

	/* 	Initialize a station for the station optimization problem 
		Very specfic / unique conditions. Do not use for main optimization. */
	public Station(String pixel) {
		String[] coordinates = pixel.split(", ");
		cx = Double.parseDouble(coordinates[0].split("\"")[1]);
		cy = Double.parseDouble(coordinates[1].split("\"")[0]);
	}

	public Station(String pixel, boolean opt) {
		String[] coordinates = pixel.split(", ");

		if (opt) {
			cx = Double.parseDouble(coordinates[0]);
			cy = Double.parseDouble(coordinates[1]);
		}

		else {
			cx = Double.parseDouble(coordinates[0].split("\"")[1]);
			cy = Double.parseDouble(coordinates[1].split("\"")[0]);
		}
	}

	/* General intialization of an empty station */
	public Station(double x, double y) {
		cx = x;
		cy = y;

		dTrips = new TreeSet<Taxi>();
		aTrips = new TreeSet<Taxi>(new ArrivalComparator());

		intercountyDnodes = 0;
		intercountyAnodes = 0;
	}

	/* The first round in the optimization.
		Combines departure trip nodes as long as the max number of nodes in a trip doesn't exceed 3,
		and if departures are within 5 minutes of each other and within 20% max circuity.
		NOTE: at this point, simulation is dealing with an infinite fleet where every taxi only has one trip */
	public List<Station> initializeDepartures(List<Station> stations) {

		TreeMap<Double, List<Taxi>> deleted = new TreeMap<Double, List<Taxi>>();
		TreeSet<Taxi> added = new TreeSet<Taxi>();

		System.out.println(totalDepartureNodes() +"\t"+totalArrivalNodes());

		int count = 0;
		for (Taxi dTaxi : dTrips) {
			boolean optimized = false;

			Trip thisTrip = dTaxi.currentTrip();

			// only check dTaxis whose nodes are < 3
			if (thisTrip.nodes() == 3) continue;

			// delete all traces of dTaxi
			List<Taxi> taxis = null;
			if (deleted.containsKey(dTaxi.dTime()))
				taxis = deleted.get(dTaxi.dTime());
			else taxis = new ArrayList<Taxi>();
			taxis.add(dTaxi);
			deleted.put(dTaxi.dTime(), taxis);

			// get the taxis that are scheduled to depart before this one
			SortedSet<Taxi> list = dTrips.headSet(dTaxi, false);

			for (Taxi taxi : list) {
				// if taxi has been removed, skip
				if (deleted.containsKey(taxi.dTime()))
					if (deleted.get(taxi.dTime()).contains(taxi))
						continue;
				

				// only look at taxis within the 5 minutes before this one
				if (taxi.dTime() < (dTaxi.dTime() - 300)) continue;

				// attempt to combine initial trip nodes
				if (dTaxi.combineInitialTripNodes(taxi)) {
					count++;

					optimized = true;

					added.add(dTaxi);

					// delete all traces of combined taxi
					if (deleted.containsKey(taxi.dTime())) 
						taxis = deleted.get(taxi.dTime());
					else taxis = new ArrayList<Taxi>();
					taxis.add(taxi);
					deleted.put(taxi.dTime(), taxis);

					break;
				}
			}
			if (!optimized) 
				if (deleted.containsKey(dTaxi.dTime())) {
					taxis = deleted.get(dTaxi.dTime());
					if (!taxis.remove(dTaxi)) System.out.println("failure in deleted not optimized");
					if (taxis.isEmpty()) deleted.remove(dTaxi.dTime());
					else deleted.put(dTaxi.dTime(), taxis);

				}
		}

		System.out.println(dTrips.size());

		for (double time : deleted.keySet()) {
			List<Taxi> taxis = deleted.get(time);
			for (Taxi t : taxis) 
				stations = t.removeAllTraces(stations);
		}

		for (Taxi taxi : added){
			// update cStation
			if (!taxi.currentTrip().aCounty().equals(taxi.firstTrip().dCounty())) taxi.setCurrent(null);
			else taxi.setCurrent(taxi.currentTrip().findClosestCurrentStation(stations));
			stations = taxi.updateEverywhere(stations);
			if (taxi.totalTripNodes() > 3) System.out.println("happens in initializeDepartures");
		}

		System.out.println(count);
		System.out.println(dTrips.size());
		System.out.println(totalDepartureNodes() +"\t"+totalArrivalNodes());

		return stations;
	}


	/* The second round of optimization.
		Combines intercounty departure and arrival taxis, if departure comes within 10 minutes after an
		arrival and is headed for a pixel in the foreign county. If there is no optimization to be made,
		sends the empty arrival taxi to closest station in the foreign county */
	public List<Station> intercountyOptimization(List<Station> stations) {
		TreeSet<Taxi> deletedDepartures = new TreeSet<Taxi>();
		TreeMap<Taxi, Taxi> updatedTaxis = new TreeMap<Taxi, Taxi>(new ArrivalComparator());

		int count = 0;
		
		for (Taxi aTaxi : aTrips) {
			if (updatedTaxis.containsKey(aTaxi)) continue;
			if (aTaxi.oStation() != null) continue; // only deal with intercounty trips
			// int count = 0;

			boolean optimized = false; // keep track of whether taxi is optimized

			updatedTaxis.put(aTaxi, null);

			// get all departures within 10 minutes after arrival
			Taxi lowerbound = new Taxi();
			lowerbound.departureComparison(aTaxi.timeAvailable());
			SortedSet<Taxi> departures = dTrips.tailSet(lowerbound, true);

			for (Taxi dTaxi : departures) {
				if (deletedDepartures.contains(dTaxi)) continue;

				// once departures are more than 10 minutes after arrival, stop looking
				if (dTaxi.dTime() > aTaxi.timeAvailable()+600) break;

				if (dTaxi.cStation() != null) continue; // only deal with intercounty trips

				// get most recent trip of each taxi
				Trip dTrip = dTaxi.currentTrip();
				Trip aTrip = aTaxi.currentTrip();

				// // if the arrival county of departing trip is the same as the origin county of the arriving trip
				if (dTrip.aCounty().equals(aTrip.dCounty())) {

					// add dTaxi to list of taxis to be deleted
					deletedDepartures.add(dTaxi);

					// stations = dTaxi.removeAllTraces(stations);
					intercountyDnodes += dTaxi.totalTripNodes();
					intercountyAnodes += aTaxi.totalTripNodes();

					// update trips for this taxi and save to be updated at the end
					Taxi updated = new Taxi();
					updated.addTrips(aTaxi);
					updated.addTrips(dTaxi);
					updatedTaxis.put(aTaxi, updated);

					count++;

					break;							
				}

			}

			if (updatedTaxis.get(aTaxi) == null) updatedTaxis.remove(aTaxi);

		}


		for (Taxi taxi : updatedTaxis.keySet()){

			Taxi updated = updatedTaxis.get(taxi);

			if (updated == null) System.out.println("uh oh");

			stations = taxi.removeAllTraces(stations);
			if (updated == null) System.out.println("uh oh");

			stations = updated.updateEverywhere(stations);

			if (updated.numTrips() == 1 && updated.totalTripNodes() > 3) System.out.println("happens in intercountyOptimization - 1");

		}
		for (Taxi taxi : deletedDepartures) {
			
			stations = taxi.removeAllTraces(stations);

		}

		// DEAL WITH INTERCOUNTY DEPARTURES THAT HAVE NOT BEEN OPTIMIZED
		updatedTaxis = new TreeMap<Taxi, Taxi>();

		double emptyMiles = 0;
		for (Taxi taxi : dTrips) {
			if (taxi.cStation() == null) {

				// find closest station to last pixel
				Station closest = taxi.currentTrip().findClosestCurrentStation(stations);

				// save updated version
				Taxi updated = new Taxi();
				updated.addTrips(taxi); // add trips to taxi
				updated.sendTo(closest); // send to correct station
				updatedTaxis.put(taxi, updated);
				emptyMiles += updated.emptyMiles();
			}
		}

		for (Taxi taxi : updatedTaxis.keySet()){

			Taxi updated = updatedTaxis.get(taxi);

			if (updated == null) System.out.println("uh oh");
			stations = taxi.removeAllTraces(stations);
			if (updated == null) System.out.println("uh oh");

			stations = updated.updateEverywhere(stations);
			if (updated.numTrips() == 1 && updated.totalTripNodes() > 3) System.out.println("happens in intercountyOptimization - 2");

		}

		System.out.println(count);

		return stations;
	}

	/* The third round of optimization.
		Combines departure and arrival taxis if departure comes within 10 minutes after an arrival,
		and is headed for a pixel within the max circuity of the origin of the arrival taxi
		NOTE: for round 1, simulation is still technically an infinite fleet since every taxi has one trip */
	public List<Station> optimizeEmptyMiles(List<Station> stations) {
		
		TreeMap<Taxi, Taxi> updatedTaxis = new TreeMap<Taxi, Taxi>();
		TreeSet<Taxi> deletedArrivals = new TreeSet<Taxi>(new ArrivalComparator());

		for (Taxi dTaxi : dTrips) {

			if (updatedTaxis.containsKey(dTaxi)) continue;

			// keep track of second optimal station
			double closestDistance = Double.POSITIVE_INFINITY;
			Taxi nextOptimal = null;

			// we are not optimizing taxis that have already returned to the station
			// returned taxis will be optimized in the next round
			if (dTaxi.cStation() == dTaxi.oStation()) continue;

			// add taxi to list of taxis to be updated
			updatedTaxis.put(dTaxi, null);

			// get all the arrivals within 10 minutes before dTime
			Taxi upperbound = new Taxi();
			upperbound.arrivalComparison(dTaxi.dTime());

			SortedSet<Taxi> arrivals = aTrips.headSet(upperbound, true);

			for (Taxi aTaxi : arrivals) {

				// if arrival is not within 10 minutes of departure, move on
				if (aTaxi.timeAvailable() < dTaxi.dTime()-600) continue;

				// if already deleted, move on
				if (deletedArrivals.contains(aTaxi)) continue;

				// if the taxis are intracounty, and 
				// the oStation of the arrival taxi is the same as the cStation of the departure trip
				if (dTaxi.cStation().equals(aTaxi.oStation())) {
					deletedArrivals.add(aTaxi);

					Taxi updated = new Taxi();
					updated.addTrips(aTaxi);

					updatedTaxis.put(dTaxi, updated);

					break;

				}

				else if (dTaxi.cStation() == null) System.out.println("why do we still have intercounty trips??");

				else {
					// save distance between departure cStation and arrival oStation
					double distance = dTaxi.cStation().distanceTo(aTaxi.oStation());
					if (distance < closestDistance) {
						// save next optimal taxi
						nextOptimal = aTaxi;

						// update closest found distance
						closestDistance = distance;
					}
				}

			}

			// if there was no sufficient arrival, find the next optimal taxi
			if (updatedTaxis.get(dTaxi) == null) {

				if (nextOptimal != null) {
					double comparison = dTaxi.cStation().distanceTo(dTaxi.oStation());

					Taxi updated = new Taxi();
					updated.addTrips(nextOptimal);
					updatedTaxis.put(dTaxi, updated);

					deletedArrivals.add(nextOptimal);

				}

				else updatedTaxis.remove(dTaxi);
			}
		}

		for (Taxi taxi : updatedTaxis.keySet()) {
			Taxi updated = updatedTaxis.get(taxi);

			if (taxi.cStation() == taxi.oStation()) {
				System.out.println("round-trip taxi");
				System.out.println("Taxi trips: "+taxi.numTrips());
				System.out.println("Taxi nodes: "+taxi.totalTripNodes()+"\t"+taxi.currentTrip().nodes());
				System.out.println("Origin pixel: "+taxi.currentTrip().oPixel());
				System.out.println("Current pixel "+taxi.currentTrip().currentPixel());
				System.out.println();
			}

			if (updated == null) System.out.println("uh oh");
			stations = taxi.removeAllTraces(stations);
			if (updated == null) System.out.println("uh oh");
			if (taxi == null) System.out.println("where do we update???");
			updated.addTrips(taxi);

			stations = updated.updateEverywhere(stations);
		}

		for (Taxi taxi : deletedArrivals)
			stations = taxi.removeAllTraces(stations);

		return stations;
	}

	/* The third round of optimization. */
	public void cycleDepartures() {

		double emptyMiles = 0;

		// return any departures that haven't already been returned to the station
		for (Taxi taxi : dTrips) {

			if (!taxi.cStation().equals(taxi.oStation())) {
				taxi.sendTo(taxi.oStation());
			}
			emptyMiles += taxi.emptyMiles();
		}
		System.out.println(emptyMiles+" empty miles");

		System.out.println("---------");

		// create a copy of the departures, and another copy for the departures sorted by arrivals
		TreeSet<Taxi> departures = new TreeSet<Taxi>(dTrips);

		System.out.println(dTrips.size()+"\t"+totalDepartureNodes()+"\t"+totalArrivalNodes());

		for (Taxi dTaxi : departures) {
			if (!dTrips.contains(dTaxi)) continue;

			// sort departures by their time available
			List<Taxi> arrivals = new ArrayList<Taxi>(dTrips);
			Collections.sort(arrivals, new ArrivalComparator());

			// loop through the arrivals
			for (Taxi taxi : arrivals) {
				// make sure you don't optimize taxis with themselves
				if (taxi.equals(dTaxi)) continue;

				// once we get greater than the departure time, stop looking
				if (taxi.timeAvailable() > dTaxi.dTime()) break;

				if (!dTrips.contains(taxi)) continue;

				dTrips.remove(dTaxi);
				dTrips.remove(taxi);
				aTrips.remove(dTaxi);
				aTrips.remove(taxi);
				// dTaxi.removeAllTraces(stations)

				if (taxi == null || dTaxi == null) System.out.println("deleted all traces of the taxi");
				Taxi updated = new Taxi();
				updated.addTrips(taxi);
				updated.addTrips(dTaxi);

				dTrips.add(updated);

				break; // break once optimized
			}

		}

		System.out.println(dTrips.size()+"\t"+totalDepartureNodes()+"\t"+totalArrivalNodes());	
	}

	public boolean equals(Object obj) {
		// make sure the other object is a Station
		if (!(obj instanceof Station)) return false;
		Station that = (Station) obj;

		// stations are equal if centers are the same
		if (cx != that.cx) return false;
		if (cy != that.cy) return false;
	
		return true;		
	}

	public boolean addDeparture(Taxi taxi) {
		return dTrips.add(taxi);
	}

	public boolean addArrival(Taxi taxi) {
		return aTrips.add(taxi);
	}

	/* Returns distance to another station in miles */
	public Double distanceTo(Station that) {
		if (that == null) return Double.POSITIVE_INFINITY;

		// return 1.2 * the cartesian distance between the center of the two stations
		// NOTE: there is a scalar of one half because each pixel is 1/4 square miles
		return (1.2 * Math.sqrt(Math.pow(cx-that.cx, 2) + Math.pow(cy-that.cy, 2))/2);

	}

	/* Returns the distance to another pixel in miles */
	public double distanceTo(String pixel) {
		String[] coordinates = pixel.split(", ");
		double x = Double.parseDouble(coordinates[0]);
		double y = Double.parseDouble(coordinates[1]);		

		return (1.2 * Math.sqrt(Math.pow(cx-x, 2) + Math.pow(cy-y, 2))/2);
	}

	public String toString() {
		return (cx + ", " + cy);
	}

	/* Returns the total number of taxis that depart from this station */
	public int totalDepartures() {
		return dTrips.size();
	}

	/* Returns the total number of taxis that arrive to this station */
	public int totalArrivals() {
		return aTrips.size();
	}

	public double totalEmptyMiles() {
		double miles = 0;
		for (Taxi dTaxi : dTrips)
			miles += dTaxi.emptyMiles();

		return miles;
	}

	public double totalTripMiles() {
		double miles = 0;
		for (Taxi dTaxi : dTrips)
			miles += dTaxi.totalTripMiles();

		return miles;
	}

	public boolean removeArrival(Taxi aTaxi) {
		return aTrips.remove(aTaxi);
	}

	public boolean removeDeparture(Taxi dTaxi) {
		return dTrips.remove(dTaxi);
	}

	public int totalDepartureNodes() {
		int nodes = 0;
		for (Taxi dTaxi : dTrips) 
			nodes += dTaxi.totalTripNodes();

		return nodes;
	}

	public int totalArrivalNodes() {
		int nodes = 0;
		for (Taxi aTaxi : aTrips)
			nodes += aTaxi.totalTripNodes();
		return nodes;
	}

	private boolean withinRange(Station start, Station end) {
		if (start == null) return false;
		if (end == null) return false;

		// if the x-value of this station is less than the x-value of the endpoint
		if (start.cx <= end.cx) {
			if (cx < start.cx || cx > end.cx) return false;
		}
		// if the x-value of this station is greater than the x-value of the endpoint
		else {
			if (cx > start.cx || cx < end.cx) return false;
		}

		// if the y-value of this station is less than the y-value of the endpoint
		if (start.cy <= end.cy) {
			if (cy < start.cy || cy > end.cy) return false;
		}
		// if the x-value of this station is greater than the x-value of the endpoint
		else {
			if (cy > start.cy || cy < end.cy) return false;
		}

		return true;
	}

	static class ArrivalComparator implements Comparator<Taxi> {
		public int compare(Taxi t1, Taxi t2) {
			return t1.compareByArrivals(t2);
		}
	}
}