package io.jhoyt.bubbletimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class NewTimerActivity extends AppCompatActivity {
    private String durationString = "";
    InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_timer);

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

        final Button create = findViewById(R.id.createButton);
        create.setOnClickListener((view) -> {
            Intent newTimerIntent = new Intent();

            if (title != null && duration != null) {
                newTimerIntent.putExtra("timerTitle", title.getText().toString());
                newTimerIntent.putExtra("timerDuration", duration.getText().toString());
                newTimerIntent.putExtra("startTimerNow", false);

                setResult(RESULT_OK, newTimerIntent);
            } else {
                setResult(RESULT_CANCELED, newTimerIntent);
            }
            finish();
        });

        final Button createAndStart = findViewById(R.id.createAndStartButton);
        createAndStart.setOnClickListener((view) -> {
            Intent newTimerIntent = new Intent();

            if (title != null && duration != null) {
                newTimerIntent.putExtra("timerTitle", title.getText().toString());
                newTimerIntent.putExtra("timerDuration", duration.getText().toString());
                newTimerIntent.putExtra("startTimerNow", true);

                setResult(RESULT_OK, newTimerIntent);
            } else {
                setResult(RESULT_CANCELED, newTimerIntent);
            }
            finish();
        });

        final ImageButton cancel = findViewById(R.id.cancelButton);
        cancel.setOnClickListener((view) -> {
            Intent newTimerIntent = new Intent();
            setResult(RESULT_CANCELED, newTimerIntent);
            finish();
        });

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
