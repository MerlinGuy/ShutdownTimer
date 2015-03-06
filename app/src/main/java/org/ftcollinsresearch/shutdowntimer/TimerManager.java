/**
 * Created by jeff boehmer on 3/3/15.
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

public class TimerManager {

//    private Context _context = null;
    TimerDbHelper _dbHelper = null;
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

    public List<Timer> list() {
        Log.d("FCR", "+++++++++++++++++++++++++++++++++++++");

        List<Timer> list = new ArrayList<Timer>();
        try {
            SQLiteDatabase db = _dbHelper.getReadableDatabase();
            Cursor c = db.query(
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
        }
        return list;
    }

    public ArrayAdapter<Timer> getAdapter(Context context, int resource) {
        return new ArrayAdapter<Timer>(context, resource, this.list());
    }

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
        return timer;
    }

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
        return timer;
    }

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
