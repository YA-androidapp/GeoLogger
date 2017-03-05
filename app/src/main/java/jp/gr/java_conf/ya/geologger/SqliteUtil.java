package jp.gr.java_conf.ya.geologger; // Copyright (c) 2017 YA <ya.androidapp@gmail.com> All rights reserved. This software includes the work that is distributed in the Apache License 2.0

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SqliteUtil extends SQLiteOpenHelper {
    public static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    private static final int DB_VERSION = 1;

    private static final String DATABASE_NAME = "geologger";
    private static final String DATABASE_FILENAME = DATABASE_NAME + ".db";

    private static final String TABLE_NAME_EXCEPTION = "geologgerexc";
    private static final String TABLE_NAME_LOG = "geologger";

    private static final String COL_COS_LAT = "coslat";
    private static final String COL_COS_LNG = "coslng";
    public static final String COL_DATE = "date";
    public static final String COL_ID = "_id";
    public static final String COL_LAT = "lat";
    public static final String COL_LNG = "lng";
    private static final String COL_SIN_LAT = "sinlat";
    private static final String COL_SIN_LNG = "sinlng";

    private static final String TYPE_COS_LAT = "real not null";
    private static final String TYPE_COS_LNG = "real not null";
    private static final String TYPE_DATE = "text not null";
    private static final String TYPE_ID = "integer primary key autoincrement";
    private static final String TYPE_LAT = "real not null";
    private static final String TYPE_LNG = "real not null";
    private static final String TYPE_SIN_LAT = "real not null";
    private static final String TYPE_SIN_LNG = "real not null";

    public static final String SQL_CREATE_TABLE_EXCEPTION = "create table " + TABLE_NAME_EXCEPTION +
            " ( " +
            COL_ID + " " + TYPE_ID + ", " +
            COL_LAT + " " + TYPE_LAT + ", " +
            COL_LNG + " " + TYPE_LNG + ", " +
            COL_COS_LAT + " " + TYPE_COS_LAT + ", " +
            COL_COS_LNG + " " + TYPE_COS_LNG + ", " +
            COL_SIN_LAT + " " + TYPE_SIN_LAT + ", " +
            COL_SIN_LNG + " " + TYPE_SIN_LNG +
            " );";
    public static final String SQL_CREATE_TABLE_LOG = "create table " + TABLE_NAME_LOG +
            " ( " +
            COL_ID + " " + TYPE_ID + ", " +
            COL_DATE + " " + TYPE_DATE + ", " +
            COL_LAT + " " + TYPE_LAT + ", " +
            COL_LNG + " " + TYPE_LNG + ", " +
            COL_COS_LAT + " " + TYPE_COS_LAT + ", " +
            COL_COS_LNG + " " + TYPE_COS_LNG + ", " +
            COL_SIN_LAT + " " + TYPE_SIN_LAT + ", " +
            COL_SIN_LNG + " " + TYPE_SIN_LNG +
            " );";
    public static final String SQL_DROP_TABLE_EXCEPTION = "drop table if exists " + TABLE_NAME_EXCEPTION + ";";
    public static final String SQL_DROP_TABLE_LOG = "drop table if exists " + TABLE_NAME_LOG + ";";
    public static final String SQL_SELECT_EXCEPTION = "select " + COL_LAT + " , " + COL_LNG + " from " + TABLE_NAME_EXCEPTION + ";";
    public static final String SQL_SELECT_LOG = "select " + COL_DATE + " , " + COL_LAT + ", " + COL_LNG + " from " + TABLE_NAME_LOG + ";";

    public SqliteUtil(Context c) {
        super(c, DATABASE_FILENAME, null, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_EXCEPTION);
        db.execSQL(SQL_CREATE_TABLE_LOG);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE_EXCEPTION);
        db.execSQL(SQL_DROP_TABLE_LOG);
        onCreate(db);
    }

    // 全件消去
    public void clearAllExceptionAreas() {
        final SQLiteDatabase db = getReadableDatabase();
        db.delete(TABLE_NAME_EXCEPTION, null, null);
    }

    public void clearAllLogs() {
        final SQLiteDatabase db = getReadableDatabase();
        db.delete(TABLE_NAME_LOG, null, null);
    }

    public String getUrl(final String id){
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sb2 = new StringBuilder();

        sb.append("select " + COL_LAT + " , " + COL_LNG);
        sb.append(" from " + TABLE_NAME_LOG + " ");
        sb.append(" where " + COL_ID + " = " + id);

        final SQLiteDatabase db = getReadableDatabase();
        try {
            final Cursor cursor = db.rawQuery(sb.toString(), null);
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                sb2.append(cursor.getDouble(0)).append(",");
                sb2.append(cursor.getDouble(1)).append("");
                cursor.moveToNext();
            }
            cursor.close();
        } finally {
            db.close();
        }

        return sb2.toString();
    }

    // 例外区域を追加
    public void insertExceptionArea(double lat, double lng) {
        final SQLiteDatabase db = getReadableDatabase();
        try {
            final ContentValues values = new ContentValues();
            values.put(COL_COS_LAT, Math.cos(lat));
            values.put(COL_COS_LNG, Math.cos(lng));
            values.put(COL_LAT, lat);
            values.put(COL_LNG, lng);
            values.put(COL_SIN_LAT, Math.sin(lat));
            values.put(COL_SIN_LNG, Math.sin(lng));
            db.insert(TABLE_NAME_EXCEPTION, null, values);
            values.clear();
        } finally {
            db.close();
        }
    }

    // ログを記録
    public void insertLog(Date date, Location location) {
        final SQLiteDatabase db = getReadableDatabase();
        try {
            final ContentValues values = new ContentValues();
            values.put(COL_COS_LAT, Math.cos(location.getLatitude()));
            values.put(COL_COS_LNG, Math.cos(location.getLongitude()));
            values.put(COL_DATE, df.format(date));
            values.put(COL_LAT, location.getLatitude());
            values.put(COL_LNG, location.getLongitude());
            values.put(COL_SIN_LAT, Math.sin(location.getLatitude()));
            values.put(COL_SIN_LNG, Math.sin(location.getLongitude()));
            db.insert(TABLE_NAME_LOG, null, values);
            values.clear();
        } finally {
            db.close();
        }
    }

    // 非記録エリア判定用
    public boolean isContainsExceptionArea(Location location, double range_km) {
        final SQLiteDatabase db = getReadableDatabase();
        try {
            final String sql = searchNearQuery(location, range_km, false);
            final Cursor cursor = db.rawQuery(sql, null);
            final int count = cursor.getColumnCount();
            db.close();
            return (count > 0);
        } catch (Exception e) {
            db.close();
            return false;
        }
    }

    // 非記録エリア判定用
    // http://strai.x0.com/?p=200
    public String searchNearQuery(Location location, double range_km, boolean multi) {
        final double km_cos = Math.cos(range_km / 6371);    // 距離基準cos値
        final double radlat = Math.toRadians(location.getLatitude()), radlong = Math.toRadians(location.getLongitude());
        final double qsinlat = Math.sin(radlat), qcoslat = Math.cos(radlat);
        final double qsinlng = Math.sin(radlong), qcoslng = Math.cos(radlong);

        final StringBuilder sb = new StringBuilder();
        sb.append("select " + COL_ID + " ");
        sb.append("(" + COL_SIN_LAT + "*" + qsinlat + " + " + COL_COS_LAT + "*" + qcoslat + "*(" + COL_COS_LNG + "*" + qcoslng + "+" + COL_SIN_LNG + "*" + qsinlng + ")) as distcos ");
        sb.append(" from " + TABLE_NAME_EXCEPTION + " ");
        sb.append(" where distcos > " + km_cos); // 値が大きい方が近い
        if(multi) {
            sb.append(" order by distcos desc ");
        }else{
            sb.append(" limit 1");
        }
        return sb.toString();
    }

    // 全件取得
    public String selectAllExceptionAreas() {
        final StringBuilder sb = new StringBuilder();

        final SQLiteDatabase db = getReadableDatabase();
        try {
            final Cursor cursor = db.rawQuery(SQL_SELECT_EXCEPTION, null);
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                sb.append(cursor.getDouble(0)).append(",");
                sb.append(cursor.getDouble(1)).append("\n");
                cursor.moveToNext();
            }
            cursor.close();
        } finally {
            db.close();
        }

        return sb.toString();
    }

    // 全件取得
    public String selectAllLogs() {
        final StringBuilder sb = new StringBuilder();

        final SQLiteDatabase db = getReadableDatabase();
        try {
            final Cursor cursor = db.rawQuery(SQL_SELECT_LOG, null);
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