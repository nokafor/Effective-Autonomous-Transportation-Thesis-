import java.util.*;
import java.io.*;

/* Program to get all the land-use statistics for the top 3 stations in the given county */
public class PixelStatistics {	
	private static List<String> topStations; // list of the top 3 stations in the given county

	public static void main(String[] args) {
		topStations = new ArrayList<String>(3);

		// filename with all the pixel statistics
		String filename = "PixelStatisticsNJ.csv";

		// get name of county we are working with
		String countyName = args[0].toLowerCase();

		// get the top 3 stations
		getTopStations(countyName);

		// get land-use statistics for the top 3 pixels
		List<Double[]> statistics = getStatistics(filename);

		// print header
		System.out.println("County\tPixel\tLatitude\tLongitude\tPopulation\tEmployment\tSchool Enrollment\tActivity Locations");
		for (String station : topStations) {
			Double[] stats = statistics.get(topStations.indexOf(station));

			System.out.print(countyName.toUpperCase() + "\t" + station);

			for (int i = 0; i < 6; i++) {
				System.out.print("\t" + stats[i]);
			}

			System.out.println();
		}

	}

	// gets the land-use statistics for the saved pixels
	public static List<Double[]> getStatistics(String filename) {
		List<Double[]> statistics = new ArrayList<Double[]>();
		BufferedReader reader = null;

		try {
			String line;
			reader = new BufferedReader(new FileReader(filename));
			line = reader.readLine(); // read first header line
			line = reader.readLine(); // read second header line

			String currentPixel = "";
			String previousLine = "";

			// read in the doc line by line until the end of the file 
			// or until all the statistics have been found
			while ((line = reader.readLine()) != null && statistics.size() < 3) {
				String[] splitLine = line.split(","); // split the data

				String newPixel = splitLine[7] + ", " + splitLine[6]; // get the pixel

				if (!newPixel.equals(currentPixel)) {

					if (topStations.contains(currentPixel)) {
						// split the previous line to get relevant information
						String[] info = previousLine.split(",");

						// create the array to save the statistics
						Double[] stats = new Double[6];
						stats[0] = Double.parseDouble(info[8]); // save latitude
						stats[1] = Double.parseDouble(info[9]); // save longitude
						stats[2] = Double.parseDouble(info[14]); // save population
						stats[3] = Double.parseDouble(info[15]); // save employment
						stats[4] = Double.parseDouble(info[16]); // save school slots
						stats[5] = Double.parseDouble(info[13]); // save activity points						

						statistics.add(topStations.indexOf(currentPixel), stats); // save the information
						// System.out.println(statistics.size());
					}

					currentPixel = newPixel;
				}
				previousLine = line;
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
		return statistics;
	}

	// gets the top 3 stations from the saved file
	public static void getTopStations(String countyName) {
		String filename = "Stations/" + countyName + ".txt";
		BufferedReader reader = null;

		try {
			String line;
			reader = new BufferedReader(new FileReader(filename));

			// read in the top 3 stations of the county
			for (int i = 0; i < 3; i++) {
				// save the pixel with the decimal values stripped
				topStations.add(reader.readLine().replace(".0", ""));
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