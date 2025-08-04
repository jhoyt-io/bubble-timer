package io.jhoyt.bubbletimer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.List;

import io.jhoyt.bubbletimer.db.SharedTimer;

public class SharedTimerAdapter extends RecyclerView.Adapter<SharedTimerAdapter.ViewHolder> {
    private List<SharedTimer> sharedTimers;
    private final OnTimerActionListener actionListener;

    public interface OnTimerActionListener {
        void onAcceptTimer(String timerId);
        void onRejectTimer(String timerId);
    }

    public SharedTimerAdapter(List<SharedTimer> sharedTimers, OnTimerActionListener actionListener) {
        this.sharedTimers = sharedTimers;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shared_timer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SharedTimer sharedTimer = sharedTimers.get(position);
        holder.bind(sharedTimer);
    }

    @Override
    public int getItemCount() {
        return sharedTimers.size();
    }

    public void updateSharedTimers(List<SharedTimer> newSharedTimers) {
        this.sharedTimers = newSharedTimers;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView timerName;
        private final TextView sharedBy;
        private final TextView timerDuration;
        private final Button acceptButton;
        private final Button rejectButton;

        ViewHolder(View itemView) {
            super(itemView);
            timerName = itemView.findViewById(R.id.timerName);
            sharedBy = itemView.findViewById(R.id.sharedBy);
            timerDuration = itemView.findViewById(R.id.timerDuration);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }

        void bind(SharedTimer sharedTimer) {
            timerName.setText(sharedTimer.name);
            sharedBy.setText("Shared by: " + sharedTimer.sharedBy);
            
            if (sharedTimer.totalDuration != null) {
                timerDuration.setText("Duration: " + DurationUtil.getFormattedDuration(sharedTimer.totalDuration));
            } else {
                timerDuration.setText("Duration: Unknown");
            }

            acceptButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAcceptTimer(sharedTimer.timerId);
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRejectTimer(sharedTimer.timerId);
                }
            });
        }
    }
} 