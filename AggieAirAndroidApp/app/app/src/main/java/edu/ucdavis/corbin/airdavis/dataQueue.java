package edu.ucdavis.corbin.airdavis;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;

import java.util.LinkedList;
import java.util.Queue;

/**
 * dataQueue
 *
 * tags incoming data with location and attempts to push it to db service
 *
 */

public class dataQueue extends Service {
    private LocationRequest locationRequest;
    private final static String TAG = dataQueue.class.getSimpleName();
    private FusedLocationProviderClient fusedLocationClient;
    Queue<dataSet> q = new LinkedList<dataSet>();
    private dataBaseService dbService;
    private Location lastKnown;

    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 500; // 1/2second
    private static final float LOCATION_DISTANCE = 10f; // accuracy

    // define listener class, updates last known location
    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
    // listener on gps provider and network
    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        dataQueue getService() {
            return dataQueue.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unbindService(dbServiceConnection);
        dbService.stopSelf();
        dbService = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private ServiceConnection dbServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            dbService = ((dataBaseService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected dbService= " + dbService);
            if (!dbService.initialize()) {
                Log.e(TAG, "Unable to initialize database connection");
            }
        }
        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            dbService = null;
        }
    };


    public boolean initialize() {
        Intent bindIntent = new Intent(this, dataBaseService.class);
        bindService(bindIntent, dbServiceConnection, Context.BIND_AUTO_CREATE);

        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        return true;
    }



    public void Enqueue(final int pm25) {
        Location location = null;
        if(mLocationListeners[0].mLastLocation != null) { // GPS is better, try that first
            location = mLocationListeners[0].mLastLocation;
        } else if (mLocationListeners[1].mLastLocation != null) { // if no GPS provider, try network location
            location = mLocationListeners[1].mLastLocation;
        }
        if(location == null) return;
        if(location.getLatitude() < 38 || location.getLatitude() > 39) return; // not even close to davis
        int unsignedPM = pm25;
        if(unsignedPM < 0) unsignedPM += 256; // convert from twos complement
        lastKnown = location;
        // Logic to handle location object
        dataSet temp = new dataSet(location, pm25);
        if(dbService!=null && dbService.init) {
            dbService.addItem(temp);
        }
    }
}
