package com.cosmerearchives.dndroguelike;

public final class RigTorsoPoseTest {
    public static void main(String[] args) {
        RigTorsoPose pose = new RigTorsoPose();
        check(pose.shoulderSpread(-20f, -128f), -1.6f);
        check(pose.shoulderSpread(20f, -128f), 1.6f);
        check(pose.shoulderSpread(20f, RigTorsoPose.WAIST_Y), 0f);
        check(pose.shoulderSpread(20f, -150f), 0f);

        pose.sample(false, 1f, .7f, -.3f, .2f, .1f);
        pose.applyPunch(3f);
        for (float mirror : new float[]{1f, -1f}) {
            RigTorsoPose.Mesh torso = new RigTorsoPose.Mesh();
            torso.pose(pose, 20f * mirror, -128f, 24f * mirror, -82f);
            float[] arm = new float[8];
            pose.bindShoulder(arm, 20f, -128f, 1f, 1f, 1, 1,
                20f, -128f, mirror, 0f, 0f, new float[2]);
            // The active walk pose adds its standard half-unit seam overlap.
            check(arm[0], torso.vertices[0] * mirror - .5f);
            check(arm[1], torso.vertices[1]);
        }

        pose.sample(true, 1f, .7f, -.3f, .2f, .1f);
        check(pose.shoulderSpread(20f, -128f), 0f);
    }

    private static void check(float actual, float expected) {
        if (Math.abs(actual - expected) > .0001f) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
