package org.ftcollinsresearch.shutdowntimer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import static org.ftcollinsresearch.shutdowntimer.Timer.*;

/**
 * Created by jeff boehmer on 3/3/15.
 */
public class TimerDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Timer.db";

    public static final String CREATE_DDL = "CREATE TABLE timer (" +
            Timer.ID + " INTEGER PRIMARY KEY," +
            Timer.NAME + " TEXT," +
            Timer.RUN_TIME + " INT," +
            Timer.RUN_APP + " TEXT," +
            Timer.DIS_WIFI + " INT," +
            Timer.DIS_BLUETOOTH + " INT," +
            Timer.MUTE + " INT)";

    TimerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
//        Log.d("FCR", CREATE_DDL);
        db.execSQL(CREATE_DDL);
    }

    @Override
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
