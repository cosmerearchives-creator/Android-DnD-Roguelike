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
        for(int d:new int[]{0,2}) {
            float baseWidth = RigViewProfile.shoulderX(d,1) - RigViewProfile.shoulderX(d,0);
            float wideWidth = RigViewProfile.shoulderX(d,1,RigViewProfile.WIDENED_SHOULDER_RATIO)
                - RigViewProfile.shoulderX(d,0,RigViewProfile.WIDENED_SHOULDER_RATIO);
            check(Math.abs(wideWidth/baseWidth-RigViewProfile.WIDENED_SHOULDER_RATIO)<.0001f,
                "front/back shoulder joints must actually widen 18.8%");
            check(RigViewProfile.elbowX(d,0,RigViewProfile.WIDENED_SHOULDER_RATIO)
                < RigViewProfile.elbowX(d,0), "elbow must follow shoulder outward");
            check(RigViewProfile.handX(d,1,RigViewProfile.WIDENED_SHOULDER_RATIO)
                > RigViewProfile.handX(d,1), "wrist must follow shoulder outward");
            check(!RigViewProfile.isSide(d),"front/back should retain broad profile");
            check(!RigViewProfile.drawSwordArmBehindTorso(d),
                "front/back arms should remain individually visible");
        }
        System.out.println("View projection passed: narrow profiles, depth-ordered arms, instant mirroring.");
    }
}
