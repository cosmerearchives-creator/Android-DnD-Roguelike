package com.cosmerearchives.dndroguelike;

final class RigIdlePoseTest {
    public static void main(String[] args) {
        RigIdlePose idle = new RigIdlePose();
        idle.sample(RigIdlePose.PERIOD / 4f, 0f, 0f, false);
        check(idle.hipX > 1f && idle.lean < 0f && idle.breath < -.96f, "weight and breath");
        float shift = idle.hipX;
        float breath = idle.breath;
        idle.sample(1.25f * RigIdlePose.PERIOD, 0f, 0f, false);
        check(Math.abs(idle.hipX - shift) < .001f && Math.abs(idle.breath - breath) < .001f,
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
        RigTorsoPose torso = new RigTorsoPose();
        float[] points = new float[4];
        torso.sample(true, 0f, 0f, 0f, 1f, 0f);
        torso.offsets(12f, RigTorsoPose.CHEST_Y, points, 0);
        torso.offsets(12f, RigTorsoPose.WAIST_Y, points, 2);
        check(points[0] > 1f && points[0] < 1.6f && points[1] < -.5f, "side chest expands and rises");
        check(points[2] == 0f && points[3] == 0f, "waist stays attached");
        torso.offsets(-8f, -136f, points, 0);
        torso.offsets(8f, -151f, points, 2);
        check(points[0] == points[2] && points[1] == points[3], "head moves as a rigid shape");
        torso.sample(false, 1f, 1f, 0f, 0f, 0f);
        torso.offsets(-16f, RigTorsoPose.CHEST_Y, points, 0);
        torso.offsets(16f, RigTorsoPose.CHEST_Y, points, 2);
        check(points[1] < -.75f && points[3] > .75f, "shoulders counter-rotate during walking");
        // The armhole-fitting mesh was removed: idle motion still moves the
        // torso, but shoulder-width changes must not alter any torso vertex.
        RigTorsoPose.Mesh mesh = new RigTorsoPose.Mesh();
        torso.setShoulderFitEnabled(false);
        mesh.pose(torso, -18f, -110f, 22f, -82f);
        float[] original = mesh.vertices.clone();
        torso.setShoulderFitEnabled(true);
        mesh.pose(torso, -18f, -110f, 22f, -82f);
        for (int i = 0; i < original.length; i++) {
            check(Math.abs(mesh.vertices[i] - original[i]) < .001f,
                "idle torso geometry remains fixed when shoulder anchors move");
        }
        // Separate illustrated characters need different front/rear offsets.
        for (int facing : new int[]{0,2}) {
            float classicLeft=RigViewProfile.shoulderFitX("classic",facing,0);
            float classicRight=RigViewProfile.shoulderFitX("classic",facing,1);
            float bronzeLeft=RigViewProfile.shoulderFitX("bronze",facing,0);
            float bronzeRight=RigViewProfile.shoulderFitX("bronze",facing,1);
            check(classicLeft>0f && classicRight<0f,
                "broad classic artwork tucks inward");
            check(bronzeLeft<0f && bronzeRight>0f,
                "narrow bronze artwork gets wider connections");
        }
        System.out.println("Idle poses passed: weight transfer, looping, transitions, and planted feet.");
    }

    private static void neutral(RigIdlePose idle) {
        check(idle.hipX == 0f && idle.hipY == 0f && idle.lean == 0f
            && idle.breath == 0f && idle.armDrift == 0f, "idle must yield to active movement");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
