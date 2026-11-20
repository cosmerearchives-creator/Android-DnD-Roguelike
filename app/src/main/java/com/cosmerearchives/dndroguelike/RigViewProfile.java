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
        if(isSide(facing)) return arm==0 ? -6f : 2f;
        return arm==0 ? -18f : 18f;
    }

    static float elbowX(int facing,int arm) {
        if(isSide(facing)) return arm==0 ? -14f : 13f;
        return arm==0 ? -29f : 29f;
    }

    static float handX(int facing,int arm) {
        if(isSide(facing)) return arm==0 ? -16f : 17f;
        return arm==0 ? -31f : 32f;
    }

    static float handY(int facing,int arm) {
        return isSide(facing) ? (arm==0 ? -54f : -58f) : -55f;
    }

    static boolean drawSwordArmBehindTorso(int facing) {
        return isSide(facing);
    }

    static float sideToFrontShoulderRatio() {
        return Math.abs(shoulderX(1,1)-shoulderX(1,0)) /
               Math.abs(shoulderX(0,1)-shoulderX(0,0));
    }
}
