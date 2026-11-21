package com.cosmerearchives.dndroguelike;

/** Chest and neck motion relative to the fixed waist and pelvis. */
final class RigTorsoPose {
    // This toggles per-appearance attachment corrections, never torso geometry.
    private boolean shoulderFitEnabled = true;

    void setShoulderFitEnabled(boolean enabled) {
        shoulderFitEnabled = enabled;
    }

    boolean shoulderFitEnabled() {
        return shoulderFitEnabled;
    }
    static final float WAIST_Y = -82f;
    static final float CHEST_Y = -108f;
    static final float NECK_Y = -128f;

    private boolean side;
    private float chestX, neckX, chestY, neckY, expansion, roll;

    void sample(boolean side, float walk, float stride, float step, float breath, float hipShift) {
        this.side = side;
        breath *= .78f;
        chestX = walk * (side ? 2.3f + stride * 1.15f : -1.15f * stride) - hipShift * .8f;
        neckX = chestX * .76f;
        chestY = -.75f * breath + .5f * walk * step;
        neckY = -.27f * breath + .16f * walk * step;
        expansion = breath * (side ? .115f : .057f);
        roll = side ? 0f : walk * stride * .052f;
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
                    // Preserve the original torso silhouette at every shoulder ratio.
                    vertices[at++] += x;
                    vertices[at++] += y;
                }
            }
        }
    }
}
