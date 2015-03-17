/**
 * Created by jeff boehmer on 3/3/15.
 *
 * This program is free software and covered under the Apache License, Version 2.0 license
 */
package org.ftcollinsresearch.shutdowntimer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides helper methods for interacting with the Timer table in the
 * SQLiteDatabase.
 */
public class TimerManager {

    private TimerDbHelper _dbHelper = null;
    public static final String TABLE_NAME = "Timer";
    private boolean _canUpdate = false;

    public TimerManager(Context context) {
        try {
            _dbHelper = new TimerDbHelper(context);
        } catch (Exception e) {
            Log.e("FCR", e.getMessage());
        }
    }

    public void setCanUpdate(boolean canUpdate) {
        _canUpdate = canUpdate;
    }

    /**
     * This class will either update a timer or create a new one. If
     * the passed in timer object's id field is greater than 0 then
     * the timer record is updated if found, otherwise a new record is created
     *
     * @param timer hows the data for the new or updated record
     * @return True if the action is successful otherwise false
     */
    public boolean update(Timer timer) {
        if (! _canUpdate ) return false;

//        Log.d("FCR", "---------------------------");
        timer.log(false);
        SQLiteDatabase db = _dbHelper.getWritableDatabase();
        ContentValues values = timer.getContent();
        if (timer.id > 0) {
            String [] args = { String.valueOf(timer.id)};
            int count = db.update(TABLE_NAME,values,Timer.ID + " = ?",args);
            return count == 1;

        } else {
            values.remove(Timer.ID);
            timer.id = db.insert(TABLE_NAME, null, values);
            return timer.id > 0;
        }
    }

    /**
     * This method returns all the timers in the timer table in List form
     * @return a List<Timer> of timers
     */
    public List<Timer> list() {
//        Log.d("FCR", "+++++++++++++++++++++++++++++++++++++");

        List<Timer> list = new ArrayList<Timer>();
        Cursor c = null;
        try {
            SQLiteDatabase db = _dbHelper.getReadableDatabase();
            c = db.query(
                    TABLE_NAME,
                    Timer.COLUMNS,
                    null,                            // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                            // don't group the rows
                    null,                            // don't filter by row groups
                    Timer.NAME + " DESC"             // The sort order
            );

            Timer timer;
            while (c.moveToNext()) {
                timer = new Timer(c);
                timer.log(false);
                list.add(timer);
            }
        } catch (Exception e) {
            Log.e("FCR", e.getMessage());
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return list;
    }

    /**
     * Helper method to create a new ArrayAdapter of type Timer
     * @param context holds the current application context
     * @param resource holds the  ID for a layout file containing a TextView
     *                 to use when instantiating views.
     * @return an new ArrayAdapter<Timer>
     */
    public ArrayAdapter<Timer> getAdapter(Context context, int resource) {
        return new ArrayAdapter<Timer>(context, resource, this.list());
    }

    /**
     * This is a helper method to read a specified timer from the SQLiteDatabase
     * and return it in the form of a Timer object.
     *
     * @param id of the timer to retrieve from the database
     * @return the new Timer object with the record data.
     */
    public Timer read(int id) {
        SQLiteDatabase db = _dbHelper.getReadableDatabase();
        String [] selectionArgs = { String.valueOf(id)};
        Timer timer = null;

        Cursor c = db.query(
                TABLE_NAME,
                Timer.COLUMNS,
                Timer.ID + " = ?",               // The columns for the WHERE clause
                selectionArgs,                   // The values for the WHERE clause
                null,                            // don't group the rows
                null,                            // don't filter by row groups
                null                             // The sort order
        );

        if (c.moveToNext()) {
            timer = new Timer(c);
        }
        c.close();
        return timer;
    }

    /**
     * This is a helper method to read a specified timer from the SQLiteDatabase
     * and return it in the form of a Timer object.
     *
     * @param name of the timer to retrieve from the database
     * @return the new Timer object with the record data.
     */
    public Timer readByName(String name) {
        SQLiteDatabase db = _dbHelper.getReadableDatabase();
        String [] selectionArgs = { name };
        Timer timer = null;

        Cursor c = db.query(
                TABLE_NAME,
                Timer.COLUMNS,
                Timer.NAME + " = ?",             // The columns for the WHERE clause
                selectionArgs,                   // The values for the WHERE clause
                null,                            // don't group the rows
                null,                            // don't filter by row groups
                null                             // The sort order
        );

        if (c.moveToNext()) {
            timer = new Timer(c);
        }
        c.close();
        return timer;
    }

    /**
     * This is a helper method to delete a specified timer from the SQLiteDatabase
     *
     * @param timer to delete from the database
     * @return True if th 
     */
    public boolean delete(Timer timer) {
        SQLiteDatabase db = _dbHelper.getReadableDatabase();
        String [] args = { String.valueOf(timer.id)};
        return db.delete(TABLE_NAME, Timer.ID + " = ?", args ) == 1;
    }

    public void truncate() {
        SQLiteDatabase db = _dbHelper.getWritableDatabase();
        db.execSQL("DROP TABLE " + TABLE_NAME);
        db.execSQL(TimerDbHelper.CREATE_DDL);
    }
}
