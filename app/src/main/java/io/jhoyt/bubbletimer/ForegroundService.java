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

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.jhoyt.bubbletimer.db.ActiveTimer;
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

    private final ActiveTimerRepository activeTimerRepository;
    private final Map<String, Window> windowsByTimerId;

    private Window expandedWindow;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ForegroundService", "onReceive called");

            String command = intent.getStringExtra("command");

            if (command == null) {
                return;
            }

            List<Timer> timers = activeTimerRepository.getAllActiveTimers();
            if (command.equals("showOverlay")) {
                if (!timers.isEmpty()) {
                    timers.forEach(timer -> {
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
                if (!timers.isEmpty()) {
                    timers.forEach(timer -> {
                        if (windowsByTimerId.containsKey(timer.getId())) {
                            windowsByTimerId.get(timer.getId()).close();
                        }
                    });
                }
                ForegroundService.this.expandedWindow.close();
            } else if (command.equals("sendActiveTimers")) {
                sendActiveTimers();
            } else if (command.equals("activateTimer")) {
                String id = intent.getStringExtra("id");
                String userId = intent.getStringExtra("userId");
                String name = intent.getStringExtra("name");
                long totalDurationSeconds = intent.getLongExtra("totalDurationSeconds", 0L);
                long remainingDurationSeconds = intent.getLongExtra("remainingDurationSeconds", 0L);
                String timerEnd = intent.getStringExtra("timerEnd");

                Timer newTimer = new Timer(new TimerData(
                        id,
                        userId,
                        name,
                        (intent.hasExtra("totalDurationSeconds")) ? Duration.ofSeconds(totalDurationSeconds) : null,
                        (intent.hasExtra("remainingDurationSeconds")) ? Duration.ofSeconds(remainingDurationSeconds) : null,
                        (intent.hasExtra("timerEnd")) ? LocalDateTime.parse(timerEnd): null
                ));
                ForegroundService.this.activeTimerRepository.insert(newTimer);
                ForegroundService.this.windowsByTimerId.put(newTimer.getId(), new Window(getApplicationContext(), false));

                sendActiveTimers();
                sendUpdateTimerToWebsocket(newTimer, command);
            } else if (command.equals("stopTimer")) {
                String id = intent.getStringExtra("id");

                Timer timer = ForegroundService.this.activeTimerRepository.getById(id);
                if (null != timer) {
                    ForegroundService.this.activeTimerRepository.deleteById(timer.getId());
                    sendStopTimerToWebsocket(id, timer.getSharedWith());
                }

                if (windowsByTimerId.containsKey(id)) {
                    windowsByTimerId.get(id).close();
                    windowsByTimerId.remove(id);
                }

                if (vibrator != null) {
                    vibrator.cancel();
                    vibrator = null;
                } else if (vibratorManager != null) {
                    // just in case...
                    vibratorManager.cancel();
                }

                sendActiveTimers();
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

    private void updateLocalTimerList(Timer timer) {
        final String id = timer.getId();
        Timer storedTimer = this.activeTimerRepository.getById(id);
        if (null == storedTimer) {
            this.activeTimerRepository.insert(timer);
        } else {
            this.activeTimerRepository.update(timer);
        }

        if (windowsByTimerId.containsKey(id)) {
            Window window = windowsByTimerId.get(id);
            if (window.isOpen()) {
                window.open(timer, this);
            }
        } else {
            windowsByTimerId.put(id, new Window(getApplicationContext(), false));
        }
    }

    private void sendUpdateTimerToWebsocket(Timer timer, String updateReason) {
        JSONArray shareWithArray = new JSONArray();
        timer.getSharedWith().forEach(shareWithArray::put);

        String webSocketRequestString = null;
        try {
            webSocketRequestString = new JSONObject()
                    .put("action", "sendmessage")
                    .put("data", new JSONObject()
                            .put("type", "updateTimer")
                            .put("reason", updateReason)
                            .put("shareWith", shareWithArray)
                            .put("timer", Timer.timerToJson(timer))
                    )
                    .toString();
        } catch (Exception e) {
            Log.i("ForegroundService", "Websocket, sendmessage failed", e);
        }

        websocketManager.sendMessage(webSocketRequestString);
    }

    private void sendStopTimerToWebsocket(String timerId, Set<String> sharedWith) {
        JSONArray shareWithArray = new JSONArray();
        sharedWith.forEach(shareWithArray::put);

        String webSocketRequestString = null;
        try {
            webSocketRequestString = new JSONObject()
                    .put("action", "sendmessage")
                    .put("data", new JSONObject()
                            .put("type", "stopTimer")
                            .put("shareWith", shareWithArray)
                            .put("timerId", timerId)
                    )
                    .toString();
        } catch (Exception e) {
            Log.i("ForegroundService", "Websocket, sendmessage failed", e);
        }

        websocketManager.sendMessage(webSocketRequestString);
    }

    private void sendActiveTimers() {
        List<Timer> timers = activeTimerRepository.getAllActiveTimers();

        Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "receiveActiveTimers");
        message.putExtra("activeTimerCount", timers.size());
        int i = 0;
        Iterator<Timer> it = timers.iterator();
        while (it.hasNext()) {
            Timer timer = it.next();

            TimerData timerData = timer.getTimerData();

            message.putExtra("id" + i, timerData.id);
            message.putExtra("userId" + i, timerData.userId);
            message.putExtra("name" + i, timerData.name);
            if (timerData.totalDuration != null) {
                message.putExtra("totalDurationSeconds" + i, timerData.totalDuration.getSeconds());
            }
            if (timerData.remainingDurationWhenPaused != null) {
                message.putExtra("remainingDurationSeconds" + i, timerData.remainingDurationWhenPaused.getSeconds());
            }
            if (timerData.timerEnd != null) {
                message.putExtra("timerEnd" + i, timerData.timerEnd.toString());
            }

            i++;
        }


        LocalBroadcastManager.getInstance(ForegroundService.this).sendBroadcast(message);
    }

    public ForegroundService() {
        Log.i("ForegroundService", "Constructing");
        this.timerHandler = new Handler();
        this.expandedWindow = null;
        this.vibrator = null;
        this.activeTimerRepository = new ActiveTimerRepository(this, getApplication());
        this.windowsByTimerId = new HashMap<>();
        this.websocketManager = new WebsocketManager(
                new WebsocketManager.WebsocketMessageListener() {
                    @Override
                    public void onFailure(String reason) {
                        Log.i("ForegroundService", "Websocket failure: " + reason);

                        Intent message = new Intent(MainActivity.MESSAGE_RECEIVER_ACTION);
                        message.putExtra("command", "sendAuthToken");
                        LocalBroadcastManager.getInstance(ForegroundService.this).sendBroadcast(message);
                    }

                    @Override
                    public void onActiveTimerList(JSONArray timerList) throws JSONException {
                        for (int i = 0; i < timerList.length(); i++) {
                            updateLocalTimerList(Timer.timerFromJson(timerList.getJSONObject(i)));
                        }

                        sendActiveTimers();
                    }

                    @Override
                    public void onUpdateTimer(JSONObject timer) throws JSONException {
                        updateLocalTimerList(Timer.timerFromJson(timer));

                        sendActiveTimers();
                    }

                    @Override
                    public void onStopTimer(String timerId) throws JSONException {
                        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
                        message.putExtra("command", "stopTimer");
                        message.putExtra("id", timerId);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);

                        /*
                        if (timersById.containsKey(timerId)) {
                            timers.remove(timersById.get(timerId));
                            timersById.remove(timerId);
                        }

                        if (windowsByTimerId.containsKey(timerId)) {
                            windowsByTimerId.get(timerId).close();
                            windowsByTimerId.remove(timerId);
                        }

                        sendActiveTimers();
                         */
                    }
                }
        );
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

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
            List<Timer> timers = this.activeTimerRepository.getAllActiveTimers();
            if (timers != null && !timers.isEmpty()) {
                String name = timers.get(0).getName();
                Duration remainingDuration = timers.get(0).getRemainingDuration();
                String text = "[" + name + "] " + DurationUtil.getFormattedDuration(remainingDuration);

                notificationBuilder.setContentText(text);
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

                for (Timer timer : timers) {
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
    public void onDestroy() {
        super.onDestroy();

        windowsByTimerId.forEach((timerId, window) -> {
            window.close();
        });

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        websocketManager.close();
    }

    @Override
    public void onTimerUpdated(Timer timer) {
        sendUpdateTimerToWebsocket(timer, "Updated by button press");
    }

    @Override
    public void onTimerStopped(Timer timer) {
        sendStopTimerToWebsocket(timer.getId(), timer.getSharedWith());
    }

    @Override
    public void onBubbleDismiss(Timer timer) {
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }

        this.activeTimerRepository.deleteById(timer.getId());
        if (windowsByTimerId.containsKey(timer.getId())) {
            windowsByTimerId.get(timer.getId()).close();
            windowsByTimerId.remove(timer.getId());
        }

        notificationBuilder.setContentText("No active timers.");
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
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
}
