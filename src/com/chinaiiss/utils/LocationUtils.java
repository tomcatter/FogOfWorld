package com.chinaiiss.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.location.Location;
import android.util.Log;

import com.chinaiiss.location.ApproximateLocation;
import com.chinaiiss.location.LocationRectangle;
import com.tencent.tencentmap.mapsdk.map.GeoPoint;

public class LocationUtils {

	private static final String TAG = LocationUtils.class.getName();

    private static final double METER_IN_FEET = 3.2808399;
    private static final String FEET_UNIT = " ft";
    private static final String METERS_UNIT = " m";

    public static GeoPoint locationToGeoPoint(Location location) {
        return coordinatesToGeoPoint(location.getLatitude(),
                location.getLongitude());
    }

    public static ApproximateLocation geoPointToLocation(GeoPoint geoPoint) {
        return coordinatesToLocation(geoPoint.getLatitudeE6() / 1e6,
                geoPoint.getLongitudeE6() / 1e6);
    }

    /**
     * @param latitude in decimal degrees
     * @param longitude in decimal degrees
     * @return a GeoPoint with the coordinates
     */
    public static GeoPoint coordinatesToGeoPoint(double latitude,
            double longitude) {
        return new GeoPoint((int) (latitude * 1e6), (int) (longitude * 1e6));
    }

    /**
     * @param latitude in decimal degrees
     * @param longitude in decimal degrees
     * @return a ApproximateLocation with the coordinates
     */
    public static ApproximateLocation coordinatesToLocation(double latitude,
            double longitude) {
        ApproximateLocation location = new ApproximateLocation("Translator");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    /**
     * @param latitudeE6 in microdegrees
     * @param longitudeE6 in microdegrees
     * @return a ApproximateLocation with the coordinates
     */
    public static ApproximateLocation coordinatesToLocation(int latitudeE6,
            int longitudeE6) {
        ApproximateLocation location = new ApproximateLocation("Translator");
        location.setLatitude(latitudeE6 / 1e6);
        location.setLongitude(longitudeE6 / 1e6);
        return location;

    }

    /**
     * Gets the distance in the unit for the current settings. The measure value is false for metric
     * and true for imperial.
     * 
     * @param meters the distance in meters
     * @param measure metric or imperial system.
     * @return
     */
    public static String getFormattedDistance(double meters, boolean imperial) {
        if (imperial) {
            return Math.round(meters * METER_IN_FEET) + FEET_UNIT;
        }
        return Math.round(meters) + METERS_UNIT;
    }

    private static final double EARTH_RADIUS = 6317d;

    public static double haversineDistance(Location first, Location second) {
        double lat1 = first.getLatitude();
        double lat2 = second.getLatitude();
        double lon1 = first.getLongitude();
        double lon2 = second.getLongitude();
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS * c * 1000;
    }

    // FIXME broken
    // TODO: lame code calculations following, improve
    // TODO: write tests
    // TODO: zoom case is not correctly checked, rectangle A included in B
    /**
     * The A region is the region already known while the B region is the unkown one. The method
     * returns the areas of B that are not in A and the rectangles around A that are next to B. In
     * the following example the starred regions should be returned.
     * 
     * <pre>
     *   _____
     *  |**B* |**
     *  |**------
     *  |__|__| |
     *  ***| A  |
     *  ***|____|
     * </pre>
     * 
     * <pre>
     *             ^
     *            N|+
     *             |
     *             |
     * -W          |           E+
     * -------------------------->
     *             |
     *             |
     *             |
     *            S|-
     * 
     * </pre>
     * 
     * @param upperLeftA
     * @param lowerRightA
     * @param upperLeftB
     * @param lowerRightB
     * @return
     */
    public static List<LocationRectangle> complementArea(Location upperLeftA,
            Location lowerRightA, Location upperLeftB, Location lowerRightB) {

        Log.d(TAG,
                "Calculating rectangles for :"
                        + LogUtilities.locationLogList(upperLeftA, lowerRightA,
                                upperLeftB, lowerRightB));
        // north is +, south is -
        // west is - , east is +
        // yay for inventing words :D
        double northestLatitude = 0;
        double westestLongitude = 0;
        double southestLatitude = 0;
        double eastestLongitude = 0;
        // calculate upper left bounds
        // largest latitude
        northestLatitude = (upperLeftA.getLatitude() > upperLeftB.getLatitude()) ? upperLeftA
                .getLatitude() : upperLeftB.getLatitude();
        // smallest longitude
        westestLongitude = (upperLeftA.getLongitude() < upperLeftB
                .getLongitude()) ? upperLeftA.getLongitude() : upperLeftB
                .getLongitude();

        // calculate lower right bounds
        // smallest latitude
        southestLatitude = (lowerRightA.getLatitude() < lowerRightB
                .getLatitude()) ? lowerRightA.getLatitude() : lowerRightB
                .getLatitude();
        // largest longitude
        eastestLongitude = (lowerRightA.getLongitude() > lowerRightB
                .getLongitude()) ? lowerRightA.getLongitude() : lowerRightB
                .getLongitude();

        // if B is included in A return an empty array
        if (northestLatitude == upperLeftA.getLatitude()
                && westestLongitude == upperLeftA.getLongitude()
                && southestLatitude == lowerRightA.getLatitude()
                && eastestLongitude == lowerRightA.getLongitude()) {
            return Collections.emptyList();
        }
        ApproximateLocation rect1UL = new ApproximateLocation("mumu");
        ApproximateLocation rect1BR = new ApproximateLocation("mumu");

        ApproximateLocation rect2UL = new ApproximateLocation("mumu");
        ApproximateLocation rect2BR = new ApproximateLocation("mumu");

        // The coordinates for the dotted areas are to be calculated.
        // The same calculations apply if the rectangles do not overlap
        /**
         * configuration 1:
         * 
         * <pre>
         *   _____
         *  |**B* |**
         *  |**------
         *  |__|__| |
         *  ***| A  |
         *  ***|____|
         * 
         * </pre>
         */
        if (northestLatitude > upperLeftA.getLatitude()
                && westestLongitude < upperLeftA.getLongitude()) {
            rect1UL.setLatitude(northestLatitude);
            rect1UL.setLongitude(westestLongitude);

            rect1BR.setLatitude(upperLeftA.getLatitude());
            rect1BR.setLongitude(lowerRightA.getLongitude());

            rect2UL.setLatitude(upperLeftA.getLatitude());
            rect2UL.setLongitude(westestLongitude);

            rect2BR.setLatitude(lowerRightA.getLatitude());
            rect2BR.setLongitude(upperLeftA.getLongitude());
            return createList(new LocationRectangle(rect1UL, rect1BR),
                    new LocationRectangle(rect2UL, rect2BR));

        }

        /**
         * configuration 2:
         * 
         * <pre>
         *         _____
         *     ...|..B. |  
         *     ------***|
         *     |  | |***|
         *     | A _|___|
         *     |____|****
         * 
         * </pre>
         */

        if (northestLatitude > upperLeftA.getLatitude()
                && eastestLongitude > lowerRightA.getLongitude()) {

            rect1UL.setLatitude(northestLatitude);
            rect1UL.setLongitude(upperLeftA.getLongitude());

            rect1BR.setLatitude(upperLeftA.getLatitude());
            rect1BR.setLongitude(eastestLongitude);

            rect2UL.setLatitude(upperLeftA.getLatitude());
            rect2UL.setLongitude(lowerRightA.getLongitude());

            rect2BR.setLatitude(southestLatitude);
            rect2BR.setLongitude(eastestLongitude);
            return createList(new LocationRectangle(rect1UL, rect1BR),
                    new LocationRectangle(rect2UL, rect2BR));

        }

        /**
         * configuration 3:
         * 
         * <pre>
         *   _____
         *  |  A  |..
         *  |  ------
         *  |__|__|.|
         *  ***|*B .|
         *  ***|____|
         * 
         * </pre>
         */
        if (eastestLongitude > lowerRightA.getLongitude()
                && southestLatitude > lowerRightA.getLatitude()) {

            rect1UL.setLatitude(upperLeftA.getLatitude());
            rect1UL.setLongitude(lowerRightA.getLongitude());

            rect1BR.setLatitude(southestLatitude);
            rect1BR.setLongitude(eastestLongitude);

            rect2UL.setLatitude(lowerRightA.getLatitude());
            rect2UL.setLongitude(upperLeftA.getLongitude());

            rect2BR.setLatitude(southestLatitude);
            rect2BR.setLongitude(lowerRightA.getLongitude());
            return createList(new LocationRectangle(rect1UL, rect1BR),
                    new LocationRectangle(rect2UL, rect2BR));

        }

        /**
         * configuration 4:
         * 
         * <pre>
         *         _____
         *     ...|  A  |  
         *     ------   |
         *     |..| |   |
         *     |.B _|___|
         *     |...*|***
         * 
         * </pre>
         */

        if (southestLatitude < lowerRightA.getLatitude()
                && westestLongitude > upperLeftA.getLongitude()) {

            rect1UL.setLatitude(upperLeftA.getLatitude());
            rect1UL.setLongitude(westestLongitude);

            rect1BR.setLatitude(southestLatitude);
            rect1BR.setLongitude(upperLeftA.getLongitude());

            rect2UL.setLatitude(lowerRightA.getLatitude());
            rect2UL.setLongitude(upperLeftA.getLongitude());

            rect2BR.setLatitude(southestLatitude);
            rect2BR.setLongitude(lowerRightA.getLongitude());
            return createList(new LocationRectangle(rect1UL, rect1BR),
                    new LocationRectangle(rect2UL, rect2BR));
        }
        assert false : "Unexpected rectangle values were passed : "
                + upperLeftA + " " + lowerRightA + " " + upperLeftB + " "
                + lowerRightB;

        return Collections.emptyList();
    }

    private static List<LocationRectangle> createList(
            LocationRectangle... locations) {
		ArrayList<LocationRectangle> rectangles = new ArrayList<LocationRectangle>();
        for (LocationRectangle rectangle : locations) {
            rectangles.add(rectangle);
        }
        return rectangles;

    }
}
