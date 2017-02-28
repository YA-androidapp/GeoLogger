package jp.gr.java_conf.ya.geologger; // Copyright (c) 2017 YA <ya.androidapp@gmail.com> All rights reserved. This software includes the work that is distributed in the Apache License 2.0

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Loc loc;
    private ArrayAdapter<String> adapter;
    private BroadcastReceiver locationUpdateReceiver;
    private Button clearButton, clearExceptionAreaButton, insertExceptionAreaButton, startButton, stopButton, writeButton;
    private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private EditText exceptionAreaCenterLat, exceptionAreaCenterLng;
    private ListView listView1;

    private static final String filenameExc = "GeoLoggerExc.txt";
    private static final String filenameLog = "GeoLoggerLog.txt";

    // 設定値
    private boolean enable_offset;
    private boolean enable_add_random_error_to_offset;
    private boolean enable_exception_area;
    private double exception_areas_radius;
    private double offset_lat;
    private double offset_lng;

    private boolean init() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        enable_offset = sharedPreferences.getBoolean("enable_offset", false);
        enable_add_random_error_to_offset = sharedPreferences.getBoolean("enable_add_random_error_to_offset", false);
        enable_exception_area = sharedPreferences.getBoolean("enable_exception_area", false);
        try {
            exception_areas_radius = Double.parseDouble(sharedPreferences.getString("exception_areas_radius", "0.0"));
        }catch (Exception e){
            exception_areas_radius = 0;
        }
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

        // EditTextを準備
        exceptionAreaCenterLat = (EditText) findViewById(R.id.exceptionAreaCenterLat);
        exceptionAreaCenterLng = (EditText) findViewById(R.id.exceptionAreaCenterLng);

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
        clearButton = (Button) this.findViewById(R.id.clearButton);
        clearExceptionAreaButton = (Button) this.findViewById(R.id.clearExceptionAreaButton);
        insertExceptionAreaButton = (Button) this.findViewById(R.id.insertExceptionAreaButton);
        startButton = (Button) this.findViewById(R.id.startButton);
        stopButton = (Button) this.findViewById(R.id.stopButton);
        writeButton = (Button) this.findViewById(R.id.writeButton);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 確認後、全件削除
                try {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.clear_all_locations)
                            .setMessage(R.string.confirm_clear)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final SqliteUtil sqliteUtil = new SqliteUtil(MainActivity.this);
                                    sqliteUtil.clearAllLogs();

                                    Toast.makeText(MainActivity.this, getString(R.string.done),Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } catch (Exception e) {
                }
            }
        });
        clearExceptionAreaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 確認後、全件削除
                try {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.clear_all_exception_areas)
                            .setMessage(R.string.confirm_clear)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SqliteUtil sqliteUtil = new SqliteUtil(MainActivity.this);
                                    sqliteUtil.clearAllExceptionAreas();

                                    Toast.makeText(MainActivity.this, getString(R.string.done),Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } catch (Exception e) {
                }
            }
        });
        insertExceptionAreaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( (!exceptionAreaCenterLat.getText().toString().equals("")) && (!exceptionAreaCenterLng.getText().toString().equals("")) ) {
                    double lat, lng;
                    try {
                        lat = Double.parseDouble(exceptionAreaCenterLat.getText().toString());
                    } catch (Exception e) {
                        lat = 0;
                    }
                    try {
                        lng = Double.parseDouble(exceptionAreaCenterLng.getText().toString());
                    } catch (Exception e) {
                        lng = 0;
                    }

                    final SqliteUtil sqliteUtil = new SqliteUtil(MainActivity.this);
                    sqliteUtil.insertExceptionArea(lat, lng);

                    Toast.makeText(MainActivity.this, (exceptionAreaCenterLat.getText().toString() + "," + exceptionAreaCenterLng.getText().toString()), Toast.LENGTH_SHORT).show();
                }
            }
        });
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
                    final String resultLog = FileUtil.writeFile(
                            filenameLog,
                            (new SqliteUtil(MainActivity.this)).selectAllLogs()
                    );
                    Toast.makeText(MainActivity.this, resultLog, Toast.LENGTH_SHORT).show();

                    final String resultExc = FileUtil.writeFile(
                            filenameExc,
                            (new SqliteUtil(MainActivity.this)).selectAllExceptionAreas()
                    );
                    Toast.makeText(MainActivity.this, resultExc, Toast.LENGTH_SHORT).show();
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
