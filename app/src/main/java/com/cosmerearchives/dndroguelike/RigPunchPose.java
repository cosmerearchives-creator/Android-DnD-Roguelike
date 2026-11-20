package com.cosmerearchives.dndroguelike;

/** A single unarmed strike projected into the four Wildlands views. */
final class RigPunchPose {
    private static final float[] TIMES = {0f, .30f, .45f, .53f, .66f, .78f, 1f};
    // Elbow and wrist offsets, torso lean, brace, off-hand guard, and shoulder drive.
    private static final float[][] PROFILE = {
        {0,0,0,0,0,0,0,0}, {-12,-11,4,-41,-2,1.4f,.75f,-1},
        {18,-22,37,-42,2.5f,2.4f,.85f,2.9f}, {17,-22,36,-42,2,2,.85f,2.4f},
        {-5,-14,14,-38,.5f,1,.68f,.7f}, {-9,-10,6,-35,-.5f,.6f,.55f,-.4f},
        {0,0,0,0,0,0,0,0}
    };
    private static final float[][] FRONT = {
        {0,0,0,0,0,0,0,0}, {4,-9,-4,-25,1.5f,1.4f,.7f,1},
        {-4,-12,-13,-39,-2,2.2f,.78f,-2.7f}, {-3,-12,-12,-38,-1.7f,1.8f,.78f,-2.3f},
        {2,-10,-8,-31,-.4f,1,.62f,-.7f}, {3,-9,-5,-27,.4f,.6f,.5f,.4f},
        {0,0,0,0,0,0,0,0}
    };
    private static final float[][] REAR = {
        {0,0,0,0,0,0,0,0}, {3,-13,0,-34,1.5f,1.4f,.7f,-1},
        {5,-19,5,-45,-2,2.2f,.78f,2.3f}, {4,-19,4,-44,-1.8f,1.8f,.78f,2},
        {3,-14,3,-37,-.4f,1,.62f,.6f}, {2,-12,1,-32,.4f,.6f,.5f,-.4f},
        {0,0,0,0,0,0,0,0}
    };

    float elbowX, elbowY, wristX, wristY, lean, brace, guard, shoulderDrive, closure, impact;
    boolean reaching;

    void sample(float progress, int facing) {
        float time = Math.max(0f, Math.min(1f, progress));
        float[][] keys = RigViewProfile.isSide(facing) ? PROFILE : facing == 2 ? REAR : FRONT;
        int next = 1;
        while (next < TIMES.length - 1 && time > TIMES[next]) next++;
        float eased = ease(time, TIMES[next - 1], TIMES[next]);
        float[] from = keys[next - 1], to = keys[next];
        elbowX = mix(from[0], to[0], eased);
        elbowY = mix(from[1], to[1], eased);
        wristX = mix(from[2], to[2], eased);
        wristY = mix(from[3], to[3], eased);
        lean = mix(from[4], to[4], eased);
        brace = mix(from[5], to[5], eased);
        guard = mix(from[6], to[6], eased);
        shoulderDrive = mix(from[7], to[7], eased);
        closure = ease(time, .04f, .21f) * (1f - ease(time, .77f, .97f));
        impact = ease(time, .32f, .45f) * (1f - ease(time, .52f, .67f));
        reaching = time >= .32f && time < .72f;
    }

    private static float ease(float value, float start, float end) {
        float t = Math.max(0f, Math.min(1f, (value - start) / (end - start)));
        return t * t * (3f - 2f * t);
    }

    private static float mix(float from, float to, float t) { return from + (to - from) * t; }
}
