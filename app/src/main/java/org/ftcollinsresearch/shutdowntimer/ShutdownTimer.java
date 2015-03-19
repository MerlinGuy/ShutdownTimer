/**
 * Created by jeff boehmer on 2/27/15.
 *
 * This program is free software and covered under the Apache License, Version 2.0 license
 *
 */
package org.ftcollinsresearch.shutdowntimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShutdownTimer extends Activity
                            implements OnSharedPreferenceChangeListener,
                                            OnGetAppListCompleted {

    final static int NOT_STARTED = 0, RUNNING = 1, PAUSED = 2;

    private int[] _btnsTime = new int[] {R.id.btn1,R.id.btn5,R.id.btn10,R.id.btn30,R.id.btn60
            ,R.id.btnMinus1,R.id.btnMinus5,R.id.btnMinus10,R.id.btnMinus30,R.id.btnMinus60};

    private TimerManager _timerMgr = null;

    private int _testSeconds = 10;
    private int _runSeconds = _testSeconds;

    private SharedPreferences _prefs = null;
    private ProgressDialog _splash = null;

    private CountDownTimer _cdTimer = null;
    private int _runState = NOT_STARTED;

    private BluetoothAdapter _BluetoothAdapter = null;
    private CheckBox _cbBluetooth = null;
    private CheckBox _cbWifi = null;
    private CheckBox _cbMute = null;

    private List<PInfo> _PInfos = null;
    private ArrayAdapter<PInfo> _appAdapter = null;
    private Spinner _spnApp = null;
    private Spinner _spnTimer = null;
    private ArrayAdapter<Timer> _timerAdapter = null;

    private boolean IS_TEST = false;
    private String TEST_MODE = "";
    private String KILL_DONE = "";
    private boolean _kill_on_complete = false;

    private String UNSELECTED = null;
    private String LAST_TIMER = null;
    private String _pref_timer = null;
    private Timer _runTimer = null;
    private boolean _settingTimer = false;

    /**
     *
     * @param savedInstanceState is the bundle passed into the parent class's onCreate method
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shutdowntimer);

        TEST_MODE = getString(R.string.pref_test_mode);
        KILL_DONE = getString(R.string.pref_kill_done);
        UNSELECTED = getResources().getString(R.string.unselected);
        LAST_TIMER = getResources().getString(R.string.last_timer);

        _timerMgr = new TimerManager(this);

        _spnApp = (Spinner) findViewById(R.id.spnApp);
        _spnTimer = (Spinner) findViewById(R.id.spnTimer);
        _BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        _cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
        _cbWifi = (CheckBox) findViewById(R.id.cbWifi);
        _cbMute = (CheckBox) findViewById(R.id.cbMute);

        _prefs = PreferenceManager.getDefaultSharedPreferences(this);
        _prefs.registerOnSharedPreferenceChangeListener(this);

        loadPreferences();

        _splash = new ProgressDialog(this);
        _splash.setMessage("Loading. Please wait...");
        _splash.setCancelable(true);
        _splash.show();

        new GetAppList(this).execute();

        _timerMgr.setCanUpdate(true);
    }

    /**
     *
     * @param menu is the menu to create and inflate
     * @return true always at this time
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu_shutdowntimer, menu);
        return true;
    }

    /**
     *
     * @param item is the menu item to create an intent
     * @return always returns true at this time.
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     *
     * @param prefs is the passed in preferences
     * @param key this name fo the
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(KILL_DONE)) {
            _kill_on_complete = prefs.getBoolean(KILL_DONE, false);
        } else if (key.equals(TEST_MODE)) {
            setTest();
        }
    }

    /**
     * Reloads the global variables for Test mode, kill vs reset on complete,
     * and the last saved Timer
     */
    private void loadPreferences() {
        // Set Test mode if selected in preferences
        setTest();
        _kill_on_complete = _prefs.getBoolean(KILL_DONE, false);
        _pref_timer =  _prefs.getString(LAST_TIMER,null);
    }

    /**
     * This method setups up the OnClick and ItemSelected listeners for
     * the main Activity's buttons, checkboxes, and spinner controls.
     */
    private void setupListeners() {

        final OnClickListener ocl = new OnClickListener() {
            public void onClick(final View v) {
                _runSeconds += Integer.parseInt((String) v.getTag()) * 60;
                Timer timer = (Timer) _spnTimer.getSelectedItem();

                if (_runSeconds < 0) {
                    _runSeconds = 0;
                }
                timer.run_time  = _runSeconds;
                _timerMgr.update(timer);
                setRunSeconds();
            }
        };

        for (int btnID : _btnsTime) {
            findViewById(btnID).setOnClickListener(ocl);
        }

        findViewById(R.id.btnCancel).setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    clearNotification();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                }
            }
        );

        findViewById(R.id.btnStartTimer).setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    startCountdown();
                }
            }
        );

        findViewById(R.id.btnAddTimer).setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    showTimerDialog();
                }
            }
        );

        _cbBluetooth.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Timer timer = (Timer) _spnTimer.getSelectedItem();
                    timer.dis_bluetooth = ((CheckBox)v).isChecked();
                    _timerMgr.update(timer);
                }
            }
        );

        _cbWifi.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Timer timer = (Timer) _spnTimer.getSelectedItem();
                        timer.dis_wifi = ((CheckBox)v).isChecked();
                        _timerMgr.update(timer);
                    }
                }
        );
        _cbMute.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Timer timer = (Timer) _spnTimer.getSelectedItem();
                        timer.mute = ((CheckBox)v).isChecked();
                        _timerMgr.update(timer);
                    }
                }
        );

        _spnApp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (! _settingTimer) {
                    PInfo pinfo = (PInfo) _spnApp.getSelectedItem();
                    Timer timer = (Timer) _spnTimer.getSelectedItem();
                    timer.run_app = pinfo.pname;
                    _timerMgr.update(timer);
                }
                _settingTimer = false;
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

        _spnTimer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                _settingTimer = true;
                _runTimer = (Timer) _spnTimer.getSelectedItem();
                setTimerFields();
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
    }

    /**
     * The method displays the Activity for renaming and deleting
     * the current selected Timer
     *
     */
    private void showTimerDialog() {
        final Activity mActivity = this;
        final Timer timer = (Timer) _spnTimer.getSelectedItem();
        final String orgName = timer.name;

        DialogInterface.OnCancelListener ocl = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                reloadTimerList(orgName);
            }
        };

        AlertDialog.Builder dialog = new AlertDialog.Builder(ShutdownTimer.this);
        dialog.setTitle("Manage Timers");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(timer.name);
        dialog.setView(input);
        dialog.setOnCancelListener(ocl);

        // Saves the changes made to the current timer.  If the name is changed
        // it creates a new timer with the current settings
        dialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                timer.name =  input.getText().toString();
                if (! timer.name.equals(orgName)) {
                    timer.id = -1;
                }
                _timerMgr.update(timer);
                dialog.cancel();
            }
        });

        dialog.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                FileManager.deleteFile(mActivity, input.getText().toString());
                dialog.cancel();
            }
        });

        dialog.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        dialog.create().show();
    }

    /**
     * Saves the current Timer name to preferences and sets the display fields
     * to the current _runTimer values.
     *
     */
    private void setTimerFields() {
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(LAST_TIMER, _runTimer.name);
        editor.apply();

        _runSeconds = IS_TEST ? 10 : _runTimer.run_time;
        _cbBluetooth.setChecked(_runTimer.dis_bluetooth);
        _cbWifi.setChecked(_runTimer.dis_wifi);
        _cbMute.setChecked(_runTimer.mute);
        setRunApp(_runTimer.run_app);
        setRunSeconds();
    }

    /**
     * This method sets the _spnApp spinner to the packageName passed in if
     * found.
     *
     * @param packageName the name of the package to search for in the _spnApp spinner
     */
    private void setRunApp(String packageName) {
        int position = 0;
        PInfo pi;

        if (packageName != null) {
            for (Object obj : _PInfos) {
                pi = (PInfo)obj;
                if (pi.pname.equalsIgnoreCase(packageName)) {
                    position = _appAdapter.getPosition(pi);
                    break;
                }
            }
        }
        _spnApp.setSelection(position);
    }

    /**
     * This method reloads the list of currently saved Timers.  It uses the
     * TimerManager class to retrieve the list from the sqlite database.  If
     * no timers are found it adds a default timer.
     *
     * @param timerName is the timer name to set as the selected timer if provided.
     */
    private void reloadTimerList( String timerName ) {

        _timerAdapter = _timerMgr.getAdapter(this,R.layout.spinner_timer);
        if (_timerAdapter.getCount() == 0) {
            _timerAdapter.add(new Timer());
        }
        _spnTimer.setAdapter(_timerAdapter);
        if ( timerName != null) {
            _runTimer = getTimerByName( timerName, true );
        }

    }

    /**
     * This method retrieves the timer by name from the list of loaded timers.  If the
     * setSpinner is true it makes the found timer the current selected timer.
     *
     * @param timerName is the name of the timer to search for in the list of saved Timers.
     * @param setSpinner if set selects the found timer in the _spnTimer spinner
     * @return the Timer from the _timerAdapter that matches the passed in timerName
     */
    private Timer getTimerByName(String timerName, boolean setSpinner) {

        if (timerName == null) return null;

        for (int i=0;i<_timerAdapter.getCount();i++) {
            Timer timer = _timerAdapter.getItem(i);
            if (timer.name.equals(timerName)) {
                if (setSpinner) {
                    _spnTimer.setSelection(i, false);
                    _runTimer = timer;
                    setTimerFields();
                }
                return timer;
            }
        }
        return null;
    }

    /**
     * This method starts, pauses, and restarts the current selected Timer.
     * It also starts the Timer's application if set.
     */
    private void startCountdown() {

        if ((_runState == NOT_STARTED) || (_runState == PAUSED)) {
            showShutdownNotification();
            createCountDown();
            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.pause_word);

            if (_runState != PAUSED) {
                startApp(_runTimer);
            }
            _runState = RUNNING;
        }
        else if (_runState == RUNNING) {
            _cdTimer.cancel();
            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.start_word);
            _runState = PAUSED;
            clearNotification();
        }
    }

    /**
     * This method starts the Timer passed in as a new Activity
     *
     * @param timer is the timer to start
     */
    private void startApp(Timer timer) {
        if (timer == null) return;
        try {
            String runApp = timer.run_app;
            if (runApp == null || runApp.equals(UNSELECTED) || runApp.equals("none")) return;

            Intent intent = getPackageManager().getLaunchIntentForPackage(runApp);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("FCR", e.getMessage());
        }
    }

    /**
     * This method creates a CountDownTimer that sets it's onTick method to
     * update the remaining time in the UI
     *
     */
    private void createCountDown() {
        _cdTimer = new CountDownTimer(_runSeconds * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                _runSeconds = (int) (millisUntilFinished / 1000);
                setRunSeconds();
            }

            public void onFinish() {
                shutdown();
            }
        }.start();
    }

    /**
     * This method handles the shutting down of selected features.  It also will kill off
     * the application if that is the preference or reset the UI to the current timer
     * specifications.
     *
     */
    private void shutdown() {
        try {
            if (_runTimer.dis_bluetooth ) {
                if (_runTimer.mute) {
                    muteDevice();
                }
                _BluetoothAdapter.disable();
            }

            if (_runTimer.dis_wifi) {
                WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(false);
            }

            if (_runTimer.mute) {
                muteDevice();
            }


        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }

        _runSeconds = 0;
        setRunSeconds();
        clearNotification();
        if (_kill_on_complete) {
            clearNotification();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);

        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Timer Complete", Toast.LENGTH_SHORT);
            toast.show();
            this.setTimerFields();
            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.start_word);
        }
    }

    /**
     * This method mutes the media audio of the device
     */
    private void muteDevice() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    /**
     * This method takes the current remaining run seconds and formats it to
     * an Hours : Minutes : Seconds string and updates the UI's txtCountdown control
     *
     */
    private void setRunSeconds() {
        int hours = _runSeconds / 3600;
        int remain = _runSeconds - (hours * 3600);
        int minutes = remain / 60;
        int seconds = (remain - (minutes * 60));
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        EditText editText = (EditText)findViewById(R.id.txtCountdown);
        editText.setText(timeString);
    }

    /**
     * This method gets the current IS_TEST settings' preference and updates the UI
     * look accordingly.
     *
     */
    private void setTest() {
        IS_TEST = _prefs.getBoolean(TEST_MODE, false);
        for (int btnID : _btnsTime) {
            findViewById(btnID).setEnabled(! IS_TEST);
        }
        View view = findViewById(R.id.txtCountdown);
        if (IS_TEST) {
            view.setBackgroundResource(R.drawable.red_border);
        } else {
            view.setBackgroundResource(R.drawable.black_border);
        }
    }

    /**
     * This method shows the ShutdownTimer icon in the Notification bar while running.  This
     * allows the user to reshow the app if another application takes the display control.
     *
     */
    @SuppressWarnings("deprecation")
    private void showShutdownNotification(){

        Context context = getApplicationContext();
        Intent intent = new Intent(context, ShutdownTimer.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle("Shutdown Timer")
                .setContentText("Shutdown Timer")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.fcr_sdt)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.fcr_sdt))
                ;

        Notification n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            n = builder.build();
        } else {
            n = builder.getNotification();
        }

        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        NotificationManager notificationManager;
        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, n);
    }

    /**
     * Clears the ShutdownTimer icon from the Notification bar.
     */
    private void clearNotification() {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    /**
     * This method populates the Application spinner with the names of all non-system
     * applications.
     */
    public void onGetAppListCompleted() {

        setupListeners();

        _appAdapter = new ArrayAdapter<PInfo>(this, R.layout.spinner_app, _PInfos);
        _appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spnApp.setAdapter(_appAdapter);
        _spnApp.setSelection(0, false);

        reloadTimerList(_pref_timer);
        _splash.cancel();
    }

    /**
     * AsyncTask to load the installed packages into the _PInfos Arraylist
     */
    private class GetAppList extends AsyncTask<Object, Object, Object> {

        private OnGetAppListCompleted _listener;

        public GetAppList(OnGetAppListCompleted listener) {
            _listener = listener;
        }

        @Override
        protected Object doInBackground(Object... object) {
            final PackageManager pm = getPackageManager();
            List<PackageInfo> pkgs = pm.getInstalledPackages(0);
            _PInfos = new ArrayList<PInfo>();

            _PInfos.add(new PInfo(UNSELECTED, "none"));

            for (PackageInfo pi : pkgs) {
                if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    _PInfos.add( new PInfo( pi, pm) );
                }
            }

            Collections.sort(_PInfos, new PInfoComparator());

            return null;
        }

        protected void onPostExecute(Object o) {
            _listener.onGetAppListCompleted();
        }
    }
}
