/* Trip data structure 
   Saves all the trip info provided in the trip files for a trip
*/

import java.util.*;

public class Trip {
    private int nodes; // total # of nodes in the trip
    private Node first; // first node on trip
    private Node last; // last node on trip
    private String dCounty; // origin county of trip
    private String aCounty;  // arrival county of trip
    private String oPixel; // origin pixel of trip
    private double dTime; // departure time
    private double aTime; // arrival time
    private double vehMiles; // total miles of the trip
    private double delay; // # of seconds original departure is currently delayed
    

    private class Node {
        private String pixel; // destination pixel of node
        private int riders; // # of riders on this leg of trip
        private Node next; // link to the node that comes next
    }

    /* Create a trip from the line of trip data in the trip file */
    public Trip(String line) {
        first = null;
        last = null;
        delay = 0;

        String [] info = line.split(","); // split the line of data

        /* save the trip info */
        nodes = Integer.parseInt(info[4]);
        for (int i = 0, x = 0; i < nodes; i++, x+=4) {
            Node node = new Node();
            node.pixel = info[6+x] + ", " + info[7+x];
            node.riders = Integer.parseInt(info[8+x]);

            if (isEmpty()) {
                first = node;
                last = node;
            }
            else {
                last.next = node;
                last = node;
            }
        }
        dCounty = info[0].split("-")[0];
        aCounty = info[21].split("-")[0];
        oPixel = info[1] + ", " + info[2];
        dTime = Double.parseDouble(info[3]);
        aTime = Double.parseDouble(info[24]);
        vehMiles = Double.parseDouble(info[18]);
    }

    /* Return the total number of riders across all nodes of this trip */
    public int totalRiders() {
        int riders = 0;
        Node curr = first;

        while (curr != null) {
            riders += curr.riders;
            curr = curr.next;
        }

        return riders;
    }

    public boolean isEmpty() {
        return first == null;
    }

    public String dCounty() {
        return dCounty;
    }

    public double delay() {
        return delay;
    }

    public String aCounty() {
        return aCounty;
    }

    public Double dTime() {
        return dTime;
    }

    public Double aTime() {
        return aTime;
    }

    public String currentPixel() {
        return last.pixel;
    }

    public String oPixel() {
        return oPixel;
    }

    public Double vehMiles() {
        return vehMiles;
    }

    public Integer nodes() {
        return nodes;
    }

    /* Attempt to combine nodes of another trip, if within max circuity */
    public boolean combineNodes(Trip that) {
        // initialize miles with distance between two pixels
        double newMiles = new Station(first.pixel, true).distanceTo(new Station(that.first.pixel, true)); 

        // check max circuity
        if (dTime <= that.dTime) {
            // add distance to all the nodes in this trip
            Node previous = that.first;
            Node current = first.next;

            while (current != null) {
                newMiles += new Station(previous.pixel, true).distanceTo(new Station(current.pixel, true));
                previous = current;
                current = current.next;
            }

            if ((newMiles / vehMiles) > 1.2) return false; // check the max circuity for this trip

            // add distance to all the nodes in this trip
            current = that.first.next;
            while (current != null) {
                newMiles += new Station(previous.pixel, true).distanceTo(new Station(current.pixel, true));
                previous = current;
                current = current.next;             
            }

            // check the max circuity for the other trip
            if (((newMiles - new Station(first.pixel, true).distanceTo(new Station(that.first.pixel, true))) / that.vehMiles) > 1.2) 
                return false; 

            // update anything specific to that trip being before this 
            aCounty = that.aCounty;

            Node beg = new Node();
            beg.pixel = that.oPixel;
            beg.riders = 0;
            beg.next = first;
            last.next = that.first;
            last = that.last;

        }

        if (dTime > that.dTime) {
            // add distance to all the nodes in this trip
            Node previous = first;
            Node current = that.first.next;

            while (current != null) {
                newMiles += new Station(previous.pixel, true).distanceTo(new Station(current.pixel, true));
                previous = current;
                current = current.next;
            }

            if ((newMiles / that.vehMiles) > 1.2) return false; // check the max circuity for this trip

            // add distance to all the nodes in this trip
            current = first.next;
            while (current != null) {
                newMiles += new Station(previous.pixel, true).distanceTo(new Station(current.pixel, true));
                previous = current;
                current = current.next;             
            }

            // check the max circuity for the other trip
            if (((newMiles - new Station(first.pixel, true).distanceTo(new Station(that.first.pixel, true))) / vehMiles) > 1.2) 
                return false; 

            Node beg = new Node();
            beg.pixel = oPixel;
            beg.riders = 0;
            beg.next = that.first;
            that.last.next = first;
            first = beg;

            dTime = that.dTime;
            oPixel = that.oPixel;

        }
        // update trip appropriately
        vehMiles = newMiles;
        aTime = dTime + (vehMiles *3600/30);
        nodes += that.nodes;
        delay += Math.abs(dTime - that.dTime);

        return true;
    }

    public boolean addNode(Trip trip) {
        if ((nodes + trip.nodes) > 3) return false; // if already have max number of nodes, do not combine
        
        // do not delay departure for more than 5 min
        if ((delay + Math.abs(trip.dTime - dTime)) > 300) return false; 

        if (trip.dTime >= dTime) {
            // add to the end of the node list
            last.next = trip.first;
            last = trip.last;

            // update arrival county
            aCounty = trip.aCounty;

            // update dTime
            dTime = trip.dTime;

        }
        else {
            // add to the beginning of node list
            trip.last.next = first;
            first = trip.first;
        }

        // update nodes
        nodes += trip.nodes;

        // update vehicle miles
        vehMiles += trip.vehMiles;

        // update arrival time
        aTime = vehMiles * 3600 / 30;

        // delay departure
        delay += Math.abs(trip.dTime - dTime);

        return true;
    }

    public boolean equals(Object obj) {
        // make sure the other object is a Trip
        if (!(obj instanceof Trip)) return false;
        Trip that = (Trip) obj;

        // check to make sure all nodes are the same
        if (nodes != that.nodes) return false;
        
        if (first != null && that.first != null) {
            Node curr = first;
            Node tCurr = that.first;

            while (curr != null && tCurr != null) {
                if (!curr.pixel.equals(tCurr.pixel)) return false;
                if (curr.riders != tCurr.riders) return false;

                curr = curr.next;
                tCurr = tCurr.next;
            }

        }
        
        //NOTE: no way for both to be null because every created trip has a first value
        else return false; 

        // check the rest of the parameters
        if (!dCounty.equals(that.dCounty) || !aCounty.equals(that.aCounty) || !oPixel.equals(that.oPixel)) 
            return false;
        if (dTime != that.dTime || aTime != that.aTime) return false;
        if (vehMiles != that.vehMiles) return false;

        return true;

    }

    /*  Function that finds the closest station in the list of stations to the destination pixel */
    public Station findClosestCurrentStation(List<Station> stations) {
        Station closest = null;
        double minDist = Double.POSITIVE_INFINITY;
        // go through each of the available stations
        for (Station station : stations) {
            // find the distance between this pixel and the station
            double dist = station.distanceTo(last.pixel);
            
            // if distance is the closest dist thus far, save station
            if (dist < minDist) {
                minDist = dist;
                closest = station;
            }
        }

        return closest;
    }

    /*  Function that finds the closest station in the list of stations to the origin pixel */
    public Station findClosestOriginStation(List<Station> stations) {
        Station closest = null;
        double minDist = Double.POSITIVE_INFINITY;
        // go through each of the available stations
        for (Station station : stations) {
            // find the distance between this pixel and the station
            double dist = station.distanceTo(oPixel);
            
            // if distance is the closest dist thus far, save station
            if (dist < minDist) {
                minDist = dist;
                closest = station;
            }
        }

        return closest;
    }
}
