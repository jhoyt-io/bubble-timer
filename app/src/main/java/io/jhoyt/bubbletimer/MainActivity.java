package io.jhoyt.bubbletimer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import io.jhoyt.bubbletimer.db.TimerViewModel;

public class MainActivity extends AppCompatActivity {
    public static String MESSAGE_RECEIVER_ACTION = "main-activity-message-receiver";

    private Handler timerHandler;
    private Runnable updater;

    private Set<Timer> activeTimers;
    private String userId;

    private ActiveTimerViewModel activeTimerViewModel;
    private TimerViewModel timerViewModel;
    
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra("command");

            if (command.equals("receiveActiveTimers")) {
                long activeTimerCount = intent.getIntExtra("activeTimerCount", 0);

                Set<Timer> activeTimers = new HashSet<>();
                for (int i = 0; i < activeTimerCount; i++) {
                    String id = intent.getStringExtra("id" + i);
                    String userId = intent.getStringExtra("userId" + i);
                    String name = intent.getStringExtra("name" + i);
                    long totalDurationSeconds = intent.getLongExtra("totalDurationSeconds" + i, 0L);
                    long remainingDurationSeconds = intent.getLongExtra("remainingDurationSeconds" + i, 0L);
                    String timerEnd = intent.getStringExtra("timerEnd" + i);

                    Timer newTimer = new Timer(new TimerData(
                            id,
                            userId,
                            name,
                            (intent.hasExtra("totalDurationSeconds" + i)) ? Duration.ofSeconds(totalDurationSeconds) : null,
                            (intent.hasExtra("remainingDurationSeconds" + i)) ? Duration.ofSeconds(remainingDurationSeconds) : null,
                            (intent.hasExtra("timerEnd" + i)) ? LocalDateTime.parse(timerEnd): null
                    ));
                    activeTimers.add(newTimer);
                }
                activeTimerViewModel.resetActiveTimers(activeTimers, activeTimers.isEmpty() ?
                        null :
                        (Timer)activeTimers.toArray()[0]);
            } else if (command.equals("sendAuthToken")) {
                // Fetch auth token and send to the foreground service
                Amplify.Auth.fetchAuthSession(authSession -> {
                    AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) authSession;

                    Amplify.Auth.getCurrentUser(authUser -> {
                        userId = authUser.getUsername();
                        String idToken = cognitoAuthSession.getUserPoolTokensResult().getValue().getIdToken();
                        idToken = idToken != null ? idToken : "";
                        Log.i("MainActivity", "Token: " + idToken);
                        Log.i("MainActivity", "Username: " + userId);

                        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
                        message.putExtra("command", "receiveAuthToken");
                        message.putExtra("authToken", idToken);
                        message.putExtra("userId", userId);

                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
                    }, error -> {

                    });
                }, error -> {
                    Log.i("MainActivity", "ERROR: " + error.getMessage() +
                            ", recovery suggestion: " + error.getRecoverySuggestion() +
                            ", umm: " + error.getStackTrace()[0].toString());
                });
            }
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (PackageManager.PERMISSION_DENIED ==
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        this.timerHandler = new Handler();
        final ViewGroup activeTimerList = findViewById(R.id.activeTimerListFragment);
        this.updater = () -> {
            LinearLayout layout = activeTimerList.findViewById(R.id.activeTimerList);
            if (layout != null) {
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View cardTimer = layout.getChildAt(i);
                    TimerView timerView = (TimerView) cardTimer.findViewById(R.id.timer);
                    timerView.invalidate();
                }
            }

            timerHandler.postDelayed(updater, 1000);
        };
        timerHandler.post(updater);

        this.findViewById(R.id.button).setOnClickListener(v -> {
            // TODO: create new timer dialog

            // easy debug:
            // this.startService(69);

            Intent intent = new Intent(MainActivity.this, NewTimerActivity.class);
            startActivityForResult(intent, 0);

            //Amplify.Auth.signOut(onComplete -> {

            //});
        });

        this.findViewById(R.id.loginButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        }

        Intent intent = new Intent(this, ForegroundService.class);
        startForegroundService(intent);

        this.activeTimerViewModel = new ViewModelProvider(this).get(ActiveTimerViewModel.class);
        activeTimerViewModel.getActiveTimers().observe(this, timers -> {
            this.activeTimers = timers;
        });

        this.timerViewModel = new ViewModelProvider(this).get(TimerViewModel.class);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(MainActivity.MESSAGE_RECEIVER_ACTION));

        // Request active timers from the service, if there are any
        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "sendActiveTimers");
        LocalBroadcastManager.getInstance(this).sendBroadcast(message);

        message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "sendAuthToken");
        LocalBroadcastManager.getInstance(this).sendBroadcast(message);
    }

    public void startTimer(String name, Duration duration) {
        if (userId == null) {
            Log.i("MainActivity", "userId still null - not starting timer");
        }

        Timer timer = new Timer(userId, name, duration);

        timer.unpause();
        sendActivateTimer(timer);
    }

    public void stopTimer(Timer timer) {
        sendStopTimer(timer);
    }

    public void deleteTimer(int id) {
        this.timerViewModel.deleteById(id);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (this.activeTimers != null) {
            if (Settings.canDrawOverlays(this)) {
                Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
                message.putExtra("command", "showOverlay");

                LocalBroadcastManager.getInstance(this).sendBroadcast(message);
            }
        }
    }

    private void sendActivateTimer(Timer timer) {
        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "activateTimer");

        TimerData timerData = timer.getTimerData();

        message.putExtra("id", timerData.id);
        message.putExtra("userId", timerData.userId);
        message.putExtra("name", timerData.name);
        if (timerData.totalDuration != null) {
            message.putExtra("totalDurationSeconds", timerData.totalDuration.getSeconds());
        }
        if (timerData.remainingDurationWhenPaused != null) {
            message.putExtra("remainingDurationSeconds", timerData.remainingDurationWhenPaused.getSeconds());
        }
        if (timerData.timerEnd != null) {
            message.putExtra("timerEnd", timerData.timerEnd.toString());
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(message);
    }

    private void sendStopTimer(Timer timer) {
        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "stopTimer");

        TimerData timerData = timer.getTimerData();
        message.putExtra("id", timerData.id);

        LocalBroadcastManager.getInstance(this).sendBroadcast(message);
    }

    @Override
    protected void onResume() {
        super.onResume();

        {
            Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
            message.putExtra("command", "hideOverlay");
            LocalBroadcastManager.getInstance(this).sendBroadcast(message);
        }

        {
            Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
            message.putExtra("command", "sendActiveTimers");
            LocalBroadcastManager.getInstance(this).sendBroadcast(message);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            return;
        }

        if (resultCode == RESULT_OK) {
            String title = data.getStringExtra("timerTitle");
            String durationString = data.getStringExtra("timerDuration");
            boolean startTimerNow = data.getBooleanExtra("startTimerNow", false);

            String[] durationStringSplit = durationString.split(":");
            Duration duration = Duration.ofSeconds(5);

            if (durationStringSplit.length == 1) {
                int seconds = Integer.valueOf(durationStringSplit[0]);
                duration = Duration.ofSeconds(seconds);

                io.jhoyt.bubbletimer.db.Timer timer = new io.jhoyt.bubbletimer.db.Timer(
                        title,
                        duration
                );
                this.timerViewModel.insert(timer);
            }

            if (durationStringSplit.length == 2) {
                int minutes = Integer.valueOf(durationStringSplit[0]);
                int seconds = Integer.valueOf(durationStringSplit[1]);
                duration = Duration.ofMinutes(minutes);
                duration = duration.plus(Duration.ofSeconds(seconds));

                io.jhoyt.bubbletimer.db.Timer timer = new io.jhoyt.bubbletimer.db.Timer(
                        title,
                        duration
                );
                this.timerViewModel.insert(timer);
            }

            if (durationStringSplit.length == 3) {
                int hours   = Integer.valueOf(durationStringSplit[0]);
                int minutes = Integer.valueOf(durationStringSplit[1]);
                int seconds = Integer.valueOf(durationStringSplit[2]);
                duration = Duration.ofHours(hours);
                duration = duration.plus(Duration.ofMinutes(minutes));
                duration = duration.plus(Duration.ofSeconds(seconds));

                io.jhoyt.bubbletimer.db.Timer timer = new io.jhoyt.bubbletimer.db.Timer(
                        title,
                        duration
                );
                this.timerViewModel.insert(timer);
            }

            if (startTimerNow) {
                startTimer(title, duration);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        timerHandler.removeCallbacks(updater);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}
