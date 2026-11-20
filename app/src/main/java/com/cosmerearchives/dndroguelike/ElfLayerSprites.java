package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * The visual character's rig-independent, illustrated layers. Both the base
 * undergarment and armor assets share 4 direction columns and 5 body-part rows:
 * torso, upper arm, forearm, thigh, shin/boot.
 *
 * Bones, stride speed, knee positions and attack transforms are never changed by
 * the armor toggle. Base art always renders FIRST, armor over it at the same
 * anchors. The head layer is handled by RiggedElf.
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

    private void paintLayers(Canvas canvas, boolean armored) {
        canvas.drawBitmap(base, src, dest, paint);
        if (armored && armor != null) canvas.drawBitmap(armor, src, dest, paint);
    }

    boolean torso(Canvas canvas,int facing,float halfWidth,boolean armored) {
        if (base == null) return false;
        float width = halfWidth * 2f + 4f;
        selectCell(column(facing), TORSO);
        // The atlas collar starts at row 8 and its original hem around row 184.
        float yScale = 56f / (184f - 8f);
        float xScale = width / 170f;
        float top = COLLAR_Y - 8f * yScale;
        float half = CELL * xScale * .5f;
        dest.set(-half, top, half, top + CELL * yScale);
        paintLayers(canvas, armored);
        return true;
    }

    void limb(Canvas canvas,int facing,int part,int index,
              float x1,float y1,float x2,float y2,float width,boolean armored) {
        float dx=x2-x1,dy=y2-y1;
        float length=(float)Math.hypot(dx,dy);
        if (length < 2f) return;
        int column = column(facing);
        int[] joint = JOINTS[part - 1][column];
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
        canvas.concat(jointTransform);
        selectCell(column, part);
        dest.set(0, 0, CELL, CELL);
        paintLayers(canvas, armored);
        canvas.restore();
    }
}
