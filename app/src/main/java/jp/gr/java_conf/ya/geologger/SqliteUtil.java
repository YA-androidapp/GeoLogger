package jp.gr.java_conf.ya.geologger; // Copyright (c) 2017 YA <ya.androidapp@gmail.com> All rights reserved. This software includes the work that is distributed in the Apache License 2.0

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SqliteUtil extends SQLiteOpenHelper {
    public static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private static final int DB_VERSION = 1;

    public static final String DB_NAME = "geologger";

    private static final String DB_FILENAME = DB_NAME + ".db";

    public static final String COL_ID = "_id";
    public static final String COL_DATE = "date";
    public static final String COL_LAT = "lat";
    public static final String COL_LNG = "lng";
    public static final String COL_COSLAT = "coslat";
    public static final String COL_COSLNG = "coslng";
    public static final String COL_SINLAT = "sinlat";
    public static final String COL_SINLNG = "sinlng";

    private static final String TYPE_ID = "integer primary key autoincrement";
    private static final String TYPE_DATE = "text not null";
    private static final String TYPE_LAT = "real not null";
    private static final String TYPE_LNG = "real not null";
    private static final String TYPE_COSLAT = "real not null";
    private static final String TYPE_COSLNG = "real not null";
    private static final String TYPE_SINLAT = "real not null";
    private static final String TYPE_SINLNG = "real not null";

    public static final String SQL_CREATE_TABLE = "create table " + DB_NAME +
            " ( " +
            COL_ID + " " + TYPE_ID + ", " +
            COL_DATE + " " + TYPE_DATE + ", " +
            COL_LAT + " " + TYPE_LAT + ", " +
            COL_LNG + " " + TYPE_LNG + ", " +
            COL_COSLAT + " " + TYPE_COSLAT + ", " +
            COL_COSLNG + " " + TYPE_COSLNG + ", " +
            COL_SINLAT + " " + TYPE_SINLAT + ", " +
            COL_SINLNG + " " + TYPE_SINLNG +
            " );";
    public static final String SQL_DROP_TABLE = "drop table if exists " + DB_NAME + ";";
    public static final String SQL_SELECT = "SELECT " + COL_DATE + " , " + COL_LAT + ", " + COL_LNG + " FROM " + DB_NAME + "";

    public SqliteUtil(Context c) {
        super(c, DB_FILENAME, null, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }

    //

    // ログ記録
    public void insertLoc(Date date, Location location) {
        final SQLiteDatabase db = getReadableDatabase();
        try {
            final ContentValues values = new ContentValues();
            values.put(COL_DATE, df.format(date));
            values.put(COL_LAT, location.getLatitude());
            values.put(COL_LNG, location.getLongitude());
            values.put(COL_COSLAT, Math.cos(location.getLatitude()));
            values.put(COL_COSLNG, Math.cos(location.getLongitude()));
            values.put(COL_SINLAT, Math.sin(location.getLatitude()));
            values.put(COL_SINLNG, Math.sin(location.getLongitude()));
            db.insert(DB_NAME, null, values);
            values.clear();
        } finally {
            db.close();
        }

        // Log.w("SqliteUtil", select());
    }

    // 非記録エリア判定用
    public String searchNear(Location location, double range_km) {
        final StringBuilder sb = new StringBuilder();

        final SQLiteDatabase db = getReadableDatabase();
        try {
            final String sql = searchNearQuery(location, range_km);
            final Cursor cursor = db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                sb.append(cursor.getInt(0));
                sb.append("," + cursor.getString(1));
                sb.append("," + cursor.getString(2));
                sb.append("," + cursor.getString(3));
                sb.append("\n");
            }
        } finally {
            db.close();
        }

        return sb.toString();
    }

    // 非記録エリア判定用
    public int searchNearCount(Location location, double range_km) {
        final SQLiteDatabase db = getReadableDatabase();
        try {
            final String sql = searchNearQuery(location, range_km);
            final Cursor cursor = db.rawQuery(sql, null);
            final int count = cursor.getColumnCount();
            db.close();
            return count;
        } catch (Exception e) {
            db.close();
            return 0;
        }
    }

    // 非記録エリア判定用
    // http://strai.x0.com/?p=200
    public String searchNearQuery(Location location, double range_km) {
        final double km_cos = Math.cos(range_km / 6371);    // 距離基準cos値
        final double radlat = Math.toRadians(location.getLatitude()), radlong = Math.toRadians(location.getLongitude());
        final double qsinlat = Math.sin(radlat), qcoslat = Math.cos(radlat);
        final double qsinlng = Math.sin(radlong), qcoslng = Math.cos(radlong);

        final StringBuilder sb = new StringBuilder();
        sb.append("SELECT " + COL_ID + " ");
        sb.append("(" + COL_SINLAT + "*" + qsinlat + " + " + COL_COSLAT + "*" + qcoslat + "*(" + COL_COSLNG + "*" + qcoslng + "+" + COL_SINLNG + "*" + qsinlng + ")) AS distcos ");
        sb.append(" FROM " + DB_NAME + " ");
        sb.append(" WHERE distcos > " + km_cos); // 値が大きい方が近い
        sb.append(" ORDER BY distcos DESC ");
        return sb.toString();
    }

    // 全件取得
    public String select() {
        final StringBuilder sb = new StringBuilder();

        final SQLiteDatabase db = getReadableDatabase();
        try {
            final Cursor cursor = db.rawQuery(SQL_SELECT, null);
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                sb.append(cursor.getString(0)).append(",");
                sb.append(cursor.getDouble(1)).append(",");
                sb.append(cursor.getDouble(2)).append("\n");
                cursor.moveToNext();
            }
            cursor.close();
        } finally {
            db.close();
        }

        return sb.toString();
    }
}