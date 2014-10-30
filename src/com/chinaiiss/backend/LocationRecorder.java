package com.chinaiiss.backend;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.chinaiiss.location.ApproximateLocation;

public class LocationRecorder implements ExploredProvider {

	private static final String TAG = LocationRecorder.class.getName();
	private static final String DATABASE_NAME = "visited.db";

	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "coordinates";
	private static final String LONGITUDE = "longitude";
	private static final String LATITUDE = "latitude";

	private static final String DATABASE_PROVIDER = "Visited";

	private static final String INSERT = "insert into " + TABLE_NAME + "("
			+ LATITUDE + "," + LONGITUDE + ") values (?, ?)";

	private Context mContext;

	private SQLiteDatabase mDatabase;
	private SQLiteStatement mInsertStmt;
	private OpenHelper openHelper; 
	public LocationRecorder(Context context) {
		this.mContext = context;
		if(openHelper == null){
			openHelper = new OpenHelper(context);
		}
//		this.mDatabase = openHelper.getWritableDatabase();
//		this.mInsertStmt = this.mDatabase.compileStatement(INSERT);
	}

	@Override
	public long insert(ApproximateLocation location) {
//		this.mInsertStmt.bindDouble(1, location.getLatitude());
//		this.mInsertStmt.bindDouble(2, location.getLongitude());
//		long index = this.mInsertStmt.executeInsert();
		mDatabase = openHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(LATITUDE, location.getLatitude());
		values.put(LONGITUDE, location.getLongitude());
		long index = mDatabase.insert(TABLE_NAME, null, values);
		return index;
	}

	public void insert(List<ApproximateLocation> locations) {
//		DatabaseUtils.InsertHelper batchInserter = new DatabaseUtils.InsertHelper(
//				this.mDatabase, TABLE_NAME);
//		int latitudeIndex = batchInserter.getColumnIndex(LATITUDE);
//		int longitudeIndex = batchInserter.getColumnIndex(LONGITUDE);
//		for (ApproximateLocation approximateLocation : locations) {
//			batchInserter.prepareForInsert();
//			batchInserter
//					.bind(latitudeIndex, approximateLocation.getLatitude());
//			batchInserter.bind(longitudeIndex,
//					approximateLocation.getLongitude());
//			batchInserter.execute();
//		}
//		batchInserter.close();
		mDatabase = openHelper.getWritableDatabase();
		if(mDatabase.isOpen()){
			mDatabase.beginTransaction();
			
			try {
				for (ApproximateLocation approximateLocation : locations) {
					ContentValues values = new ContentValues();
					values.put(LATITUDE, approximateLocation.getLatitude());
					values.put(LONGITUDE, approximateLocation.getLongitude());
					long index = mDatabase.insert(TABLE_NAME, null, values);
				}
				mDatabase.setTransactionSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				mDatabase.endTransaction();
			}
			mDatabase.close();
		}
		
		
	}

	@Override
	public List<ApproximateLocation> selectAll() {
		List<ApproximateLocation> list = new ArrayList<ApproximateLocation>();
		mDatabase = openHelper.getReadableDatabase();
		Cursor cursor = mDatabase.query(TABLE_NAME, new String[] {
				LATITUDE, LONGITUDE }, null, null, null, null, LONGITUDE
				+ " desc");
		Log.d(TAG, "Results obtained: " + cursor.getCount());
		if (cursor.moveToFirst()) {
			do {
				ApproximateLocation location = new ApproximateLocation(
						DATABASE_PROVIDER);
				location.setLatitude(cursor.getDouble(0));
				location.setLongitude(cursor.getDouble(1));
				list.add(location);
			} while (cursor.moveToNext());
		}
		if (!cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	@Override
	public List<ApproximateLocation> selectVisited(
			ApproximateLocation upperLeft, ApproximateLocation bottomRight) {
		List<ApproximateLocation> list = new ArrayList<ApproximateLocation>();
		double longitudeMin = upperLeft.getLongitude();
		double latitudeMax = upperLeft.getLatitude();
		double longitudeMax = bottomRight.getLongitude();
		double latitudeMin = bottomRight.getLatitude();

		String condition = LONGITUDE + " >= " + longitudeMin + " AND "
				+ LONGITUDE + " <= " + longitudeMax + " AND " + LATITUDE
				+ " >= " + latitudeMin + " AND " + LATITUDE + " <= "
				+ latitudeMax;

		Log.v(TAG, "Select condition is " + condition);
		mDatabase = openHelper.getReadableDatabase();
		Cursor cursor = mDatabase.query(TABLE_NAME, new String[] {
				LATITUDE, LONGITUDE }, condition, null, null, null, LATITUDE
				+ " desc");
		Log.d(TAG, "Results obtained: " + cursor.getCount());
		if (cursor.moveToFirst()) {
			do {
				ApproximateLocation location = new ApproximateLocation(
						DATABASE_PROVIDER);
				location.setLatitude(cursor.getDouble(0));
				location.setLongitude(cursor.getDouble(1));
				Log.v(TAG, "Added to list of results obtained: " + location);
				list.add(location);
			} while (cursor.moveToNext());
		}
		if (!cursor.isClosed()) {
			cursor.close();
		}
		if (mDatabase.isOpen()){
			mDatabase.close();
		}
		return list;
	}

	@Override
	public void destroy() {
	}

	/**
	 * 数据库帮助类
	 * 
	 * @author tom
	 * 
	 */
	private static class OpenHelper extends SQLiteOpenHelper {

		public OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "Creating database :" + DATABASE_NAME);
			db.execSQL("CREATE TABLE " + TABLE_NAME
					+ "(id INTEGER PRIMARY KEY AUTOINCREMENT," + LATITUDE + " REAL, "
					+ LONGITUDE + " REAL)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			if (newVersion > oldVersion) {
				db.beginTransaction();
				boolean success = true;
				for (int i = oldVersion; i < newVersion; ++i) {
					int nextVersion = i + 1;
					switch (nextVersion) {
					case 2:
						break;

					default:
						break;
					}

					if (!success) {
						break;
					}
				}

				if (success) {
					db.setTransactionSuccessful();
				}
				db.endTransaction();
			} else {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
				onCreate(db);
			}
		}

	}

	@Override
	public void deleteAll() {
		this.mDatabase.delete(TABLE_NAME, null, null);
	}

}
