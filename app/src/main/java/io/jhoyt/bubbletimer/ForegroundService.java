package io.jhoyt.bubbletimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings.Secure;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jhoyt.bubbletimer.db.ActiveTimerRepository;

public class ForegroundService extends LifecycleService implements Window.BubbleEventListener {
    public static final int NOTIFICATION_ID = 2;
    public static String MESSAGE_RECEIVER_ACTION = "foreground-service-message-receiver";
    static final String channelId = "jhoyt.io.permanence.v3";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private VibratorManager vibratorManager;
    private Vibrator vibrator;

    private PowerManager.WakeLock wakeLock;

    private final Handler timerHandler;
    private Runnable updater;

    private final WebsocketManager websocketManager;

    private ActiveTimerRepository activeTimerRepository;
    private List<Timer> activeTimers;
    private Map<String, Window> windowsByTimerId;

    private Window expandedWindow;

    private Boolean isOverlayShown = false;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ForegroundService", "onReceive called");

            String command = intent.getStringExtra("command");

            if (command == null) {
                return;
            }

            if (command.equals("showOverlay")) {
                isOverlayShown = true;
                if (!activeTimers.isEmpty()) {
                    activeTimers.forEach(timer -> {
                        if (windowsByTimerId.containsKey(timer.getId())) {
                            windowsByTimerId.get(timer.getId())
                                    .open(timer, ForegroundService.this);
                        }
                    });
                } else {
                    notificationBuilder.setContentText("No active timers.");
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                }
            } else if (command.equals("hideOverlay")) {
                isOverlayShown = false;
                if (!activeTimers.isEmpty()) {
                    activeTimers.forEach(timer -> {
                        if (windowsByTimerId.containsKey(timer.getId())) {
                            windowsByTimerId.get(timer.getId()).close();
                        }
                    });
                }

                if (ForegroundService.this.expandedWindow != null) {
                    ForegroundService.this.expandedWindow.close();
                }
            } else if (command.equals("receiveAuthToken")) {
                Log.i("ForegroundService", "Auth token received");

                String authToken = intent.getStringExtra("authToken");
                String userId = intent.getStringExtra("userId");
                String androidId = Secure.getString(
                        getApplicationContext().getContentResolver(),
                        Secure.ANDROID_ID);

                Log.i("ForegroundService", "Auth token: " + authToken);
                Log.i("ForegroundService", "User id: " + userId);
                Log.i("ForegroundService", "Android id: " + androidId);

                websocketManager.initialize(authToken, androidId);
            }
        }
    };



    public ForegroundService() {
        Log.i("ForegroundService", "Constructing");
        this.timerHandler = new Handler();
        this.expandedWindow = null;
        this.vibrator = null;

        this.websocketManager = new WebsocketManager(
                new WebsocketManager.WebsocketMessageListener() {
                    @Override
                    public void onFailure(String reason) {
                        Log.i("ForegroundService", "Websocket failure: " + reason);

                        Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
                        message.putExtra("command", "sendAuthToken");
                        LocalBroadcastManager.getInstance(ForegroundService.this).sendBroadcast(message);
                    }
                },
                activeTimerRepository,
                this
        );
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.activeTimerRepository = new ActiveTimerRepository(getApplication());
        this.activeTimers = new ArrayList<>();
        this.windowsByTimerId = new HashMap<>();

        this.activeTimerRepository.getAllActiveTimers().observe(this, timers -> {
            DiffUtil.calculateDiff(
                    new DiffUtil.Callback() {
                        @Override
                        public int getOldListSize() {
                            return activeTimers.size();
                        }

                        @Override
                        public int getNewListSize() {
                            return timers.size();
                        }

                        @Override
                        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                            return activeTimers.get(oldItemPosition).getId().equals(timers.get(newItemPosition).getId());
                        }

                        @Override
                        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                            return activeTimers.get(oldItemPosition).equals(timers.get(newItemPosition));
                        }
                    }
            ).dispatchUpdatesTo(new ListUpdateCallback() {
                @Override
                public void onInserted(int position, int count) {
                    timers.subList(position, position + count).forEach(timer -> {
                        Window window = new Window(getApplicationContext(), false);

                        if (isOverlayShown) {
                            window.open(timer, ForegroundService.this);
                        }

                        windowsByTimerId.put(timer.getId(), window);
                    });
                }

                @Override
                public void onRemoved(int position, int count) {
                    // stop vibrator every time - if the timer is still alarming it will start again
                    if (vibrator != null) {
                        vibrator.cancel();
                        vibrator = null;
                    }

                    activeTimers.subList(position, position + count).forEach(timer -> {
                        windowsByTimerId.get(timer.getId()).close();
                        windowsByTimerId.remove(timer.getId());
                    });
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {

                }

                @Override
                public void onChanged(int position, int count, @Nullable Object payload) {
                    activeTimers.subList(position, position + count).forEach(timer -> {
                    });
                }
            });
            this.activeTimers = List.copyOf(timers);
        });

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);

        String channelName = "Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Bubble Timer's service for managing timer bubbles");
        channel.setVibrationPattern(null);
        channel.setSound(null, null);
        channel.enableLights(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentTitle("Bubble Timer")
                .setContentText("Timers active")
                .setSmallIcon(R.drawable.bubble_logo)
                .setContentIntent(contentPendingIntent);

        this.startForeground(2, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(ForegroundService.MESSAGE_RECEIVER_ACTION));

        this.vibratorManager = (VibratorManager) getApplicationContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);

        Context context = getApplicationContext();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "bubbletimer::WakeLock");

        this.updater = () -> {
            windowsByTimerId.forEach((timerId, window) -> {
                window.invalidate();
            });

            if (this.expandedWindow != null) {
                this.expandedWindow.invalidate();
            }

            boolean shouldAlarm = false;
            if (!activeTimers.isEmpty()) {
                String name = activeTimers.get(0).getName();
                Duration remainingDuration = activeTimers.get(0).getRemainingDuration();
                String text = "[" + name + "] " + DurationUtil.getFormattedDuration(remainingDuration);

                notificationBuilder.setContentText(text);
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

                for (Timer timer : activeTimers) {
                    if (timer.getRemainingDuration().isNegative() || timer.getRemainingDuration().isZero()) {
                        shouldAlarm = true;
                        break;
                    }
                }
            }

            if (shouldAlarm && vibrator == null) {
                long[] timing   = {600L, 400L};
                int[] amplitude = { 250,    0};
                vibratorManager.vibrate(
                        CombinedVibration.createParallel(
                                VibrationEffect.createWaveform(timing, amplitude, 0)
                        )
                );
                vibrator = vibratorManager.getDefaultVibrator();


                if (wakeLock != null) {
                    //acquire will turn on the display
                    wakeLock.acquire();

                    //release will release the lock from CPU, in case of that, screen will go back to sleep mode in defined time bt device settings
                    wakeLock.release();
                }
            }
            timerHandler.postDelayed(updater, 1000);
        };
        timerHandler.post(updater);

        // Request auth data to initialize websocket
        // TODO: disabled becuase this was happening too soon before db initialization
        /*
        Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "sendAuthToken");
        LocalBroadcastManager.getInstance(ForegroundService.this).sendBroadcast(message);
         */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (this.expandedWindow == null) {
                this.expandedWindow = new Window(getApplicationContext(), true);
            }
        }

        timerHandler.post(updater);

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onTimerUpdated(Timer timer) {
        this.activeTimerRepository.update(timer);
    }

    @Override
    public void onTimerStopped(Timer timer) {
        this.activeTimerRepository.deleteById(timer.getId());

        notificationBuilder.setContentText("No active timers.");
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onBubbleDismiss(Timer timer) {
        this.onTimerStopped(timer);
    }

    @Override
    public void onBubbleClick(Timer clickedTimer) {
        Window window = windowsByTimerId.get(clickedTimer.getId());
        if (window != null) {
            Timer timer = this.activeTimerRepository.getById(clickedTimer.getId());
            if (window.isOpen()) {
                window.close();
                expandedWindow.open(timer, this);
            } else {
                expandedWindow.close();
                window.open(timer, this);
            }
        } else if (expandedWindow != null && expandedWindow.isOpen()) {
            expandedWindow.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        windowsByTimerId.forEach((timerId, window) -> {
            window.close();
        });

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        websocketManager.close();
    }
}
