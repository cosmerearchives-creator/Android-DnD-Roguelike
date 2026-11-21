package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Directional neutral artwork bound to the Explorer's existing limb poses. */
final class ElfBaseArt {
    private static final int BODY_WIDTH = 288;
    private static final int ATLAS_HEIGHT = 400;
    private static final int WIDTH = 128, HEIGHT = 320, COLUMNS = 4, ROWS = 40;
    private static final float UNIT = 1f / 4.3f;
    private static final int CENTER_X = 271, SOLE_Y = 1192;

    // Coordinates within each limb cell, followed by the cell's origin in the
    // facing artwork. Rear arm art is flipped to match the renderer's projection.
    private static final int[][][] MALE = {
        {{56,47,42,138,29,227,135,626}, {43,47,61,136,71,226,306,626},
         {55,38,51,137,48,275,179,841}, {52,39,57,135,55,275,255,841}},
        {{44,42,40,143,62,225,220,630}, {44,42,40,143,62,225,220,630},
         {49,38,54,138,46,274,227,842}, {49,38,54,138,46,274,227,842}},
        {{86,42,69,136,64,222,108,631}, {77,41,95,137,107,222,270,631},
         {57,40,58,141,51,275,177,841}, {51,40,55,139,54,275,256,841}},
        {{44,42,40,143,62,225,220,630}, {44,42,40,143,62,225,220,630},
         {50,38,55,139,47,274,226,842}, {50,38,55,139,47,274,226,842}}
    };

    private final String appearanceId;
    private final Facing[] facings = new Facing[4];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ElfFistArt fistArt;
    private final RigTorsoPose.Mesh bodyMesh = new RigTorsoPose.Mesh();
    private final RectF bootBounds = new RectF();
    private final RectF bodyBounds = new RectF(
        (125 - CENTER_X) * UNIT, (510 - SOLE_Y) * UNIT,
        (125 + BODY_WIDTH - CENTER_X) * UNIT, (510 + ATLAS_HEIGHT - SOLE_Y) * UNIT);

    ElfBaseArt(Context context, ElfAppearance appearance) {
        appearanceId = appearance.id;
        fistArt = new ElfFistArt(appearance.fistSkin);
        boolean classic = appearance == ElfAppearance.CLASSIC;
        String directory = "elf_roster/" + appearance.id + "/";
        int[][][] registrations = classic ? MALE : readRegistration(context, directory + "rig.txt");
        if (registrations == null) return;

        for (int facing = 0; facing < facings.length; facing++) {
            String path = classic ? "elf_male_facing_" + facing + ".webp"
                : directory + facing + ".webp";
            try (InputStream stream = context.getAssets().open(path)) {
                Bitmap atlas = BitmapFactory.decodeStream(stream);
                if (atlas != null && atlas.getWidth() == BODY_WIDTH + 4 * WIDTH
                    && atlas.getHeight() == ATLAS_HEIGHT) {
                    facings[facing] = new Facing(atlas, registrations[facing], facing, classic);
                }
            } catch (IOException ignored) {
                // The segmented renderer remains available for missing artwork.
            }
        }
    }

    private static int[][][] readRegistration(Context context, String path) {
        int[][][] result = new int[4][4][8];
        try (InputStream stream = context.getAssets().open(path);
             Scanner scanner = new Scanner(stream, "UTF-8")) {
            for (int[][] facing : result) {
                for (int[] part : facing) {
                    for (int i = 0; i < part.length; i++) {
                        part[i] = scanner.nextInt();
                    }
                }
            }
            return scanner.hasNext() ? null : result;
        } catch (IOException | NoSuchElementException ignored) {
            return null;
        }
    }

    boolean ready(int facing) {
        return facings[facing] != null;
    }

    float fittedShoulderX(int facing, int index, boolean useFit) {
        return useFit ? RigViewProfile.shoulderFitX(appearanceId, facing, index) : 0f;
    }

    float fittedShoulderY(int facing, int index, boolean useFit) {
        if (!useFit) return 0f;
        return RigViewProfile.shoulderFitY(
            appearanceId, facing, facings[facing].limbs[index].rootY);
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
        // Translate the entire arm from its illustrated rest anchors. The torso
        // and arm textures keep their original shape and proportions.
        float elbowDx = elbowX - RigViewProfile.elbowX(facing, index);
        float elbowDy = elbowY - RigViewProfile.elbowY(facing, index);
        float wristDx = wristX - RigViewProfile.handX(facing, index);
        float wristDy = wristY - RigViewProfile.handY(facing, index);
        arm.pose(shoulderX - RigViewProfile.shoulderX(facing, index), shoulderY + 108f,
            elbowDx, elbowDy, wristDx, wristDy);
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

        Facing(Bitmap atlas, int[][] registration, int facing, boolean classic) {
            body = Bitmap.createBitmap(atlas, 0, 0, BODY_WIDTH, ATLAS_HEIGHT);
            // The two independently painted profiles have different overlap at the hem.
            float rearHipFit = classic ? (facing == 1 ? 1.65f : facing == 3 ? .85f : 0f) : 0f;
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
