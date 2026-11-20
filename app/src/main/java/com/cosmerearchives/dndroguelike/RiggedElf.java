package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

/** Articulated High Elf with independent head, body, and equipment layers. */
final class RiggedElf {
    private static final int INK=0xFF202632, SILVER=0xFFADB8B7;
    private static final int SHADOW=0xFF526375, LIGHT=0xFFE4E3D5;
    private static final int GOLD=0xFFE1B46B, DEEP_GOLD=0xFF94703F;
    private static final int BLUE=0xFF304D70, DEEP_BLUE=0xFF1D334E;
    private static final int LEATHER=0xFF50392D, SKIN=0xFFEBC09F;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path shape=new Path();
    private static final int HEAD_WIDTH = 192, HEAD_HEIGHT = 152;
    private static final float HEAD_SCALE = .35f;
    private static final float PROFILE_HEAD_LIFT = 5f;
    private final Bitmap headAtlas;
    private final ElfLayerSprites layers;
    private final Rect headSrc = new Rect();
    private final RectF headDst = new RectF();

    RiggedElf(Context context) {
        headAtlas = loadHeadAtlas(context);
        layers = new ElfLayerSprites(context);
    }

    private static Bitmap loadHeadAtlas(Context context) {
        try (java.io.InputStream input = context.getAssets().open("elf_head_layers.webp")) {
            Bitmap atlas = BitmapFactory.decodeStream(input);
            return atlas != null && atlas.getWidth() == HEAD_WIDTH * 3
                && atlas.getHeight() == HEAD_HEIGHT ? atlas : null;
        } catch (java.io.IOException ignored) {
            return null;
        }
    }

    void draw(Canvas canvas,float footX,float footY,float scale,int facing,
              float distanceCycles,float movementBlend,float idleElapsed,
              float attackProgress,boolean armored) {
        Canvas c=canvas;
        float blend=clamp(movementBlend,0f,1f);
        boolean side=RigViewProfile.isSide(facing);
        boolean back=facing==2;
        float cyc=GaitModel.fractional(distanceCycles);
        float omega=(float)(2.0*Math.PI*cyc);
        float bounce=-1.6f*blend*(float)Math.abs(Math.sin(omega*2f));
        float breathe=(1f-blend)*(float)Math.sin(idleElapsed*1.6f)*.65f;
        float sway=blend*(float)Math.sin(omega)*1.6f;
        // When a leg advances, its opposite arm advances. In front and back
        // views most of that motion projects along screen depth.
        float armStride = -blend * (float)Math.cos(omega);
        float armIdle = (1f - blend) * (float)Math.sin(idleElapsed * 1.6f) * .55f;
        float armSwing = armStride * (side ? 5f : .8f) + armIdle;
        float armDepth = side ? 0f : armStride * (back ? -2f : 2f);
        float strike=attackProgress<=0f?0f:
            (float)Math.sin(Math.PI*clamp(attackProgress,0f,1f))*23f;

        c.save();
        c.translate(footX,footY);
        c.scale(scale,scale);
        if(facing==3)c.scale(-1f,1f); // Real side profile mirrored instantly.

        RigLegPose.Pose left=RigLegPose.solve(distanceCycles,0,facing,blend,bounce);
        RigLegPose.Pose right=RigLegPose.solve(distanceCycles,1,facing,blend,bounce);

        // Far-side leg always lies beneath the near leg in a profile.
        drawLeg(c,side,back,right,facing,1,armored);
        drawLeg(c,side,back,left,facing,0,armored);

        c.save();
        c.translate(sway*.35f,bounce+breathe);
        float leftShoulder=RigViewProfile.shoulderX(facing,0);
        float rightShoulder=RigViewProfile.shoulderX(facing,1);
        float shoulderY = -108f;
        float leftElbow=RigViewProfile.elbowX(facing,0)+armSwing*.48f;
        float rightElbow=RigViewProfile.elbowX(facing,1)-armSwing*.48f;
        float leftHand=RigViewProfile.handX(facing,0)+armSwing;
        float rightHand=RigViewProfile.handX(facing,1)-armSwing+strike;
        float leftElbowY = -81f + armDepth*.42f;
        float rightElbowY = (side ? -80f : -81f) - armDepth*.42f;
        float leftHandY=RigViewProfile.handY(facing,0)+armDepth;
        float rightHandY=RigViewProfile.handY(facing,1)-armDepth-strike*.65f;

        if(side) {
            // Sword hand is the FAR arm. Draw behind breastplate, rather than
            // putting two equally prominent shoulders on the front plane.
            drawArm(c,rightShoulder,shoulderY,rightElbow,rightElbowY,
                    rightHand,rightHandY,1,facing,armored);
            if(armored)drawSword(c,rightHand,rightHandY,true);
        }
        float neckX = side ? -3f : 0f;
        drawNeck(c, neckX, side, back);
        drawIllustratedTorso(c,facing,side,armored);
        if (headAtlas != null) {
            drawHead(c, facing, neckX);
        } else {
            c.save();
            c.translate(neckX, side ? 2f - PROFILE_HEAD_LIFT : 2f);
            drawHeadFallback(c, back);
            c.restore();
        }

        drawArm(c,leftShoulder,shoulderY,leftElbow,leftElbowY,leftHand,leftHandY,0,facing,armored);
        if(armored)drawShield(c,leftHand,leftHandY,side,back);

        if(!side) {
            drawArm(c,rightShoulder,shoulderY,rightElbow,rightElbowY,
                    rightHand,rightHandY,1,facing,armored);
            if(armored)drawSword(c,rightHand,rightHandY,false);
        }

        c.restore();
        c.restore();
    }

    private void drawNeck(Canvas c, float x, boolean side, boolean back) {
        float collar = ElfLayerSprites.COLLAR_Y;
        if (side) {
            // The jaw sits forward of the nape. Draw the throat before the
            // collar and hair so both can cover the ends of the neck.
            shape.reset();
            shape.moveTo(x-4f,collar-12f);
            shape.lineTo(x+5f,collar-12f);
            shape.quadTo(x+2.9f,collar-5f,x+4.2f,collar+3f);
            shape.lineTo(x-5f,collar+3f);
            shape.quadTo(x-2f,collar-4f,x-4f,collar-12f);
            shape.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFCA9678);
            c.drawPath(shape,p);
            polygon(c,0xFFE9B695,x+1.5f,collar-10f,x+4.5f,collar-11f,
                x+3.3f,collar-5f,x+4f,collar+2f,x+2f,collar+2f,x+.5f,collar-5f);
            return;
        }
        polygon(c, back ? 0xFFC49D80 : 0xFFE5BA99,
            x-4.6f,collar-4f,x+4.6f,collar-4f,x+5.6f,collar+6f,x-5.6f,collar+6f);
    }

    private void drawHead(Canvas c, int facing, float neckX) {
        int column = facing == 3 ? 1 : facing; // The caller mirrors the side profile.
        int left = column * HEAD_WIDTH;
        headSrc.set(left, 0, left + HEAD_WIDTH, HEAD_HEIGHT);
        // The atlas anchor is at the jaw/collar join. The profile uses extra neck clearance.
        float x = neckX - 96 * HEAD_SCALE;
        float collarOffset = RigViewProfile.isSide(facing) ? PROFILE_HEAD_LIFT : 0f;
        float y = ElfLayerSprites.COLLAR_Y - collarOffset - 88 * HEAD_SCALE;
        headDst.set(x, y, x + HEAD_WIDTH * HEAD_SCALE, y + HEAD_HEIGHT * HEAD_SCALE);
        c.drawBitmap(headAtlas, headSrc, headDst, bitmapPaint);
    }

    /** The painted torso uses a vector fallback if the body atlas fails to load. */
    private void drawIllustratedTorso(Canvas c,int facing,boolean side,boolean armored) {
        float half=RigViewProfile.torsoHalfWidth(facing);
        if(layers.torso(c,facing,half,armored))return;
        // Layered fabric follows the profile rather than the old rectangular torso.
        float capeShift=side?-5f:0f;
        polygon(c,DEEP_BLUE,capeShift-half+3f,-80f,capeShift+half-4f,-81f,
            capeShift+half+4f,-36f,capeShift+3f,-29f,capeShift-half-5f,-37f);
        polygon(c,BLUE,-half*.45f,-73f,half*.46f,-72f,half*.42f,-28f,
            1f,-22f,-half*.50f,-31f);
        stripe(c,GOLD,1.5f,0,-67f,0,-28f);
        polygon(c,INK,-half-2f,-112f,half+2f,-112f,half,-79f,
            half*.56f,-65f,-half*.65f,-65f,-half,-80f);
        polygon(c,0xFF8898A6,-half,-109f,half,-109f,
            half*.85f,-85f,half*.46f,-72f,-half*.58f,-72f,-half*.9f,-86f);

        polygon(c,SILVER,-half+3,-109,half-3,-109,half*.56f,-88,
            half*.35f,-77,-half*.35f,-77,-half*.6f,-89);
        // The gold cinch hides the bottom texture seam and retains her blue tabard.
        polygon(c,DEEP_GOLD,-half*.75f,-72f,half*.65f,-72f,
            half*.5f,-65f,-half*.65f,-65f);
        stripe(c,GOLD,2f,-half*.64f,-69f,half*.55f,-69f);
        polygon(c,GOLD,-3.5f,-72f,3f,-72f,2f,-65f,-2.5f,-65f);
        polygon(c,SKIN,-5.5f,-121f,5.5f,-121f,6f,-112f,-6f,-112f);
    }

    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}

    /**
     * Paints articulated armor from the independently tested 2D projection.
     * Knee/ankle positions are never solved using the screen-X plane for
     * frontal/backward facings; that previously produced crab-like knees.
     */
    private void drawLeg(Canvas c,boolean side,boolean back,RigLegPose.Pose leg,int facing,int index,boolean armored) {
        float hipX=leg.hipX,hipY=leg.hipY;
        float kneeX=leg.kneeX,kneeY=leg.kneeY;
        float footX=leg.footX,footY=leg.footY;
        float ankleY=leg.ankleY;
        if(layers.ready()){
            // Lower leg goes behind the upper leg at the shared knee.
            // This conceals the boot segment's rectangular source edge.
            layers.limb(c,facing,ElfLayerSprites.SHIN,index,kneeX,kneeY,footX,footY,
                        16f,armored);
            layers.limb(c,facing,ElfLayerSprites.THIGH,index,hipX,hipY,kneeX,kneeY,
                        17f,armored);
            return;
        }
        // Fallback only if illustration assets are unavailable.
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
                         float handX,float handY,int hand,int facing,boolean armored) {
        if(layers.ready()){
            layers.limb(c,facing,ElfLayerSprites.UPPER_ARM,hand,
                        shoulderX,shoulderY,elbowX,elbowY,14f,armored);
            layers.limb(c,facing,ElfLayerSprites.FOREARM,hand,
                        elbowX,elbowY,handX,handY,13f,armored);
            return;
        }
        // Fallback only: preserve drawing if external layer assets are unavailable.
        bone(c,shoulderX,shoulderY,elbowX,elbowY,12f,INK);
        bone(c,shoulderX,shoulderY,elbowX,elbowY,9f,SKIN);
        bone(c,elbowX,elbowY,handX,handY,12f,INK);
        bone(c,elbowX,elbowY,handX,handY,9f,SHADOW);
        bone(c,elbowX-1.6f,elbowY,handX-1.6f,handY,1.6f,LIGHT);
        stripe(c,GOLD,1.65f,elbowX+2,elbowY+3,handX+2,handY-3);
        polygon(c,INK,shoulderX-12f,shoulderY-6f,shoulderX-7f,shoulderY-14f,
            shoulderX+5f,shoulderY-14f,shoulderX+13f,shoulderY-4f,
            shoulderX+7f,shoulderY+7f,shoulderX-10f,shoulderY+7f);
        polygon(c,SILVER,shoulderX-9f,shoulderY-5f,shoulderX-5f,shoulderY-11f,
            shoulderX+4f,shoulderY-11f,shoulderX+10f,shoulderY-4f,
            shoulderX+5f,shoulderY+4f,shoulderX-7f,shoulderY+4f);
        stripe(c,GOLD,1.8f,shoulderX-6f,shoulderY+4,shoulderX+5f,shoulderY+4);
        joint(c,handX,handY,6f,LEATHER);
        joint(c,handX,handY,3.5f,0xFFB39A73);
    }

    private void drawShield(Canvas c,float handX,float handY,boolean side,boolean back){
        // Shield and star emboss move as one assembly with the left hand.
        c.save();
        c.translate(handX-7f,handY+2f);
        // A turned fighter presents the shield at an angle, not full-on.
        if(side)c.scale(.66f,1f);
        c.rotate(side?8f:-8f);
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
    private void drawHeadFallback(Canvas c,boolean back){
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