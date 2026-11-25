package com.cosmerearchives.dndroguelike;

import java.util.Locale;

/** Elapsed world time, driven by monotonic time only while gameplay owns focus. */
final class WorldClock {
    static final long SECOND = 1_000_000_000L;
    static final long HOUR = 3600 * SECOND;
    static final long DAY = 24 * HOUR;
    static final long START = 8 * HOUR;

    private long elapsed;
    private long anchor;
    private boolean running;

    WorldClock(long saved) {
        elapsed = Math.max(0, saved);
    }

    long time(long now) {
        return elapsed + (running ? Math.max(0, now - anchor) : 0);
    }

    void setRunning(boolean active, long now) {
        if (active == running) return;
        elapsed = time(now);
        anchor = now;
        running = active;
    }

    void waitUntil(int hour, long now) {
        elapsed = nextHour(time(now), hour);
        anchor = now;
    }

    static long nextHour(long time, int hour) {
        if (hour < 0 || hour >= 24) throw new IllegalArgumentException("Hour outside day");
        long candidate = day(time) * DAY + hour * HOUR;
        return candidate > time ? candidate : candidate + DAY;
    }

    static long day(long time) { return time / DAY; }

    static String format(long time, boolean seconds) {
        int hours = (int)(time % DAY / HOUR);
        int minutes = (int)(time / (60 * SECOND) % 60);
        String label = (hours % 12 == 0 ? 12 : hours % 12) + ":" + twoDigits(minutes);
        if (seconds) label += ":" + twoDigits(time / SECOND % 60);
        return label + (hours < 12 ? " AM" : " PM");
    }

    static String duration(long interval) {
        long seconds = (interval + SECOND - 1) / SECOND;
        return String.format(Locale.US, "%dh %02dm %02ds", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    private static String twoDigits(long value) { return value < 10 ? "0" + value : Long.toString(value); }
}
