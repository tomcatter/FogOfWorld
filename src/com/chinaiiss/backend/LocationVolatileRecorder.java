package com.chinaiiss.backend;

import java.util.ArrayList;
import java.util.List;

import com.chinaiiss.location.ApproximateLocation;

public class LocationVolatileRecorder implements ExploredProvider {
	
	
	private List<ApproximateLocation> mLocations = new ArrayList<ApproximateLocation>();

	private static LocationVolatileRecorder mInstance;

	private LocationVolatileRecorder() {
		super();
	}

	public static LocationVolatileRecorder getInstance() {
		return (mInstance == null) ? mInstance = new LocationVolatileRecorder()
				: mInstance;
	}

	@Override
	public void deleteAll() {
		mLocations.clear();

	}

	@Override
	public synchronized long insert(ApproximateLocation location) {
		mLocations.add(location);
		return mLocations.size();
	}

	@Override
	public List<ApproximateLocation> selectAll() {
		return mLocations;
	}

	@Override
	public List<ApproximateLocation> selectVisited(
			ApproximateLocation upperLeft, ApproximateLocation bottomRight) {
		ArrayList<ApproximateLocation> visited = new ArrayList<ApproximateLocation>();
		for (ApproximateLocation location : mLocations) {
			if (location.getLatitude() >= upperLeft.getLatitude()
					&& location.getLatitude() <= bottomRight.getLatitude()
					&& location.getLongitude() >= upperLeft.getLongitude()
					&& location.getLongitude() <= bottomRight.getLongitude()) {
				visited.add(location);
			}
		}
		return visited;
	}

	@Override
	public void destroy() {

	}

}
