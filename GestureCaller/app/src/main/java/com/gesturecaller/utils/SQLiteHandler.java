package com.gesturecaller.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gesturecaller.models.ExceptionContact;
import com.gesturecaller.models.MyLocation;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("ALL")
public class SQLiteHandler extends SQLiteOpenHelper {
    private static final String TAG = SQLiteHandler.class.getSimpleName();

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 3;

    // Database Name
    private static final String DATABASE_NAME = "gesture_caller";

    // Prefs
    private static final String sTABLE_PREFS = "table_prefs";
    private static final String sPREF_KEY = "pref_key";
    private static final String sPREF_VALUE = "pref_value";

    private static final String CREATE_PREF_TABLE = "create table " + sTABLE_PREFS + "(" +
            sPREF_KEY + " text primary key, " + sPREF_VALUE + " text)";

    // ExceptionList
    private static final String sTABLE_EXCEPTIONS = "table_exceptions";
    private static final String sEXCEPTION_ID = "exceptions_id";
    private static final String sEXCEPTION_NAME = "exception_name";
    private static final String sEXCEPTION_CONTACT = "exception_contact";

    private static final String CREATE_EXCEPTIONS_TABLE = "create table " + sTABLE_EXCEPTIONS + "("
            + sEXCEPTION_ID + " integer primary key, "
            + sEXCEPTION_NAME + " text, "
            + sEXCEPTION_CONTACT + " text unique)";

    // locations table
    private static final String sTABLE_LOCATIONS = "table_locations";
    private static final String sLOCATION_ID = "location_id";
    private static final String sLOCATION_NAME = "location_name";
    private static final String sLOCATION_LAT = "location_lat";
    private static final String sLOCATION_LON = "location_lon";
    private static final String sLOCATION_ENABLED = "location_enabled";
    private static final String sLOCATION_MESSAGE = "location_message";

    private static final String CREATE_LOCATIONS_TABLE = "create table " + sTABLE_LOCATIONS + "("
            + sLOCATION_ID + " integer primary key, "
            + sLOCATION_NAME + " text, "
            + sLOCATION_LAT + " real, "
            + sLOCATION_LON + " real, "
            + sLOCATION_MESSAGE + " text, "
            + sLOCATION_ENABLED + " integer)";

    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PREF_TABLE);
        db.execSQL(CREATE_EXCEPTIONS_TABLE);
        db.execSQL(CREATE_LOCATIONS_TABLE);

        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // upgrading tables

        db.execSQL("DROP TABLE IF EXISTS " + sTABLE_PREFS);
        db.execSQL(CREATE_PREF_TABLE);

        db.execSQL("DROP TABLE IF EXISTS " + sTABLE_EXCEPTIONS);
        db.execSQL(CREATE_EXCEPTIONS_TABLE);

        db.execSQL("DROP TABLE IF EXISTS " + sTABLE_LOCATIONS);
        db.execSQL(CREATE_LOCATIONS_TABLE);
    }

    private void createPref(String prefKey, String prefValue) {
        ContentValues values = new ContentValues();
        values.put(sPREF_KEY, prefKey);
        values.put(sPREF_VALUE, prefValue);

        SQLiteDatabase db = this.getWritableDatabase();
        long insertId = db.insert(sTABLE_PREFS, null, values);
        close();
        Log.i(TAG, "ID : " + insertId + "[ Added pref with key : " + prefKey + ", value : " + prefValue + " ]");
    }

    public boolean addExceptionContact(ExceptionContact ec) {
        ContentValues values = new ContentValues();
        values.put(sEXCEPTION_NAME, ec.getContactName());
        values.put(sEXCEPTION_CONTACT, ec.getContact());

        SQLiteDatabase db = this.getWritableDatabase();
        long insertId = db.insert(sTABLE_EXCEPTIONS, null, values);
        close();
        Log.e(TAG, "addExceptionContact: " + insertId);
        return insertId > 0;
    }

    public boolean addLocation(MyLocation location) {
        ContentValues values = new ContentValues();
        values.put(sLOCATION_NAME, location.getName());
        values.put(sLOCATION_LAT, location.getLatitude());
        values.put(sLOCATION_LON, location.getLongitude());
        values.put(sLOCATION_MESSAGE, location.getMessage());
        values.put(sLOCATION_ENABLED, location.isEnabled() ? 1 : 0);

        SQLiteDatabase db = this.getWritableDatabase();
        long insertId = db.insert(sTABLE_LOCATIONS, null, values);
        close();
        Log.e(TAG, "addLocation: " + insertId);
        return insertId > 0;
    }

    public void deleteExceptionContactWithId(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        long deleteId = db.delete(sTABLE_EXCEPTIONS, sEXCEPTION_ID + " = ?", new String[]{String.valueOf(id)});
        close();
    }

    public void deleteExceptionContactWithContact(String contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        long deleteId = db.delete(sTABLE_EXCEPTIONS, sEXCEPTION_CONTACT + " = ?", new String[]{contact});
        close();
    }

    public void deleteLocationWithId(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        long deleteId = db.delete(sTABLE_LOCATIONS, sLOCATION_ID + " = ?", new String[]{String.valueOf(id)});
        close();
    }

    public void setPref(String prefKey, String prefValue) {
        ContentValues values = new ContentValues();
        values.put(sPREF_VALUE, prefValue);

        SQLiteDatabase db = this.getWritableDatabase();
        long valuesChanged = db.update(sTABLE_PREFS, values, sPREF_KEY + " = ?", new String[]{String.valueOf(prefKey)});
        if (valuesChanged == 0) {
            createPref(prefKey, prefValue);
        }
        close();
        Log.i(TAG, "Values Updated : " + valuesChanged + "[ Updated pref with key : " + prefKey + ", value : " + prefValue + " ]");
    }

    public String getPref(String prefKey, String defValue) {
        String prefValue = defValue;
        String selectQuery = "SELECT  * FROM " + sTABLE_PREFS + " where " + sPREF_KEY + " = ?";

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, new String[]{prefKey});
            // Move to first row
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                prefValue = cursor.getString(1);
            }
            cursor.close();
            db.close();
        } catch (Exception ignored) {
            Log.d(TAG, "Fetching Pref Error : " + ignored);
        }

        Log.d(TAG, "Fetching pref : (" + prefKey + " , " + prefValue + ")");
        return prefValue;
    }

    public boolean isContactExceptional(String contact) {
        String selectQuery = "SELECT  * FROM " + sTABLE_EXCEPTIONS + " where " + sEXCEPTION_CONTACT + " = ?";
        boolean isContactExceptional = false;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, new String[]{contact});
            // Move to first row
            if (cursor.getCount() > 0) {
                isContactExceptional = true;
            }
            cursor.close();
            db.close();
        } catch (Exception ignored) {
            Log.d(TAG, "Fetching Pref Error : " + ignored);
        }

        return isContactExceptional;
    }

    public HashMap<String, String> getAllSettings() {
        HashMap<String, String> hashMap = new HashMap<>();

        String selectQuery = "SELECT  * FROM " + sTABLE_PREFS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String key;
            String value;

            while (!cursor.isAfterLast()) {
                key = cursor.getString(0);
                value = cursor.getString(1);

                cursor.moveToNext();
                hashMap.put(key, value);
            }
        }
        cursor.close();
        db.close();

        return hashMap;
    }

    public ArrayList<ExceptionContact> getAllExceptionContacts() {
        ArrayList<ExceptionContact> list = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + sTABLE_EXCEPTIONS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                ExceptionContact ec = new ExceptionContact();
                ec.setId(cursor.getInt(cursor.getColumnIndex(sEXCEPTION_ID)));
                ec.setContactName(cursor.getString(cursor.getColumnIndex(sEXCEPTION_NAME)));
                ec.setContact(cursor.getString(cursor.getColumnIndex(sEXCEPTION_CONTACT)));

                list.add(ec);
                cursor.moveToNext();
            }
        }
        cursor.close();
        db.close();

        return list;
    }

    public boolean setLocationEnabled(int id, boolean enabled) {
        ContentValues values = new ContentValues();
        values.put(sLOCATION_ENABLED, enabled ? 1 : 0);

        SQLiteDatabase db = this.getWritableDatabase();
        long updateId = db.update(sTABLE_LOCATIONS, values, sLOCATION_ID + " = ?", new String[]{String.valueOf(id)});
        close();
        return updateId > 0;
    }

    public ArrayList<MyLocation> getAllLocations() {
        ArrayList<MyLocation> list = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + sTABLE_LOCATIONS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                MyLocation location = new MyLocation();
                location.setId(cursor.getInt(cursor.getColumnIndex(sLOCATION_ID)));
                location.setName(cursor.getString(cursor.getColumnIndex(sLOCATION_NAME)));
                location.setLatitude(cursor.getDouble(cursor.getColumnIndex(sLOCATION_LAT)));
                location.setLongitude(cursor.getDouble(cursor.getColumnIndex(sLOCATION_LON)));
                location.setMessage(cursor.getString(cursor.getColumnIndex(sLOCATION_MESSAGE)));
                location.setEnabled(cursor.getInt(cursor.getColumnIndex(sLOCATION_ENABLED)) == 1);

                list.add(location);
                cursor.moveToNext();
            }
        }
        cursor.close();
        db.close();

        return list;
    }

    private void logInfo(String message) {
        Log.i(TAG, message);
    }

    public void reset() {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + sTABLE_PREFS);
        db.execSQL(CREATE_PREF_TABLE);

        db.execSQL("DROP TABLE IF EXISTS " + sTABLE_EXCEPTIONS);
        db.execSQL(CREATE_EXCEPTIONS_TABLE);

        db.execSQL("DROP TABLE IF EXISTS " + sTABLE_LOCATIONS);
        db.execSQL(CREATE_LOCATIONS_TABLE);

        db.close();
    }

    public String getPref(String prefKey) {
        String prefValue = null;
        String selectQuery = "SELECT  * FROM " + sTABLE_PREFS + " where " + sPREF_KEY + " = ?";

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, new String[]{prefKey});
            // Move to first row
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                prefValue = cursor.getString(1);
            }
            cursor.close();
            db.close();
        } catch (Exception ignored) {
            prefValue = "error";
            Log.d(TAG, "Fetching Pref Error : " + ignored);
        }

        Log.d(TAG, "Fetching pref : (" + prefKey + " , " + prefValue + ")");
        return prefValue;
    }
}