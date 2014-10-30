package com.chinaiiss.location;

import android.location.Location;

public class ApproximateLocation extends Location {

	private double mX;
	private double mY;
	private double mZ;

	public ApproximateLocation(String provider) {
		super(provider);
	}

	public ApproximateLocation(Location l) {
		super(l);
	}

	private void updateNVector() {
		mX = Math.cos(getLatitude()) * Math.cos(getLongitude());
		mY = Math.cos(getLatitude()) * Math.sin(getLongitude());
		mZ = Math.sin(getLatitude());
	}

	@Override
	public void setLatitude(double latitude) {
		super.setLatitude(latitude);
		updateNVector();
	}

	@Override
	public void setLongitude(double longitude) {
		super.setLongitude(longitude);
		updateNVector();
	}

	@Override
	public boolean equals(Object location) {
		if (!(location instanceof ApproximateLocation)) {
			return false;
		}
		return this.distanceTo((ApproximateLocation) location) <= LocationOrder.METERS_RADIUS;
	}

	@Override
	public int hashCode() {
		int randomPrime = 47;
		int result = 42;
		long hashLong = Double.doubleToLongBits(this.getLongitude());
		long hashLat = Double.doubleToLongBits(this.getLatitude());
		result = (int) (randomPrime * result + hashLong);
		result = (int) (randomPrime * result + hashLat);
		return result;
	}
}
