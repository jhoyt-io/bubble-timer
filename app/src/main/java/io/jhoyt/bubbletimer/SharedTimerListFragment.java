package io.jhoyt.bubbletimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import io.jhoyt.bubbletimer.db.SharedTimer;
import io.jhoyt.bubbletimer.db.SharedTimerViewModel;

public class SharedTimerListFragment extends Fragment {
    private static final String TAG = "SharedTimerListFragment";
    
    private SharedTimerViewModel viewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedTimerAdapter adapter;
    private TextView emptyStateText;

    private final BroadcastReceiver authTokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra("command");
            if ("receiveAuthToken".equals(command)) {
                String authToken = intent.getStringExtra("authToken");
                String callback = intent.getStringExtra("callback");
                
                if ("rejectSharedTimer".equals(callback)) {
                    String timerId = intent.getStringExtra("timerId");
                    Log.i(TAG, "Received auth token for rejecting shared timer: " + timerId);
                    viewModel.rejectSharedTimerWithToken(timerId, authToken);
                } else {
                    Log.i(TAG, "Received auth token for refreshing shared timers");
                    viewModel.refreshSharedTimersWithToken(authToken);
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shared_timers, container, false);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(SharedTimerViewModel.class);
        
        // Setup views
        recyclerView = view.findViewById(R.id.sharedTimersRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup SwipeRefreshLayout
        setupSwipeRefresh();
        
        // Observe data
        observeData();
        
        // Register for auth token broadcasts
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(authTokenReceiver, new IntentFilter(ForegroundService.MESSAGE_RECEIVER_ACTION));
        
        return view;
    }

    private void setupRecyclerView() {
        adapter = new SharedTimerAdapter(new ArrayList<>(), new SharedTimerAdapter.OnTimerActionListener() {
            @Override
            public void onAcceptTimer(String timerId) {
                SharedTimerListFragment.this.onAcceptTimer(timerId);
            }

            @Override
            public void onRejectTimer(String timerId) {
                SharedTimerListFragment.this.onRejectTimer(timerId);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.i(TAG, "Pull-to-refresh triggered");
            viewModel.refreshSharedTimers();
        });
    }

    private void observeData() {
        // Observe shared timers
        viewModel.getPendingSharedTimers().observe(getViewLifecycleOwner(), sharedTimers -> {
            Log.i(TAG, "Shared timers updated: " + (sharedTimers != null ? sharedTimers.size() : 0));
            updateUI(sharedTimers);
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            swipeRefreshLayout.setRefreshing(isLoading);
        });

        // Observe errors
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.e(TAG, "Error: " + error);
                // Show error message to user
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Error: " + error);
            }
        });
    }

    private void updateUI(List<SharedTimer> sharedTimers) {
        if (sharedTimers == null || sharedTimers.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText("No shared timer invitations");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            adapter.updateSharedTimers(sharedTimers);
        }
    }

    private void onAcceptTimer(String timerId) {
        Log.i(TAG, "Accepting timer: " + timerId);
        viewModel.acceptSharedTimer(timerId);
    }

    private void onRejectTimer(String timerId) {
        Log.i(TAG, "Rejecting timer: " + timerId);
        viewModel.rejectSharedTimer(timerId);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        viewModel.refreshSharedTimers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(authTokenReceiver);
    }
} 