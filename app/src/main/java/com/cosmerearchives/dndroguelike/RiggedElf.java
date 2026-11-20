package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * First truly articulated 2D walking prototype for the approved High Elf Fighter.
 * The pelvis, thighs, knees, shins, boots, arms and weapon grips move as separate
 * pieces. No static full-body image is translated to impersonate walking.
 *
 * The original face/hair is cropped from the approved four-facing illustration;
 * lower-body armor and joints are temporary procedural vector components.
 * This is a gait / rigging test, NOT a final illustrated equipment atlas.
 */
final class RiggedElf {
    private static final int INK=0xFF202632, SILVER=0xFFADB8B7;
    private static final int SHADOW=0xFF526375, LIGHT=0xFFE4E3D5;
    private static final int GOLD=0xFFE1B46B, DEEP_GOLD=0xFF94703F;
    private static final int BLUE=0xFF304D70, DEEP_BLUE=0xFF1D334E;
    private static final int LEATHER=0xFF50392D, SKIN=0xFFE9C5A8;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path shape=new Path();
    private final Bitmap approvedAtlas;
    private final Rect portraitSrc=new Rect();
    private final RectF portraitDst=new RectF();

    RiggedElf(Context context) {
        Bitmap illustration=null;
        try(java.io.InputStream input=context.getAssets().open("elf_fighter_atlas.webp")) {
            illustration=BitmapFactory.decodeStream(input);
        }catch(java.io.IOException ignored){ }
        approvedAtlas=illustration;
    }

    /** Animation coordinates live in source world pixels, anchored on the ground. */
    void draw(Canvas canvas,float footX,float footY,float scale,int facing,
              float distanceCycles,float movementBlend,float idleElapsed,float attackProgress) {
        Canvas c=canvas;
        float blend=clamp(movementBlend,0f,1f);
        boolean side=facing==1||facing==3;
        boolean back=facing==2;
        float cyc=GaitModel.fractional(distanceCycles);
        float omega=(float)(2.0*Math.PI*cyc);
        float bounce=-1.8f*blend*(float)Math.abs(Math.sin(omega*2f));
        float breathe=blend==0f?(float)Math.sin(idleElapsed*1.6f)*.65f:0f;
        float sway=blend*(float)Math.sin(omega)*2f;
        float leftArm=blend*(float)Math.sin(omega)*5f;
        float rightArm=-leftArm;

        c.save();c.translate(footX,footY);c.scale(scale,scale);
        if(facing==3)c.scale(-1f,1f); // Instant direction swap, never turn animation.

        // The original game paints the ground shadow in world coordinates. The rig
        // shadows its individual raised foot rather than moving the entire body.
        // Solve knees in the forward/depth plane first, then project that plane
        // into the chosen facing. Front/back knees no longer swing out sideways.
        RigLegPose.Pose a=RigLegPose.solve(distanceCycles,0,facing,blend,bounce);
        RigLegPose.Pose b=RigLegPose.solve(distanceCycles,1,facing,blend,bounce);
        drawLeg(c,side,back,a);
        drawLeg(c,side,back,b);

        c.save();
        c.translate(sway*.38f,bounce+breathe);
        // Dark underlayers, shaped silver armor, and blue split tabard echo our
        // chosen B/Fantasy Adventurer design, while retaining full rigging.
        polygon(c,INK,-19,-113,19,-113,17,-65,12,-57,-12,-57,-19,-66);
        polygon(c,DEEP_BLUE,-20,-71,15,-70,23,-37,10,-33,4,-53,-4,-54,-9,-33,-25,-39);
        polygon(c,BLUE,-10,-66,9,-66,10,-28,1,-22,-11,-31);
        stripe(c,GOLD,2f,0,-62,0,-27);
        polygon(c,DEEP_GOLD,-18,-67,17,-67,14,-61,-15,-61);
        polygon(c,0xFF8898A6,-23,-106,-17,-115,16,-115,24,-106,16,-80,11,-68,-12,-68,-18,-81);
        polygon(c,SILVER,-19,-106,-12,-112,11,-112,19,-105,12,-90,7,-79,-8,-79,-14,-88);
        polygon(c,0xFFD8D6CC,-14,-106,-5,-110,-1,-99,-10,-94);
        polygon(c,0xFFBCC6C4,5,-110,15,-106,11,-94,2,-99);
        // Stylized sculpted plate and slimmer waist, not heavy utilitarian armor.
        stripe(c,GOLD,1.7f,-16,-91,0,-82);
        stripe(c,GOLD,1.7f,0,-82,17,-91);
        polygon(c,GOLD,-4,-85,4,-85,2,-77,-2,-77);
        polygon(c,DEEP_BLUE,-11,-78,12,-78,8,-68,-9,-68);
        stripe(c,GOLD,2,-10,-68,10,-68);
        polygon(c,SKIN,-7,-121,7,-121,8,-112,-8,-112);

        // Two separately articulated shoulders / elbow / hands. Weapons attach
        // to those hands and follow the swing, not the stationary silhouette.
        float shoulder=side?18f:21f;
        drawArm(c,-shoulder,-104,-30+leftArm*.28f,-80,-31+leftArm*.6f,-55,0);
        float swingStrike=attackProgress<=0f?0f:
            (float)Math.sin(Math.PI*clamp(attackProgress,0f,1f))*24f;
        drawArm(c,shoulder,-104,30+rightArm*.28f+swingStrike*.25f,-81,
                32+rightArm*.55f+swingStrike,-55-swingStrike*.65f,1);
        drawShield(c,-31+leftArm*.6f,-55,side,back);
        drawSword(c,32+rightArm*.55f+swingStrike,-55-swingStrike*.65f,side);

        // Keep the approved face, pointed ears and hairstyle rather than
        // replacing them with a generic procedural face.
        if(approvedAtlas!=null)drawOriginalPortrait(c,facing,side);
        else drawHeadFallback(c,back,side);
        c.restore();
        c.restore();
    }

    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}

    /**
     * Paints articulated armor from the independently tested 2D projection.
     * Knee/ankle positions are never solved using the screen-X plane for
     * frontal/backward facings; that previously produced crab-like knees.
     */
    private void drawLeg(Canvas c,boolean side,boolean back,RigLegPose.Pose leg) {
        float hipX=leg.hipX,hipY=leg.hipY;
        float kneeX=leg.kneeX,kneeY=leg.kneeY;
        float footX=leg.footX,footY=leg.footY;
        float ankleY=leg.ankleY;
        // Upper leg, knee guard and a true two-segment shin/greave.
        bone(c,hipX,hipY,kneeX,kneeY,15f,INK);
        bone(c,hipX,hipY,kneeX,kneeY,11.5f,back?0xFF65717E:0xFF8D9BA5);
        bone(c,hipX-1f,hipY+1,kneeX-1f,kneeY,4f,0xFFB7C1C1);
        bone(c,hipX+2.5f,hipY+2,kneeX+2f,kneeY,1.6f,GOLD);
        joint(c,kneeX,kneeY,8.2f,INK);
        joint(c,kneeX,kneeY,6.4f,SILVER);
        joint(c,kneeX,kneeY,2.6f,GOLD);
        bone(c,kneeX,kneeY,footX,ankleY,14f,INK);
        bone(c,kneeX,kneeY,footX,ankleY,11f,SHADOW);
        bone(c,kneeX-2f,kneeY,footX-2f,ankleY-1f,3.4f,LIGHT);
        bone(c,kneeX+2.5f,kneeY,footX+2.5f,ankleY,1.5f,GOLD);

        if(side) {
            // Side view uses an elongated toe. Its planted stance stays at
            // terrain height while a swinging foot actually leaves ground.
            polygon(c,INK,footX-8,footY-12,footX+9,footY-12,
                    footX+13,footY-1,footX+1,footY+2,footX-10,footY+1);
            polygon(c,0xFF9BA9B0,footX-7,footY-11,footX+7,footY-11,
                    footX+11,footY-2,footX,footY,footX-8,footY-1);
        }else {
            // Front/back boots must point along screen depth, not flare
            // horizontally. Their thin silhouettes emphasize alternation.
            float toe=back?-3f:2f;
            polygon(c,INK,footX-6,footY-13,footX+6,footY-13,
                    footX+7,footY+toe,footX,footY+toe+2,footX-7,footY+toe);
            polygon(c,0xFF9BA9B0,footX-4.5f,footY-11,footX+4.5f,footY-11,
                    footX+5,footY+toe-1,footX,footY+toe,footX-5,footY+toe-1);
        }
        stripe(c,GOLD,1.9f,footX-4.5f,footY-5,footX+5,footY-5);
        // Projected depth does not equal foot lift; only swing lifts.
        if(!leg.planted && leg.lift>1f){
            float groundY=side?0f:footY+leg.lift;
            oval(c,0x33101B16,footX-5,groundY,footX+5,groundY+1.8f);
        }
    }

    private void drawArm(Canvas c,float shoulderX,float shoulderY,float elbowX,float elbowY,
                         float handX,float handY,int hand) {
        bone(c,shoulderX,shoulderY,elbowX,elbowY,13f,INK);
        bone(c,shoulderX,shoulderY,elbowX,elbowY,10f,SKIN);
        joint(c,shoulderX,shoulderY,13f,INK);
        joint(c,shoulderX,shoulderY,10.5f,SILVER);
        joint(c,shoulderX-2f,shoulderY-3f,5f,LIGHT);
        stripe(c,GOLD,2.2f,shoulderX-9f,shoulderY+5,shoulderX+7f,shoulderY+6);
        bone(c,elbowX,elbowY,handX,handY,13f,INK);
        bone(c,elbowX,elbowY,handX,handY,9f,SHADOW);
        bone(c,elbowX-2f,elbowY,handX-2f,handY,2f,LIGHT);
        joint(c,handX,handY,6.5f,LEATHER);
        joint(c,handX,handY,4f,0xFF9C7850);
    }

    private void drawShield(Canvas c,float handX,float handY,boolean side,boolean back){
        // Shield and star emboss move as one assembly with the left hand.
        c.save();c.translate(handX-7f,handY+2f);c.rotate(side?8f:-8f);
        polygon(c,INK,-21,-21,4,-29,17,-19,16,11,-2,29,-20,9);
        polygon(c,GOLD,-19,-20,4,-27,15,-18,14,10,-2,26,-18,8);
        polygon(c,DEEP_BLUE,-16,-16,4,-23,12,-15,10,8,-2,22,-15,6);
        polygon(c,0xFF4C6C87,-9,-14,3,-20,6,-13,4,13,-5,17);
        polygon(c,GOLD,-1,-17,3,-4,16,1,3,5,-1,19,-5,5,-17,1,-5,-4);
        joint(c,-1,1,3f,LIGHT);
        c.restore();
    }
    private void drawSword(Canvas c,float handX,float handY,boolean side){
        c.save();c.translate(handX,handY);c.rotate(side?7f:-12f);
        bone(c,0,0,4,12,5f,LEATHER);
        stripe(c,GOLD,4f,-9,-3,9,1);
        polygon(c,SHADOW,0,9,4,9,3,55,1,68,-3,55,-4,9);
        polygon(c,0xFFDDE6E8,-2,9,2,9,1,61,-2,54);
        stripe(c,LIGHT,1.3f,0,12,0,53);
        c.restore();
    }
    private void drawOriginalPortrait(Canvas c,int facing,boolean side){
        int fw=approvedAtlas.getWidth()/4;
        int fh=approvedAtlas.getHeight();
        // Narrow crop avoids baked-in weapons. The crop is intentionally limited
        // to the HEAD and hair; articulated body parts never come from this bitmap.
        int left=fw*27/100,right=fw*73/100,bot=fh*34/100;
        // Left-facing canvas is mirrored, so source the RIGHT portrait before mirroring.
        int artDirection=facing==3?1:facing;
        portraitSrc.set(fw*artDirection+left,0,fw*artDirection+right,bot);
        portraitDst.set(side?-28f:-32f,-167f,side?28f:32f,-109f);
        c.drawBitmap(approvedAtlas,portraitSrc,portraitDst,bitmapPaint);
    }
    private void drawHeadFallback(Canvas c,boolean back,boolean side){
        joint(c,0,-143f,19f,0xFFCDA969);
        joint(c,0,-142f,15f,SKIN);
        polygon(c,SKIN,-10,-135,-24,-141,-13,-126);
        polygon(c,SKIN,10,-135,24,-141,13,-126);
        if(!back){
            oval(c,0xFF547CA0,-8,-144,-4,-141);
            oval(c,0xFF547CA0,4,-144,8,-141);
        }
        polygon(c,0xFFDDB965,-17,-144,-14,-159,-4,-166,7,-163,16,-150,
                9,-154,-2,-159,-11,-148);
        if(back)polygon(c,0xFFDDB965,-15,-150,15,-151,17,-113,5,-105,-16,-111);
    }
    private void bone(Canvas c,float x1,float y1,float x2,float y2,float width,int color){
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(width);p.setColor(color);c.drawLine(x1,y1,x2,y2,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void stripe(Canvas c,int color,float width,float x1,float y1,float x2,float y2){
        bone(c,x1,y1,x2,y2,width,color);
    }
    private void joint(Canvas c,float x,float y,float radius,int color){
        p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,radius,p);
    }
    private void oval(Canvas c,int color,float l,float t,float r,float b){
        p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawOval(l,t,r,b,p);
    }
    private void polygon(Canvas c,int color,float... xy){
        shape.reset();shape.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2)shape.lineTo(xy[i],xy[i+1]);
        shape.close();p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawPath(shape,p);
    }
}