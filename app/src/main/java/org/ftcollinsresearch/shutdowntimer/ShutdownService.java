package org.ftcollinsresearch.shutdowntimer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.widget.Button;

/**
 * Created by jeff boehmer on 2/23/15.
 */
public class ShutdownService extends Service {

    private final int NOT_STARTED = 0, RUNNING = 1, PAUSED = 2, COMPLETE = 3;
    private int _runState = NOT_STARTED;
    private CountDownTimer _cdTimer = null;
    private int _runSeconds = 10;


    private final IBinder localBinder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _runSeconds = intent.getIntExtra(ShutdownTimer.RUN_SECS, _runSeconds);
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }


    private void startCountdown() {

//        if (_runState == NOT_STARTED) {
//            createCountDown();
//            if (! _runApp.equalsIgnoreCase(_UNSELECTED_)) {
//                Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(_runApp);
//                startActivity(LaunchIntent);
//            }
//            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.pause_word);
//            _runState = RUNNING;
//        }
//        else if (_runState == RUNNING) {
//            _cdTimer.cancel();
//            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.start_word);
//            _runState = PAUSED;
//        } else if (_runState == PAUSED) {
//            ((Button)findViewById(R.id.btnStartTimer)).setText(R.string.pause_word);
//            _runState = RUNNING;
//        }
    }

    private void createCountDown() {
//        _cdTimer = new CountDownTimer(_runSeconds * 1000, 1000) {
//
//            public void onTick(long millisUntilFinished) {
//                _runSeconds = (int) (millisUntilFinished / 1000);
//                setRunSeconds();
//            }
//
//            public void onFinish() {
//                shutdown();
//            }
//        }.start();
    }

    public class LocalBinder extends Binder {
        ShutdownService getService() {
            return ShutdownService.this;
        }
    }
}
