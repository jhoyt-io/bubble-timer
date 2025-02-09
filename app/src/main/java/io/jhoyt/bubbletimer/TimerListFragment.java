package io.jhoyt.bubbletimer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;

import io.jhoyt.bubbletimer.db.TimerViewModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TimerListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TimerListFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public TimerListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TimerListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TimerListFragment newInstance(String param1, String param2) {
        TimerListFragment fragment = new TimerListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_timer_list, container, false);

        LinearLayout listLayout = view.findViewById(R.id.timerList);

        TimerViewModel timerViewModel = new ViewModelProvider(requireActivity()).get(TimerViewModel.class);
        timerViewModel.getAllTimers().observe(requireActivity(), timers -> {
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
                        null
                ), new HashSet<>()));

                ((Button)cardTimer.findViewById(R.id.startButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).startTimer(timer.title, timerView.getRemainingDuration());
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
        });

        return view;
    }
}
