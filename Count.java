import java.util.*;
import java.io.*;

public class Count {
	public static void main(String[] args) {
		String countyname = args[0];
		String filename = "Stations/"+countyname+".txt";

		int count = 0;

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(filename)); // prep the file to be read

			// count the stations
			while (reader.readLine() != null)
				count++;

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if (reader != null){
				try {reader.close();} catch (Exception e) {}
			}
		}

		System.out.println("Total stations: " + count);
	}
}