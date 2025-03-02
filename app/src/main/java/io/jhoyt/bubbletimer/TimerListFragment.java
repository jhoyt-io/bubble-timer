package io.jhoyt.bubbletimer;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.jhoyt.bubbletimer.db.Tag;
import io.jhoyt.bubbletimer.db.TagViewModel;
import io.jhoyt.bubbletimer.db.TimerViewModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimerListFragment extends Fragment {

    private static final String ARG_TAG = "tag";

    private String tag;

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
    public static TimerListFragment newInstance(String tag) {
        TimerListFragment fragment = new TimerListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tag = getArguments().getString(ARG_TAG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final TimerViewModel timerViewModel = new ViewModelProvider(requireActivity()).get(TimerViewModel.class);
        final TagViewModel tagViewModel = new ViewModelProvider(requireActivity()).get(TagViewModel.class);

        final boolean isAllTab = tag.equals("ALL");

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_timer_list, container, false);

        final LinearLayout scrollFragmentContainer = view.findViewById(R.id.scrollFragmentContainer);

        // Configure tab button
        final TextView configureTab = view.findViewById(R.id.configureTab);
        if (isAllTab) {
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

        // List of timers
        final LinearLayout listLayout = view.findViewById(R.id.timerList);

        Observer<List<io.jhoyt.bubbletimer.db.Timer>> observer = timers -> {
            listLayout.removeAllViews();

            timers.sort(new Comparator<io.jhoyt.bubbletimer.db.Timer>() {
                @Override
                public int compare(io.jhoyt.bubbletimer.db.Timer o1, io.jhoyt.bubbletimer.db.Timer o2) {
                    if (o1.duration.getSeconds() < o2.duration.getSeconds()) {
                        return -1;
                    }

                    if (o1.duration.getSeconds() == o2.duration.getSeconds()) {
                        return 0;
                    }

                    return 1;
                }
            });

            timers.forEach(timer -> {
                View cardTimer = inflater.inflate(R.layout.card_timer, listLayout, false);

                TimerView timerView = cardTimer.findViewById(R.id.timer);
                // TODO: this seems to be the wrong use of these types
                timerView.setTimer(new Timer(new TimerData(
                        "" + timer.id,
                        "",
                        timer.title,
                        timer.duration,
                        null,
                        null,
                        (timer.tagsString == null) ?
                                Set.of()
                                : Set.of(timer.tagsString.split("#~#"))
                ), new HashSet<>()));

                ((Button)cardTimer.findViewById(R.id.startButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).startTimer(timer.title, timerView.getRemainingDuration(), timerView.getTags());
                    }
                });

                cardTimer.findViewById(R.id.editButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), EditTimerActivity.class);
                        intent.putExtra("timerId", timer.id);
                        getActivity().startActivityForResult(intent, MainActivity.EDIT_TIMER_REQUEST);
                    }
                });

                ((ImageButton)cardTimer.findViewById(R.id.delete)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).deleteTimer(timer.id);
                    }
                });

                listLayout.addView(cardTimer);
            });
        };

        if (tag.equals("ALL")) {
            timerViewModel.getAllTimers().observe(requireActivity(), observer);
        } else {
            timerViewModel.getAllTimersWithTag(tag).observe(requireActivity(), observer);
        }

        return view;
    }
}
