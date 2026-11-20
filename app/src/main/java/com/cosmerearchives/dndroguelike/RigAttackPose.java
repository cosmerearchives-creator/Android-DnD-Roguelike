package com.cosmerearchives.dndroguelike;

/** Angle tracks for the Wildlands attack preview. All poses are in rig space. */
final class RigAttackPose {
    static final float DURATION = .72f;
    private static final float[] TIMES = {0f, .30f, .47f, .66f, 1f};
    // Upper arm rotation, forearm rotation, sword angle, body lean, and downward brace.
    private static final float[][] SIDE = {
        {0f, 0f, 7f, 0f, 0f},
        {47f, 132f, 152f, -7f, 2f},
        {-58f, -76f, 311f, 9f, 4f},
        {-28f, -38f, 331f, 5f, 2f},
        {0f, 0f, 367f, 0f, 0f}
    };
    private static final float[][] FRONT_BACK = {
        {0f, 0f, -12f, 0f, 0f},
        {-44f, -128f, -151f, 7f, 2f},
        {26f, 63f, -292f, -8f, 4f},
        {17f, 35f, -323f, -4f, 2f},
        {0f, 0f, -372f, 0f, 0f}
    };

    float upper, forearm, blade, lean, brace, guard;
    boolean swordInFront;

    void sample(float progress, boolean side) {
        float time = Math.max(0f, Math.min(1f, progress));
        int next = 1;
        while (next < TIMES.length - 1 && time > TIMES[next]) next++;
        float t = (time - TIMES[next - 1]) / (TIMES[next] - TIMES[next - 1]);
        // Draw the slash in a continuous arc, with ease at the changes of direction.
        float eased = t * t * (3f - 2f * t);
        float[] from = (side ? SIDE : FRONT_BACK)[next - 1];
        float[] to = (side ? SIDE : FRONT_BACK)[next];
        upper = mix(from[0], to[0], eased);
        forearm = mix(from[1], to[1], eased);
        blade = mix(from[2], to[2], eased);
        lean = mix(from[3], to[3], eased);
        brace = mix(from[4], to[4], eased);
        guard = brace * 2f;
        swordInFront = time >= .36f && time < .83f;
    }

    private static float mix(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }
}
