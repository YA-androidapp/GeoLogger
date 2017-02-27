package jp.gr.java_conf.ya.geologger; // Copyright (c) 2017 YA <ya.androidapp@gmail.com> All rights reserved. This software includes the work that is distributed in the Apache License 2.0

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Loc loc;
    private ArrayAdapter<String> adapter;
    private BroadcastReceiver locationUpdateReceiver;
    private Button startButton, stopButton, writeButton;
    private DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private ListView listView1;

    // 設定値
    private boolean enable_offset;
    private boolean enable_add_random_error_to_offset;
    private boolean enable_exception_area;
    private double offset_lat;
    private double offset_lng;
    private double exception_areas_radius;
    private double exception_areas_centers;

    private boolean init() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        enable_offset = sharedPreferences.getBoolean("enable_offset", false);
        enable_add_random_error_to_offset = sharedPreferences.getBoolean("enable_add_random_error_to_offset", false);
        enable_exception_area = sharedPreferences.getBoolean("enable_exception_area", false);

        offset_lat = Double.parseDouble(sharedPreferences.getString("offset_lat", "0.0"));
        offset_lng = Double.parseDouble(sharedPreferences.getString("offset_lng", "0.0"));
        exception_areas_radius = Double.parseDouble(sharedPreferences.getString("exception_areas_radius", "0.0"));
        exception_areas_centers = Double.parseDouble(sharedPreferences.getString("exception_areas_centers", "0.0"));

        return true;
    }

    // 位置情報Permissionが許可されているか判定する
    private boolean locationPermissionGranted() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        setContentView(R.layout.activity_main);

        // ListViewを準備
        listView1 = (ListView) findViewById(R.id.listView1);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView1.setAdapter(adapter);
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ListView listView = (ListView) parent;
                final String item = (((String) listView.getItemAtPosition(position)).split(";"))[0];
                final Uri uri = Uri.parse("geo:" + item + "?z=15");
                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        listView1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        listView1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });

        // Buttonを準備
        startButton = (Button) this.findViewById(R.id.startButton);
        stopButton = (Button) this.findViewById(R.id.stopButton);
        writeButton = (Button) this.findViewById(R.id.writeButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.INVISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                loc.startLogging();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.INVISIBLE);
                loc.stopLogging();
            }
        });
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!storagePermissionGranted()) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                } else {
                    final String result = FileUtil.writeFile(
                            "GeoLogger.csv",
                            (new SqliteUtil(MainActivity.this)).select()
                    );
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Serviceを準備
        final Intent locService = new Intent(this.getApplication(), Loc.class);
        this.getApplication().startService(locService);
        this.getApplication().bindService(locService, serviceConnection, Context.BIND_AUTO_CREATE);
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final Location location = intent.getParcelableExtra("location");
                final Date date = new Date();
                final String row = location.getLatitude() + "," + location.getLongitude() + "; " + df.format(date);
                adapter.insert(row, 0);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                locationUpdateReceiver,
                new IntentFilter("LocationUpdated")
        );
    }

    @Override
    protected void onDestroy() {
        if (loc != null)
            loc.stopLogging();

        super.onDestroy();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            final String name = className.getClassName();
            if (name.endsWith("Loc")) {
                loc = ((Loc.LocationServiceBinder) service).getService();
                if (!locationPermissionGranted()) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    loc.startUpdatingLocation();
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("Loc")) {
                if (loc != null)
                    loc.stopUpdatingLocation();
                loc = null;
            }
        }
    };

    // ストレージ書出しPermissionが許可されているか判定する
    private boolean storagePermissionGranted() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }
}
