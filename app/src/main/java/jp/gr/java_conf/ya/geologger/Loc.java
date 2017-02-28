package jp.gr.java_conf.ya.geologger; // Copyright (c) 2017 YA <ya.androidapp@gmail.com> All rights reserved. This software includes the work that is distributed in the Apache License 2.0

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

public class Loc extends Service implements LocationListener, GpsStatus.Listener {
    // private ArrayList<Location> locationList;
    private boolean flgLocationManager, flgLogging;
    private double offset_lat;
    private double offset_lng;
    private static final int GPS_INTERVAL = -1;
    private static final int GPS_DISTANCE = -1;
    private LocationManager locationManager;
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private SqliteUtil sqliteUtil;

    public class LocationServiceBinder extends Binder {
        public Loc getService() {
            return Loc.this;
        }
    }

    private void notifyLocationProviderStatusUpdated(boolean isLocationProviderAvailable) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        flgLocationManager = false;
        flgLogging = false;
        // locationList = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        if (locationManager == null)
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                locationManager.removeUpdates(this);
        } catch (SecurityException e) {
        } catch (RuntimeException e) {
        } catch (Exception e) {
        }
    }

    public void onGpsStatusChanged(int event) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (flgLogging) {
            // locationList.add(location);

            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            try {
                offset_lat = Double.parseDouble(sharedPreferences.getString("offset_lat", "0.0"));
            }catch (Exception e){
                offset_lat = 0;
            }
            try {
                offset_lng = Double.parseDouble(sharedPreferences.getString("offset_lng", "0.0"));
            }catch (Exception e){
                offset_lng = 0;
            }
            if(offset_lat != 0 || offset_lng != 0) {
                final Location newLocation = new Location(location);
                newLocation.setLatitude(location.getLatitude() + offset_lat);
                newLocation.setLatitude(location.getLongitude() + offset_lng);
                location = newLocation;
            }

            // Intent発行
            final Intent intent = new Intent("LocationUpdated");
            intent.putExtra("location", location);
            LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

            // SQLite DBに挿入
            if (sqliteUtil == null)
                sqliteUtil = new SqliteUtil(this);
            final Date date = new Date();
            sqliteUtil.insertLog(date, location);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER))
            notifyLocationProviderStatusUpdated(false);
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER))
            notifyLocationProviderStatusUpdated(true);
    }

    @Override
    public void onRebind(Intent intent) {
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        super.onStartCommand(i, flags, startId);
        return Service.START_STICKY;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                notifyLocationProviderStatusUpdated(false);
            } else {
                notifyLocationProviderStatusUpdated(true);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopUpdatingLocation();

        stopSelf();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public void startLogging() {
        flgLogging = true;
    }

    public void startUpdatingLocation() {

        if (flgLocationManager == false) {
            flgLocationManager = true;
            // locationList.clear();

            if (locationManager == null)
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                final Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(false);
                criteria.setCostAllowed(true);
                criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                criteria.setSpeedRequired(false);
                criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
                locationManager.addGpsStatusListener(this);
                locationManager.requestLocationUpdates(GPS_INTERVAL, GPS_DISTANCE, criteria, this, null);
            } catch (SecurityException e) {
            } catch (RuntimeException e) {
            } catch (Exception e) {
            }
        }
    }

    public void stopLogging() {
        flgLogging = false;
    }

    public void stopUpdatingLocation() {
        if (this.flgLocationManager) {
            if (locationManager == null)
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                locationManager.removeUpdates(this);
            flgLocationManager = false;
        }
    }
}
