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
import androidx.viewpager2.widget.ViewPager2;

import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.jhoyt.bubbletimer.db.ActiveTimerViewModel;
import io.jhoyt.bubbletimer.db.Tag;
import io.jhoyt.bubbletimer.db.TagViewModel;
import io.jhoyt.bubbletimer.db.TimerViewModel;

public class MainActivity extends AppCompatActivity {
    public static String MESSAGE_RECEIVER_ACTION = "main-activity-message-receiver";
    public static int NEW_TIMER_REQUEST = 0;
    public static int EDIT_TIMER_REQUEST = 1;

    private Handler timerHandler;
    private Runnable updater;

    private List<Timer> activeTimers;
    private String userId;

    private ActiveTimerViewModel activeTimerViewModel;
    private TimerViewModel timerViewModel;
    private TagViewModel tagViewModel;
    
    private long lastAuthTokenRequest = 0;
    private static final long AUTH_TOKEN_DEBOUNCE_MS = 5000; // 5 seconds

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra("command");

            if (command.equals("sendAuthToken")) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAuthTokenRequest < AUTH_TOKEN_DEBOUNCE_MS) {
                    Log.i("MainActivity", "Ignoring sendAuthToken request - too soon since last request");
                    return;
                }
                lastAuthTokenRequest = currentTime;
                
                Log.i("MainActivity", "sendAuthToken command received, fetching auth session...");
                // Fetch auth token and send to the foreground service
                Amplify.Auth.fetchAuthSession(authSession -> {
                    Log.i("MainActivity", "Auth session fetched successfully");
                    AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) authSession;

                    Amplify.Auth.getCurrentUser(authUser -> {
                        userId = authUser.getUsername();
                        String idToken = cognitoAuthSession.getUserPoolTokensResult().getValue().getIdToken();
                        idToken = idToken != null ? idToken : "";
                        Log.i("MainActivity", "Token length: " + (idToken != null ? idToken.length() : 0));
                        Log.i("MainActivity", "Username: " + userId);
                        Log.i("MainActivity", "Is signed in: " + cognitoAuthSession.isSignedIn());

                        if (userId == null || userId.isEmpty()) {
                            Log.e("MainActivity", "ERROR: userId is null or empty after authentication!");
                        }
                        
                        if (idToken == null || idToken.isEmpty()) {
                            Log.e("MainActivity", "ERROR: idToken is null or empty after authentication!");
                        }

                        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
                        message.putExtra("command", "receiveAuthToken");
                        message.putExtra("authToken", idToken);
                        message.putExtra("userId", userId);

                        Log.i("MainActivity", "Sending auth token to ForegroundService");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(message);
                        
                        // Only update the adapter if it hasn't been set up yet
                        ViewPager2 viewPager = findViewById(R.id.timerPager);
                        if (viewPager.getAdapter() == null) {
                            runOnUiThread(() -> setupTabsAndAdapterIfReady());
                        }
                    }, error -> {
                        Log.e("MainActivity", "Error getting current user", error);
                        Log.e("MainActivity", "Error details: " + error.getCause());
                    });
                }, error -> {
                    Log.e("MainActivity", "Error fetching auth session", error);
                    Log.e("MainActivity", "Auth session error details: " + error.getCause());
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
                boolean needsUpdate = false;
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View cardTimer = layout.getChildAt(i);
                    TimerView timerView = (TimerView) cardTimer.findViewById(R.id.timer);
                    if (timerView != null) {
                        needsUpdate = true;
                        timerView.invalidate();
                    }
                }
                
                // Only schedule next update if we actually updated something
                if (needsUpdate) {
                    timerHandler.postDelayed(updater, 1000);
                } else {
                    timerHandler.postDelayed(updater, 100); // Check more frequently if no updates needed
                }
            }
        };
        timerHandler.post(updater);

        this.findViewById(R.id.button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewTimerActivity.class);
            startActivityForResult(intent, 0);
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
        this.activeTimerViewModel.getAllActiveTimers().observe(this, timers -> {
            this.activeTimers = timers;
        });

        this.timerViewModel = new ViewModelProvider(this).get(TimerViewModel.class);
        this.tagViewModel = new ViewModelProvider(this).get(TagViewModel.class);

        // Set up ViewPager2 immediately with local data
        setupTabsAndAdapterIfReady();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, new IntentFilter(MainActivity.MESSAGE_RECEIVER_ACTION));

        // Request auth token on behalf of the service  (TODO: why??)
        Log.i("MainActivity", "Requesting auth token for ForegroundService");
        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "sendAuthToken");
        LocalBroadcastManager.getInstance(this).sendBroadcast(message);
        
        // Also check if user is already signed in
        Amplify.Auth.getCurrentUser(authUser -> {
            Log.i("MainActivity", "Current user check - Username: " + authUser.getUsername());
            userId = authUser.getUsername();
        }, error -> {
            Log.e("MainActivity", "Error getting current user on startup: ", error);
        });
    }

    private void setupTabsAndAdapterIfReady() {
        if (tagViewModel == null) {
            Log.d("MainActivity", "setupTabsAndAdapterIfReady - initializing tagViewModel");
            tagViewModel = new ViewModelProvider(this).get(TagViewModel.class);
        }
        tagViewModel.getAllTags().observe(this, tags -> {
            Log.d("MainActivity", "setupTabsAndAdapterIfReady - got " + tags.size() + " tags");
            List<String> tabs = new ArrayList<>(tags.size()+1);
            tabs.add("ALL");
            tags.forEach(tag -> tabs.add(tag.name));

            ViewPager2 viewPager = findViewById(R.id.timerPager);
            Log.d("MainActivity", "setupTabsAndAdapterIfReady - setting up ViewPager2 with " + tabs.size() + " tabs");
            viewPager.setAdapter(new TimerListCollectionAdapter(this, userId, tabs));

            TabLayout tabLayout = findViewById(R.id.tabLayout);
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                Log.d("MainActivity", "setupTabsAndAdapterIfReady - setting tab " + position + " to: " + tabs.get(position));
                tab.setText(tabs.get(position));
            }).attach();
        });
    }

    public void startTimer(String name, Duration duration, Set<String> tags) {
        if (userId == null) {
            Log.i("MainActivity", "userId still null - not starting timer");
        }

        Timer timer = new Timer(userId, name, duration, tags);
        timer.unpause();

        this.activeTimerViewModel.insert(timer);
    }

    public void deleteTimer(int id) {
        // Show loading state if needed
        timerViewModel.isDeleting().observe(this, isDeleting -> {
            if (isDeleting) {
                // You could show a loading indicator here if needed
                // findViewById(R.id.loadingIndicator).setVisibility(View.VISIBLE);
            } else {
                // findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
            }
        });
        
        // Delete timer - this will update UI immediately through the cache
        timerViewModel.deleteById(id);
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

    @Override
    protected void onResume() {
        super.onResume();

        Intent message = new Intent(ForegroundService.MESSAGE_RECEIVER_ACTION);
        message.putExtra("command", "hideOverlay");
        LocalBroadcastManager.getInstance(this).sendBroadcast(message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            return;
        }

        if (requestCode == NEW_TIMER_REQUEST || requestCode == EDIT_TIMER_REQUEST) {
            if (resultCode == RESULT_OK) {
                String title = data.getStringExtra("timerTitle");
                String durationString = data.getStringExtra("timerDuration");
                String tagsString = data.getStringExtra("tagsString");

                String[] durationStringSplit = durationString.split(":");
                Duration duration = Duration.ofSeconds(5);

                Set<String> tags = new HashSet<>();
                if (tagsString != null) {
                    tags = Set.of(tagsString.split("#~#"));
                }

                final List<String> allTags =
                    this.tagViewModel.getAllTags().getValue() == null
                    ? new ArrayList<>()
                    : this.tagViewModel.getAllTags().getValue().stream()
                            .map(tag -> tag.name)
                            .collect(Collectors.toList());
                tags.forEach(tag -> {
                    if (!tag.trim().isEmpty() && !allTags.contains(tag) ) {
                        this.tagViewModel.insert(new Tag(tag));
                    }
                });

                if (durationStringSplit.length == 1) {
                    int seconds = Integer.valueOf(durationStringSplit[0]);
                    duration = Duration.ofSeconds(seconds);
                }

                if (durationStringSplit.length == 2) {
                    int minutes = Integer.valueOf(durationStringSplit[0]);
                    int seconds = Integer.valueOf(durationStringSplit[1]);
                    duration = Duration.ofMinutes(minutes);
                    duration = duration.plus(Duration.ofSeconds(seconds));
                }

                if (durationStringSplit.length == 3) {
                    int hours   = Integer.valueOf(durationStringSplit[0]);
                    int minutes = Integer.valueOf(durationStringSplit[1]);
                    int seconds = Integer.valueOf(durationStringSplit[2]);
                    duration = Duration.ofHours(hours);
                    duration = duration.plus(Duration.ofMinutes(minutes));
                    duration = duration.plus(Duration.ofSeconds(seconds));
                }

                if (requestCode == NEW_TIMER_REQUEST) {
                    io.jhoyt.bubbletimer.db.Timer timer = new io.jhoyt.bubbletimer.db.Timer(
                            title,
                            duration,
                            tagsString
                    );
                    this.timerViewModel.insert(timer);

                    boolean startTimerNow = data.getBooleanExtra("startTimerNow", false);
                    if (startTimerNow) {
                        startTimer(title, duration, tags);
                    }
                } else if (requestCode == EDIT_TIMER_REQUEST) {
                    int timerId = data.getIntExtra("timerId", -1);

                    if (timerId > -1) {
                        io.jhoyt.bubbletimer.db.Timer timer = new io.jhoyt.bubbletimer.db.Timer(
                                timerId,
                                title,
                                duration,
                                tagsString
                        );
                        this.timerViewModel.update(timer);
                    }
                }
            }
        }

    }

    public String getUserId() {
        return userId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        timerHandler.removeCallbacks(updater);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}
