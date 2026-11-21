package com.cosmerearchives.dndroguelike;

/** Body-space proportions and layer ordering for the four snap facings.
 * The two side directions use the same profile, mirrored by the renderer.
 * No in-between turning poses are needed.
 */
final class RigViewProfile {
    private RigViewProfile() {}

    static boolean isSide(int facing) { return facing==1 || facing==3; }

    static float torsoHalfWidth(int facing) {
        return isSide(facing) ? 12.5f : 20f;
    }

    static float shoulderX(int facing,int arm) {
        if(isSide(facing)) return arm==0 ? -4.5f : -3.5f;
        return arm==0 ? -16.5f : 16.5f;
    }

    /**
     * Align an arm's authored shoulder root with its rig attachment point.
     * Registration stores each limb cell's atlas origin and local shoulder root.
     */
    static float shoulderFitX(int[] registration, int facing, int arm,
                              float centerX, float unit) {
        if (isSide(facing)) return 0f;
        float authoredRootX = (registration[6] + registration[0] - centerX) * unit;
        return shoulderX(facing, arm) - authoredRootX;
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
