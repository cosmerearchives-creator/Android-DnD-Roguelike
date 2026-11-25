package com.cosmerearchives.dndroguelike;

import java.util.ArrayList;
import java.util.List;

final class WorldLighting {
    enum Ambient {
        DARKNESS(Senses.Light.DARK, false, "Darkness"),
        TWILIGHT(Senses.Light.DIM, false, "Dim light"),
        DAYLIGHT(Senses.Light.NORMAL, false, "Normal light"),
        SUNLIGHT(Senses.Light.NORMAL, true, "Direct sunlight"),
        MAGICAL(Senses.Light.MAGICAL_DARK, false, "Magical darkness");

        final Senses.Light intensity;
        final boolean sunlight;
        final String label;

        Ambient(Senses.Light intensity, boolean sunlight, String label) {
            this.intensity = intensity;
            this.sunlight = sunlight;
            this.label = label;
        }

        static Ambient at(long time) {
            long hour = time % WorldClock.DAY / WorldClock.HOUR;
            if (hour < 4 || hour >= 20) return DARKNESS;
            if (hour < 6 || hour >= 18) return TWILIGHT;
            return hour >= 8 && hour < 16 ? SUNLIGHT : DAYLIGHT;
        }
    }

    enum Weather {
        CLEAR(null, false, false);

        /** Null retains the scheduled intensity. */
        final Senses.Light ambientOverride;
        final boolean blocksSun, naturalObscuration;

        Weather(Senses.Light ambientOverride, boolean blocksSun, boolean naturalObscuration) {
            this.ambientOverride = ambientOverride;
            this.blocksSun = blocksSun;
            this.naturalObscuration = naturalObscuration;
        }

        static Weather load(String id) {
            for (Weather value : values()) if (value.name().equals(id)) return value;
            return CLEAR;
        }
    }

    enum Cover { OPEN, CANOPY, ROOF }

    interface Source {
        float x();
        float y();
        Senses.Light illumination(float distanceFeet);
    }

    interface Geometry {
        Cover cover(float x, float y);
        boolean lightBlocked(float ax, float ay, float bx, float by);
        boolean magicalDarkness(float x, float y);
    }

    private final Geometry geometry;
    private final Weather weather;
    private final List<Source> sources = new ArrayList<>();
    private Ambient ambient = Ambient.SUNLIGHT;
    private long revision;

    WorldLighting(Geometry geometry, Weather weather) {
        this.geometry = geometry;
        this.weather = weather;
    }

    boolean outdoorDaylight() { return ambient.intensity == Senses.Light.NORMAL; }

    long revision() { return revision; }
    void invalidate() { revision++; }

    void setAmbient(Ambient value) {
        if (value != ambient) {
            ambient = value;
            invalidate();
        }
    }

    void addSource(Source source) { sources.add(source); invalidate(); }
    void removeSource(Source source) { if (sources.remove(source)) invalidate(); }
    // Moving sources update their coordinates and invalidate after movement.

    Senses.Light illumination(float x, float y) {
        Senses.Light level = weather.ambientOverride == null ? ambient.intensity : weather.ambientOverride;
        Cover cover = geometry.cover(x, y);
        if (cover == Cover.ROOF) level = Senses.Light.DARK;
        else if (cover == Cover.CANOPY && level == Senses.Light.NORMAL) level = Senses.Light.DIM;
        for (Source source : sources) {
            Senses.Light proposed = source.illumination(Senses.distanceFeet(source.x(), source.y(), x, y));
            if (intensity(proposed) > intensity(level) && !geometry.lightBlocked(source.x(), source.y(), x, y))
                level = proposed;
        }
        return ambient == Ambient.MAGICAL || geometry.magicalDarkness(x, y) ? Senses.Light.MAGICAL_DARK : level;
    }

    boolean exposed(float x, float y) {
        return ambient.sunlight && !weather.blocksSun && geometry.cover(x, y) == Cover.OPEN
            && !geometry.magicalDarkness(x, y);
    }

    boolean naturalObscuration() { return weather.naturalObscuration; }

    static boolean canopyContains(float across, float depth, float width, float halfDepth) {
        float a = across / (width * .5f), b = depth / halfDepth;
        return a * a + b * b <= 1;
    }

    private static int intensity(Senses.Light light) {
        return light == Senses.Light.NORMAL ? 2 : light == Senses.Light.DIM ? 1 : 0;
    }
}
