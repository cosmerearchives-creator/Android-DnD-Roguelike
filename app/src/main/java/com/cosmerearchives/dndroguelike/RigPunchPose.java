package com.cosmerearchives.dndroguelike;

/** A single unarmed strike projected into the four Wildlands views. */
final class RigPunchPose {
    private static final float[] TIMES = {0f, .28f, .46f, .54f, .72f, 1f};
    // Elbow and wrist offsets, torso lean, brace, and off-hand guard.
    private static final float[][] PROFILE = {
        {0,0,0,0,0,0,0}, {-10,-9,5,-40,-3,1,.85f},
        {20,-22,39,-43,4,2,1}, {18,-22,37,-43,4,2,1},
        {-5,-15,10,-37,1,1,.6f}, {0,0,0,0,0,0,0}
    };
    private static final float[][] FRONT = {
        {0,0,0,0,0,0,0}, {3,-10,-5,-26,2,1,.85f},
        {-4,-16,-9,-33,-3,2,1}, {-4,-15,-9,-32,-3,2,1},
        {1,-12,-7,-29,-1,1,.6f}, {0,0,0,0,0,0,0}
    };
    private static final float[][] REAR = {
        {0,0,0,0,0,0,0}, {4,-14,1,-39,2,1,.85f},
        {4,-22,3,-48,-3,2,1}, {4,-21,2,-47,-3,2,1},
        {2,-14,2,-34,-1,1,.6f}, {0,0,0,0,0,0,0}
    };

    float elbowX, elbowY, wristX, wristY, lean, brace, guard;
    boolean fist, reaching;

    void sample(float progress, int facing) {
        float time = Math.max(0f, Math.min(1f, progress));
        float[][] keys = RigViewProfile.isSide(facing) ? PROFILE : facing == 2 ? REAR : FRONT;
        int next = 1;
        while (next < TIMES.length - 1 && time > TIMES[next]) next++;
        float t = (time - TIMES[next - 1]) / (TIMES[next] - TIMES[next - 1]);
        float eased = t * t * (3f - 2f * t);
        float[] from = keys[next - 1], to = keys[next];
        elbowX = mix(from[0], to[0], eased);
        elbowY = mix(from[1], to[1], eased);
        wristX = mix(from[2], to[2], eased);
        wristY = mix(from[3], to[3], eased);
        lean = mix(from[4], to[4], eased);
        brace = mix(from[5], to[5], eased);
        guard = mix(from[6], to[6], eased);
        fist = time >= .11f && time < .86f;
        reaching = time >= .32f && time < .68f;
    }

    private static float mix(float from, float to, float t) { return from + (to - from) * t; }
}
