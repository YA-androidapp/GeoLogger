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

import static jp.gr.java_conf.ya.geologger.SqliteUtil.df;

public class Loc extends Service implements LocationListener, GpsStatus.Listener {
    private ArrayList<Location> locationList;
    private boolean enable_add_random_error_to_offset;
    private boolean enable_exception_area;
    private boolean enable_offset;
    private boolean flgLocationManager, flgLogging;
    private double exception_areas_radius;
    private double maximum_value_of_random_error_in_offset;
    private double offset_lat;
    private double offset_lng;
    private int gps_distance;
    private int gps_interval;
    private LocationManager locationManager;
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private SqliteUtil sqliteUtil;

    public class LocationServiceBinder extends Binder {
        public Loc getService() {
            return Loc.this;
        }
    }

    private Location addOffset(final Location location, final boolean containsError) {
        if (containsError) {
            offset_lat += maximum_value_of_random_error_in_offset * Math.random();
            offset_lng += maximum_value_of_random_error_in_offset * Math.random();
        }

        final Location newLocation = new Location(location);
        newLocation.setLatitude(location.getLatitude() + offset_lat);
        newLocation.setLatitude(location.getLongitude() + offset_lng);
        return newLocation;
    }

    private boolean init() {
        locationList = new ArrayList<>();

        // 設定値
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        enable_add_random_error_to_offset = sharedPreferences.getBoolean("enable_add_random_error_to_offset", false);
        enable_exception_area = sharedPreferences.getBoolean("enable_exception_area", false);
        enable_offset = sharedPreferences.getBoolean("enable_offset", false);

        try {
            exception_areas_radius = Double.parseDouble(sharedPreferences.getString("exception_areas_radius", "0.0"));
        } catch (Exception e) {
            exception_areas_radius = 0;
        }


        try {
            gps_distance = Integer.parseInt(sharedPreferences.getString("gps_distance", "-1"));
        } catch (Exception e) {
            gps_distance = -1;
        }
        try {
            gps_interval = Integer.parseInt(sharedPreferences.getString("gps_interval", "-1"));
        } catch (Exception e) {
            gps_interval = -1;
        }

        try {
            maximum_value_of_random_error_in_offset = Double.parseDouble(sharedPreferences.getString("maximum_value_of_random_error_in_offset", "0.0"));
        } catch (Exception e) {
            maximum_value_of_random_error_in_offset = 0;
        }
        try {
            offset_lat = Double.parseDouble(sharedPreferences.getString("offset_lat", "0.0"));
        } catch (Exception e) {
            offset_lat = 0;
        }
        try {
            offset_lng = Double.parseDouble(sharedPreferences.getString("offset_lng", "0.0"));
        } catch (Exception e) {
            offset_lng = 0;
        }

        return true;
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

        init();
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
        if (sqliteUtil == null)
            sqliteUtil = new SqliteUtil(this);

        if (flgLogging) {
            if ((enable_exception_area && !sqliteUtil.isContainsExceptionArea(location, exception_areas_radius)) // 例外区域有効かつ例外区域に含まれていない
                    || (!enable_exception_area)) {
                if (enable_offset)
                    location = addOffset(location, enable_add_random_error_to_offset);

                // ArrayListに追加
                locationList.add(location);

                // Intent発行
                final Intent intent = new Intent("LocationUpdated");
                intent.putExtra("location", location);
                LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

                // SQLite DBに挿入
                final Date date = new Date();
                sqliteUtil.insertLog(date, location);
            }
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
            locationList.clear();

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
                locationManager.requestLocationUpdates(gps_interval, gps_distance, criteria, this, null);
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
