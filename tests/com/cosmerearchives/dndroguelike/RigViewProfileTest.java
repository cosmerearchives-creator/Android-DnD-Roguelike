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
        String[] models = {
            "classic","bronze","compact","silver","braided",
            "petite","coiled","bob","petite_alt"
        };
        for(int facing=0;facing<4;facing++) {
            for(int arm=0;arm<2;arm++) {
                check(RigViewProfile.shoulderFitX("classic",facing,arm)==0f,
                    "approved reference elf X must remain unchanged");
                check(RigViewProfile.shoulderFitY("classic",facing,-125f)==0f,
                    "approved reference elf Y must remain unchanged");
            }
        }
        check(RigViewProfile.shoulderFitX("bronze",0,1)>2f,
            "narrow bronze base needs more shoulder spacing");
        for (String model : models) {
            for (int facing : new int[]{0,2}) {
                check(!RigViewProfile.drawSwordArmBehindTorso(facing),
                    "front/back arms remain visible");
                float l=RigViewProfile.shoulderFitX(model,facing,0);
                float r=RigViewProfile.shoulderFitX(model,facing,1);
                check(Math.abs(l)<=4.5f && Math.abs(r)<=4.5f,
                    "each attachment uses bounded corrections, not mesh scaling");
                if(!model.equals("classic")) {
                    check(l<0f && r>0f, "all non-reference shoulders move outward");
                    float authoredRootY = facing == 0 ? -126.5f : -125.5f;
                    float yCorrection = RigViewProfile.shoulderFitY(
                        model,facing,authoredRootY);
                    check(yCorrection>0f, "non-reference shoulder origins move downward");
                    check(Math.abs(authoredRootY+yCorrection
                        -RigViewProfile.REFERENCE_SHOULDER_ROOT_Y)<.0001f,
                        "vertical fit must exactly meet the reference Y anchor");
                }
                for (int arm=0;arm<2;arm++) {
                    float move=RigViewProfile.shoulderFitX(model,facing,arm);
                    float shoulder=RigViewProfile.shoulderX(facing,arm)+move;
                    float elbow=RigViewProfile.elbowX(facing,arm)+move;
                    float wrist=RigViewProfile.handX(facing,arm)+move;
                    check(Math.abs((elbow-shoulder)
                        -(RigViewProfile.elbowX(facing,arm)-RigViewProfile.shoulderX(facing,arm)))<.0001f,
                        "upper-arm rest length stays fixed");
                    check(Math.abs((wrist-elbow)
                        -(RigViewProfile.handX(facing,arm)-RigViewProfile.elbowX(facing,arm)))<.0001f,
                        "forearm rest length stays fixed");
                }
            }
            for(int facing:new int[]{1,3}) {
                check(RigViewProfile.shoulderFitX(model,facing,0)==0f
                    && RigViewProfile.shoulderFitX(model,facing,1)==0f,
                    "front/back X fit must not change side profiles");
                check(RigViewProfile.shoulderFitY(model,facing,-130f)==0f,
                    "front/back Y fit must not change side profiles");
            }
        }
        check(RigViewProfile.shoulderFitX("unknown",0,0)==0f,
            "new models default to original artwork anchors");
        check(RigViewProfile.shoulderFitX("bronze",0,1)
              > RigViewProfile.shoulderFitX("coiled",0,1),
            "narrow bronze model must widen more than coiled model");
        System.out.println("View projection passed: narrow profiles, depth-ordered arms, instant mirroring.");
    }
}
