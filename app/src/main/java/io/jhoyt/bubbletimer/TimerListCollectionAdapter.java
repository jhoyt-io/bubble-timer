package io.jhoyt.bubbletimer;

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
        return TimerListFragment.newInstance(userId, tags.get(position));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }
}
