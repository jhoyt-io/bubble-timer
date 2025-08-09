package io.jhoyt.bubbletimer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.jhoyt.bubbletimer.R;
import io.jhoyt.bubbletimer.domain.entities.Timer;
import io.jhoyt.bubbletimer.domain.entities.TimerState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adapter for displaying timers in a RecyclerView.
 * This adapter works with domain entities and provides callbacks for timer actions.
 */
public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.TimerViewHolder> {
    
    private List<Timer> timers = new ArrayList<>();
    private final Consumer<Timer> onPauseCallback;
    private final Consumer<Timer> onResumeCallback;
    private final Consumer<Timer> onStopCallback;
    private final Consumer<Timer> onDeleteCallback;
    
    public TimerAdapter(
            Consumer<Timer> onPauseCallback,
            Consumer<Timer> onResumeCallback,
            Consumer<Timer> onStopCallback,
            Consumer<Timer> onDeleteCallback) {
        this.onPauseCallback = onPauseCallback;
        this.onResumeCallback = onResumeCallback;
        this.onStopCallback = onStopCallback;
        this.onDeleteCallback = onDeleteCallback;
    }
    
    @NonNull
    @Override
    public TimerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timer_new, parent, false);
        return new TimerViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TimerViewHolder holder, int position) {
        Timer timer = timers.get(position);
        holder.bind(timer);
    }
    
    @Override
    public int getItemCount() {
        return timers.size();
    }
    
    public void updateTimers(List<Timer> newTimers) {
        this.timers = newTimers != null ? newTimers : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    class TimerViewHolder extends RecyclerView.ViewHolder {
        private final TextView timerNameText;
        private final TextView timerDurationText;
        private final TextView timerStateText;
        private final Button pauseButton;
        private final Button resumeButton;
        private final Button stopButton;
        private final Button deleteButton;
        
        public TimerViewHolder(@NonNull View itemView) {
            super(itemView);
            timerNameText = itemView.findViewById(R.id.timer_name_text);
            timerDurationText = itemView.findViewById(R.id.timer_duration_text);
            timerStateText = itemView.findViewById(R.id.timer_state_text);
            pauseButton = itemView.findViewById(R.id.pause_button);
            resumeButton = itemView.findViewById(R.id.resume_button);
            stopButton = itemView.findViewById(R.id.stop_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
        
        public void bind(Timer timer) {
            timerNameText.setText(timer.getName());
            timerDurationText.setText("Duration: " + timer.getTotalDuration().toMinutes() + " minutes");
            timerStateText.setText("State: " + timer.getState());
            
            // Set up button visibility and click listeners based on timer state
            setupButtons(timer);
        }
        
        private void setupButtons(Timer timer) {
            // Hide all buttons initially
            pauseButton.setVisibility(View.GONE);
            resumeButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            
            // Show appropriate buttons based on timer state
            switch (timer.getState()) {
                case RUNNING:
                    pauseButton.setVisibility(View.VISIBLE);
                    stopButton.setVisibility(View.VISIBLE);
                    
                    pauseButton.setOnClickListener(v -> onPauseCallback.accept(timer));
                    stopButton.setOnClickListener(v -> onStopCallback.accept(timer));
                    break;
                    
                case PAUSED:
                    resumeButton.setVisibility(View.VISIBLE);
                    stopButton.setVisibility(View.VISIBLE);
                    
                    resumeButton.setOnClickListener(v -> onResumeCallback.accept(timer));
                    stopButton.setOnClickListener(v -> onStopCallback.accept(timer));
                    break;
                    
                case EXPIRED:
                case STOPPED:
                    deleteButton.setVisibility(View.VISIBLE);
                    
                    deleteButton.setOnClickListener(v -> onDeleteCallback.accept(timer));
                    break;
            }
        }
    }
}
