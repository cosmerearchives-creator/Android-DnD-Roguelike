package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

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
    private static final float BLADE_TIP_Y = 55f;
    private static final float THIGH_ART_DROP = 6f;
    private static final float THIGH_PELVIS_OVERLAP = 4.5f;
    private final RigAttackPose attack = new RigAttackPose();
    private final Shader throatTone = new LinearGradient(0,-126f,0,-113f,
        new int[]{0xFFC38D73,0xFFE6B999,0x00E6B999},
        new float[]{0f,.44f,1f},Shader.TileMode.CLAMP);
    private final Bitmap headAtlas;
    private final ElfLayerSprites layers;
    private final ElfBaseArt baseArt;
    private final Rect headSrc = new Rect();
    private final RectF headDst = new RectF();

    RiggedElf(Context context) {
        headAtlas = loadHeadAtlas(context);
        layers = new ElfLayerSprites(context);
        baseArt = new ElfBaseArt(context);
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
              float attackProgress,boolean armored,float closeUpWidth) {
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
        attack.sample(attackProgress, side);
        bounce += attack.brace;

        boolean boundBase = !armored && baseArt.ready(facing);
        boolean frontFallback = facing == 0 && !armored && !boundBase;
        float leftShoulder=RigViewProfile.shoulderX(facing,0) - (frontFallback ? .6f : 0f);
        float rightShoulder=RigViewProfile.shoulderX(facing,1) + (frontFallback ? .6f : 0f);
        float shoulderY = frontFallback ? -109f : -108f;
        float leftElbow=RigViewProfile.elbowX(facing,0)+armSwing*.48f;
        float rightElbow=RigViewProfile.elbowX(facing,1)-armSwing*.48f;
        float leftHand=RigViewProfile.handX(facing,0)+armSwing;
        float rightHand=RigViewProfile.handX(facing,1)-armSwing;
        float leftElbowY = RigViewProfile.elbowY(facing, 0) + armDepth*.42f;
        float rightElbowY = RigViewProfile.elbowY(facing, 1) - armDepth*.42f;
        float leftHandY=RigViewProfile.handY(facing,0)+armDepth-attack.guard;
        float rightHandY=RigViewProfile.handY(facing,1)-armDepth;
        if (frontFallback) {
            // Continue the upper arm's line into the lower arm and finish
            // with a relaxed wrist and fingers at the reference height.
            leftElbow += .4f;
            rightElbow -= .4f;
            leftHand += .35f;
            rightHand -= .65f;
            leftHandY -= 1.2f;
            rightHandY -= 1.2f;
        }

        // Rotate the two bone vectors independently around their joints. This keeps
        // the forearm and hand attached even at the top of the wind-up.
        if (attackProgress > 0f) {
            float upperX = rightElbow - rightShoulder, upperY = rightElbowY - shoulderY;
            float lowerX = rightHand - rightElbow, lowerY = rightHandY - rightElbowY;
            float upperAngle = (float)Math.toRadians(attack.upper);
            float lowerAngle = (float)Math.toRadians(attack.forearm);
            rightElbow = rightShoulder + rotateX(upperX, upperY, upperAngle);
            rightElbowY = shoulderY + rotateY(upperX, upperY, upperAngle);
            rightHand = rightElbow + rotateX(lowerX, lowerY, lowerAngle);
            rightHandY = rightElbowY + rotateY(lowerX, lowerY, lowerAngle);
        }
        float torsoX = sway * .35f;
        if (closeUpWidth > 0f) {
            // Keep a steady close-up until the pose needs room for the blade.
            float blade = (float)Math.toRadians(attack.blade);
            float reach = armored ? BLADE_TIP_Y : 0f;
            float hipY = RigLegPose.HIP_Y;
            float lean = (float)Math.toRadians(attack.lean);
            float tipX = torsoX + rotateX(rightHand + rotateX(0f, reach, blade),
                rightHandY - hipY + rotateY(0f, reach, blade), lean);
            float wristX = torsoX + rotateX(rightHand, rightHandY - hipY, lean);
            float elbowX = torsoX + rotateX(rightElbow, rightElbowY - hipY, lean);
            float left = Math.min(side ? -36f : -53f, Math.min(tipX-3f,
                Math.min(wristX-14f, elbowX-10f)));
            float right = Math.max(side ? 36f : 48f, Math.max(tipX+3f,
                Math.max(wristX+14f, elbowX+10f)));
            if (facing == 3 || back) {
                float originalLeft = left;
                left = -right;
                right = -originalLeft;
            }
            scale = Math.min(scale, closeUpWidth / (right-left+16f));
            footX = clamp(footX, (8f-left)*scale, closeUpWidth-(8f+right)*scale);
            oval(c, 0x99112125, footX-23f*scale, footY-4f*scale,
                footX+23f*scale, footY+5f*scale);
        }

        c.save();
        c.translate(footX,footY);
        c.scale(scale,scale);
        if(facing==3)c.scale(-1f,1f); // Real side profile mirrored instantly.

        boolean swordInBack = RigViewProfile.drawSwordArmBehindTorso(facing) && !attack.swordInFront;
        boolean shieldInBack = facing == 3;
        boolean showFarLimbs = !boundBase || !side || blend > .02f || attackProgress > 0f;
        if (showFarLimbs && (swordInBack || shieldInBack)) {
            c.save();
            c.translate(torsoX,bounce+breathe);
            c.rotate(attack.lean,0f,RigLegPose.HIP_Y);
            // Far-side hands pass behind the hips and thighs as well as the chest.
            if (swordInBack) {
                if (armored) drawSword(c,rightHand,rightHandY,attack.blade);
                drawArm(c,rightShoulder,shoulderY,rightElbow,rightElbowY,
                    rightHand,rightHandY,1,facing,armored);
                if (armored) drawSwordGrip(c,rightHand,rightHandY,attack.blade);
            }
            if (shieldInBack) {
                drawArm(c,leftShoulder,shoulderY,leftElbow,leftElbowY,
                    leftHand,leftHandY,0,facing,armored);
                if (armored) drawShield(c,leftElbow,leftElbowY,leftHand,leftHandY,true,true);
            }
            c.restore();
        }

        RigLegPose.Pose left=RigLegPose.solve(distanceCycles,0,facing,blend,bounce);
        RigLegPose.Pose right=RigLegPose.solve(distanceCycles,1,facing,blend,bounce);
        if (boundBase && facing == 3) {
            if (showFarLimbs) baseArt.drawLeg(c,facing,0,left);
            baseArt.drawLeg(c,facing,1,right);
        } else {
            if (showFarLimbs) drawLeg(c,side,back,right,facing,1,armored);
            drawLeg(c,side,back,left,facing,0,armored);
        }

        c.save();
        c.translate(torsoX,bounce+breathe);
        c.rotate(attack.lean,0f,RigLegPose.HIP_Y);
        if (boundBase) {
            baseArt.drawBody(c, facing);
        } else {
            float neckX = side ? -3f : 0f;
            drawNeck(c, neckX, side, back);
            if (side) {
                // Fill the narrow opening inside the armhole with its lining.
                oval(c,armored ? 0xFF3B3845 : 0xFF4B4145,-10.6f,-115f,3.2f,-101f);
            } else if (!armored) {
                // Extend the vest over the shoulder root so the deltoid has a
                // continuous edge into the torso in the front and rear projections.
                oval(c,0xFF2C2E38,-18.4f,-115f,-9f,-103f);
                oval(c,0xFF2C2E38,9f,-115f,18.4f,-103f);
            }
            drawIllustratedTorso(c,facing,side,armored);
            if (facing == 0 && !armored) drawThroatJoin(c);
            if (back && armored) layers.rearTabard(c);
            if (headAtlas != null) {
                drawHead(c, facing, neckX);
            } else {
                c.save();
                c.translate(neckX, side ? 2f - PROFILE_HEAD_LIFT : 2f);
                drawHeadFallback(c, back);
                c.restore();
            }
        }

        // Front and rear views look at opposite sides of the same hands.
        if (back) c.scale(-1f,1f);
        if (!shieldInBack) {
            if (back && armored) drawShield(c,leftElbow,leftElbowY,leftHand,leftHandY,false,true);
            drawArm(c,leftShoulder,shoulderY,leftElbow,leftElbowY,
                leftHand,leftHandY,0,facing,armored);
            if (armored) {
                if (back) drawShieldStraps(c,leftElbow,leftElbowY,leftHand,leftHandY);
                else drawShield(c,leftElbow,leftElbowY,leftHand,leftHandY,side,false);
            }
        }
        if (!swordInBack) {
            if (armored) drawSword(c,rightHand,rightHandY,attack.blade);
            drawArm(c,rightShoulder,shoulderY,rightElbow,rightElbowY,
                rightHand,rightHandY,1,facing,armored);
            if (armored) drawSwordGrip(c,rightHand,rightHandY,attack.blade);
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
            shape.lineTo(x-4.3f,collar+3f);
            shape.quadTo(x-2.5f,collar-5f,x-3.6f,collar-12f);
            shape.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFD19F83);
            c.drawPath(shape,p);
            polygon(c,0xFFE1AD91,x+1.9f,collar-10f,x+4.3f,collar-11f,
                x+3.3f,collar-5f,x+3.9f,collar+1f,x+2.4f,collar+1f,x+1.1f,collar-5f);
            return;
        }
        shape.reset();
        shape.moveTo(x-4.1f,collar+5f);
        shape.quadTo(x-3.7f,collar,x-4.2f,collar-5.5f);
        shape.quadTo(x,collar-8f,x+4.2f,collar-5.5f);
        shape.quadTo(x+3.7f,collar,x+4.9f,collar+5f);
        shape.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(back ? 0xFFC49D80 : 0xFFD4A183);
        c.drawPath(shape,p);
    }

    private void drawThroatJoin(Canvas c) {
        // Cover the square top edge of the painted vest neck. The lower color
        // fades into its existing shading and the head covers the upper edge.
        shape.reset();
        shape.moveTo(-4.5f,-126f);
        shape.quadTo(-4.1f,-120f,-5.3f,-118f);
        shape.quadTo(-3.5f,-114f,0,-113f);
        shape.quadTo(3.5f,-114f,5.3f,-118f);
        shape.quadTo(4.1f,-120f,4.5f,-126f);
        shape.close();
        p.setShader(throatTone);
        c.drawPath(shape,p);
        p.setShader(null);
    }

    private void drawHead(Canvas c, int facing, float neckX) {
        int column = facing == 3 ? 1 : facing; // The caller mirrors the side profile.
        int left = column * HEAD_WIDTH;
        headSrc.set(left, 0, left + HEAD_WIDTH, HEAD_HEIGHT);
        // The atlas anchor is at the jaw/collar join. The profile uses extra neck clearance.
        float x = neckX - 96 * HEAD_SCALE;
        float collarOffset = RigViewProfile.isSide(facing) ? PROFILE_HEAD_LIFT : facing == 0 ? 1.5f : 0f;
        float y = ElfLayerSprites.COLLAR_Y - collarOffset - 88 * HEAD_SCALE;
        headDst.set(x, y, x + HEAD_WIDTH * HEAD_SCALE, y + HEAD_HEIGHT * HEAD_SCALE);
        c.save();
        if (facing == 2) {
            shape.reset();
            shape.moveTo(x,y);
            shape.lineTo(x+HEAD_WIDTH*HEAD_SCALE,y);
            shape.lineTo(x+HEAD_WIDTH*HEAD_SCALE,y+119f*HEAD_SCALE);
            shape.lineTo(x+141f*HEAD_SCALE,y+119f*HEAD_SCALE);
            shape.cubicTo(x+137f*HEAD_SCALE,y+124f*HEAD_SCALE,
                x+125f*HEAD_SCALE,y+133f*HEAD_SCALE,x+119f*HEAD_SCALE,y+132f*HEAD_SCALE);
            shape.cubicTo(x+115f*HEAD_SCALE,y+132f*HEAD_SCALE,
                x+119f*HEAD_SCALE,y+127f*HEAD_SCALE,x+114f*HEAD_SCALE,y+128f*HEAD_SCALE);
            shape.cubicTo(x+109f*HEAD_SCALE,y+128f*HEAD_SCALE,
                x+103f*HEAD_SCALE,y+136f*HEAD_SCALE,x+98f*HEAD_SCALE,y+134f*HEAD_SCALE);
            shape.cubicTo(x+95f*HEAD_SCALE,y+133f*HEAD_SCALE,
                x+98f*HEAD_SCALE,y+129f*HEAD_SCALE,x+94f*HEAD_SCALE,y+130f*HEAD_SCALE);
            shape.cubicTo(x+89f*HEAD_SCALE,y+130f*HEAD_SCALE,
                x+84f*HEAD_SCALE,y+132f*HEAD_SCALE,x+79f*HEAD_SCALE,y+127f*HEAD_SCALE);
            shape.quadTo(x+71f*HEAD_SCALE,y+126f*HEAD_SCALE,x+64f*HEAD_SCALE,y+119f*HEAD_SCALE);
            shape.lineTo(x,y+117f*HEAD_SCALE);
            shape.close();
            c.clipPath(shape);
        }
        c.drawBitmap(headAtlas, headSrc, headDst, bitmapPaint);
        c.restore();
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
        if (!armored && baseArt.ready(facing)) {
            baseArt.drawLeg(c,facing,index,leg);
            return;
        }
        if(layers.ready()){
            // Lower leg goes behind the upper leg at the shared knee.
            // This conceals the boot segment's rectangular source edge.
            layers.shinAndBoot(c,facing,index,kneeX,kneeY,footX,footY,armored);
            // Atlas cells include painted material above the hip anchor. Keep
            // that overlap inside the pelvis even when the thigh stretches
            // or tilts during a stride.
            float paintedHipY = hipY + THIGH_ART_DROP;
            c.save();
            c.clipRect(hipX-100f,hipY-THIGH_PELVIS_OVERLAP,hipX+100f,kneeY+100f);
            if (back && armored) {
                // The cropped rear armor cell leaves the leg visible under the skirt.
                layers.limb(c,facing,ElfLayerSprites.THIGH,index,hipX,paintedHipY,kneeX,kneeY,
                    15f,false);
            }
            layers.limb(c,facing,ElfLayerSprites.THIGH,index,hipX,paintedHipY,kneeX,kneeY,
                        back && armored ? 16f : 17f,armored);
            c.restore();
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
        if (!armored && baseArt.ready(facing)) {
            baseArt.drawArm(c,facing,hand,shoulderX,shoulderY,elbowX,elbowY,handX,handY);
            return;
        }
        if(layers.ready()){
            layers.limb(c,facing,ElfLayerSprites.UPPER_ARM,hand,
                        shoulderX,shoulderY,elbowX,elbowY,14f,armored);
            float gloveWidth = facing == 0 && !armored ? 11.9f : 13f;
            layers.limb(c,facing,ElfLayerSprites.FOREARM,hand,
                        elbowX,elbowY,handX,handY,gloveWidth,armored,hand==1);
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

    private void drawShield(Canvas c,float elbowX,float elbowY,float handX,float handY,
                            boolean side,boolean inside){
        c.save();
        c.translate(handX,handY);
        c.rotate((float)Math.toDegrees(Math.atan2(elbowX-handX,handY-elbowY)));
        c.translate(-3f,-9f);
        c.scale(side ? .49f : .83f,.84f);
        polygon(c,INK,-21,-21,4,-29,17,-19,16,11,-2,29,-20,9);
        polygon(c,GOLD,-19,-20,4,-27,15,-18,14,10,-2,26,-18,8);
        if (inside) {
            polygon(c,0xFF51423C,-16,-16,4,-23,12,-15,10,8,-2,22,-15,6);
            bone(c,-11,-9,10,-9,4f,LEATHER);
            bone(c,-11,4,10,4,4f,LEATHER);
            bone(c,-3,4,5,4,2.5f,0xFFAE8359);
        } else {
            polygon(c,DEEP_BLUE,-16,-16,4,-23,12,-15,10,8,-2,22,-15,6);
            polygon(c,0xFF4C6C87,-9,-14,3,-20,6,-13,4,13,-5,17);
            polygon(c,GOLD,-1,-17,3,-4,16,1,3,5,-1,19,-5,5,-17,1,-5,-4);
            joint(c,-1,1,3f,LIGHT);
        }
        c.restore();
    }
    private void drawShieldStraps(Canvas c,float elbowX,float elbowY,float handX,float handY) {
        c.save();
        c.translate(handX,handY);
        c.rotate((float)Math.toDegrees(Math.atan2(elbowX-handX,handY-elbowY)));
        for (int y = -17; y <= -7; y += 10) {
            shape.reset();
            shape.moveTo(-6f,y-1.2f);
            shape.quadTo(0,y+2.4f,6f,y-1.3f);
            shape.lineTo(6f,y+1.3f);
            shape.quadTo(0,y+5f,-6f,y+1.7f);
            shape.close();
            p.setColor(0xFF633F29);
            c.drawPath(shape,p);
            stripe(c,0xFFBD9161,1f,-1.9f,y+2.1f,1.2f,y+2.2f);
        }
        c.restore();
    }

    private static float rotateX(float x,float y,float radians) {
        return (float)(x*Math.cos(radians)-y*Math.sin(radians));
    }

    private static float rotateY(float x,float y,float radians) {
        return (float)(x*Math.sin(radians)+y*Math.cos(radians));
    }

    private void drawSword(Canvas c,float handX,float handY,float angle){
        c.save();
        c.translate(handX,handY);
        c.rotate(angle);
        bone(c,0,-7,0,8,4.5f,LEATHER);
        joint(c,0,-7f,2.5f,GOLD);
        stripe(c,GOLD,3f,-7,6.7f,7,6.7f);
        polygon(c,SHADOW,-3.5f,7,3.5f,7,2.5f,45,0,BLADE_TIP_Y,-2.5f,45);
        polygon(c,0xFFDDE6E8,-1.7f,9,1.7f,9,.8f,49,-1.7f,44);
        stripe(c,LIGHT,1f,0,11,0,44);
        c.restore();
    }
    private void drawSwordGrip(Canvas c,float x,float y,float angle) {
        c.save();
        c.translate(x,y);
        c.rotate(angle);
        // A bent thumb and knuckles wrap over the grip just above the guard.
        shape.reset();
        shape.moveTo(-3.2f,-4.5f);
        shape.quadTo(-1.1f,-5.7f,1.3f,-4.6f);
        shape.quadTo(3.7f,-3.8f,3.8f,-1.8f);
        shape.quadTo(4.1f,.4f,3.1f,2f);
        shape.quadTo(3.3f,4f,1.1f,4.9f);
        shape.quadTo(-1.7f,5.4f,-3.3f,3.7f);
        shape.quadTo(-4.9f,.2f,-3.2f,-4.5f);
        shape.close();
        p.setColor(0xFF352C27);
        c.drawPath(shape,p);
        oval(c,0xFF635047,-3.3f,-3.7f,-.4f,2.8f);
        stripe(c,0xFF836B58,.48f,-.6f,-2.9f,2.4f,-2.1f);
        stripe(c,0xFF786355,.48f,-.1f,-.8f,2.8f,-.2f);
        stripe(c,0xFF6E594B,.45f,.1f,1.3f,2.4f,1.8f);
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