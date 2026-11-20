package com.cosmerearchives.dndroguelike;

final class RigAttackPoseTest {
    public static void main(String[] args) {
        checkTrack(true);
        checkTrack(false);
        RigPunchPose punch = new RigPunchPose();
        for (int facing = 0; facing < 4; facing++) {
            punch.sample(.28f, facing);
            if (punch.closure != 1f || punch.guard <= 0f ) {
                throw new AssertionError("punch wind-up and guard for facing " + facing);
            }
            float windupWristY = punch.wristY;
            punch.sample(.12f, facing);
            require(punch.closure > 0f && punch.closure < 1f, "hand closes gradually");
            punch.sample(.46f, facing);
            if (punch.wristY >= windupWristY || punch.impact < .95f) {
                throw new AssertionError("punch release for facing " + facing);
            }
            if (RigViewProfile.isSide(facing) && punch.wristX <= punch.elbowX + 15f) {
                throw new AssertionError("profile punch should extend forward");
            }
            if (facing == 2) {
                require(punch.elbowX <= 0f && punch.wristX <= 0f, "rear strike stays inside its shoulder line");
            }
            punch.sample(1f, facing);
            if (punch.closure != 0f  || punch.elbowX != 0f || punch.wristY != 0f
                || punch.lean != 0f || punch.guard != 0f || punch.brace != 0f
                || punch.impact != 0f || punch.shoulderDrive != 0f) {
                throw new AssertionError("punch returns to neutral");
            }
        }
        System.out.println("Attack poses passed: wind-up, swing, recovery and return to idle.");
    }

    private static void checkTrack(boolean side) {
        RigAttackPose pose = new RigAttackPose();
        pose.sample(0f, side);
        float idleBlade = pose.blade;
        pose.sample(.30f, side);
        require(Math.cos(Math.toRadians(pose.blade)) < -.8, "wind-up lifts the blade");
        require(Math.abs(pose.forearm) > 90f, "wind-up bends the elbow");
        float windupBlade = pose.blade;
        pose.sample(.47f, side);
        require(pose.swordInFront && pose.brace > 0f, "strike draws forward while bracing");
        require(Math.abs(pose.blade - windupBlade) < 180f, "swing takes the overhead arc");
        pose.sample(.83f, side);
        require(!pose.swordInFront && pose.brace > 0f, "recovery lowers the arm");
        pose.sample(1f, side);
        require(pose.upper == 0f && pose.forearm == 0f && pose.lean == 0f && pose.brace == 0f,
            "reset to the idle pose");
        require((pose.blade - idleBlade) % 360f == 0f, "blade returns to idle angle");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
