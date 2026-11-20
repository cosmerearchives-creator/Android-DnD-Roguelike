package com.cosmerearchives.dndroguelike;

/** Slow weight transfer and breathing offsets for a grounded idle stance. */
final class RigIdlePose {
    static final float PERIOD = 9.6f;

    float hipX, hipY, lean, breath, armDrift;

    void sample(float elapsed, float walkBlend, float attackProgress, boolean side) {
        float phase = (float) (2.0 * Math.PI) * GaitModel.fractional(elapsed / PERIOD);
        float activity = 1f - Math.max(0f, Math.min(1f, walkBlend));
        float attack = Math.max(0f, Math.min(1f, attackProgress));
        // Ease back to idle at the start and end of the swing.
        float transition = Math.min(1f, 8f * Math.min(attack, 1f - attack));
        float strength = activity * activity * (1f - transition * transition * (3f - 2f * transition));

        float transfer = (float) Math.sin(phase);
        breath = strength * (float) Math.sin(3f * phase);
        hipX = strength * transfer * (side ? .75f : 1.45f);
        hipY = strength * transfer * transfer * .42f;
        lean = strength * transfer * (side ? -.35f : -.5f);
        armDrift = strength * (transfer * .6f + (float) Math.sin(3f * phase - .65f) * .23f);
    }
}
