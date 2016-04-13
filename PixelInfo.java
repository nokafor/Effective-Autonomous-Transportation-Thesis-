import java.util.*;
import java.io.*;

/* Program to tabulate how many trips originate from each pixel of the state of NJ */
public class PixelInfo {
	private static TreeMap<String, Double[]> countyInfo; /* map to save pixels and their general trip info */
	private static TreeMap<String, TreeSet<Double>> pixelDepartures; /* map to save pixels and their departure info */
	
	public static void main(String[] args) {
		countyInfo = new TreeMap<String, Double[]>();
		pixelDepartures = new TreeMap<String, TreeSet<Double>>();
		String countyName = "";

		int N = args.length;

		for (int i = 0; i < N; i++) {
			countyName = getPixelInfo(args[i]);
		}

		// countyName = countyName.split("-")[0];
		
		// System.out.println("# Pixels in " + countyName + ": " + countyInfo.size());
		// print header
		System.out.println("County Name\tPixel\tTotal Trips\tTotal Intercounty Trips\tTotal Riders\tAverage Time b/w Trips\tAverage AVO");

		for (String pixel : countyInfo.keySet()) {
			// System.out.println(pixel + "\n=====================");
			// get saved info on pixel
			Double[] info = countyInfo.get(pixel);
			TreeSet<Double> departures = pixelDepartures.get(pixel);

			// calculate average time between departures
			List<Double> differences = new ArrayList<Double>();

			int sum = 0;
			for (Double time : departures) {
				Double lower = departures.lower(time);
				if (lower != null) {
					double difference = Math.abs(time - lower);
					differences.add(difference);
					sum += difference;
				}
			}

			System.out.println(countyName + "\t" + pixel + "\t" + info[0] + "\t" + info[3] + "\t" + info[1] + "\t" + sum/info[0] + "\t" + info[2]/info[0]);
		}
	}

	// gets pixel info and returns county name
	public static String getPixelInfo(String fileName) {
		BufferedReader reader = null;
		String countyName = "";

		try {
			String line;
			reader = new BufferedReader(new FileReader(fileName));
			line = reader.readLine(); // read the header line

			while ((line = reader.readLine()) != null) {
				String[] tripInfo = line.split(","); // split the trip info

				// get pixel name
				String pixel = tripInfo[1] + ", " + tripInfo[2];

				// get departure time
				double dTime = Double.parseDouble(tripInfo[3]);

				// update saved pixel data
				if (countyInfo.containsKey(pixel)) {
					Double[] info = countyInfo.get(pixel);
					TreeSet<Double> departures = pixelDepartures.get(pixel);
					info[0]++;
					info[1] += Double.parseDouble(tripInfo[17]);
					info[2] += Double.parseDouble(tripInfo[20]);
					// System.out.println(tripInfo[0].split("-")[0] +"\t"+ tripInfo[21].split("-")[0] + "\t" + tripInfo[0].split("-")[0].equals(tripInfo[21].split("-")[0]));
					// if intercounty trip
					if (!tripInfo[0].split("-")[0].equals(tripInfo[21].split("-")[0])) {
						info[3]++;
					}

					departures.add(dTime);
					countyInfo.put(pixel, info);
					pixelDepartures.put(pixel, departures);
				}

				// if data not already saved, create new data
				else {
					Double[] info = new Double[4]; /* array to save the total trip (index = 0), total rider (index = 1), AVO (index = 2), intercounty trips (index = 3) info */
					TreeSet<Double> departures = new TreeSet<Double>();
					info[0] = 1.0; // initialize counter for total trips
					info[1] = Double.parseDouble(tripInfo[17]); // initialize sum of total # riders
					info[2] = Double.parseDouble(tripInfo[20]); // initialize sum of AVOs
					info[3] = 0.0; // initialize sum of total out-of-county trips

					// System.out.println(tripInfo[0].split("-")[0] +"\t"+ tripInfo[21].split("-")[0] + "\t" + tripInfo[0].split("-")[0].equals(tripInfo[21].split("-")[0]));
					// if intercounty trip
					if (!tripInfo[0].split("-")[0].equals(tripInfo[21].split("-")[0])) {
						info[3]++;
					}

					departures.add(dTime);
					countyInfo.put(pixel, info);
					pixelDepartures.put(pixel, departures);
				}

				countyName = tripInfo[0].split("-")[0];
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

		return countyName;

	}
}