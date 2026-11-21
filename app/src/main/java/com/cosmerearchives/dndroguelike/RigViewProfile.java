package com.cosmerearchives.dndroguelike;

/** Body-space proportions and layer ordering for the four snap facings.
 * The two side directions use the same profile, mirrored by the renderer.
 * No in-between turning poses are needed.
 */
final class RigViewProfile {
    static final float WIDENED_SHOULDER_RATIO = 1.08f * 1.10f;
    private RigViewProfile() {}

    static boolean isSide(int facing) { return facing==1 || facing==3; }

    static float torsoHalfWidth(int facing) {
        return isSide(facing) ? 12.5f : 20f;
    }

    static float shoulderX(int facing,int arm) {
        if(isSide(facing)) return arm==0 ? -4.5f : -3.5f;
        return arm==0 ? -16.5f : 16.5f;
    }

    // Shoulder, elbow and wrist all follow a single width ratio.
    // Side profiles retain their existing narrow projection.
    static float shoulderX(int facing,int arm,float ratio) {
        float base = shoulderX(facing, arm);
        return isSide(facing) ? base : base * ratio;
    }

    static float elbowX(int facing,int arm,float ratio) {
        return elbowX(facing, arm) + .40f *
            (shoulderX(facing, arm, ratio) - shoulderX(facing, arm));
    }

    static float handX(int facing,int arm,float ratio) {
        return handX(facing, arm) + .10f *
            (shoulderX(facing, arm, ratio) - shoulderX(facing, arm));
    }

    static float elbowX(int facing,int arm) {
        if(isSide(facing)) return arm==0 ? -6.5f : -5.5f;
        return arm==0 ? -21f : 21f;
    }

    static float elbowY(int facing, int arm) {
        return isSide(facing) && arm == 1 ? -80f : -81f;
    }

    static float handX(int facing,int arm) {
        if(isSide(facing)) return arm==0 ? -6f : -6.5f;
        return arm==0 ? -22f : 23f;
    }

    static float handY(int facing,int arm) {
        if (!isSide(facing)) return -55f;
        boolean far = facing == 1 ? arm == 1 : arm == 0;
        return far ? -57f : -54f;
    }

    static boolean drawSwordArmBehindTorso(int facing) {
        return facing == 1;
    }

    static float sideToFrontShoulderRatio() {
        return Math.abs(shoulderX(1,1)-shoulderX(1,0)) /
               Math.abs(shoulderX(0,1)-shoulderX(0,0));
    }
}
