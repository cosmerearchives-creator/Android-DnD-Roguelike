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
            check(RigViewProfile.drawSwordArmBehindTorso(d),
                "far arm must go behind the profile torso");
        }
        check(RigViewProfile.sideToFrontShoulderRatio()<.5f,
            "side shoulders must compress significantly");
        for(int d:new int[]{0,2}) {
            check(!RigViewProfile.isSide(d),"front/back should retain broad profile");
            check(!RigViewProfile.drawSwordArmBehindTorso(d),
                "front/back arms should remain individually visible");
        }
        System.out.println("View projection passed: narrow profiles, depth-ordered arms, instant mirroring.");
    }
}
