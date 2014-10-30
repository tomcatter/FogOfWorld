package com.chinaiiss.backend;

import java.util.List;

import com.chinaiiss.location.ApproximateLocation;

public interface ExploredProvider {

	long insert(ApproximateLocation location);
	
	void deleteAll();

	List<ApproximateLocation> selectAll();

	/**
	 * 返回一个指定区域中的所有访问过的坐点
	 * 
	 * @param upperLeft
	 * @param bottomRight
	 * @return
	 */
	List<ApproximateLocation> selectVisited(ApproximateLocation upperLeft,
			ApproximateLocation bottomRight);

	void destroy();
}
