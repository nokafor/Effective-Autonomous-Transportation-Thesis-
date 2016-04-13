import java.util.*;
import java.io.*;

public class GetTotalDistribution {
	public static void main(String[] args) {
		String countyname = args[0];
		String filename = "Departures/"+countyname+".csv";
		int[] distVec = new int[1442];

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(filename)); // prep the file to be read

			String line;
			reader.readLine(); // read header line

			// count the stations
			while ((line = reader.readLine()) != null) {

				String[] info = line.split(",");

				// convert dTime and aTime to minutes
				int dTime = (int) Math.round(Double.parseDouble(info[3]) / 60);
				int aTime = (int) Math.round(Double.parseDouble(info[24]) / 60);

				if (aTime > 1440) {
					// need to wrap around time
					for (int i = dTime; i < 1440; i++)
						distVec[i]++;

					aTime = aTime - 1440;

					for (int i = 0; i < aTime; i++)
						distVec[i]++;
				}
				else {
					for (int i = dTime; i < aTime; i++)
						distVec[i]++;
				}

				// if (aTime < dTime) System.out.println("checkpoint");
				
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


		int max = 0;
		for (int i = 0; i < 1442; i++) {
			System.out.println(distVec[i]);
			if (distVec[i] > max) {
				max = distVec[i];
			}
		}

		System.out.println("Max value: "+max);

	}
}