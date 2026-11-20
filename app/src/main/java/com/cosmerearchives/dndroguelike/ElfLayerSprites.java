package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * The visual character's rig-independent, illustrated layers. Both the base
 * undergarment and armor assets share 4 direction columns and 5 body-part rows:
 * torso, upper arm, forearm, thigh, shin/boot.
 *
 * Base and armored illustrations use the same skeleton. Their painted joint
 * landmarks are registered independently so their visible seams meet the joints.
 * The head layer is handled by RiggedElf.
 */
final class ElfLayerSprites {
    static final int TORSO=0, UPPER_ARM=1, FOREARM=2, THIGH=3, SHIN=4;
    static final float COLLAR_Y = -122f;
    private static final int CELL=192;
    private static final int BOOT_TOP_ROW = 111;
    private static final int BASE_BOOT_TOP_ROW = 119;
    private static final int ANKLE_ROW = 128;
    private static final float BOOT_Y_SCALE = .255f;
    private static final float SHIN_WIDTH = 16f;
    // Pixel centers of the proximal and distal joints in each 192px cell.
    // Order: front, right profile, back. Left uses the mirrored right profile.
    // Rows: upper arm, forearm, thigh, shin. The shin ends at the sole.
    private static final int[][][] JOINTS = {
        {{118, 39, 65, 145}, {96, 32, 108, 152}, {97, 32, 94, 159}},
        {{101, 23, 72, 136}, {69, 24, 112, 137}, {101, 23, 76, 137}},
        {{110, 45, 99, 169}, {108, 48, 91, 170}, {103, 49, 94, 169}},
        {{94, 41, 96, 183}, {80, 33, 63, 183}, {98, 33, 97, 183}},
    };
    // Armor cells include their own skin and gloves, with different landmarks.
    private static final int[][][] ARMOR_JOINTS = {
        {{118, 47, 65, 149}, {94, 44, 106, 154}, {110, 50, 79, 155}},
        {{104, 25, 83, 151}, {70, 26, 115, 150}, {103, 26, 79, 151}},
        {{110, 45, 99, 161}, {108, 48, 91, 156}, {94, 48, 92, 151}},
        {{94, 30, 96, 176}, {80, 32, 63, 176}, {98, 32, 97, 179}},
    };
    // Interior skin samples keep the exposed joints in the same painted style
    // as the rest of each limb. Coordinates are relative to their atlas cell.
    private static final int[][] CALF_SKIN = {{69,76,130,149},{65,82,120,149},{69,76,129,150}};
    private static final int[][] FOREARM_SKIN = {{72,52,108,116},{67,50,127,126},{63,51,128,128}};
    private final Path clip = new Path();
    private final Path skin = new Path();
    private final Paint skinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Shader skinTone = new LinearGradient(-7f,0,7f,0,
        new int[]{0xFFAB755D,0xFFD6A686,0xFFF0C8AC,0xFFD6A180,0xFF9E694D},
        new float[]{0f,.18f,.42f,.72f,1f},Shader.TileMode.CLAMP);
    private final Bitmap base,armor,frontSkin;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect src=new Rect();
    private final RectF dest=new RectF();
    private final Matrix jointTransform = new Matrix();
    private final float[] matrixValues = {1f,0f,0f, 0f,1f,0f, 0f,0f,1f};

    ElfLayerSprites(Context context) {
        base=decode(context,"elf_base_layers.webp");
        armor=decode(context,"elf_armor_layers.webp");
        frontSkin=decode(context,"elf_front_lower_limbs.webp",CELL*3,CELL);
        skinPaint.setShader(skinTone);
    }

    private static Bitmap decode(Context ctx,String name) {
        return decode(ctx,name,CELL*4,CELL*5);
    }

    private static Bitmap decode(Context ctx,String name,int width,int height) {
        try(java.io.InputStream stream=ctx.getAssets().open(name)){
            Bitmap result=BitmapFactory.decodeStream(stream);
            return result!=null && result.getWidth()==width
                && result.getHeight()==height ? result : null;
        }catch(java.io.IOException ignored){return null;}
    }

    boolean ready(){return base!=null;}

    private static int column(int facing) {
        return facing == 3 ? 1 : facing;
    }

    private void selectCell(int column, int part) {
        int x = column * CELL;
        int y = part * CELL;
        src.set(x, y, x + CELL, y + CELL);
    }

    boolean torso(Canvas canvas,int facing,float halfWidth,boolean armored) {
        if (base == null) return false;
        int column = column(facing);
        float width = halfWidth * 2f + 4f;
        float yScale = 56f / (184f - 8f);
        float xScale = width / 170f;
        selectCell(column, TORSO);
        boolean hasArmor = armored && armor != null;
        canvas.save();
        canvas.translate(-CELL*xScale*.5f, COLLAR_Y - (hasArmor ? 15f : 8f)*yScale);
        canvas.scale(xScale,yScale);
        dest.set(0, 0, CELL, CELL);
        if (hasArmor && column == 1) {
            // Exclude the painted black arm socket. The moving upper arm and
            // the fitted shoulder fabric behind it supply the actual joint.
            clip.reset();
            clip.setFillType(Path.FillType.EVEN_ODD);
            clip.addRect(0,0,CELL,CELL,Path.Direction.CW);
            clip.moveTo(54,36);
            clip.cubicTo(69,24,102,24,120,35);
            clip.cubicTo(127,46,110,54,98,62);
            clip.cubicTo(80,78,51,77,48,54);
            clip.quadTo(46,44,54,36);
            clip.close();
            canvas.clipPath(clip);
        }
        if (hasArmor && column == 2) {
            // Give the rear skirt a curved hem. The centered tabard covers the join.
            clip.reset();
            clip.setFillType(Path.FillType.WINDING);
            clip.moveTo(0,0); clip.lineTo(CELL,0); clip.lineTo(CELL,153);
            clip.quadTo(151,178,96,175);
            clip.quadTo(41,178,0,153); clip.close();
            canvas.clipPath(clip);
        }
        canvas.drawBitmap(hasArmor ? armor : base, src, dest, paint);
        canvas.restore();
        return true;
    }

    void rearTabard(Canvas canvas) {
        if (armor == null) return;
        // The rear thigh cell contains a cloth panel along its right edge.
        // Use it once, anchored to the belt, so it doesn't follow either leg.
        src.set(384+124,576+28,384+176,576+182);
        dest.set(-9f,-84f,0f,-41f);
        clip.reset();
        clip.setFillType(Path.FillType.WINDING);
        clip.moveTo(-9f,-84f); clip.lineTo(9f,-84f);
        clip.lineTo(7f,-54f); clip.lineTo(0f,-41f);
        clip.lineTo(-7f,-54f); clip.close();
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawBitmap(armor, src, dest, paint);
        canvas.scale(-1f,1f);
        canvas.drawBitmap(armor, src, dest, paint);
        canvas.restore();
    }

    void shinAndBoot(Canvas canvas,int facing,int index,float kneeX,float kneeY,
                     float footX,float footY,boolean armored) {
        int column = column(facing);
        boolean hasArmor = armored && armor != null;
        int[] joint = (hasArmor ? ARMOR_JOINTS : JOINTS)[SHIN - 1][column];
        int bootTop = hasArmor ? BOOT_TOP_ROW : BASE_BOOT_TOP_ROW;
        if (hasArmor) {
            float ankleY = footY - (joint[3] - ANKLE_ROW)*BOOT_Y_SCALE;
            limb(canvas,facing,SHIN,index,kneeX,kneeY,footX,ankleY,SHIN_WIDTH,true);
        } else {
            float cuffY = footY - (joint[3] - bootTop)*BOOT_Y_SCALE;
            canvas.save();
            canvas.translate(kneeX,kneeY);
            float dx = footX-kneeX, dy = cuffY+3f-kneeY;
            canvas.rotate((float)Math.toDegrees(Math.atan2(dy,dx))-90f);
            if (index == 1 && !RigViewProfile.isSide(facing)) canvas.scale(-1f,1f);
            drawExposedSkin(canvas,(float)Math.hypot(dx,dy),true,column);
            canvas.restore();
        }

        // The boot is a rigid piece registered at the sole.
        selectCell(column, SHIN);
        src.top += bootTop;
        float xScale = SHIN_WIDTH / 155f;
        float left = -joint[2]*xScale;
        dest.set(left,(bootTop-joint[3])*BOOT_Y_SCALE,
            left+CELL*xScale,(CELL-joint[3])*BOOT_Y_SCALE);
        canvas.save();
        canvas.translate(footX,footY);
        if (index == 1 && !RigViewProfile.isSide(facing)) canvas.scale(-1f,1f);
        canvas.drawBitmap(hasArmor ? armor : base,src,dest,paint);
        canvas.restore();
    }

    private void drawExposedSkin(Canvas canvas,float length,boolean calf,int column) {
        if (column == 0 && frontSkin != null) {
            int artX = calf ? CELL : 0;
            float halfWidth = calf ? 5.9f : 4.65f;
            src.set(artX,0,artX+CELL,CELL);
            dest.set(-halfWidth,calf ? -1.7f : -4.2f,halfWidth,length+2f);
            canvas.drawBitmap(frontSkin,src,dest,paint);
            return;
        }
        float kneeOrElbow = calf ? 3.8f : 3.6f;
        float belly = calf ? 6.4f : 5f;
        float end = calf ? 3.2f : 3.5f;
        skin.reset();
        skin.moveTo(-kneeOrElbow,0);
        skin.quadTo(0,-5f,kneeOrElbow,0);
        skin.cubicTo(kneeOrElbow,length*.14f,belly,length*.38f,belly-.3f,length*.52f);
        skin.quadTo(belly-.5f,length*.74f,end,length);
        skin.quadTo(0,length+2.5f,-end,length);
        skin.quadTo(-belly+.5f,length*.74f,-belly+.3f,length*.52f);
        skin.cubicTo(-belly,length*.38f,-kneeOrElbow,length*.14f,-kneeOrElbow,0);
        skin.close();
        canvas.drawPath(skin,skinPaint);

        int[] sample = (calf ? CALF_SKIN : FOREARM_SKIN)[column];
        int ox = column*CELL, oy = (calf ? THIGH : UPPER_ARM)*CELL;
        src.set(ox+sample[0],oy+sample[1],ox+sample[2],oy+sample[3]);
        dest.set(-belly,-3f,belly,length+2f);
        canvas.save();
        canvas.clipPath(skin);
        paint.setAlpha(200);
        canvas.drawBitmap(base,src,dest,paint);
        paint.setAlpha(255);
        canvas.restore();
    }

    void limb(Canvas canvas,int facing,int part,int index,
              float x1,float y1,float x2,float y2,float width,boolean armored) {
        limb(canvas,facing,part,index,x1,y1,x2,y2,width,armored,false);
    }

    void limb(Canvas canvas,int facing,int part,int index,
              float x1,float y1,float x2,float y2,float width,boolean armored,boolean gripping) {
        float dx=x2-x1,dy=y2-y1;
        float length=(float)Math.hypot(dx,dy);
        if (length < 2f) return;
        int column = column(facing);
        boolean hasArmor = armored && armor != null;
        // The old rear-facing cell shows a palm. Use the dorsal glove art
        // with the existing rear arm mirroring, so the thumbs face inward.
        int artColumn = !hasArmor && part == FOREARM && column == 2 ? 0 : column;
        int[] joint = (hasArmor ? ARMOR_JOINTS : JOINTS)[part - 1][artColumn];
        float dyPixels = (part == SHIN ? ANKLE_ROW : joint[3]) - joint[1];
        float xScale = width / 155f;
        float yScale = length / dyPixels;
        // Remove the slant in the source drawing so both art joints land on the bone.
        float shear = -(joint[2] - joint[0]) * xScale / dyPixels;
        matrixValues[0] = xScale;
        matrixValues[1] = shear;
        matrixValues[2] = -joint[0] * xScale - joint[1] * shear;
        matrixValues[4] = yScale;
        matrixValues[5] = -joint[1] * yScale;
        jointTransform.setValues(matrixValues);

        canvas.save();
        canvas.translate(x1,y1);
        canvas.rotate((float)Math.toDegrees(Math.atan2(dy,dx))-90f);
        if (index == 1 && !RigViewProfile.isSide(facing)) canvas.scale(-1f, 1f);
        clip.reset();
        clip.setFillType(Path.FillType.WINDING);
        if (part == UPPER_ARM || part == THIGH && !hasArmor) {
            // Taper the bare end of each upper segment into the cuff below it.
            float taper = part == UPPER_ARM ? 5.5f : 4.2f;
            float radius = part == UPPER_ARM ? 4.2f : column == 1 ? 3.8f : 4.65f;
            clip.moveTo(-25,-25);
            clip.lineTo(25,-25); clip.lineTo(25,length-taper-2f);
            clip.cubicTo(radius+3f,length-taper,radius+1f,length-2f,radius,length+.7f);
            clip.quadTo(0,length+3.8f,-radius,length+.7f);
            clip.cubicTo(-radius-1f,length-2f,-radius-3f,length-taper,-25,length-taper-2f);
            clip.close();
            canvas.clipPath(clip);
        }
        if (part == FOREARM || part == SHIN) {
            // Expose the elbow in the base layer while keeping the existing
            // wrist and finger registration farther down the same art cell.
            float cuffFraction = column == 0 ? .63f : .49f;
            float cuffStart = part == FOREARM && !hasArmor ? length*cuffFraction : 0f;
            if (cuffStart > 0f) drawExposedSkin(canvas,cuffStart+6f,false,column);
            float rim = part == SHIN ? 2.4f : 1.5f;
            float rimEnds = part == SHIN ? 2.6f : 1.4f;
            clip.reset();
            clip.moveTo(-25,cuffStart+rimEnds);
            clip.cubicTo(-7f,cuffStart+rimEnds,-4.6f,cuffStart-rim,0,cuffStart-rim);
            clip.cubicTo(4.6f,cuffStart-rim,7f,cuffStart+rimEnds,25,cuffStart+rimEnds);
            clip.lineTo(25,length+20f); clip.lineTo(-25,length+20f); clip.close();
            canvas.clipPath(clip);
        }
        canvas.concat(jointTransform);
        if (hasArmor && part == THIGH && column == 2) {
            // Only the plate and its strap are worn by this leg. The cloth is
            // attached separately at the waist and the base thigh supplies skin.
            clip.reset();
            clip.moveTo(0,18); clip.lineTo(117,18); clip.lineTo(115,69);
            clip.lineTo(104,107); clip.quadTo(96,123,82,124);
            clip.lineTo(45,118); clip.lineTo(0,118); clip.close();
            canvas.clipPath(clip);
        }
        if (part == SHIN) canvas.clipRect(0,0,CELL,ANKLE_ROW+6);
        if (gripping && hasArmor && part == FOREARM) {
            canvas.clipRect(0,0,CELL,joint[3]+5f);
        }
        Bitmap atlas = hasArmor ? armor : base;
        selectCell(artColumn, part);
        if (!hasArmor && part == THIGH && column == 0 && frontSkin != null) {
            // Soften the end of the painted thigh where it overlaps the new shin.
            atlas = frontSkin;
            src.set(CELL*2,0,CELL*3,CELL);
        }
        dest.set(0, 0, CELL, CELL);
        canvas.drawBitmap(atlas, src, dest, paint);
        canvas.restore();
    }
}
