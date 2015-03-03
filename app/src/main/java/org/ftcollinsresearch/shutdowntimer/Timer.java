package org.ftcollinsresearch.shutdowntimer;

import android.content.ContentValues;
import android.database.Cursor;

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

}
