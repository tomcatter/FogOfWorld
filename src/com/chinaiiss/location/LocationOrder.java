package com.chinaiiss.location;

import java.io.Serializable;
import java.util.Comparator;

import com.chinaiiss.utils.LocationUtils;

import android.location.Location;

public class LocationOrder implements Comparator<Location>, Serializable {

	private static final long serialVersionUID = -8961248565694555407L;

	
	 /**
     * The distance in meters to which we consider something explored.
     * 
     * <pre>
     * At equator:
     * decimal places  degrees          distance
     * 0                1.0             111 km
     * 1                0.1             11.1 km
     * 2                0.01            1.11 km
     * 3                0.001           111 m
     * 4                0.0001          11.1 m
     * 5                0.00001         1.11 m
     * 6                0.000001        0.111 m
     * 7                0.0000001       1.11 cm
     * 8                0.00000001      1.11 mm
     * </pre>
     */
	public static final double DEGREES_RADIUS = 0.0005;
	public static final double METERS_RADIUS = 55.5;

	@Override
	public int compare(Location firstLocation, Location secondLocation) {
		  // if (firstLocation.distanceTo(secondLocation) < METERS_RADIUS) {
        // return 0;
        // }

        // check if inside the same area

        // compare by longitude
        if (LocationUtils.haversineDistance(firstLocation, secondLocation) <= METERS_RADIUS) {
            return 0;

        }

        // compare by longitude
        if (firstLocation.getLongitude() > secondLocation.getLongitude()
                + DEGREES_RADIUS) {
            return 1;

        }

        if (firstLocation.getLongitude() < secondLocation.getLongitude()
                - DEGREES_RADIUS) {
            return -1;
        }
        // compare by latitude

        if (firstLocation.getLatitude() > secondLocation.getLatitude()
                + DEGREES_RADIUS) {
            return 1;

        }

        // if (firstLocation.getLatitude() < secondLocation.getLatitude() -
        // DEGREES_RADIUS) {
        return -1;
        // }
	}

}
