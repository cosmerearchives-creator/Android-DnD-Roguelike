package com.cosmerearchives.dndroguelike;

/** Pixel-space gait projection regressions independent of Android graphics. */
final class RigLegPoseTest {
    private static void check(boolean condition,String message) {
        if(!condition) throw new AssertionError(message);
    }
    private static void finite(float value,String name) {
        check(!Float.isNaN(value)&&!Float.isInfinite(value),name+" must remain finite");
    }
    public static void main(String[] args){
        for(int facing=0;facing<4;facing++){
            for(int index=0;index<2;index++){
                for(int step=0;step<=120;step++){
                    float phase=step/120f;
                    RigLegPose.Pose p=RigLegPose.solve(phase,index,facing,1f,-1.5f);
                    finite(p.hipX,"hipX");finite(p.hipY,"hipY");
                    finite(p.kneeX,"kneeX");finite(p.kneeY,"kneeY");
                    finite(p.footX,"footX");finite(p.footY,"footY");
                    check(p.hipY<=-73f,"hips must be high enough for an upright fighter");
                    check(p.kneeY>p.hipY&&p.kneeY<p.footY,
                        "knee must stay between hip and foot");
                    if(facing==0||facing==2){
                        check(Math.abs(p.footX)<=11f,"front/back foot shouldn't crab-step sideways");
                        check(Math.abs(p.kneeX)<=11f,"front/back knee shouldn't flare sideways");
                    }
                }
                RigLegPose.Pose idle=RigLegPose.solve(0f,index,facing,0f,0f);
                check(Math.abs(idle.kneeX-idle.hipX)<=.01f,
                    "idle leg should stay straight under hip");
                check(Math.abs(idle.hipY)>=73f,"idle hip height");
            }
        }
        // Front/back walking alternates feet along screen depth, not screen X.
        RigLegPose.Pose frontA=RigLegPose.solve(0f,0,0,1f,0f);
        RigLegPose.Pose frontB=RigLegPose.solve(.32f,0,0,1f,0f);
        check(Math.abs(frontA.footY-frontB.footY)>7f,
            "front foot must visibly advance in depth");
        check(Math.abs(frontA.footX-frontB.footX)<2f,
            "front foot must not side-step across screen");
        RigLegPose.Pose backA=RigLegPose.solve(0f,0,2,1f,0f);
        RigLegPose.Pose backB=RigLegPose.solve(.32f,0,2,1f,0f);
        check(Math.abs(backA.footY-backB.footY)>7f,
            "back foot must visibly advance in depth");
        check(Math.abs(backA.footX-backB.footX)<2f,
            "back foot must not side-step across screen");
        // Side-facing foot movement stays horizontal, knees remain high.
        RigLegPose.Pose sideA=RigLegPose.solve(0f,0,1,1f,0f);
        RigLegPose.Pose sideB=RigLegPose.solve(.32f,0,1,1f,0f);
        check(Math.abs(sideA.footX-sideB.footX)>12f,
            "side foot needs substantial stride");
        System.out.println("Rig projections passed: upright stance, narrow frontal gait, depth steps");
    }
}
