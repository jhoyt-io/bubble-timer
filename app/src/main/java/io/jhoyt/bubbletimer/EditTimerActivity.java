package io.jhoyt.bubbletimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.stream.Collectors;

import io.jhoyt.bubbletimer.db.TagViewModel;
import io.jhoyt.bubbletimer.db.Timer;
import io.jhoyt.bubbletimer.db.TimerViewModel;

public class EditTimerActivity extends AppCompatActivity {
    private String durationString = "";
    InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_timer);

        this.inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);

        final EditText title = findViewById(R.id.editTextTimerName);
        title.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(view);
            }
        });

        final EditText duration = findViewById(R.id.editTextTimerDuration);
        duration.setOnClickListener((view) -> {
            hideKeyboard(view);
        });

        final AutoCompleteTextView tagsView = findViewById(R.id.editTextTimerTags);
        tagsView.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(view);
            } else {
                tagsView.showDropDown();
            }
        });

        TagViewModel tagViewModel = new ViewModelProvider(this).get(TagViewModel.class);
        tagViewModel.getAllTags().observe(this, tags -> {
            List<String> tagList = tags.stream().map(tag -> tag.name).collect(Collectors.toList());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line,
                    tagList);
            tagsView.setAdapter(adapter);
        });

        // Get the timer data for the passed timer id, fills in elements
        int id = getIntent().getIntExtra("timerId", -1);

        if (id != -1) {
            TimerViewModel timerViewModel = new ViewModelProvider(this).get(TimerViewModel.class);
            final LiveData<Timer> timerLiveData = timerViewModel.getById(id);
            timerLiveData.observe(this, timer -> {
                title.setText(timer.title);

                this.durationString = "" + timer.duration.toHours()
                        + timer.duration.toMinutes() % 60
                        + timer.duration.toSeconds() % 60;
                duration.setText(getDisplayDuration());

                tagsView.setText(timer.tagsString);
            });
        }

        // Activity action buttons
        final Button createAndStart = findViewById(R.id.saveButton);
        createAndStart.setOnClickListener((view) -> {
            Intent editTimerIntent = new Intent();

            editTimerIntent.putExtra("timerId", id);
            editTimerIntent.putExtra("timerTitle", title.getText().toString());
            editTimerIntent.putExtra("timerDuration", duration.getText().toString());

            // TODO if we allow multiple tags need to revisit
            String tags = tagsView.getText().toString();
            if (!tags.isEmpty()) {
                editTimerIntent.putExtra("tagsString", tags);
            }

            setResult(RESULT_OK, editTimerIntent);

            finish();
        });

        final ImageButton cancel = findViewById(R.id.cancelButton);
        cancel.setOnClickListener((view) -> {
            Intent newTimerIntent = new Intent();
            setResult(RESULT_CANCELED, newTimerIntent);
            finish();
        });

        // Big keypad buttons

        final ImageButton button1 = findViewById(R.id.imageButton1);
        button1.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "1";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button2 = findViewById(R.id.imageButton2);
        button2.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "2";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button3 = findViewById(R.id.imageButton3);
        button3.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "3";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button4 = findViewById(R.id.imageButton4);
        button4.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "4";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button5 = findViewById(R.id.imageButton5);
        button5.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "5";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button6 = findViewById(R.id.imageButton6);
        button6.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "6";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button7 = findViewById(R.id.imageButton7);
        button7.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "7";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button8 = findViewById(R.id.imageButton8);
        button8.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "8";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button9 = findViewById(R.id.imageButton9);
        button9.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "9";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button00 = findViewById(R.id.imageButton00);
        button00.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "00";
            duration.setText(getDisplayDuration());
        });
        final ImageButton button0 = findViewById(R.id.imageButton0);
        button0.setOnClickListener((view) -> {
            title.clearFocus();
            this.durationString = this.durationString + "0";
            duration.setText(getDisplayDuration());
        });
        final ImageButton buttonDelete = findViewById(R.id.imageButtonDelete);
        buttonDelete.setOnClickListener((view) -> {
            title.clearFocus();
            int length = this.durationString.length();
            if (length > 0) {
                this.durationString = this.durationString.substring(0, length - 1);
            }

            duration.setText(getDisplayDuration());
        });
    }

    private void hideKeyboard(View view) {
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private String getDisplayDuration() {
        String result = this.durationString;
        if (result.isEmpty()) {
            return null;
        }

        String hours = "0";
        String minutes = "00";
        String seconds;
        if (result.length() > 4) {
            hours = result.substring(0, result.length() - 4);
            minutes = result.substring(result.length() - 4, result.length() - 2);
            seconds = result.substring(result.length() - 2);
        } else if (result.length() > 2) {
            minutes = ((result.length() == 3) ? "0" : "") +
                    result.substring(0, result.length() - 2);
            seconds = result.substring(result.length() - 2);
        } else {
            seconds = ((result.length() == 1) ? "0" : "") + result;
        }

        return hours + ":" + minutes + ":" + seconds;
    }
}
