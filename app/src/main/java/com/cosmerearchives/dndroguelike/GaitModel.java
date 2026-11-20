package com.cosmerearchives.dndroguelike;

/**
 * Distance-driven two-foot gait. This class deliberately has no Android dependencies
 * so its foot planting and swing timing can be verified in a plain JVM test.
 *
 * A whole gait cycle is one left + one right step. Each foot spends 64% of its
 * cycle on the ground (stance) and 36% advancing through the air (swing).
 * The foot's stance offset moves backward relative to the advancing pelvis;
 * with the stride calibrated to character speed this holds its world position.
 */
final class GaitModel {
    static final float CYCLE_TILES = 1.20f;
    static final float STANCE = .64f;
    private float cycles;

    static final class Foot {
        /** -0.5 behind the hip, +0.5 ahead of the hip, in stride units. */
        final float along;
        /** 0 on ground, 1 at highest point of swing. */
        final float lift;
        final boolean planted;
        Foot(float along, float lift, boolean planted) {
            this.along=along;
            this.lift=lift;
            this.planted=planted;
        }
    }

    void advance(float distanceInTiles) {
        if (distanceInTiles <= 0 || Float.isNaN(distanceInTiles) ||
                Float.isInfinite(distanceInTiles)) return;
        cycles += distanceInTiles / CYCLE_TILES;
        if (cycles > 4096f) cycles -= 4096f;
    }
    void reset() { cycles = 0f; }
    float cycles() { return cycles; }

    static Foot sample(float cycle, int leg) {
        float p = fractional(cycle + (leg == 0 ? 0f : .5f));
        if (p < STANCE) {
            // Stance: no vertical lift; the ground-contact foot travels backward
            // in pelvis space as the character moves forward in world space.
            return new Foot(.5f - p / STANCE, 0f, true);
        }
        // Swing: knee bends, foot lifts and returns ahead for the next contact.
        float t = (p - STANCE) / (1f - STANCE);
        float eased = t * t * (3f - 2f*t);
        return new Foot(-.5f + eased,
                (float)Math.sin(Math.PI*t), false);
    }

    static float fractional(float value) {
        return value - (float)Math.floor(value);
    }
}