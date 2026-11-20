package com.cosmerearchives.dndroguelike;

/**
 * World-distance-driven gait projected into the four existing screen facings.
 *
 * The skeleton bends in the sagittal (forward/depth) plane. Projecting that
 * bend into SCREEN X for front/back is what caused the crab-legged look in
 * v0.3.2. Front/back depth must project into SCREEN Y instead.
 *
 * Math is independent of Android so we can test it in CI.
 */
final class RigLegPose {
    static final float HIP_Y=-74f;
    static final float THIGH=36f, SHIN=35f;
    static final float STEP_DEPTH=31f;
    private static final float FRONT_DEPTH_PROJECT=.78f;
    private static final float BOOT_ANKLE=7f;

    static final class Pose {
        final float hipX,hipY,kneeX,kneeY,footX,footY,ankleY;
        final boolean planted;
        final float lift;
        Pose(float hx,float hy,float kx,float ky,float fx,float fy,
             boolean plant,float height) {
            hipX=hx; hipY=hy; kneeX=kx; kneeY=ky;
            footX=fx; footY=fy; ankleY=fy-BOOT_ANKLE;
            planted=plant; lift=height;
        }
    }

    static Pose solveProfile(float cycles, int index, float blend, float bounce) {
        float phase = GaitModel.fractional(cycles + .5f * index);
        GaitModel.Foot foot = GaitModel.sample(phase, 0);
        float depth = foot.along * STEP_DEPTH * blend;
        float lift = foot.lift * 7.5f * blend;
        float hipY = HIP_Y + bounce;
        float ankle = -BOOT_ANKLE - lift;

        // The stance leg carries the body with only a small knee bend. The
        // swing leg flexes as the heel lifts and extends before contact.
        float stanceProgress = Math.min(1f, phase / GaitModel.STANCE);
        float kneeFlex = blend * (2f * (float) Math.sin(Math.PI * stanceProgress)
            + 9f * foot.lift);
        float kneeX = depth * .5f + kneeFlex;
        float kneeY = (hipY + ankle) * .5f - kneeFlex * depth / (ankle - hipY);
        return new Pose(0f, hipY, kneeX, kneeY, depth, -lift, foot.planted, lift);
    }

    static Pose solve(float cycles,int index,int facing,float blend,float bounce) {
        float amount=Math.max(0f,Math.min(1f,blend));
        boolean side=facing==1||facing==3;
        GaitModel.Foot gait=GaitModel.sample(GaitModel.fractional(cycles),index);
        float depth=gait.along*STEP_DEPTH*amount;
        float lift=gait.lift*11f*amount;
        float hy=HIP_Y+bounce;
        float ankleVertical=-BOOT_ANKLE-lift;
        float deltaY=ankleVertical-hy;
        float length=(float)Math.hypot(depth,deltaY);
        float reachable=Math.max(.001f,Math.min(length,THIGH+SHIN-.001f));
        float ux=depth/Math.max(.001f,length);
        float uy=deltaY/Math.max(.001f,length);
        float axial=(THIGH*THIGH-SHIN*SHIN+reachable*reachable)/(2f*reachable);
        float bend=(float)Math.sqrt(Math.max(0f,THIGH*THIGH-axial*axial));

        // Anatomically coherent sagittal IK: both knees bend forward relative
        // to their hips, rather than symmetrically out to the sides.
        float kneeDepth=ux*axial+uy*bend;
        float kneeVertical=hy+uy*axial-ux*bend;

        float hx,fx,kx,fy,ky;
        if(side) {
            float separation = .2f + 2.8f*amount;
            hx=index==0 ? -separation : separation;
            fx=hx+depth;
            float layer=index==0?-1f:1f;
            fy=-lift+layer*.55f*amount;
            kx=hx+kneeDepth;
            ky=kneeVertical+layer*.35f*amount;
        }else {
            hx=index==0?-8f:8f;
            // Perspective: both legs stay beneath their respective hips.
            // Depth steps move up/down on screen, NEVER sideways.
            fx=hx+gait.along*amount*.9f;
            kx=hx+(fx-hx)*.5f;
            float projectedDepth=(facing==2?-1f:1f)*FRONT_DEPTH_PROJECT;
            fy=-lift+depth*projectedDepth;
            ky=kneeVertical+kneeDepth*projectedDepth;
        }

        // Near-straight neutral legs; use full IK knee flexion only when moving.
        float straightX=(hx+fx)*.5f;
        float straightY=(hy+(fy-BOOT_ANKLE))*.5f;
        kx=straightX+(kx-straightX)*amount;
        ky=straightY+(ky-straightY)*amount;
        return new Pose(hx,hy,kx,ky,fx,fy,gait.planted,lift);
    }
}
