package com.cosmerearchives.dndroguelike;

/** Shoulder attachment width must never deform the torso's geometry. */
public final class RigTorsoPoseTest {
    public static void main(String[] args) {
        RigTorsoPose pose = new RigTorsoPose();
        pose.sample(false, 1f, .7f, -.3f, .2f, .1f);
        pose.applyPunch(3f);

        // Torso vertices remain pixel-identical with either shoulder attachment setting.
        RigTorsoPose.Mesh neutral = new RigTorsoPose.Mesh();
        RigTorsoPose.Mesh widened = new RigTorsoPose.Mesh();
        pose.setShoulderRatio(1f);
        neutral.pose(pose, -33f, -128f, 33f, -82f);
        pose.setShoulderRatio(RigViewProfile.WIDENED_SHOULDER_RATIO);
        widened.pose(pose, -33f, -128f, 33f, -82f);
        for (int i = 0; i < neutral.vertices.length; i++) {
            check(neutral.vertices[i], widened.vertices[i]);
        }

        // Also verify rear view, where the artwork is mirrored when painted.
        pose.setShoulderRatio(1f);
        neutral.pose(pose, 33f, -128f, -33f, -82f);
        pose.setShoulderRatio(RigViewProfile.WIDENED_SHOULDER_RATIO);
        widened.pose(pose, 33f, -128f, -33f, -82f);
        for (int i = 0; i < neutral.vertices.length; i++) {
            check(neutral.vertices[i], widened.vertices[i]);
        }
        System.out.println("Shoulder ratio leaves the torso mesh unmodified.");
    }

    private static void check(float actual, float expected) {
        if (Math.abs(actual - expected) > .0001f) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
