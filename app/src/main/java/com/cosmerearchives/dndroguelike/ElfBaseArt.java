package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
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
    private static final int[][][] FEMALE = {
        {{56,45,43,142,30,232,146,666}, {46,45,53,142,70,232,300,666},
         {60,45,60,161,57,272,174,847}, {56,45,49,161,50,272,256,847}},
        {{65,46,61,145,73,235,183,661}, {65,46,61,145,73,235,183,661},
         {57,41,66,163,68,292,205,847}, {57,41,66,163,68,292,205,847}},
        {{86,43,74,136,63,221,111,670}, {74,42,85,136,101,221,267,670},
         {56,49,62,160,56,273,179,847}, {48,49,51,160,56,273,260,847}},
        {{64,46,61,145,73,234,184,659}, {64,46,61,145,73,234,184,659},
         {60,39,65,162,68,292,204,847}, {60,39,65,162,68,292,204,847}}
    };

    private static final int[][][] MALE = {
        {{70,47,55,138,45,227,128,626}, {57,47,74,136,84,226,296,626},
         {55,38,51,137,48,275,179,841}, {52,39,57,135,55,275,255,841}},
        {{63,42,59,143,75,225,192,630}, {63,42,59,143,75,225,192,630},
         {49,38,54,138,66,295,227,842}, {49,38,54,138,66,295,227,842}},
        {{72,42,57,136,43,222,121,631}, {57,41,72,137,85,222,283,631},
         {57,40,58,141,51,275,177,841}, {51,40,55,139,54,275,256,841}},
        {{63,42,59,143,75,225,192,630}, {63,42,59,143,75,225,192,630},
         {50,38,55,139,66,295,226,842}, {50,38,55,139,66,295,226,842}}
    };

    private final Facing[] facings = new Facing[4];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ElfFistArt fistArt = new ElfFistArt();
    private final float[] shoulderPoint = new float[2];
    private final RigTorsoPose.Mesh bodyMesh = new RigTorsoPose.Mesh();
    private final RectF bootBounds = new RectF();
    private final RectF bodyBounds = new RectF(
        (125 - CENTER_X) * UNIT, (510 - SOLE_Y) * UNIT,
        (125 + BODY_WIDTH - CENTER_X) * UNIT, (510 + ATLAS_HEIGHT - SOLE_Y) * UNIT);

    ElfBaseArt(Context context, boolean male) {
        int[][][] registrations = male ? MALE : FEMALE;
        String prefix = male ? "elf_male_facing_" : "elf_base_facing_";
        for (int facing = 0; facing < facings.length; facing++) {
            try (InputStream stream = context.getAssets().open(prefix + facing + ".webp")) {
                Bitmap atlas = BitmapFactory.decodeStream(stream);
                if (atlas != null && atlas.getWidth() == BODY_WIDTH + 4 * WIDTH
                    && atlas.getHeight() == ATLAS_HEIGHT) {
                    facings[facing] = new Facing(atlas, registrations[facing], facing, male);
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
                 float wristX, float wristY, float closure, float impact) {
        Sprite arm = facings[facing].limbs[index];
        float elbowDx = elbowX - RigViewProfile.elbowX(facing, index);
        float elbowDy = elbowY - RigViewProfile.elbowY(facing, index);
        float wristDx = wristX - RigViewProfile.handX(facing, index);
        float wristDy = wristY - RigViewProfile.handY(facing, index);
        arm.pose(shoulderX - RigViewProfile.shoulderX(facing, index), shoulderY + 108f,
            elbowDx, elbowDy, wristDx, wristDy);
        if (!RigViewProfile.isSide(facing)) {
            // Skin at the top of the arm follows the same deformation as its armhole.
            float armholeFit = facing == 0 ? (index == 0 ? 1.7f : 1.05f)
                : index == 1 ? 2f : 0f;
            torso.bindShoulder(arm.mesh.vertices, (arm.sourceX - CENTER_X) * UNIT,
                (arm.sourceY - SOLE_Y) * UNIT, WIDTH * UNIT, HEIGHT * UNIT, COLUMNS, ROWS,
                arm.rootX, arm.rootY, facing == 2 ? -1f : 1f, armholeFit,
                facing == 2 && index == 1 ? closure : 0f, shoulderPoint);
        }
        if (closure > 0f && arm.sleeve != null) {
            boolean fistBehindArm = facing == 2;
            if (fistBehindArm) {
                fistArt.draw(canvas, facing, arm.jointX + elbowDx, arm.jointY + elbowDy,
                    arm.tipX + wristDx, arm.tipY + wristDy, closure, impact);
            }
            arm.draw(canvas, paint, arm.sleeve);
            if (closure < 1f) {
                paint.setAlpha(Math.round(255f * (1f - closure)));
                arm.draw(canvas, paint, arm.openHand);
                paint.setAlpha(255);
            }
            if (!fistBehindArm) {
                fistArt.draw(canvas, facing, arm.jointX + elbowDx, arm.jointY + elbowDy,
                    arm.tipX + wristDx, arm.tipY + wristDy, closure, impact);
            }
        } else {
            arm.draw(canvas, paint, arm.bitmap);
        }
    }

    void drawRearPunchShoulder(Canvas canvas) {
        Sprite arm = facings[2].limbs[1];
        // Reuse the exact deformation drawn for the far arm in this frame.
        arm.draw(canvas, paint, arm.shoulderCover);
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
        limb.pose(pelvisX, pelvisY, pose.kneeX - rest.kneeX + kneeSway, kneeY, footX + view.ankleFit, footY);
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

        final float ankleFit;

        Facing(Bitmap atlas, int[][] registration, int facing, boolean male) {
            body = assembleBody(atlas, facing, male);
            // Profile boots have a cuff behind the center of their image cell.
            boolean profile = RigViewProfile.isSide(facing);
            int cuffX = male ? 47 : 48;
            ankleFit = profile ? (cuffX - registration[2][4]) * UNIT : 0f;
            // The two independently painted profiles have different overlap at the hem.
            float rearHipFit = facing == 1 ? 1.65f : facing == 3 ? .85f : 0f;
            for (int i = 0; i < limbs.length; i++) {
                int x = BODY_WIDTH + i * WIDTH;
                limbs[i] = new Sprite(Bitmap.createBitmap(atlas, x, 0, WIDTH, HEIGHT),
                    registration[i], i >= 2, i >= 2 ? rearHipFit : 0f, i == 1, facing == 2 && i == 1);
            }
            for (int i = 0; i < boots.length; i++) {
                int x = BODY_WIDTH + (i + 2) * WIDTH;
                boots[i] = Bitmap.createBitmap(atlas, x, HEIGHT, WIDTH, ATLAS_HEIGHT - HEIGHT);
                rest[i] = RigViewProfile.isSide(facing) ? RigLegPose.solveProfile(0f, i, 0f, 0f)
                    : RigLegPose.solve(0f, i, facing, 0f, 0f);
            }
        }
    }

    private static Bitmap assembleBody(Bitmap atlas, int facing, boolean male) {
        Bitmap body = Bitmap.createBitmap(atlas, 0, 0, BODY_WIDTH, ATLAS_HEIGHT);
        boolean profile = RigViewProfile.isSide(facing);
        Bitmap assembled = Bitmap.createBitmap(BODY_WIDTH, ATLAS_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(assembled);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        RectF bounds = new RectF(0, 0, BODY_WIDTH, ATLAS_HEIGHT);
        if (profile) {
            canvas.drawBitmap(body, null, bounds, paint);
            // Keep the armhole trim and shade the opening behind the moving arm.
            float top = male ? 151f : 164f;
            float bottom = male ? 210f : 226f;
            float center = male ? 128f : facing == 1 ? 133f : 135f;
            Path opening = new Path();
            opening.moveTo(center, top);
            opening.cubicTo(center - 18f, top, center - 31f, top + 18f,
                center - 31f, top + 37f);
            opening.cubicTo(center - 31f, bottom - 9f, center - 16f, bottom, center, bottom);
            opening.cubicTo(center + 18f, bottom, center + 31f, bottom - 9f,
                center + 31f, top + 37f);
            opening.cubicTo(center + 31f, top + 18f, center + 17f, top, center, top);
            opening.close();
            paint.setShader(new LinearGradient(center, top, center, bottom,
                0xFF46434C, 0xFF24252E, Shader.TileMode.CLAMP));
            canvas.drawPath(opening, paint);
        } else {
            canvas.clipPath(vestOutline(male));
            canvas.drawBitmap(body, null, bounds, paint);
        }
        return assembled;
    }

    private static Path vestOutline(boolean male) {
        // The body cells include remnants of the shoulders outside the vest.
        Path vest = new Path();
        vest.moveTo(0f, 0f);
        if (male) {
            vest.lineTo(0f, 142f);
            vest.lineTo(76f, 142f);
            vest.cubicTo(88f, 156f, 92f, 181f, 82f, 203f);
            vest.lineTo(71f, 213f);
            vest.lineTo(0f, 213f);
            vest.lineTo(0f, ATLAS_HEIGHT);
            vest.lineTo(BODY_WIDTH, ATLAS_HEIGHT);
            vest.lineTo(BODY_WIDTH, 213f);
            vest.lineTo(218f, 213f);
            vest.lineTo(206f, 203f);
            vest.cubicTo(196f, 181f, 202f, 156f, 213f, 142f);
            vest.lineTo(BODY_WIDTH, 142f);
        } else {
            // Start below the hair and follow the diagonal edge of each strap.
            vest.lineTo(0f, 164f);
            vest.lineTo(83f, 164f);
            vest.cubicTo(90f, 172f, 97f, 185f, 96f, 202f);
            vest.lineTo(0f, 208f);
            vest.lineTo(0f, ATLAS_HEIGHT);
            vest.lineTo(BODY_WIDTH, ATLAS_HEIGHT);
            vest.lineTo(BODY_WIDTH, 208f);
            vest.lineTo(192f, 202f);
            vest.cubicTo(191f, 185f, 199f, 172f, 207f, 164f);
            vest.lineTo(BODY_WIDTH, 164f);
        }
        vest.lineTo(BODY_WIDTH, 0f);
        vest.close();
        return vest;
    }

    private static final class Sprite {
        final Bitmap bitmap, sleeve, openHand, shoulderCover;
        final LimbMesh mesh;
        final int sourceX, sourceY;
        private final float rootX, rootY, jointX, jointY, tipX, tipY;

        Sprite(Bitmap bitmap, int[] points, boolean attached, float rearHipFit,
               boolean punchingArm, boolean rearPunchArm) {
            this.bitmap = bitmap;
            shoulderCover = rearPunchArm ? armSection(bitmap, 0, points[1] + 33) : null;
            if (punchingArm) {
                int wrist = points[5] + 3;
                sleeve = armSection(bitmap, 0, wrist);
                openHand = armSection(bitmap, wrist, HEIGHT);
            } else {
                sleeve = null;
                openHand = null;
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

        private static Bitmap armSection(Bitmap bitmap, int top, int bottom) {
            Bitmap section = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas cutout = new Canvas(section);
            cutout.clipRect(0, top, WIDTH, bottom);
            cutout.drawBitmap(bitmap, null, new RectF(0, 0, WIDTH, HEIGHT), null);
            return section;
        }

        void pose(float dx0, float dy0, float dx1, float dy1, float dx2, float dy2) {
            mesh.pose(rootX + dx0, rootY + dy0, jointX + dx1, jointY + dy1, tipX + dx2, tipY + dy2);
        }

        void draw(Canvas canvas, Paint paint, Bitmap image) {
            canvas.drawBitmapMesh(image, COLUMNS, ROWS, mesh.vertices, 0, null, 0, paint);
        }
    }
}
