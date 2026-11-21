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
    // Illustration registration is character-specific: NEVER use a global
    // percentage, and never widen the torso or rescale shoulder sprites.
    // A fit shifts each arm as a rigid 2-bone chain by the same X/Y offset.
    // Classic is the approved reference and must remain completely unchanged.
    static float shoulderFitX(String appearance, int facing, int arm) {
        if (isSide(facing)) return 0f;
        boolean rear = facing == 2;
        float left, right;
        switch (appearance) {
            case "classic":
                left = rear ? 0.00f : 0.00f;
                right = rear ? 0.00f : 0.00f;
                break;
            case "bronze":
                left = rear ? -4.25f : -4.35f;
                right = rear ? 4.25f : 4.35f;
                break;
            case "compact":
                left = rear ? -1.85f : -1.95f;
                right = rear ? 1.85f : 1.95f;
                break;
            case "silver":
                left = rear ? -0.85f : -0.75f;
                right = rear ? 0.85f : 0.75f;
                break;
            case "braided":
                left = rear ? -3.30f : -3.45f;
                right = rear ? 3.30f : 3.45f;
                break;
            case "petite":
                left = rear ? -2.90f : -3.05f;
                right = rear ? 2.90f : 3.05f;
                break;
            case "coiled":
                left = rear ? -2.00f : -1.90f;
                right = rear ? 2.00f : 1.90f;
                break;
            case "bob":
                left = rear ? -2.80f : -2.60f;
                right = rear ? 2.80f : 2.60f;
                break;
            case "petite_alt":
                left = rear ? -3.10f : -3.20f;
                right = rear ? 3.10f : 3.20f;
                break;
            default:
                return 0f;
        }
        return arm == 0 ? left : right;
    }

    // The approved Classic Elf artwork has its shoulder origins approximately
    // at -120.7 in BOTH front and rear views. The other eight atlases were
    // authored with roots 2–7 rig units higher, so simply changing X could
    // never fix their visually elevated arm connections.
    static final float REFERENCE_SHOULDER_ROOT_Y = (626f + 47f - 1192f) / 4.3f;

    // Tawny Bob's front is approved. Its rear shoulder seams need only a
    // small upward adjustment; keep both arms level and all other rigs fixed.
    private static final float BOB_REAR_SHOULDER_Y_ADJUST = -1.5f;

    static float shoulderFitY(String appearance, int facing, float sourceRootY) {
        if (isSide(facing) || "classic".equals(appearance)) return 0f;
        float registrationFit = REFERENCE_SHOULDER_ROOT_Y - sourceRootY;
        return "bob".equals(appearance) && facing == 2
            ? registrationFit + BOB_REAR_SHOULDER_Y_ADJUST
            : registrationFit;
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
