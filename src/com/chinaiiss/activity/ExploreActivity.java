package com.chinaiiss.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.chinaiiss.backend.ExploredProvider;
import com.chinaiiss.io.GpxImporter;
import com.chinaiiss.location.ApproximateLocation;
import com.chinaiiss.overlay.ExploreOverlay;
import com.chinaiiss.service.LocationService;
import com.chinaiiss.utils.LocationUtils;
import com.tencent.tencentmap.mapsdk.map.MapActivity;
import com.tencent.tencentmap.mapsdk.map.MapController;
import com.tencent.tencentmap.mapsdk.map.MapView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import static com.chinaiiss.utils.LocationUtils.coordinatesToGeoPoint;
import static com.chinaiiss.utils.LocationUtils.coordinatesToLocation;

public class ExploreActivity extends MapActivity {
	
	private static final String TAG = ExploreActivity.class.getName();
	
	private static final int INITIAL_ZOOM = 17;
	
	private static final int ZOOM_CHECKING_DELAY = 500;
	
	  /**
     * Constant used for saving the accuracy value between screen rotations.
     */
    private static final String BUNDLE_ACCURACY = "com.chinaiiss.accuracy";
    /**
     * Constant used for saving the latitude value between screen rotations.
     */
    private static final String BUNDLE_LATITUDE = "com.chinaiiss.latitude";
    /**
     * Constant used for saving the longitude value between screen rotations.
     */
    private static final String BUNDLE_LONGITUDE = "com.chinaiiss.longitude";
    /**
     * Constant used for saving the zoom level between screen rotations.
     */
    private static final String BUNDLE_ZOOM = "com.chinaiiss.zoom";
    
    
    /**
     * Intent named used for starting the location service
     *
     * @see LocationService
     */
    private static final String SERVICE_INTENT_NAME = "com.chinaiiss.service.LocationService";
    
    /**
     * Dialog displayed while loading the explored points at application start.
     */
    private ProgressDialog mloadProgress;

    /**
     * Location service intent.
     *
     * @see LocationService
     */
    private Intent mLocationServiceIntent;

    /**
     * Map overlay displaying the explored area. Updated on location changed.
     */
    private ExploreOverlay mExplored;
    
    
    private MapController mMapController;

    /**
     * Source for obtaining explored area information.
     */
    private ExploredProvider mRecorder;
    /**
     * Current device latitude. Updated on every location change.
     */
    private double mCurrentLat;
    /**
     * Current device longitude. Updated on every location change.
     */
    private double mCurrentLong;
    /**
     * Current location accuracy . Updated on every location change.
     */
    private double mCurrentAccuracy;

    /**
     * Flag signaling if the application is visible. Used to stop overlay updates if the map is
     * currently not visible.
     */
    private boolean mVisible = true;
    /**
     * Flag signaling if the user is walking or driving. It is passed to the location service in
     * order to change location update frequency.
     *
     * @see LocationService
     */
    private boolean mDrive;

    /**
     * Handler used to update the overlay if a pan or zoom action occurs.
     */
    private Handler mZoomPanHandler = new Handler();

    /**
     * Messenger for communicating with service.
     */
    private Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    private boolean mIsBound;
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private SharedPreferences mSettings;
    
    
    /**
     * Runnable to be executed by the pan and zoom handler.
     */
    private Runnable mZoomChecker = new Runnable() {
		private int oldZoom = -1;
		private int oldCenterLat = -1;
		private int oldCenterLong = -1;
    	
		@Override
		public void run() {
			MapView mapView = (MapView) findViewById(R.id.mapview);
			int mapCenterLat = mapView.getMapCenter().getLatitudeE6();
			int mapCenterLong = mapView.getMapCenter().getLongitudeE6();
			//检查缩放
			if(mapView.getZoomLevel() != oldZoom ||
					oldCenterLat != mapCenterLat ||
					oldCenterLong != mapCenterLong){
				//TODO
				redrawOverlay();
				oldZoom = mapView.getZoomLevel();
				oldCenterLat = mapCenterLat;
				oldCenterLong = mapCenterLong;
			}
			
			//开始新一轮的检查
			mZoomPanHandler.removeCallbacks(mZoomChecker);
			mZoomPanHandler.postDelayed(mZoomChecker, ZOOM_CHECKING_DELAY);
		}
	};
    
	
	  /**
     * Handler of incoming messages from service.
     */
    private class IncomingHandler extends Handler {
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationService.MSG_LOCATION_CHANGED:
                    if (msg.obj != null) {
                        Log.d(TAG, ((Location) msg.obj).toString());

                        mCurrentLat = ((Location) msg.obj).getLatitude();
                        mCurrentLong = ((Location) msg.obj).getLongitude();
                        mCurrentAccuracy = ((Location) msg.obj).getAccuracy();
                        redrawOverlay();

                    } else {
                        Log.d(TAG, "Null object received");
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            Log.d(TAG, "Location service attached.");
            // register client
            sendMessage(LocationService.MSG_REGISTER_CLIENT);
            // register interface
            sendMessage(LocationService.MSG_REGISTER_INTERFACE);

            // send walk or drive mode
            sendMessage(mDrive ? LocationService.MSG_DRIVE
                    : LocationService.MSG_WALK);
        }

        /*
         * (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content
         * .ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName className) {
            // Called when the connection with the service has been
            // unexpectedly disconnected / process crashed.
            mService = null;
            Log.d(TAG, "Disconnected from location service");
        }
    };
    
    /**
     * Drive or walk preference listener. A listener is necessary for this option as the location
     * service needs to be notified of the change in order to change location update frequency. The
     * preference is sent when the activity comes into view and rebinds to the location service.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "Settings changed :" + sharedPreferences + " " + key);
            mDrive = mSettings.getBoolean(Preferences.DRIVE_MODE, false);
        }
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		mloadProgress = ProgressDialog.show(this, "", "Loading. Please wait...", true);
		mSettings.registerOnSharedPreferenceChangeListener(mPrefListener);
		setContentView(R.layout.explore);
		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
//		mapView.setBackgroundColor(Color.RED);
		mExplored = new ExploreOverlay(this);
		mapView.addOverlay(mExplored);
		mMapController = mapView.getController();
		mMapController.setZoom(INITIAL_ZOOM);
		//启动服务
	    mLocationServiceIntent = new Intent(SERVICE_INTENT_NAME);
	    startService(mLocationServiceIntent);
	    mRecorder = ((ExploreApplication)getApplication()).getCache();
	    loadFileFromIntent();
	 // check we still have access to GPS info
        checkConnectivity();
	}

	/**
     * Loads a gpx data from a file path send through an intent.
     */
    private void loadFileFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {

            Uri data = intent.getData();

            if (data != null) {

                final String filePath = data.getEncodedPath();

                final ProgressDialog progress = new ProgressDialog(this);

                progress.setCancelable(false);
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setMessage(getString(R.string.importing_locations));
                progress.show();

                Runnable importer = new Runnable() {

                    @Override
                    public void run() {
                        ExploredProvider cache = ((ExploreApplication) getApplication())
                                .getCache();

                        try {
                            GpxImporter.importGPXFile(new FileInputStream(
                                    new File(filePath)), cache);
                        } catch (ParserConfigurationException e) {
                            Log.e(TAG, "Error parsing file", e);
                        } catch (SAXException e) {
                            Log.e(TAG, "Error parsing file", e);
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading file", e);
                        }

                        progress.dismiss();
                    }
                };
                new Thread(importer).start();
            }
        }
    }
	
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	// TODO Auto-generated method stub
    	// restore accuracy and coordinates from saved state
        mCurrentAccuracy = savedInstanceState.getDouble(BUNDLE_ACCURACY);
        mCurrentLat = savedInstanceState.getDouble(BUNDLE_LATITUDE);
        mCurrentLong = savedInstanceState.getDouble(BUNDLE_LONGITUDE);
        mMapController.setZoom(savedInstanceState.getInt(BUNDLE_ZOOM));
    	super.onRestoreInstanceState(savedInstanceState);
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // save accuracy and coordinates
        outState.putDouble(BUNDLE_ACCURACY, mCurrentAccuracy);
        outState.putDouble(BUNDLE_LATITUDE, mCurrentLat);
        outState.putDouble(BUNDLE_LONGITUDE, mCurrentLong);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        outState.putInt(BUNDLE_ZOOM, mapView.getZoomLevel());
        super.onSaveInstanceState(outState);
    }
    
    
    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        // register zoom && pan mZoomPanHandler
        mZoomPanHandler.postDelayed(mZoomChecker, ZOOM_CHECKING_DELAY);
        // set the visibility flag to start overlay updates
        mVisible = true;
        redrawOverlay();
        mloadProgress.cancel();
        Log.d(TAG, "onResume completed.");
        // bind to location service
        doBindService();
    }

    
    

    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        mZoomPanHandler.removeCallbacks(mZoomChecker);
        mVisible = false;
        // unbind from service as the activity does
        // not display location info (is hidden or stopped)
        doUnbindService();
        Log.d(TAG, "onPause completed.");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop completed.");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onRestart()
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart completed.");
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mZoomPanHandler.removeCallbacks(mZoomChecker);
        Log.d(TAG, "onDestroy completed.");
    }

    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.where_am_i).setIcon(
                android.R.drawable.ic_menu_mylocation);
        menu.findItem(R.id.settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.findItem(R.id.help).setIcon(android.R.drawable.ic_menu_help);
        menu.findItem(R.id.exit).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        return result;
    }
    
    /*
    * (non-Javadoc)
    * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
    */
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle item selection
            switch (item.getItemId()) {
                case R.id.where_am_i:
                    Log.d(TAG, "Moving to current location...");
                    mMapController.setCenter(coordinatesToGeoPoint(mCurrentLat,
                            mCurrentLong));
                    redrawOverlay();
                    return true;
                case R.id.help:
                    Log.d(TAG, "Showing help...");
                    Intent helpIntent = new Intent(this, Help.class);
                    startActivity(helpIntent);
                    return true;
                case R.id.exit:
                    Log.d(TAG, "Exit requested...");
                    doUnbindService();
                    // cleanup
                    stopService(mLocationServiceIntent);
                    mRecorder.destroy();
                    finish();
                    return true;
                case R.id.settings:
                    Intent settingsIntent = new Intent(this, Preferences.class);
                    startActivity(settingsIntent);
                    return true;
            }
            return super.onOptionsItemSelected(item);
        }
    
        
        
    
	/**更新当前的位置并且重绘覆盖物*/
	private void redrawOverlay(){
		//判断本界面是否可见，不可见则不重绘
		if(!mVisible){
			return;
		}
		
		//获取可见区域的坐标位置
		final MapView mapView = (MapView) findViewById(R.id.mapview);
		final int halfLatSpan = mapView.getLatitudeSpan() / 2;
		final int halfLongSpan = mapView.getLongitudeSpan() / 2;
		final int mapCenterLat = mapView.getMapCenter().getLatitudeE6();
		final int mapCenterLong = mapView.getMapCenter().getLongitudeE6();
		
		final ApproximateLocation upperLeft = coordinatesToLocation(mapCenterLat + halfLatSpan, 
				mapCenterLong - halfLongSpan);
		final ApproximateLocation bottomRight = coordinatesToLocation(
                mapCenterLat - halfLatSpan, mapCenterLong + halfLongSpan);
		
		  // update the current location for the overlay
        mExplored.setCurrent(mCurrentLat, mCurrentLong, mCurrentAccuracy);
        // update the overlay with the currently visible explored area
        mExplored.setExplored(mRecorder.selectVisited(upperLeft, bottomRight));
        // animate the map to the user position if the options to do so is
        // selected
        if (mSettings.getBoolean(Preferences.ANIMATE, false)) {
            mMapController.animateTo(LocationUtils.coordinatesToGeoPoint(
                    mCurrentLat, mCurrentLong));
        }
        // call overlay redraw
        mapView.postInvalidate();
	}
	
    /**
     * Checks GPS and network connectivity. Displays a dialog asking the user to start the GPS if
     * not started and also displays a toast warning it no network connectivity is available.
     */
    private void checkConnectivity() {

        boolean isGPS = ((LocationManager) getSystemService(LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGPS) {
            createGPSDialog().show();
        }
        displayConnectivityWarning();
    }
	
    
    /**
     * Displays a toast warning if no network is available.
     */
    private void displayConnectivityWarning() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = false;
        for (NetworkInfo info : connectivityManager.getAllNetworkInfo()) {
            if (info.getState() == NetworkInfo.State.CONNECTED
                    || info.getState() == NetworkInfo.State.CONNECTING) {
                connected = true;
                break;
            }
        }
        if (!connected) {
            Toast.makeText(getApplicationContext(),
                    R.string.connectivity_warning, Toast.LENGTH_LONG).show();
        }
    }
    
    
    /**
     * Creates the GPS dialog displayed if the GPS is not started.
     *
     * @return the GPS Dialog
     */
    private Dialog createGPSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gps_dialog).setCancelable(false);

        final AlertDialog alert = builder.create();

        alert.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.start_gps_btn),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        alert.dismiss();
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                }
        );

        alert.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.continue_no_gps),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        alert.dismiss();
                    }
                }
        );
        return alert;
    }
    
    
    /**
     * Binds to the location service. Called when the activity becomes visible.
     */
    private void doBindService() {
        bindService(mLocationServiceIntent, mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.d(TAG, "Binding to location service");
    }

    /**
     * Unbinds from the location service. Called when the activity is stopped or closed.
     */
    private void doUnbindService() {
        if (mIsBound) {
            // test if we have a valid service registration
            if (mService != null) {
                sendMessage(LocationService.MSG_UNREGISTER_INTERFACE);
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.d(TAG, "Unbinding map from location service.");
        }
    }
    
    
	private void sendMessage(int message) {
        // TODO check message
        try {
            Message msg = Message.obtain(null, message);
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            // NO-OP
            // Nothing special to do if the service
            // has crashed.
        }

    }
}
