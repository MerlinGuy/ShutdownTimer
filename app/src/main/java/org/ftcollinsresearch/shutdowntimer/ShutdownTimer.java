package org.ftcollinsresearch.shutdowntimer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
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


import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShutdownTimer extends Activity {

    private final int NOT_STARTED = 0, RUNNING = 1, PAUSED = 2, COMPLETE = 3;

    public final static String RUN_SECS = "run_seconds";
    private final String DIS_BLUE = "disable_bluetooth";
    private final String DIS_WIFI = "disable_wifi";
    private final String RUN_APP = "run_app";
    private final String _FILENAME_ = "org.ftcollinsresearch.shutdowntimer";
    private final String _UNSELECTED_ = "_unselected_";

    private final int _defaultRunSeconds = 1800;
    private int _runSeconds = 10;
    private CountDownTimer _cdTimer = null;
    private int _runState = NOT_STARTED;
    private boolean _initialized = false;

    private BluetoothAdapter _BluetoothAdapter = null;
    private CheckBox _cbBluetooth = null;

    private WifiManager _wifiManager = null;
    private CheckBox _cbWifi = null;

    private List _PInfos = null;
    private ArrayAdapter<PInfo> _appAdapter = null;
    private Spinner _appSpinner = null;
    private String _runApp = null;

    private int _notification_id = -1;

//    private ShutdownService _shutdownService;
//    private boolean _isBound = false;


//    private ServiceConnection _connShutdownService = new ServiceConnection() {
//
//        public void onServiceConnected(ComponentName className,
//                                       IBinder service) {
//            ShutdownService.LocalBinder binder = (ShutdownService.LocalBinder) service;
//            _shutdownService = binder.getService();
//            _isBound = true;
//        }
//
//        public void onServiceDisconnected(ComponentName arg0) {
//            _isBound = false;
//        }
//
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shutdowntimer);

//        Intent intent = new Intent(this, ShutdownService.class);
//        bindService(intent, _connShutdownService, Context.BIND_AUTO_CREATE);

        initButtons();

        setupBluetooth();

        setupWifi();

        setupApps();

        restoreState();

        _initialized = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_shutdowntimer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveState() {
        if (! _initialized) return;
        try {
            JSONObject json = new JSONObject();
            json.put(RUN_SECS, _runSeconds);
            json.put(DIS_BLUE, _cbBluetooth.isChecked());
            json.put(DIS_WIFI, _cbWifi.isChecked());
            PInfo pinfo = (PInfo) _appSpinner.getSelectedItem();
            _runApp = pinfo.pname;
            json.put(RUN_APP, pinfo.pname);
            byte[] encBytes = Base64.encode(json.toString().getBytes(), Base64.DEFAULT);
            FileOutputStream fos = openFileOutput(_FILENAME_, Context.MODE_PRIVATE);
            fos.write(encBytes);
            fos.close();

            Log.d("FCR", "saveState:  " + json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreState() {
        InputStream in = null;

        try {
            File file = getBaseContext().getFileStreamPath(_FILENAME_);
            JSONObject json;
            if (file.exists()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                in = new FileInputStream(file);

                while ( (bytesRead = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] bytes = Base64.decode(baos.toByteArray(), Base64.DEFAULT);
                json = new JSONObject(new String(bytes));
            } else {
                json = new JSONObject();
            }

            Log.d("FCR", "restoreState:  " + json.toString());
            _runSeconds = json.optInt(RUN_SECS, _defaultRunSeconds);
            _cbBluetooth.setChecked(json.optBoolean(DIS_BLUE, false));
            _cbWifi.setChecked(json.optBoolean(DIS_WIFI, false));
            setRunApp(json.optString(RUN_APP, _UNSELECTED_));
            setRunSeconds();

        } catch (Exception e) {
            Log.e("restoreState", e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch(Exception e){
                }
            }
        }
    }

    private void setRunApp(String packageName) {
        int position = -1;
        PInfo pi;

        if (packageName != null) {
            for (Object obj : _PInfos) {
                pi = (PInfo)obj;
                if (pi.pname.equalsIgnoreCase(packageName)) {
                    position = _appAdapter.getPosition(pi);
                    _runApp = pi.pname;
                    break;
                }
            }
        }
        _appSpinner.setSelection(position);
    }

    private void setupApps() {

        _appSpinner = (Spinner) findViewById(R.id.spnApps);
        final PackageManager pm = getPackageManager();
        List<PackageInfo> pkgs = pm.getInstalledPackages(0);
        _PInfos = new ArrayList<PInfo>();

        _PInfos.add( new PInfo("_unselected", "none") );

        for (PackageInfo pi : pkgs) {
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                _PInfos.add( new PInfo( pi, pm) );
            }

        }

        Collections.sort(_PInfos, new PInfoComparator());

        _appAdapter = new ArrayAdapter<PInfo>(this,android.R.layout.simple_spinner_item, _PInfos);
        _appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _appSpinner.setAdapter(_appAdapter);
        _appSpinner.setSelection(0, false);
        _appSpinner.setOnItemSelectedListener(new  AdapterView.OnItemSelectedListener() {

            public void onItemSelected( AdapterView<?> parent, View view, int pos, long id){
                saveState();
            }
            public void onNothingSelected(AdapterView<?> parent){

            }

        });
    }

    private void initButtons() {

        final OnClickListener ocl = new OnClickListener() {
            public void onClick(final View v) {
                String tag = (String) v.getTag();
                _runSeconds += Integer.parseInt(tag) * 60;
                if (_runSeconds < 0) {
                    _runSeconds = 0;
                }
                saveState();
                setRunSeconds();
            }
        };

        findViewById(R.id.btn1).setOnClickListener(ocl);
        findViewById(R.id.btn5).setOnClickListener(ocl);
        findViewById(R.id.btn10).setOnClickListener(ocl);
        findViewById(R.id.btn30).setOnClickListener(ocl);
        findViewById(R.id.btn60).setOnClickListener(ocl);
        findViewById(R.id.btnMinus1).setOnClickListener(ocl);
        findViewById(R.id.btnMinus5).setOnClickListener(ocl);
        findViewById(R.id.btnMinus10).setOnClickListener(ocl);
        findViewById(R.id.btnMinus30).setOnClickListener(ocl);
        findViewById(R.id.btnMinus60).setOnClickListener(ocl);


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

        findViewById(R.id.cbBluetooth).setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    saveState();
                }
            }
        );

        findViewById(R.id.cbWifi).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        saveState();
                    }
                }
        );
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

    private void startCountdown() {

        if ((_runState == NOT_STARTED) || (_runState == PAUSED)) {
            showShutdownNotification();
            createCountDown();
            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.pause_word);
            _runState = RUNNING;
//            if ((_runState == PAUSED) && (! _runApp.equalsIgnoreCase(_UNSELECTED_))) {
//                Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(_runApp);
//                startActivity(LaunchIntent);
//            }
        }
        else if (_runState == RUNNING) {
            _cdTimer.cancel();
            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.start_word);
            _runState = PAUSED;
            clearNotification();
        }
    }

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

    private void setupBluetooth() {

        _cbBluetooth = (CheckBox) findViewById(R.id.cbBluetooth);
        _BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        _cbBluetooth.setEnabled( (_BluetoothAdapter != null) && _BluetoothAdapter.isEnabled() );

        BroadcastReceiver br = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            _cbBluetooth.setEnabled(false);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            break;
                        case BluetoothAdapter.STATE_ON:
                            _cbBluetooth.setEnabled(true);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            break;
                    }
                }
            }
        };

        registerReceiver(
                br,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        );
    }

    private void setupWifi() {
        _cbWifi = (CheckBox) findViewById(R.id.cbWifi);
        _wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        _cbWifi.setEnabled(_wifiManager != null && _wifiManager.isWifiEnabled());

        BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo != null &&
                        netInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                        netInfo.isConnectedOrConnecting() )
                    _cbWifi.setEnabled(true);
                else
                    _cbWifi.setEnabled(false);
            }
        };

        registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        );

    }

}
