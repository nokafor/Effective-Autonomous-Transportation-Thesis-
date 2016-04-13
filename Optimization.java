import java.util.*;
import java.io.*;

/* 	Program that runs the optimization for a county using the created stations and all the trip files.
	Must be run after the stations algorithm, in order to work properly. */
public class Optimization {
	static List<Station> stations; // list of stations for given county

	public static void main(String[] args) {

		// Arguments should be county abbreviation in all lowercase and number of departure files for the county
		// (NOTE: number of departure and arrival files should be the same)
		// Departure files should be in a folder named "Departures". 
		// Arrival files should be in folder named "Arrivals".
		// Station file should be in folder named "Stations".

		if (args.length != 2) {
			System.out.println("Must have two arguments: [file name] [number of departure files]");
			return;
		}

		String countyname = args[0];
		int numFiles = Integer.parseInt(args[1]);
		List<Trip> departures = new ArrayList<Trip>();
		List<Trip> arrivals = new ArrayList<Trip>();
		

		System.out.println("\nCounty being analyzed: " + countyname.toUpperCase());

		long startTime = System.currentTimeMillis(); // start keeping track of run time

		// Get all the created stations for given county
		System.out.println("Getting Station Info\n=============");
		String filename = "Stations/" + countyname + ".txt";
		stations = new ArrayList<Station>();
		getStations(filename);
		System.out.println();


		// Save relevant departures from county
		System.out.println("Getting Departure Info\n=============");
		if (numFiles == 1) { // if there is only one departure file
			filename = "Departures/" + countyname + ".csv";
			departures = getTrips(filename, countyname, departures);
		}
		else { // otherwise
			for (int i = 1; i <= numFiles; i++) {
				filename = "Departures/" + countyname + i + ".csv";
				departures = getTrips(filename, countyname, departures);
			}
		}
		System.out.println();

		int totalDepartures1 = 0;
		int totalArrivals1 = 0;	

		for (Station station : stations) {
			totalDepartures1 += station.totalDepartures();
			totalArrivals1 += station.totalArrivals();
		}

		System.out.println("Departures accounted for: " + totalDepartures1);
		System.out.println("Arrivals accounted for: " + totalArrivals1);
		// System.out.println("Sanity Check\n=================");
		// System.out.println("Departures: " + departures.size());
		// System.out.println("Arrivals: " + arrivals.size());
		System.out.println();




		// Save relevant arrivals from county
		System.out.println("Getting Arrival Info\n=============");
		if (numFiles == 1) { // if there is only one departure file
			filename = "Arrivals/A" + countyname + ".csv";
			arrivals = getTrips(filename, countyname, arrivals);
		}
		else { // otherwise
			for (int i = 1; i <= numFiles; i++) {
				filename = "Arrivals/A" + countyname + i + ".csv";
				arrivals = getTrips(filename, countyname, arrivals);
			}
		}
		System.out.println();

		int totalDepartures2 = 0;
		int totalArrivals2 = 0;	

		for (Station station : stations) {
			totalDepartures2 += station.totalDepartures();
			totalArrivals2 += station.totalArrivals();
		}

		System.out.println("Departures accounted for: " + totalDepartures2);
		System.out.println("Arrivals accounted for: " + totalArrivals2);
		System.out.println();

		int initialDepartureNodes = 0;
		int initialArrivalNodes = 0;
		for (Station station : stations) {
			initialDepartureNodes += station.totalDepartureNodes();
			initialArrivalNodes += station.totalArrivalNodes();
		}


		/* Print Summary */
		System.out.println(countyname.toUpperCase() + " Checkpoint 1\n=============");
		System.out.println("Total stations created: " + stations.size());
		System.out.println("Initial number of departures to be analyzed: " + departures.size());
		System.out.println("Initial number of arrivals  to be analyzed: " + arrivals.size());
		System.out.println("Initial number of departure nodes: " + initialDepartureNodes);
		System.out.println("Initial number of arrival nodes: " + initialArrivalNodes);
		System.out.println();


		long startTime1 = System.currentTimeMillis(); // start keeping track of run time





		/* Initialize Departures */
		// System.out.println("Initializing Departures\n=========================");

		int previousTotal = departures.size();
		int difference = 0;
		int round = 1;

		Collections.sort(stations, Collections.reverseOrder(new StationComparator())); // sort stations by number of departures
		Object[] stationsCopy = stations.toArray(); // create shallow copy of stations
		int N = stationsCopy.length;
		int totalDepartureTaxis = 0;
		int currentDepartureNodes = 0;
		int currentArrivalNodes = 0;

		do {
			System.out.println("Initializing Departures: Round " + round+ "\n=========================");
			totalDepartureTaxis = 0;
			currentDepartureNodes = 0;
			currentArrivalNodes = 0;

			for (int i = 0; i < N; i++) {
				Station current = stations.get(stations.indexOf((Station)stationsCopy[i]));
				stations = current.initializeDepartures(stations);
				System.out.println("Station " + (i+1) + " complete");
				totalDepartureTaxis += current.totalDepartures();
				currentDepartureNodes += current.totalDepartureNodes();
				currentArrivalNodes += current.totalArrivalNodes();
			}

			difference = previousTotal - totalDepartureTaxis;
			System.out.println(difference);
			previousTotal = totalDepartureTaxis;
			round++;
			System.out.println();

		} while (difference > 0);

		// long endTime1 = System.currentTimeMillis(); // start keeping track of run time

		// System.out.println((endTime1 - startTime1)/1000);

		/* Print Summary */
		System.out.println(countyname.toUpperCase() + " Checkpoint 2\n=============");
		System.out.println("Total stations analyzed: " + stations.size());
		System.out.println("Original number of taxis: " + departures.size());
		System.out.println("Current total number of taxis: " + totalDepartureTaxis);
		System.out.println("Initial number of departure nodes: " + initialDepartureNodes);
		System.out.println("Current number of departure nodes: " + currentDepartureNodes);
		System.out.println("Initial number of arrival nodes: " + initialArrivalNodes);
		System.out.println("Current number of arrival nodes: " + currentArrivalNodes);
		System.out.println();


		int previousTotalNodes = currentDepartureNodes + currentArrivalNodes;





		/* Deal with intercounty trips, and reducing the empty mile burden relative to them 
			NOTE: still dealing with infinite fleet at this point */
		System.out.println("Optimizing Intercounty Departures\n=========================");
		Collections.sort(stations, Collections.reverseOrder(new StationComparator())); // sort stations by number of departures
		stationsCopy = stations.toArray();
		totalDepartureTaxis = 0;
		currentDepartureNodes = 0;
		currentArrivalNodes = 0;
		double totalEmptyMiles = 0;

		for (int i = 0; i < N; i++) {
			Station current = stations.get(stations.indexOf((Station)stationsCopy[i]));
			stations = current.intercountyOptimization(stations);
			System.out.println("Station " + (i+1) + " complete");
			totalDepartureTaxis += current.totalDepartures();
			currentDepartureNodes += current.totalDepartureNodes();
			currentArrivalNodes += current.totalArrivalNodes();
			totalEmptyMiles += current.totalEmptyMiles();
		}
		System.out.println();

		/* Print Summary */
		System.out.println(countyname.toUpperCase() + " Checkpoint 3\n=============");
		System.out.println("Total stations analyzed: " + stations.size());
		System.out.println("Original number of taxis: " + departures.size());
		System.out.println("Current total number of taxis: " + totalDepartureTaxis);
		System.out.println("Initial number of departure nodes: " + initialDepartureNodes);
		System.out.println("Current number of departure nodes: " + currentDepartureNodes);
		System.out.println("Initial number of arrival nodes: " + initialArrivalNodes);
		System.out.println("Current number of arrival nodes: " + currentArrivalNodes);
		System.out.println("Previous number of total nodes: " + previousTotalNodes);
		System.out.println("Current number of total nodes: "+ (currentDepartureNodes+currentArrivalNodes));
		System.out.println("Current total number of empty taxi miles: "+ totalEmptyMiles);
		System.out.println();



		previousTotalNodes = currentDepartureNodes + currentArrivalNodes;





		/* Continually loop through departures and arrivals to minimize empty mile burden  */
		Collections.sort(stations, Collections.reverseOrder(new StationComparator())); // sort stations by number of departures
		stationsCopy = stations.toArray();
		
		previousTotal = totalDepartureTaxis;
		difference = 0;
		round = 1;

		do {
			System.out.println("EmptyMileBurden Optimization: Round " + round+ "\n=========================");
			totalDepartureTaxis = 0;
			currentDepartureNodes = 0;
			currentArrivalNodes = 0;
			totalEmptyMiles = 0;
			
			for (int i = 0; i < N; i++) {
				Station current = stations.get(stations.indexOf((Station)stationsCopy[i]));
				stations = current.optimizeEmptyMiles(stations);
				System.out.println("Station " + (i+1) + " complete");
				totalDepartureTaxis += current.totalDepartures();
				currentDepartureNodes += current.totalDepartureNodes();
				currentArrivalNodes += current.totalArrivalNodes();
				totalEmptyMiles += current.totalEmptyMiles();
			}
			
			difference = previousTotal - totalDepartureTaxis;
			System.out.println(difference);
			previousTotal = totalDepartureTaxis;
			System.out.println();
			round++;
		} while (difference > 0);
		

		// int currentRound = 1;
		// int previousTotal = totalDepartureTaxis;
		// int difference = Integer.MAX_VALUE;

		// // while(true) {


			

		// // 	if (initial == true && difference == 0) break; // rounds stop once there is no more optimization to be done
		// // 	currentRound++;
		// // }
		// // System.out.println();

		// do {
		// 	// run optimization for current round at each station
		// 	System.out.println("Round " + currentRound + " of EmptyMileBurden Optimization \n=========================");
		// 	Collections.sort(stations, Collections.reverseOrder(new StationComparator())); // sort stations by number of departures
			// for (int i = 0; i < N; i++) {
			// 	Station current = stations.get(stations.indexOf((Station)stationsCopy[i]));
			// 	stations = current.optimizeEmptyMiles(stations);
			// 	System.out.println("Station " + (i+1) + " complete");
			// }
		// 	System.out.println();

		// 	// find total number of departure taxis
		// 	totalDepartureTaxis = 0;
		// 	for (Station station : stations)
		// 		totalDepartureTaxis += station.totalDepartures();

		// 	System.out.println("Current total number of taxis to cover all trips: " + totalDepartureTaxis);
		// 	// System.out.println("Initial: " + initial);

		// 	// calculate the reduction
		// 	difference = previousTotal - totalDepartureTaxis;
		// 	System.out.println("Previous total: "+previousTotal);
		// 	System.out.println(difference);
		// 	previousTotal = totalDepartureTaxis;
		// 	System.out.println();

		// 	currentRound++;

		// 	// if (initial == true && difference == 0) break; 
		// 	// initial = false;

		// } while (difference > 0);

		// totalDepartureTaxis = 0;
		// for (Station station : stations) 
		// 	totalDepartureTaxis += station.totalDepartures();

		// /* Print Summary */
		System.out.println(countyname.toUpperCase() + " Checkpoint 4\n=============");
		System.out.println("Total stations: " + stations.size());
		System.out.println("Original number of taxis: " + (departures.size()));
		System.out.println("Current total number of taxis: " + totalDepartureTaxis);
		System.out.println("Initial number of departure nodes: " + initialDepartureNodes);
		System.out.println("Current number of departure nodes: " + currentDepartureNodes);
		System.out.println("Initial number of arrival nodes: " + initialArrivalNodes);
		System.out.println("Current number of arrival nodes: " + currentArrivalNodes);
		System.out.println("Previous number of total nodes: " + previousTotalNodes);
		System.out.println("Current number of total nodes: "+ (currentDepartureNodes+currentArrivalNodes));
		System.out.println("Current total number of empty taxi miles: "+ totalEmptyMiles);
		System.out.println();



		previousTotalNodes = currentDepartureNodes + currentArrivalNodes;




		/* Cycle the roundtrip departures and get final fleet size to tackle all trips */
		System.out.println("Cycling Departures\n=========================");
		// int index = 1;
		totalDepartureTaxis = 0;
		currentDepartureNodes = 0;
		currentArrivalNodes = 0;
		totalEmptyMiles = 0;

		// double totalMiles = 0;
		// double emptyMiles = 0;
		// int tripsServiced = 0;
		for (Station station : stations) {
		// 	System.out.println("Station " + index);
			station.cycleDepartures();
			// System.out.println(station.totalEmptyMiles()+"\t"+station.totalDepartureNodes()+"\t"+station.totalArrivalNodes());
			totalEmptyMiles += station.totalEmptyMiles(); 
			totalDepartureTaxis += station.totalDepartures();
			currentDepartureNodes += station.totalDepartureNodes();
			currentArrivalNodes += station.totalArrivalNodes();
			totalEmptyMiles += station.totalEmptyMiles();
			// totalMiles += station.totalTripMiles();

		// 	emptyMiles += station.totalEmptyMiles();
		// 	tripsServiced += station.tripsServiced();
		// 	System.out.println("Trips serviced: " + tripsServiced);
		// 	System.out.println();
		// 	index++;
		}
		System.out.println();

		long endTime = System.currentTimeMillis(); // get end time
		// System.out.println();


		/* Print Summary */
		System.out.println(countyname.toUpperCase() + " Checkpoint 5\n=============");
		System.out.println("Total stations: " + stations.size());
		System.out.println("Original number of taxis: " + (departures.size()));
		System.out.println("Current total number of taxis: " + totalDepartureTaxis);
		System.out.println("Initial number of departure nodes: " + initialDepartureNodes);
		System.out.println("Current number of departure nodes: " + currentDepartureNodes);
		System.out.println("Initial number of arrival nodes: " + initialArrivalNodes);
		System.out.println("Current number of arrival nodes: " + currentArrivalNodes);
		System.out.println("Previous number of total nodes: " + previousTotalNodes);
		System.out.println("Current number of total nodes: "+ (currentDepartureNodes+currentArrivalNodes));
		System.out.println("Current total number of empty taxi miles: "+ totalEmptyMiles);
		// System.out.println("Total number of taxi miles: "+ totalMiles);
		System.out.println("Total run time (seconds): " + (endTime - startTime)/1000);
		System.out.println();



		// // Optimization round based on arrivals and departures
		// boolean finished = false;
		// int currentRound = 1;
		// int previousTotal = departures.size();
		// int difference = Integer.MAX_VALUE;

		// while (!finished) {
		// 	boolean initial = true;
		// 	int subround = 1;

		// 	do {
		// 		System.out.println("Round " + currentRound + "." + subround + " of Station Optimization \n=============");
		// 		if (initial == true) System.out.println("Previous Total: "+ previousTotal + ", Difference: "+difference);
		// 		Collections.sort(stations, Collections.reverseOrder(new StationComparator())); // sort stations by number of departures
		// 		Object[] stationsCopy = stations.toArray(); // create shallow copy of stations
		// 		int N = stationsCopy.length;

		// 		for (int i = 0; i < N; i++) {
		// 			Station current = stations.get(stations.indexOf((Station)stationsCopy[i]));
		// 			stations = current.optimize(currentRound, stations);
		// 			System.out.println("Station " + (i+1) + " complete");
		// 		}
		// 		System.out.println();

		// 		int totalDepartureTaxis = 0;
		// 		// int totalArrivals = 0;		
		// 		// int totalTaxis = 0;
		// 		for (Station station : stations) {
		// 		// 	System.out.println("Station at " + station + ". Total Departures: " + station.totalDepartures() + ", Total Arrivals: " + station.totalArrivals());
		// 			totalDepartureTaxis += station.totalDepartures();
		// 			// totalArrivals += station.totalArrivals();
		// 			// totalTaxis += 
		// 		}

		// 		System.out.println("Current total number of taxis to cover all trips: " + totalDepartureTaxis);
		// 		System.out.println("Initial: " + initial);
		// 		difference = previousTotal - totalDepartureTaxis;
		// 		System.out.println("Previous total: "+previousTotal);
		// 		System.out.println(difference);
		// 		previousTotal = totalDepartureTaxis;
		// 		// System.out.println("Arrivals accounted for: " + totalArrivals);
		// 		System.out.println();

		// 		//asdf
		// 		if (initial == true && difference == 0) break; 
		// 		initial = false;
		// 		subround++;
		// 		break;
		// 	}
		// 	while (difference > 0);

		// 	if (initial == true && difference == 0) break; // rounds stop once there is no more optimization to be done
			
		// 	currentRound++;
		// 	break;
		// }

		// int totalDepartureTaxis = 0;
		// for (Station station : stations) 
		// 	totalDepartureTaxis += station.totalDepartures();

		// /* Print Summary */
		// System.out.println(countyname.toUpperCase() + " Checkpoint 2\n=============");
		// System.out.println("Total stations: " + stations.size());
		// System.out.println("Current total number of taxis: " + totalDepartureTaxis);
		// // System.out.println("Initial number of arrivals  to be analyzed: " + arrivals.size());
		// System.out.println();

		// // Cycle the remaining departures 
		// for (Station station : stations) {
		// 	station.cycleDepartures(stations);
		// }

		// totalDepartureTaxis = 0;
		// for (Station station : stations) 
		// 	totalDepartureTaxis += station.totalDepartures();

		// /* Print Summary */
		// System.out.println(countyname.toUpperCase() + " Checkpoint 3\n=============");
		// System.out.println("Total stations: " + stations.size());
		// System.out.println("Current total number of taxis: " + totalDepartureTaxis);
		// // System.out.println("Initial number of arrivals  to be analyzed: " + arrivals.size());
		// System.out.println();
	}



	/* 	Function that reads the station file for the given county, and creates a station for each of the pixels 
		that were determined to be stations for the county */
	public static List<Station> getStations(String filename) {
		BufferedReader reader = null;
		// List<Station> stations = new ArrayList<Station>();

		try {
			String pixel;
			reader = new BufferedReader(new FileReader(filename)); // prep the file to be read

			// go through every pixel in file, and create a station for it
			while ((pixel = reader.readLine()) != null) {
				String[] coordinates = pixel.split(", ");
				stations.add(new Station(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1])));
			}

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if (reader != null){
				try {reader.close();} catch (Exception e) {}
			}
		}

		return stations;
	}



	/* 	Function that reads the trip file(s) for the given county, and saves the relevant trip info from 
		each line into a trip data structure. Then, returns a list of all the trips that were created. */
	public static List<Trip> getTrips(String filename, String countyname, List<Trip> list) {
		BufferedReader reader = null;

		try {
			String line;
			reader = new BufferedReader(new FileReader(filename)); // prep the file to be read
			line = reader.readLine(); // read the header line

			// go through every line in the file
			while ((line = reader.readLine()) != null) {
				
				/* only save trips with <= 5 riders */
				int riders = Integer.parseInt(line.split(",")[17]);
				if (riders <= 6) {
					Trip trip = new Trip(line); // create a trip with the info
					list.add(trip); // save trip
					
					Taxi taxi = new Taxi(trip, countyname.toUpperCase(), stations); // create a taxi to accomodate the trip
					Station oStation = null; // taxi's origin station that will be found below
					Station cStation = null; // taxi's current station (post-trip) that will also be found below

					/* if dPixel is in the county */
					if (trip.dCounty().equals(countyname.toUpperCase())) {
						// find station closest to the dPixel
						oStation = trip.findClosestOriginStation(stations);

						// save as base station for taxi
						taxi.setBase(oStation);
					}

					/* if aPixel is in the county */
					if (trip.aCounty().equals(countyname.toUpperCase())) {
						// find station closest to the aPixel
						cStation = trip.findClosestCurrentStation(stations);

						// save as current station for taxi
						taxi.setCurrent(cStation);
					}

					/* 	after updating taxi, add it to the lists maintained by
						current and origin stations
						NOTE: these stations will be null, if the trip / taxi
						is intercounty */

					// add taxi to the oStation's departure list
					if (oStation != null) {
						if (!oStation.addDeparture(taxi)) {
							// System.out.println(taxi.currentTrip().currentPixel() + ",\tdTime: " + taxi.dTime() + "\tNodes: "+taxi.numTrips()+"\taTime: "+taxi.timeAvailable());
						}
					}

					// add taxi to the closest station's departure list
					if (cStation != null) cStation.addArrival(taxi);
				}

			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if (reader != null){
				try {reader.close();} catch (Exception e) {}
			}
		}

		return list;
	}

	/*	Function that finds the closest station in the list of stations to the given pixel */
	public static Station findClosest(String pixel) {
		Station closest = null;
		double minDist = Double.POSITIVE_INFINITY;
		// go through each of the available stations
		for (Station station : stations) {
			// find the distance between this pixel and the station
			double dist = station.distanceTo(pixel);
			
			// if distance is the closest dist thus far, save station
			if (dist < minDist) {
				minDist = dist;
				closest = station;
			}
		}

		return closest;
	}

	/* Comparator used to sort Stations by the number of departures they have */
	static class StationComparator implements Comparator<Station> {
		public int compare(Station s1, Station s2) {
			return ((Integer)s1.dTrips.size()).compareTo(s2.dTrips.size());
		}
	}
}