import java.util.*;
import java.io.*;

/* 	Program that finds the max number of stations that a county would need based on 
	the summary of information for the pixels in the county given the generated trip 
	files */
public class MaxStations {
	private static List<Station> stations;

	public static void main(String[] args) {
		
		// Argument should be the pixel info summary file for the county

		int N = args.length;
		stations = new ArrayList<Station>(); // initialize the list of stations

		getStations(args[0]);

		// System.out.println(stations.size());
		for (Station s : stations) {
			System.out.println(s);
		}
	}

	public static void getStations(String filename) {
		BufferedReader reader = null;

		try {
			String line;
			reader = new BufferedReader(new FileReader(filename));
			line = reader.readLine(); // read the header line

			while ((line = reader.readLine()) != null) {
				String[] pixelInfo = line.split("\t");
				
				// create station
				Station station = new Station(pixelInfo[1]);

				// check if this station is within 5 minutes of any other stations
				boolean withinRange = false;
				for (Station s : stations) {
					double timeTo = station.distanceTo(s) * 3600 / 30;

					if (timeTo <= 300) {
						withinRange = true;
						break;
					}
				}

				// if not within range of any other station, save the new station
				if (!withinRange) stations.add(station);
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
	}
}