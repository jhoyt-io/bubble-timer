package io.jhoyt.bubbletimer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import io.jhoyt.bubbletimer.ActiveTimerViewModel;

public class ActiveTimerListFragment extends Fragment {

    public ActiveTimerListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_active_timer_bar, container, false);

        final LinearLayout listLayout = view.findViewById(R.id.activeTimerList);

        final ActiveTimerViewModel activeTimerViewModel = new ViewModelProvider(requireActivity()).get(ActiveTimerViewModel.class);
        activeTimerViewModel.getActiveTimers().observe(requireActivity(), timers -> {
            listLayout.removeAllViews();

            timers.forEach(timer -> {
                ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.active_timer, listLayout, false);
                listLayout.addView(layout);

                TimerView timerView = layout.findViewById(R.id.timer);
                timerView.setLayoutMode(TimerView.MODE_LIST_ITEM);
                timerView.setCurrentUserId(((MainActivity)getActivity()).getUserId());
                timerView.setTimer(timer);

                layout.findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activeTimerViewModel.stopTimer(timer.getId());
                    }
                });
            });

            if (timers.isEmpty()) {
                listLayout.setMinimumHeight(0);
            } else {
                listLayout.setMinimumHeight(400);
            }
        });

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
