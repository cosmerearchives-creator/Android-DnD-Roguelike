package com.cosmerearchives.dndroguelike;

/** Chest and neck motion relative to the fixed waist and pelvis. */
final class RigTorsoPose {
    // One more 10% increase over the first 8% pass.
    private static final float SHOULDER_SPREAD = 1.08f * 1.10f - 1f;
    static final float WAIST_Y = -82f;
    static final float CHEST_Y = -108f;
    static final float NECK_Y = -128f;

    private boolean side;
    private float chestX, neckX, chestY, neckY, expansion, roll, shoulderOverlap;

    void sample(boolean side, float walk, float stride, float step, float breath, float hipShift) {
        this.side = side;
        breath *= .78f;
        chestX = walk * (side ? 2.3f + stride * 1.15f : -1.15f * stride) - hipShift * .8f;
        neckX = chestX * .76f;
        chestY = -.75f * breath + .5f * walk * step;
        neckY = -.27f * breath + .16f * walk * step;
        expansion = breath * (side ? .115f : .057f);
        roll = side ? 0f : walk * stride * .052f;
        shoulderOverlap = .5f * walk;
    }

    void applyPunch(float shoulderDrive) {
        chestX += shoulderDrive;
        neckX += shoulderDrive * .3f;
        if (!side) roll += shoulderDrive * .022f;
    }

    void offsets(float x, float y, float[] target, int at) {
        float waist = smooth((WAIST_Y - y) / (WAIST_Y - CHEST_Y));
        float neck = smooth((CHEST_Y - y) / (CHEST_Y - NECK_Y));
        float chest = waist * (1f - neck);
        float front = side ? Math.max(0f, x + 1f) : x;
        target[at] = chest * (chestX + front * expansion) + neck * neckX;
        target[at + 1] = chest * (chestY + x * roll) + neck * neckY;
    }

    // Fade into the neck and waist so only the upper torso widens.
    float shoulderSpread(float x, float y) {
        if (side) return 0f;
        return SHOULDER_SPREAD * x * smooth((WAIST_Y - y) / 30f) * smooth((y + 144f) / 14f);
    }

    void bindShoulder(float[] vertices, float left, float top, float width, float height,
                      int columns, int rows, float shoulderX, float shoulderY, float mirror,
                      float armholeFit, float rearPunch, float[] offset) {
        float tuck = -Math.signum(shoulderX) * (shoulderOverlap + armholeFit + .45f * rearPunch);
        for (int row = 0; row <= rows; row++) {
            float y = top + height * row / rows;
            float followTorso = 1f - smooth((y - shoulderY - 1f - 2.4f * rearPunch) / (9f + 2f * armholeFit));
            if (followTorso == 0f) break;
            for (int col = 0; col <= columns; col++) {
                int at = (row * (columns + 1) + col) * 2;
                float x = left + width * col / columns;
                offsets(x * mirror, y, offset, 0);
                float attachedX = x + offset[0] * mirror + tuck + shoulderSpread(x, y);
                float attachedY = y + offset[1];
                vertices[at] += (attachedX - vertices[at]) * followTorso;
                vertices[at + 1] += (attachedY - vertices[at + 1]) * followTorso;
            }
        }
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    static final class Mesh {
        static final int COLUMNS = 8, ROWS = 32;
        final float[] vertices = new float[(COLUMNS + 1) * (ROWS + 1) * 2];

        void pose(RigTorsoPose pose, float left, float top, float right, float bottom) {
            int at = 0;
            for (int row = 0; row <= ROWS; row++) {
                float y = top + (bottom - top) * row / ROWS;
                for (int col = 0; col <= COLUMNS; col++) {
                    float x = left + (right - left) * col / COLUMNS;
                    pose.offsets(x, y, vertices, at);
                    vertices[at++] += x + pose.shoulderSpread(x, y);
                    vertices[at++] += y;
                }
            }
        }
    }
}
