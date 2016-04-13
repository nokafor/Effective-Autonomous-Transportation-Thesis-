import java.util.*;
import java.io.*;

/* Program to label each NJ county by how many total trips arrive and/or depart from the county */
public class FindCountyTrips {
	public static void main(String[] args) {
		
		// arguments should be the file names containing the county trip information,
		// starting with all departure files then all arrival files (must be at least 2 - one arrival/departure)
		int N = args.length;
		Integer[] departureInfo = new Integer[2]; /* array to save the total departure trip (index = 0) and total rider (index = 1) info */
		Integer[] arrivalInfo = new Integer[2]; /* array to save the total arrival trip (index = 0) and total rider (index = 1) info */

		/* Initialize info matrices */
		for (int i = 0; i < departureInfo.length; i++) {
			departureInfo[i] = 0;
			arrivalInfo[i] = 0;
		}

		if (N >= 2) {

			// Get trip info for all the departures from county
			System.out.println("Getting Departure Info\n=============");
			for (int i = 0; i < N/2; i++) {
				System.out.println(args[i]); /* make sure the right files are analyzed as departures */
				Integer[] info = getTripInfo(args[i]);
				departureInfo[0] += info[0]; 
				departureInfo[1] += info[1];
			}
			System.out.println();

			// Get trip info for all the arrivals to county
			System.out.println("Getting Arrival Info\n=============");
			for (int i = N/2; i < N; i++) {
				System.out.println(args[i]); /* make sure the right files are analyzed as arrivals */
				Integer[] info = getTripInfo(args[i]);
				arrivalInfo[0] += info[0];
				arrivalInfo[1] += info[1];
			}
			System.out.println();
		}
		else System.out.println("Please add a departure and arrival file");

		// Print the information found
		System.out.println("Total departure trips from this county: " + departureInfo[0]);
		System.out.println("Total departing passengers from this county: " + departureInfo[1]);

		System.out.println("Total arrival trips to this county: " + arrivalInfo[0]);
		System.out.println("Total arriving passengers to this county: " + arrivalInfo[1]);
	}

	/* Function that counts the total number of trips and riders for all the trips in a given file */
	public static Integer[] getTripInfo(String filename) {
		BufferedReader reader = null; 
		Integer[] info = new Integer[2]; /* array to save the total trip (index = 0) and total rider (index = 1) info */

		/* Initialize matrix */
		for (int i = 0; i < info.length; i++) {
			info[i] = 0;
		}

		try {
			String line;
			reader = new BufferedReader(new FileReader(filename)); // prep the file to be read
			line = reader.readLine(); // read the header line

			while ((line = reader.readLine()) != null) {
				/* get trip info */
				info[0]++;

				/* get rider info */
				info[1] += Integer.parseInt(line.split(",")[17]);
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

		return info;

	}
}