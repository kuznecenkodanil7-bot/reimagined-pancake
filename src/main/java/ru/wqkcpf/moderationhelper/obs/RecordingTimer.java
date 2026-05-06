package ru.wqkcpf.moderationhelper.obs;

import java.time.Duration;
import java.time.Instant;

public final class RecordingTimer {
    private static boolean active;
    private static Instant startedAt;

    private RecordingTimer() {}

    public static void start() {
        active = true;
        startedAt = Instant.now();
    }

    public static void stop() {
        active = false;
        startedAt = null;
    }

    public static boolean isActive() {
        return active;
    }

    public static String formatted() {
        if (!active || startedAt == null) return "00:00";
        long seconds = Math.max(0, Duration.between(startedAt, Instant.now()).toSeconds());
        long minutes = seconds / 60;
        long rest = seconds % 60;
        return String.format("%02d:%02d", minutes, rest);
    }
}
