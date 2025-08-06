package io.jhoyt.bubbletimer;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class TimerListCollectionAdapter extends FragmentStateAdapter {
    private final String userId;
    private final List<String> tags;

    public TimerListCollectionAdapter(@NonNull FragmentActivity fragmentActivity, String userId, List<String> tags) {
        super(fragmentActivity);
        this.userId = userId;
        this.tags = List.copyOf(tags);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d("TimerListCollectionAdapter", "Creating fragment for position " + position + " with tag: " + tags.get(position));
        
        // Check if this is the "Shared Timers" tab
        if (position == 1 && tags.get(position).equals("SHARED")) {
            return new SharedTimerListFragment();
        }
        
        return TimerListFragment.newInstance(userId, tags.get(position));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }
}
