package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;

/** Directional neutral artwork bound to the Explorer's existing limb poses. */
final class ElfBaseArt {
    private static final int BODY_WIDTH = 288;
    private static final int ATLAS_HEIGHT = 400;
    private static final int WIDTH = 128, HEIGHT = 320, COLUMNS = 4, ROWS = 40;
    private static final float UNIT = 1f / 4.3f;
    private static final int CENTER_X = 271, SOLE_Y = 1192;

    // Coordinates within each limb cell, followed by the cell's origin in the
    // facing artwork. Rear arm art is flipped to match the renderer's projection.
    private static final int[][] FRONT = {
        {50,47,35,141,32,229,145,672}, {37,47,53,141,59,229,311,672},
        {55,48,58,163,52,277,177,845}, {60,48,53,163,52,277,254,845}
    };
    private static final int[][] RIGHT = {
        {56,53,49,149,60,235,192,664}, {56,53,49,149,60,235,192,664},
        {54,45,61,160,58,278,208,845}, {54,45,61,160,58,278,208,845}
    };
    private static final int[][] BACK = {
        {84,43,74,137,58,215,111,674}, {73,43,86,137,96,215,279,674},
        {56,51,65,161,59,276,176,847}, {54,51,50,161,52,276,254,847}
    };
    private static final int[][] LEFT = {
        {59,55,59,154,63,234,190,658}, {59,55,59,154,63,234,190,658},
        {58,43,61,158,59,276,208,845}, {58,43,61,158,59,276,208,845}
    };

    private final Facing[] facings = new Facing[4];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bootBounds = new RectF();
    private final RectF bodyBounds = new RectF(
        (125 - CENTER_X) * UNIT, (510 - SOLE_Y) * UNIT,
        (125 + BODY_WIDTH - CENTER_X) * UNIT, (510 + ATLAS_HEIGHT - SOLE_Y) * UNIT);

    ElfBaseArt(Context context) {
        int[][][] registrations = {FRONT, RIGHT, BACK, LEFT};
        for (int facing = 0; facing < facings.length; facing++) {
            try (InputStream stream = context.getAssets().open("elf_base_facing_" + facing + ".webp")) {
                Bitmap atlas = BitmapFactory.decodeStream(stream);
                if (atlas != null && atlas.getWidth() == BODY_WIDTH + 4 * WIDTH
                    && atlas.getHeight() == ATLAS_HEIGHT) {
                    facings[facing] = new Facing(atlas, registrations[facing], facing);
                }
            } catch (IOException ignored) {
                // The segmented renderer remains available for missing artwork.
            }
        }
    }

    boolean ready(int facing) {
        return facings[facing] != null;
    }

    void drawBody(Canvas canvas, int facing) {
        canvas.drawBitmap(facings[facing].body, null, bodyBounds, paint);
    }

    void drawArm(Canvas canvas, int facing, int index,
                 float shoulderX, float shoulderY, float elbowX, float elbowY,
                 float wristX, float wristY) {
        facings[facing].limbs[index].draw(canvas, paint,
            shoulderX - RigViewProfile.shoulderX(facing, index), shoulderY + 108f,
            elbowX - RigViewProfile.elbowX(facing, index), elbowY - RigViewProfile.elbowY(facing, index),
            wristX - RigViewProfile.handX(facing, index), wristY - RigViewProfile.handY(facing, index));
    }

    void drawLeg(Canvas canvas, int facing, int index, RigLegPose.Pose pose,
                 float pelvisX, float pelvisY) {
        Facing view = facings[facing];
        RigLegPose.Pose rest = view.rest[index];
        Sprite limb = view.limbs[index + 2];
        float footX = pose.footX - rest.footX;
        float footY = pose.footY - rest.footY;
        float kneeY = pose.kneeY - rest.kneeY;
        if (facing == 2) {
            // The rear projection foreshortens the lift. Keep it within the
            // authored thigh length so the knee cannot rise into the shorts.
            float hipY = pose.hipY - rest.hipY;
            kneeY = hipY + (kneeY - hipY) * .55f;
        }
        limb.draw(canvas, paint, pelvisX, pelvisY,
            pose.kneeX - rest.kneeX, kneeY, footX, footY);

        // Keep the boot grounded and rigid while the calf deforms around the ankle.
        float x = (limb.sourceX - CENTER_X) * UNIT + footX;
        float y = (1116 - SOLE_Y) * UNIT + footY;
        bootBounds.set(x, y, x + WIDTH * UNIT, y + (ATLAS_HEIGHT - HEIGHT) * UNIT);
        canvas.drawBitmap(view.boots[index], null, bootBounds, paint);
    }

    private static final class Facing {
        final Bitmap body;
        final Sprite[] limbs = new Sprite[4];
        final Bitmap[] boots = new Bitmap[2];
        final RigLegPose.Pose[] rest = new RigLegPose.Pose[2];

        Facing(Bitmap atlas, int[][] registration, int facing) {
            body = Bitmap.createBitmap(atlas, 0, 0, BODY_WIDTH, ATLAS_HEIGHT);
            // The two independently painted profiles have different overlap at the hem.
            float rearHipFit = facing == 1 ? 1.65f : facing == 3 ? .85f : 0f;
            for (int i = 0; i < limbs.length; i++) {
                int x = BODY_WIDTH + i * WIDTH;
                limbs[i] = new Sprite(Bitmap.createBitmap(atlas, x, 0, WIDTH, HEIGHT),
                    registration[i], i >= 2, i >= 2 ? rearHipFit : 0f);
            }
            for (int i = 0; i < boots.length; i++) {
                int x = BODY_WIDTH + (i + 2) * WIDTH;
                boots[i] = Bitmap.createBitmap(atlas, x, HEIGHT, WIDTH, ATLAS_HEIGHT - HEIGHT);
                rest[i] = RigViewProfile.isSide(facing) ? RigLegPose.solveProfile(0f, i, 0f, 0f)
                    : RigLegPose.solve(0f, i, facing, 0f, 0f);
            }
        }
    }

    private static final class Sprite {
        final Bitmap bitmap;
        final LimbMesh mesh;
        final int sourceX;
        private final float rootX, rootY, jointX, jointY, tipX, tipY;

        Sprite(Bitmap bitmap, int[] points, boolean attached, float rearHipFit) {
            this.bitmap = bitmap;
            sourceX = points[6];
            int y = points[7];
            rootX = (sourceX + points[0] - CENTER_X) * UNIT;
            rootY = (y + points[1] - SOLE_Y) * UNIT;
            jointX = (sourceX + points[2] - CENTER_X) * UNIT;
            jointY = (y + points[3] - SOLE_Y) * UNIT;
            tipX = (sourceX + points[4] - CENTER_X) * UNIT;
            tipY = (y + points[5] - SOLE_Y) * UNIT;
            mesh = new LimbMesh(WIDTH, HEIGHT, COLUMNS, ROWS, UNIT,
                points[0], points[1], points[2], points[3], points[4], points[5], attached, rearHipFit);
        }

        void draw(Canvas canvas, Paint paint, float dx0, float dy0, float dx1, float dy1,
                  float dx2, float dy2) {
            mesh.pose(rootX + dx0, rootY + dy0, jointX + dx1, jointY + dy1, tipX + dx2, tipY + dy2);
            canvas.drawBitmapMesh(bitmap, COLUMNS, ROWS, mesh.vertices, 0, null, 0, paint);
        }
    }
}
