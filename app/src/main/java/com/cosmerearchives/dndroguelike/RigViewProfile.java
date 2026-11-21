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
     * Appearance-specific front/back registration corrections in rig units.
     * All values are relative to each sprite's ORIGINAL painted landmarks.
     * Translation applies equally to shoulder, elbow and wrist: no mesh stretch.
     * Side views, which have separate registered art, stay untouched.
     * BASE mode ignores these corrections; FIT applies them.
     */
    static float shoulderFitX(String appearance, int facing, int arm) {
        if (isSide(facing)) return 0f;
        boolean rear = facing == 2;
        float left, right;
        switch (appearance) {
            case "classic":
                left = rear ? 0.10f : 0.15f;
                right = rear ? -0.10f : -0.15f;
                break;
            case "bronze":
                left = rear ? -2.55f : -2.65f;
                right = rear ? 2.55f : 2.65f;
                break;
            case "compact":
                left = rear ? -0.45f : -0.60f;
                right = rear ? 0.45f : 0.60f;
                break;
            case "silver":
                left = rear ? 0.10f : 0.05f;
                right = rear ? -0.10f : -0.05f;
                break;
            case "braided":
                left = rear ? -2.15f : -2.35f;
                right = rear ? 2.15f : 2.35f;
                break;
            case "petite":
                left = rear ? -1.70f : -1.85f;
                right = rear ? 1.70f : 1.85f;
                break;
            case "coiled":
                left = rear ? -0.50f : -0.45f;
                right = rear ? 0.50f : 0.45f;
                break;
            case "bob":
                left = rear ? -1.35f : -1.25f;
                right = rear ? 1.35f : 1.25f;
                break;
            case "petite_alt":
                left = rear ? -1.90f : -1.95f;
                right = rear ? 1.90f : 1.95f;
                break;
            default:
                return 0f;
        }
        return arm == 0 ? left : right;
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
