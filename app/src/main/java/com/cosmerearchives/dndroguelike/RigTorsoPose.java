package com.cosmerearchives.dndroguelike;

/** Chest and neck motion relative to the fixed waist and pelvis. */
final class RigTorsoPose {
    static final float WAIST_Y = -82f;
    static final float CHEST_Y = -108f;
    static final float NECK_Y = -128f;

    private boolean side;
    private float chestX, neckX, chestY, neckY, expansion, roll;

    void sample(boolean side, float walk, float stride, float step, float breath, float hipShift) {
        this.side = side;
        chestX = walk * (side ? 2.3f + stride * 1.15f : -1.15f * stride) - hipShift * .8f;
        neckX = chestX * .76f;
        chestY = -1.25f * breath + .5f * walk * step;
        neckY = -.45f * breath + .16f * walk * step;
        expansion = breath * (side ? .19f : .095f);
        roll = side ? 0f : walk * stride * .068f;
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
                    vertices[at++] += x;
                    vertices[at++] += y;
                }
            }
        }
    }
}
