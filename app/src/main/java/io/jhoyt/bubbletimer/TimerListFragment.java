package io.jhoyt.bubbletimer;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.jhoyt.bubbletimer.db.Tag;
import io.jhoyt.bubbletimer.db.TagViewModel;
import io.jhoyt.bubbletimer.db.TimerViewModel;
import io.jhoyt.bubbletimer.db.Timer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimerListFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private static final String ARG_TAG = "tag";

    private String userId;
    private String tag;
    private final ConcurrentHashMap<Integer, View> timerViews = new ConcurrentHashMap<>();
    private LayoutInflater inflater;
    private LinearLayout listLayout;

    public TimerListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tag Parameter 1.
     * @return A new instance of fragment TimerListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TimerListFragment newInstance(String userId, String tag) {
        TimerListFragment fragment = new TimerListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_TAG, tag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TimerListFragment", "onCreate called");
        if (getArguments() != null) {
            tag = getArguments().getString(ARG_TAG);
            userId = getArguments().getString(ARG_USER_ID);
            Log.d("TimerListFragment", "onCreate - tag: " + tag + ", userId: " + userId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("TimerListFragment", "onCreateView called");
        final TimerViewModel timerViewModel = new ViewModelProvider(requireActivity()).get(TimerViewModel.class);
        final TagViewModel tagViewModel = new ViewModelProvider(requireActivity()).get(TagViewModel.class);

        final boolean isAllTab = tag.equals("ALL");
        Log.d("TimerListFragment", "onCreateView - isAllTab: " + isAllTab);

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_timer_list, container, false);
        Log.d("TimerListFragment", "Layout inflated");

        // Store inflater for use in background threads
        this.inflater = inflater;
        
        final LinearLayout scrollFragmentContainer = view.findViewById(R.id.scrollFragmentContainer);
        this.listLayout = view.findViewById(R.id.timerList);
        final ProgressBar loadingIndicator = view.findViewById(R.id.loadingIndicator);
        final TextView emptyState = view.findViewById(R.id.emptyState);

        Log.d("TimerListFragment", "Found views - loadingIndicator: " + (loadingIndicator != null) + 
            ", emptyState: " + (emptyState != null) + 
            ", listLayout: " + (listLayout != null));

        // Show loading state initially
        loadingIndicator.setVisibility(View.VISIBLE);
        listLayout.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        Log.d("TimerListFragment", "Set initial visibility states");

        // Configure tab button
        final TextView configureTab = view.findViewById(R.id.configureTab);
        if (isAllTab) {
            Log.d("TimerListFragment", "Removing configure tab for ALL tab");
            scrollFragmentContainer.removeViewAt(0);
        } else {
            configureTab.setOnClickListener(textView -> {
                // Configure tab view isn't expanded
                if (scrollFragmentContainer.getChildCount() == 2 && !tag.equals("ALL")) {
                    final View configureTabView = inflater.inflate(R.layout.configure_tab, container, false);
                    scrollFragmentContainer.addView(configureTabView, 1);

                    final Button deleteButton = configureTabView.findViewById(R.id.deleteButton);
                    deleteButton.setOnClickListener(buttonView -> {
                        tagViewModel.delete(new Tag(tag));
                    });
                } else if (scrollFragmentContainer.getChildCount() == 3) { // Configure tab view is expanded
                    scrollFragmentContainer.removeViewAt(1);
                }
            });
        }

                Observer<List<Timer>> observer = timers -> {
            Log.d("TimerListFragment", "Observer called with " + (timers != null ? timers.size() : "null") + " timers");
            
            // Hide loading indicator
            loadingIndicator.setVisibility(View.GONE);
            Log.d("TimerListFragment", "Hidden loading indicator");

            if (timers == null || timers.isEmpty()) {
                Log.d("TimerListFragment", "No timers to display, showing empty state");
                this.listLayout.removeAllViews();
                timerViews.clear();
                emptyState.setVisibility(View.VISIBLE);
                this.listLayout.setVisibility(View.GONE);
                return;
            }

            emptyState.setVisibility(View.GONE);
            this.listLayout.setVisibility(View.VISIBLE);
            Log.d("TimerListFragment", "Showing timer list with " + timers.size() + " timers");

                        // Only do full refresh if the number of timers changed significantly
            // This prevents unnecessary full refreshes for minor updates
            int currentTimerCount = timerViews.size();
            int newTimerCount = timers.size();
            
            Log.d("TimerListFragment", "Timer count comparison - current: " + currentTimerCount + ", new: " + newTimerCount);
            
            if (Math.abs(newTimerCount - currentTimerCount) > 1 || newTimerCount > currentTimerCount) {
                // Significant change or new timers added - do full refresh
                Log.d("TimerListFragment", "Significant change or new timers detected, doing full refresh");
                refreshAllTimers(timers);
            } else {
                // Minor change (updates only) - do incremental update
                Log.d("TimerListFragment", "Minor change detected, doing incremental update");
                updateTimersIncrementally(timers);
            }
        };

        Log.d("TimerListFragment", "Setting up observer for tag: " + tag);
        if (tag.equals("ALL")) {
            Log.d("TimerListFragment", "Observing all timers");
            timerViewModel.getAllTimers().observe(getViewLifecycleOwner(), observer);
        } else {
            Log.d("TimerListFragment", "Observing timers with tag: " + tag);
            timerViewModel.getAllTimersWithTag(tag).observe(getViewLifecycleOwner(), observer);
        }

        return view;
    }
    
    private void refreshAllTimers(List<Timer> timers) {
        Log.d("TimerListFragment", "refreshAllTimers called with " + timers.size() + " timers");
        // Process timers in background to avoid blocking UI
        new Thread(() -> {
            // Check if fragment is still valid (only check inflater and listLayout in background)
            Log.d("TimerListFragment", "Background thread - inflater: " + (this.inflater != null) + ", listLayout: " + (this.listLayout != null));
            if (this.inflater == null || this.listLayout == null) {
                Log.d("TimerListFragment", "Fragment views not available, skipping refresh");
                return;
            }
            
            // Sort timers in background
            List<Timer> sortedTimers = new ArrayList<>(timers);
            sortedTimers.sort(new Comparator<Timer>() {
                @Override
                public int compare(Timer o1, Timer o2) {
                    if (o1.duration.getSeconds() < o2.duration.getSeconds()) {
                        return -1;
                    }

                    if (o1.duration.getSeconds() == o2.duration.getSeconds()) {
                        return 0;
                    }

                    return 1;
                }
            });

            // Create all views in background
            List<View> timerViewList = new ArrayList<>();
            for (Timer timer : sortedTimers) {
                Log.d("TimerListFragment", "Creating view for timer: " + timer.title + " (id: " + timer.id + ")");
                View cardTimer = this.inflater.inflate(R.layout.card_timer, this.listLayout, false);
                
                TimerView timerView = cardTimer.findViewById(R.id.timer);
                timerView.setLayoutMode(TimerView.MODE_LIST_ITEM);
                timerView.setTimer(new io.jhoyt.bubbletimer.Timer(new TimerData(
                        String.valueOf(timer.id),
                        userId,
                        timer.title,
                        timer.duration,
                        null,
                        null,
                        (timer.tagsString == null) ?
                                Set.of()
                                : Set.of(timer.tagsString.split("#~#"))
                ), new HashSet<>()));

                final Timer finalTimer = timer;
                final TimerView finalTimerView = timerView;
                
                ((Button)cardTimer.findViewById(R.id.startButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).startTimer(finalTimer.title, finalTimerView.getRemainingDuration(), finalTimerView.getTags());
                    }
                });

                cardTimer.findViewById(R.id.editButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), EditTimerActivity.class);
                        intent.putExtra("timerId", finalTimer.id);
                        getActivity().startActivityForResult(intent, MainActivity.EDIT_TIMER_REQUEST);
                    }
                });

                ((ImageButton)cardTimer.findViewById(R.id.delete)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Disable the delete button to prevent multiple clicks
                        v.setEnabled(false);
                        
                        // Optimistic UI update - remove timer immediately
                        getActivity().runOnUiThread(() -> {
                            int index = TimerListFragment.this.listLayout.indexOfChild(cardTimer);
                            if (index != -1) {
                                TimerListFragment.this.listLayout.removeViewAt(index);
                                TimerListFragment.this.timerViews.remove(finalTimer.id);
                            }
                        });
                        
                        // Delete timer in background
                        ((MainActivity)getActivity()).deleteTimer(finalTimer.id);
                    }
                });

                timerViewList.add(cardTimer);
            }

            // Update UI on main thread
            getActivity().runOnUiThread(() -> {
                // Double-check fragment is still valid on main thread
                if (getActivity() == null || this.listLayout == null || !isAdded()) {
                    Log.d("TimerListFragment", "Fragment no longer valid on main thread, skipping UI update");
                    return;
                }
                
                Log.d("TimerListFragment", "Updating UI with " + sortedTimers.size() + " timers");
                this.listLayout.removeAllViews();
                TimerListFragment.this.timerViews.clear();
                
                for (int i = 0; i < sortedTimers.size(); i++) {
                    Timer timer = sortedTimers.get(i);
                    View cardTimer = timerViewList.get(i);
                    TimerListFragment.this.timerViews.put(timer.id, cardTimer);
                    this.listLayout.addView(cardTimer);
                    Log.d("TimerListFragment", "Added timer view: " + timer.title + " (id: " + timer.id + ")");
                }
                Log.d("TimerListFragment", "UI update completed");
            });
        }).start();
    }
    
    private void updateTimersIncrementally(List<Timer> timers) {
        // For incremental updates, just update existing views without full refresh
        // This is much faster and maintains scroll position
        new Thread(() -> {
            // Check if fragment views are available (only check listLayout in background)
            if (this.listLayout == null) {
                Log.d("TimerListFragment", "Fragment views not available, skipping incremental update");
                return;
            }
            
            // Sort timers in background
            List<Timer> sortedTimers = new ArrayList<>(timers);
            sortedTimers.sort(new Comparator<Timer>() {
                @Override
                public int compare(Timer o1, Timer o2) {
                    if (o1.duration.getSeconds() < o2.duration.getSeconds()) {
                        return -1;
                    }

                    if (o1.duration.getSeconds() == o2.duration.getSeconds()) {
                        return 0;
                    }

                    return 1;
                }
            });

            // Update UI on main thread
            getActivity().runOnUiThread(() -> {
                // Double-check fragment is still valid on main thread
                if (getActivity() == null || this.listLayout == null || !isAdded()) {
                    Log.d("TimerListFragment", "Fragment no longer valid on main thread, skipping incremental update");
                    return;
                }
                
                // Update existing timer views with new data
                for (Timer timer : sortedTimers) {
                    View existingView = timerViews.get(timer.id);
                    if (existingView != null) {
                        TimerView timerView = existingView.findViewById(R.id.timer);
                        timerView.setTimer(new io.jhoyt.bubbletimer.Timer(new TimerData(
                                String.valueOf(timer.id),
                                userId != null ? userId : "unknown",
                                timer.title,
                                timer.duration,
                                null,
                                null,
                                (timer.tagsString == null) ?
                                        Set.of()
                                        : Set.of(timer.tagsString.split("#~#"))
                        ), new HashSet<>()));
                    }
                }
            });
        }).start();
    }
}
