package jp.gr.java_conf.ya.geologger; // Copyright (c) 2017 YA <ya.androidapp@gmail.com> All rights reserved. This software includes the work that is distributed in the Apache License 2.0

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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
    public Loc loc;
    private ArrayAdapter<String> adapter;
    private BroadcastReceiver locationUpdateReceiver;
    private Button startButton, stopButton;
    private DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private ListView listView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    // 位置情報Permissionが許可されているか判定する
    private Boolean permissionsGranted() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            final String name = className.getClassName();
            if (name.endsWith("Loc")) {
                loc = ((Loc.LocationServiceBinder) service).getService();
                if (!permissionsGranted()) {
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
}
