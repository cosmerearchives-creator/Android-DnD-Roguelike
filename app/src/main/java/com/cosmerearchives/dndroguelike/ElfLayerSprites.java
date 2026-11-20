package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

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
    private final Path clip = new Path();
    private final Bitmap base,armor;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect src=new Rect();
    private final RectF dest=new RectF();
    private final Matrix jointTransform = new Matrix();
    private final float[] matrixValues = {1f,0f,0f, 0f,1f,0f, 0f,0f,1f};

    ElfLayerSprites(Context context) {
        base=decode(context,"elf_base_layers.webp");
        armor=decode(context,"elf_armor_layers.webp");
    }

    private static Bitmap decode(Context ctx,String name) {
        try(java.io.InputStream stream=ctx.getAssets().open(name)){
            Bitmap result=BitmapFactory.decodeStream(stream);
            return result!=null && result.getWidth()==CELL*4
                && result.getHeight()==CELL*5 ? result : null;
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

    void limb(Canvas canvas,int facing,int part,int index,
              float x1,float y1,float x2,float y2,float width,boolean armored) {
        float dx=x2-x1,dy=y2-y1;
        float length=(float)Math.hypot(dx,dy);
        if (length < 2f) return;
        int column = column(facing);
        boolean hasArmor = armored && armor != null;
        int[] joint = (hasArmor ? ARMOR_JOINTS : JOINTS)[part - 1][column];
        float dyPixels = joint[3] - joint[1];
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
        if (part == UPPER_ARM) {
            // Round off the skin and remove the extra cuff embedded in the
            // upper-arm art. The bracer is drawn over this rounded endpoint.
            clip.reset();
            clip.setFillType(Path.FillType.WINDING);
            clip.moveTo(-24,-20);
            clip.lineTo(24,-20); clip.lineTo(24,length-9f);
            clip.quadTo(4.5f,length-5f,3.5f,length+1f);
            clip.quadTo(0,length+3f,-3.5f,length+1f);
            clip.quadTo(-4.5f,length-5f,-24,length-9f); clip.close();
            canvas.clipPath(clip);
        }
        canvas.concat(jointTransform);
        if (hasArmor && part == THIGH && column == 2) {
            clip.reset();
            clip.setFillType(Path.FillType.WINDING);
            clip.moveTo(0,18); clip.lineTo(119,18); clip.lineTo(116,83);
            clip.lineTo(108,137); clip.lineTo(128,163);
            clip.lineTo(0,181); clip.close();
            canvas.clipPath(clip);
        }
        if (!hasArmor && part == SHIN) {
            // Knock the stray corners off the skin/boot seam without trimming
            // the cuff into a horizontal strip.
            clip.reset();
            clip.setFillType(Path.FillType.WINDING);
            clip.moveTo(0,joint[1]+8f);
            clip.quadTo(joint[0],joint[1]-19f,CELL,joint[1]+8f);
            clip.lineTo(CELL,CELL); clip.lineTo(0,CELL); clip.close();
            canvas.clipPath(clip);
        }
        selectCell(column, part);
        dest.set(0, 0, CELL, CELL);
        canvas.drawBitmap(hasArmor ? armor : base, src, dest, paint);
        canvas.restore();
    }
}
