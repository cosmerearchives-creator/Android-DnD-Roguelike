package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
    private final Paint fistPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path fistShape = new Path();
    private final float[] shoulderPoint = new float[2];
    private final RigTorsoPose.Mesh bodyMesh = new RigTorsoPose.Mesh();
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

    void drawBody(Canvas canvas, int facing, RigTorsoPose pose) {
        bodyMesh.pose(pose, bodyBounds.left, bodyBounds.top, bodyBounds.right, bodyBounds.bottom);
        canvas.drawBitmapMesh(facings[facing].body, RigTorsoPose.Mesh.COLUMNS, RigTorsoPose.Mesh.ROWS,
            bodyMesh.vertices, 0, null, 0, paint);
    }

    void shoulderOffset(int facing, int index, RigTorsoPose pose, float[] offsets, int at) {
        Sprite arm = facings[facing].limbs[index];
        float mirror = facing == 2 ? -1f : 1f;
        pose.offsets(arm.rootX * mirror, arm.rootY, offsets, at);
        offsets[at] *= mirror;
    }

    void drawArm(Canvas canvas, int facing, int index, RigTorsoPose torso,
                 float shoulderX, float shoulderY, float elbowX, float elbowY,
                 float wristX, float wristY, boolean fist) {
        Sprite arm = facings[facing].limbs[index];
        float elbowDx = elbowX - RigViewProfile.elbowX(facing, index);
        float elbowDy = elbowY - RigViewProfile.elbowY(facing, index);
        float wristDx = wristX - RigViewProfile.handX(facing, index);
        float wristDy = wristY - RigViewProfile.handY(facing, index);
        arm.pose(shoulderX - RigViewProfile.shoulderX(facing, index), shoulderY + 108f,
            elbowDx, elbowDy, wristDx, wristDy);
        if (!RigViewProfile.isSide(facing)) {
            // Skin at the top of the arm follows the same deformation as its armhole.
            torso.bindShoulder(arm.mesh.vertices, (arm.sourceX - CENTER_X) * UNIT,
                (arm.sourceY - SOLE_Y) * UNIT, WIDTH * UNIT, HEIGHT * UNIT, COLUMNS, ROWS,
                arm.rootX, arm.rootY, facing == 2 ? -1f : 1f, shoulderPoint);
        }
        if (fist && arm.closedHandArm != null) {
            arm.draw(canvas, paint, arm.closedHandArm);
            drawFist(canvas, arm.jointX + elbowDx, arm.jointY + elbowDy,
                arm.tipX + wristDx, arm.tipY + wristDy);
        } else {
            arm.draw(canvas, paint, arm.bitmap);
        }
    }

    private void drawFist(Canvas canvas, float elbowX, float elbowY, float wristX, float wristY) {
        canvas.save();
        canvas.translate(wristX, wristY);
        canvas.rotate((float) Math.toDegrees(Math.atan2(wristY - elbowY, wristX - elbowX)));
        fistShape.reset();
        fistShape.moveTo(-1.4f, -2.15f);
        fistShape.lineTo(2f, -2.85f);
        fistShape.quadTo(5.3f, -3.4f, 5.8f, -.9f);
        fistShape.quadTo(6.1f, 1.9f, 4.7f, 2.6f);
        fistShape.lineTo(1.8f, 2.7f);
        fistShape.lineTo(-1.4f, 1.8f);
        fistShape.close();
        fistPaint.setColor(0xFF302E35);
        canvas.drawPath(fistShape, fistPaint);
        fistShape.reset();
        fistShape.moveTo(2.8f, -2.2f);
        fistShape.quadTo(5.35f, -3f, 5.55f, -.85f);
        fistShape.quadTo(5.95f, 1.05f, 4.75f, 1.9f);
        fistShape.lineTo(3.5f, 2.05f);
        fistShape.quadTo(2.45f, .45f, 2.8f, -2.2f);
        fistShape.close();
        fistPaint.setColor(0xFFC59063);
        canvas.drawPath(fistShape, fistPaint);
        fistPaint.setColor(0xFFE5B286);
        canvas.drawOval(3.1f, -2.25f, 5.1f, -1.05f, fistPaint);
        canvas.restore();
    }

    void drawLeg(Canvas canvas, int facing, int index, RigLegPose.Pose pose,
                 float pelvisX, float pelvisY, float kneeSway) {
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
        limb.pose(pelvisX, pelvisY, pose.kneeX - rest.kneeX + kneeSway, kneeY, footX, footY);
        limb.draw(canvas, paint, limb.bitmap);

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
                    registration[i], i >= 2, i >= 2 ? rearHipFit : 0f, i == 1);
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
        final Bitmap bitmap, closedHandArm;
        final LimbMesh mesh;
        final int sourceX, sourceY;
        private final float rootX, rootY, jointX, jointY, tipX, tipY;

        Sprite(Bitmap bitmap, int[] points, boolean attached, float rearHipFit, boolean punchingArm) {
            this.bitmap = bitmap;
            if (punchingArm) {
                // Keep the sleeve and wrist. The closed fist is drawn at the posed wrist.
                closedHandArm = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
                Canvas cutout = new Canvas(closedHandArm);
                cutout.clipRect(0, 0, WIDTH, points[5] + 3);
                cutout.drawBitmap(bitmap, null, new RectF(0, 0, WIDTH, HEIGHT), null);
            } else {
                closedHandArm = null;
            }
            sourceX = points[6];
            sourceY = points[7];
            rootX = (sourceX + points[0] - CENTER_X) * UNIT;
            rootY = (sourceY + points[1] - SOLE_Y) * UNIT;
            jointX = (sourceX + points[2] - CENTER_X) * UNIT;
            jointY = (sourceY + points[3] - SOLE_Y) * UNIT;
            tipX = (sourceX + points[4] - CENTER_X) * UNIT;
            tipY = (sourceY + points[5] - SOLE_Y) * UNIT;
            mesh = new LimbMesh(WIDTH, HEIGHT, COLUMNS, ROWS, UNIT,
                points[0], points[1], points[2], points[3], points[4], points[5], attached, rearHipFit);
        }

        void pose(float dx0, float dy0, float dx1, float dy1, float dx2, float dy2) {
            mesh.pose(rootX + dx0, rootY + dy0, jointX + dx1, jointY + dy1, tipX + dx2, tipY + dy2);
        }

        void draw(Canvas canvas, Paint paint, Bitmap image) {
            canvas.drawBitmapMesh(image, COLUMNS, ROWS, mesh.vertices, 0, null, 0, paint);
        }
    }
}
