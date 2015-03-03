package org.ftcollinsresearch.shutdowntimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

public class ShutdownTimer extends Activity {

    private final static int NOT_STARTED = 0, RUNNING = 1, PAUSED = 2;

    private int[] _btnsTime = new int[] {R.id.btn1,R.id.btn5,R.id.btn10,R.id.btn30,R.id.btn60
            ,R.id.btnMinus1,R.id.btnMinus5,R.id.btnMinus10,R.id.btnMinus30,R.id.btnMinus60};

    private TimerManager _timerMgr = null;

    private int _testSeconds = 10;
    private int _runSeconds = _testSeconds;

    private CountDownTimer _cdTimer = null;
    private int _runState = NOT_STARTED;
    private boolean _initialized = false;

    private BluetoothAdapter _BluetoothAdapter = null;
    private CheckBox _cbBluetooth = null;
    private CheckBox _cbWifi = null;
    private CheckBox _cbMute = null;

    private List<PInfo> _PInfos = null;
    private ArrayAdapter<PInfo> _appAdapter = null;
    private Spinner _spnApp = null;
    private Spinner _spnTimer = null;

    protected boolean IS_TEST = false;
    private String TEST_MODE = "";
    private String _unselected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shutdowntimer);

        TEST_MODE = getString(R.string.pref_test_mode);
        _unselected = getString(R.string.unselected);

        _timerMgr = new TimerManager(this);

        // Set Test mode if selected in preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        IS_TEST = prefs.getBoolean(TEST_MODE, false);
        setTest();

        _spnApp = (Spinner) findViewById(R.id.spnApp);
        _spnTimer = (Spinner) findViewById(R.id.spnTimer);
        _BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        _cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
        _cbWifi = (CheckBox) findViewById(R.id.cbWifi);
        _cbMute = (CheckBox) findViewById(R.id.cbMute);

        setupAppList();

        reloadTimerList(null);

        setupListeners();

        _initialized = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu_shutdowntimer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void setupListeners() {
        final Activity mActivity = this;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(TEST_MODE)) {
                    IS_TEST = prefs.getBoolean(key, false);
                    setTest();
                }
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);

        final OnClickListener ocl = new OnClickListener() {
            public void onClick(final View v) {
                _runSeconds += Integer.parseInt((String) v.getTag()) * 60;
                Timer timer = (Timer) _spnTimer.getSelectedItem();

                if (_runSeconds < 0) {
                    _runSeconds = 0;
                }
                timer.run_time  = _runSeconds;
                saveTimer(timer);
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
                    Timer timer = (Timer) _spnTimer.getSelectedItem();
                    startCountdown(timer);
                }
            }
        );

        findViewById(R.id.btnAddTimer).setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    showTimerDialog(mActivity);
                }
            }
        );

        _cbBluetooth.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Timer timer = (Timer) _spnTimer.getSelectedItem();
                    timer.dis_bluetooth = ((CheckBox)v).isChecked();
                    saveTimer(timer);
                }
            }
        );

        _cbWifi.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Timer timer = (Timer) _spnTimer.getSelectedItem();
                        timer.dis_wifi = ((CheckBox)v).isChecked();
                        saveTimer(timer);
                    }
                }
        );
        _cbMute.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Timer timer = (Timer) _spnTimer.getSelectedItem();
                        timer.mute = ((CheckBox)v).isChecked();
                        saveTimer(timer);
                    }
                }
        );

        _spnApp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                PInfo pinfo = (PInfo) _spnApp.getSelectedItem();
                Timer timer = (Timer) _spnTimer.getSelectedItem();
                timer.run_app = pinfo.pname;
                saveTimer(timer);
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

        _spnTimer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                setTimerFields((Timer) _spnTimer.getSelectedItem());
            }

            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
    }

    private void setTest() {
        for (int btnID : _btnsTime) {
            findViewById(btnID).setEnabled(! IS_TEST);
        }
        View view = findViewById(R.id.txtCountdown);
        if (IS_TEST) {
            view.setBackgroundResource(R.drawable.red_border);
        } else {
            view.setBackgroundResource(R.drawable.black_border);
        }
//        loadTimer(_curTimer.name);
    }

    private void showTimerDialog(Activity activity) {
        final Activity mActivity = activity;
        final Timer timer = (Timer) _spnTimer.getSelectedItem();
        final String orgName = new String(timer.name);

        DialogInterface.OnCancelListener ocl = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Timer timer = (Timer) _spnTimer.getSelectedItem();
                reloadTimerList(timer);
            }
        };

        AlertDialog.Builder dialog = new AlertDialog.Builder(ShutdownTimer.this);
        dialog.setTitle("Manage Timers");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(timer.name);
        dialog.setView(input);
        dialog.setOnCancelListener(ocl);

        dialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                timer.name =  input.getText().toString();
                if (!timer.name.equals(orgName)) {
                    timer.id = -1;
                }
                saveTimer(timer);
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

    private void saveTimer(Timer timer) {
        if (! _initialized) return;
        _timerMgr.update(timer);
    }

    private void setTimerFields(Timer timer) {
        _runSeconds = IS_TEST ? 10 : timer.run_time;
        _cbBluetooth.setChecked(timer.dis_bluetooth);
        _cbWifi.setChecked(timer.dis_wifi);
        _cbMute.setChecked(timer.mute);
        setRunApp(timer.run_app);
        setRunSeconds();
    }

    private void setRunApp(String packageName) {
        int position = -1;
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

    private void setupAppList() {

        final PackageManager pm = getPackageManager();
        List<PackageInfo> pkgs = pm.getInstalledPackages(0);
        _PInfos = new ArrayList<PInfo>();

        _PInfos.add(new PInfo(_unselected, "none"));

        for (PackageInfo pi : pkgs) {
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                _PInfos.add( new PInfo( pi, pm) );
            }

        }

        Collections.sort(_PInfos, new PInfoComparator());

        _appAdapter = new ArrayAdapter<PInfo>(this,android.R.layout.simple_spinner_item, _PInfos);
        _appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spnApp.setAdapter(_appAdapter);
        _spnApp.setSelection(0, false);
    }

    private void reloadTimerList(Timer selected) {
        ArrayAdapter<Timer> timers = _timerMgr.getAdapter(this,android.R.layout.simple_spinner_item );
        if (timers.getCount() == 0) {
            timers.add(new Timer());
        }
        _spnTimer.setAdapter(timers);
        int position = -1;
        if (selected != null) {
            position = timers.getPosition(selected);
        }
        if (position == -1) {
            selected = timers.getItem(0);
            position = 0;
        }
        _spnTimer.setSelection(position, false);
        setTimerFields(selected);
    }

    private void setRunSeconds() {
        int hours = _runSeconds / 3600;
        int remain = _runSeconds - (hours * 3600);
        int minutes = remain / 60;
        int seconds = (remain - (minutes * 60));
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        EditText editText = (EditText)findViewById(R.id.txtCountdown);
        editText.setText(timeString);
    }

    private void startCountdown(Timer timer) {

        if ((_runState == NOT_STARTED) || (_runState == PAUSED)) {
            showShutdownNotification();
            createCountDown();
            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.pause_word);

            if (_runState != PAUSED) {
                startApp(timer);
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

    private void startApp(Timer timer) {
        try {
            String runApp = timer.run_app;
            if (runApp == null || runApp.equalsIgnoreCase(_unselected)) return;
            Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(runApp);
            startActivity(LaunchIntent);
        } catch (Exception e) {
            Log.e("FCR", e.getMessage());
        }
    }

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

    private void clearNotification() {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

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

    private void shutdown() {
        try {
            Timer timer = (Timer) _spnTimer.getSelectedItem();
            if (timer != null && timer.mute) {
                lowerVolume(0);
            }

            if (_cbBluetooth.isEnabled() && _cbBluetooth.isChecked() ) {
                _BluetoothAdapter.disable();
            }

            if (_cbWifi.isEnabled() && _cbWifi.isChecked()) {
                WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(false);
            }

            Toast toast = Toast.makeText(getApplicationContext(), "Timer Complete", Toast.LENGTH_SHORT);
            toast.show();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }

        _runSeconds = 0;
        setRunSeconds();
        clearNotification();
    }

    private void lowerVolume(int level) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC,level,0);
    }
}
