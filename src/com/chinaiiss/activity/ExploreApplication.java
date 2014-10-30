package com.chinaiiss.activity;

import com.chinaiiss.backend.ExploredProvider;
import com.chinaiiss.backend.VisitedAreaCache;

import android.app.Application;

public class ExploreApplication extends Application {
	
	private ExploredProvider cache;

	@Override
	public void onCreate() {
		cache = new VisitedAreaCache(this);
	};

	public ExploredProvider getCache() {
		return cache;
	}

	public void setCache(ExploredProvider cache) {
		this.cache = cache;
	}
}
