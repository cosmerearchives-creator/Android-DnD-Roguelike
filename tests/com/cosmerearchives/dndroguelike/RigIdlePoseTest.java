package com.cosmerearchives.dndroguelike;

final class RigIdlePoseTest {
    public static void main(String[] args) {
        RigIdlePose idle = new RigIdlePose();
        idle.sample(RigIdlePose.PERIOD / 4f, 0f, 0f, false);
        check(idle.hipX > 1f && idle.lean < 0f && idle.spineScale < .96f, "weight and breath");
        float shift = idle.hipX;
        float breath = idle.spineScale;
        idle.sample(1.25f * RigIdlePose.PERIOD, 0f, 0f, false);
        check(Math.abs(idle.hipX - shift) < .001f && Math.abs(idle.spineScale - breath) < .001f,
            "repeatable loop");
        idle.sample(RigIdlePose.PERIOD / 4f, .5f, 0f, false);
        check(Math.abs(idle.hipX - shift * .25f) < .001f, "fade into walking");
        idle.sample(RigIdlePose.PERIOD / 4f, 1f, 0f, false);
        neutral(idle);
        idle.sample(RigIdlePose.PERIOD / 4f, 0f, .45f, true);
        neutral(idle);
        idle.sample(RigIdlePose.PERIOD / 4f, 0f, 1f, true);
        check(idle.hipX > 0f && idle.hipX < shift, "recover with a smaller profile shift");

        for (int frame = 0; frame <= 96; frame++) {
            for (int facing = 0; facing < 4; facing++) {
                boolean side = RigViewProfile.isSide(facing);
                idle.sample(frame * .1f, 0f, 0f, side);
                for (int leg = 0; leg < 2; leg++) {
                    RigLegPose.Pose rest = side ? RigLegPose.solveProfile(0f, leg, 0f, 0f)
                        : RigLegPose.solve(0f, leg, facing, 0f, 0f);
                    RigLegPose.Pose pose = side ? RigLegPose.solveProfile(0f, leg, 0f, idle.hipY)
                        : RigLegPose.solve(0f, leg, facing, 0f, idle.hipY);
                    check(rest.footX == pose.footX && rest.footY == pose.footY, "planted feet");
                }
            }
        }
        System.out.println("Idle poses passed: weight transfer, looping, transitions, and planted feet.");
    }

    private static void neutral(RigIdlePose idle) {
        check(idle.hipX == 0f && idle.hipY == 0f && idle.lean == 0f
            && idle.spineScale == 1f && idle.armDrift == 0f, "idle must yield to active movement");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
