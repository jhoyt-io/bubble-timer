package io.jhoyt.bubbletimer;

import androidx.annotation.NonNull;

import java.time.Duration;

public class DurationUtil {
    static @NonNull String getFormattedDuration(Duration remaining) {
        long remainingSeconds = remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);

        String text;
        long absRemainingSeconds = Math.abs(remainingSeconds);
        if (remainingSeconds >= 0) {
            text = "";
        } else {
            text = "-";
        }

        if (absRemainingSeconds >= 3600) {
            text = text + String.format("%d:%02d:%02d", (absRemainingSeconds % 216000) / 3600, (absRemainingSeconds % 3600) / 60, (absRemainingSeconds % 60));
        } else if (absRemainingSeconds >= 60) {
            text = text + String.format("%d:%02d", (absRemainingSeconds % 3600) / 60, (absRemainingSeconds % 60));
        } else if (absRemainingSeconds >= 0) {
            text = text + String.format("%d", (absRemainingSeconds % 60));
        }
        return text;
    }
}
