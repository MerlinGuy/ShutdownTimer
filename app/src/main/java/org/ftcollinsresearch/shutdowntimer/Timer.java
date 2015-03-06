package org.ftcollinsresearch.shutdowntimer;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by jeff boehmer on 3/3/15.
 */
public class Timer {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String RUN_TIME = "run_time";
    public static final String RUN_APP = "run_app";
    public static final String DIS_WIFI = "dis_wifi";
    public static final String DIS_BLUETOOTH = "dis_bluetooth";
    public static final String MUTE = "mute";

    public static final String[] COLUMNS = new String[] {
            Timer.ID,
            Timer.NAME,
            Timer.RUN_TIME,
            Timer.RUN_APP,
            Timer.DIS_WIFI,
            Timer.DIS_BLUETOOTH,
            Timer.MUTE
    };

    public final static int DEFAULT_RUN_SECONDS = 1800;
    public long id = -1;
    public String name = "default";
    public int run_time = DEFAULT_RUN_SECONDS;
    public String run_app = null;
    public int pid = -1;
    public boolean dis_wifi = false;
    public boolean dis_bluetooth = false;
    public boolean mute = false;

    public Timer() {  }

    public Timer (Cursor c) {
        int i = 0;
        this.id = c.getInt(i++);
        this.name = c.getString(i++);
        this.run_time = c.getInt(i++);
        this.run_app = c.getString(i++);
        this.dis_wifi = c.getInt(i++) == 1;
        this.dis_bluetooth = c.getInt(i++) == 1;
        this.mute = c.getInt(i++) == 1;
//        this.log();
    }

    public String toString() {
        return this.name;
    }

    public ContentValues getContent() {
        ContentValues values = new ContentValues();
        values.put( ID, id );
        values.put(NAME, name);
        values.put(RUN_TIME, run_time);
        values.put(RUN_APP, run_app);
        values.put(DIS_WIFI, dis_wifi);
        values.put(DIS_BLUETOOTH, dis_bluetooth);
        values.put(MUTE, mute);
        return values;
    }

    public void log(boolean line_break) {
        if (line_break) {
            Log.d("FCR", "id: " + this.id);
            Log.d("FCR", "name: " + this.name);
            Log.d("FCR", "run_time: " + this.run_time);
            Log.d("FCR", "run_app: " + this.run_app);
            Log.d("FCR", "dis_bluetooth: " + this.dis_bluetooth);
            Log.d("FCR", "dis_wifi: " + this.dis_wifi);
            Log.d("FCR", "mute: " + this.mute);
        } else {
            Log.d("FCR", "id: " + this.id + ", name: " + this.name
                    + ", run_time: " + this.run_time + ", run_app: " + this.run_app
                    + ", dis_bluetooth: " + this.dis_bluetooth + ", dis_wifi: " + this.dis_wifi
                    + ", mute: "  + this.mute);
        }
    }
}
