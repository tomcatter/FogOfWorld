package com.chinaiiss.location;

public class LocationRectangle {

	/** 左上位置 */
	private ApproximateLocation mUpperLeft;
	/** 右下位置 */
	private ApproximateLocation mLowerRight;

	public LocationRectangle(ApproximateLocation upperLeft,
			ApproximateLocation lowerRight) {
		super();
		this.mUpperLeft = upperLeft;
		this.mLowerRight = lowerRight;
	}

	public ApproximateLocation getmUpperLeft() {
		return mUpperLeft;
	}

	public void setmUpperLeft(ApproximateLocation mUpperLeft) {
		this.mUpperLeft = mUpperLeft;
	}

	public ApproximateLocation getmLowerRight() {
		return mLowerRight;
	}

	public void setmLowerRight(ApproximateLocation mLowerRight) {
		this.mLowerRight = mLowerRight;
	}

}
