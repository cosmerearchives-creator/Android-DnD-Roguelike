package com.cosmerearchives.dndroguelike;

/** Regression tests for side-view posture, proportions and depth layering. */
final class RigViewProfileTest {
    private static void check(boolean value,String message) {
        if(!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for(int d:new int[]{1,3}) {
            check(RigViewProfile.isSide(d),"side facing "+d);
            check(RigViewProfile.torsoHalfWidth(d)<.7f*RigViewProfile.torsoHalfWidth(0),
                "side torso must have a distinct profile, not a front-facing plate");
            check(Math.abs(RigViewProfile.shoulderX(d,0))<12f,
                "near arm needs to remain close to the narrow side torso");
            check(Math.abs(RigViewProfile.shoulderX(d,1))<12f,
                "far arm needs to remain close to the narrow side torso");
            check(RigViewProfile.drawSwordArmBehindTorso(d) == (d == 1),
                "sword depth must follow the same hand through both profiles");
            check(Math.abs(RigViewProfile.handX(d,0)-RigViewProfile.handX(d,1))<2f,
                "the far hand should tuck behind the near hand at rest");
            RigLegPose.Pose near=RigLegPose.solve(0f,0,d,0f,0f);
            RigLegPose.Pose far=RigLegPose.solve(0f,1,d,0f,0f);
            check(Math.abs(near.footX-far.footX)<1f && near.footY==far.footY,
                "the far boot should tuck behind the near boot at rest");
        }
        check(RigViewProfile.sideToFrontShoulderRatio()<.5f,
            "side shoulders must compress significantly");
        int[] leftRoot = {48,47,42,138,29,227,100,626};
        int[] rightRoot = {72,47,61,136,71,226,340,626};
        for (int facing : new int[]{0,2}) {
            int[][] registrations = {leftRoot, rightRoot};
            for (int arm = 0; arm < registrations.length; arm++) {
                int[] registration = registrations[arm];
                float authoredRootX =
                    (registration[6] + registration[0] - 271f) / 4.3f;
                float fit = RigViewProfile.shoulderFitX(
                    registration, facing, arm, 271f, 1f / 4.3f);
                check(Math.abs(authoredRootX + fit
                    - RigViewProfile.shoulderX(facing, arm)) < .0001f,
                    "front/back root is fitted to its rig anchor");
            }
        }
        for (int facing : new int[]{1,3}) {
            check(RigViewProfile.shoulderFitX(leftRoot, facing, 0, 271f, 1f / 4.3f)==0f,
                "side profile shoulder fit stays unchanged");
        }
        System.out.println("View projection passed: narrow profiles, depth-ordered arms, instant mirroring.");
    }
}
