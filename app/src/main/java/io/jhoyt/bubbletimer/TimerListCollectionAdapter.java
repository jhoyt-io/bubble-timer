package io.jhoyt.bubbletimer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TimerListCollectionAdapter extends FragmentStateAdapter {
    public static final String[] tags = new String[] {
            "ALL", "Hello", "World", "Tags", "Tabs"
    };

    public TimerListCollectionAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return TimerListFragment.newInstance(tags[position]);
    }

    @Override
    public int getItemCount() {
        return tags.length;
    }
}
