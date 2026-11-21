package com.cosmerearchives.dndroguelike;

/** Blends a dirt tile into compatible ground on the surrounding tiles. */
final class TrailTransitions {
    static final int SIZE = 128;
    private static final int LAST = SIZE - 1;
    private static final int[] EDGE_X = {0, 1, 0, -1, 1, 1, -1, -1};
    private static final int[] EDGE_Y = {-1, 0, 1, 0, -1, 1, 1, -1};
    private static final float[] FRINGE = new float[SIZE];

    static {
        for (int i = 0; i < SIZE; i++) {
            double phase = 2 * Math.PI * i / LAST;
            FRINGE[i] = (float) (.022 * Math.sin(phase * 5) + .012 * Math.sin(phase * 11));
        }
    }

    private TrailTransitions() {}

    static int dx(int slot) { return EDGE_X[slot]; }
    static int dy(int slot) { return EDGE_Y[slot]; }
    static int shift(int slot) { return (slot + 1) * 4; }

    static int[] blend(long key, int[][] textures) {
        int[] dirt = textures[(int) (key & 15)];
        int[][] neighbors = new int[8][];
        for (int slot = 0; slot < neighbors.length; slot++) {
            int material = (int) ((key >>> shift(slot)) & 15);
            if (material != 0) neighbors[slot] = textures[material - 1];
        }
        int[] pixels = dirt.clone();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int index = y * SIZE + x;
                float total = 0, grass = 0, red = 0, green = 0, blue = 0;
                for (int slot = 0; slot < neighbors.length; slot++) {
                    int[] texture = neighbors[slot];
                    if (texture == null) continue;
                    float distanceX = (dx(slot) > 0 ? LAST - x : x) / (float) LAST;
                    float distanceY = (dy(slot) > 0 ? LAST - y : y) / (float) LAST;
                    float distance;
                    float width;
                    if (slot >= 4) {
                        distance = distanceX + distanceY;
                        width = .20f;
                    } else if (dx(slot) == 0) {
                        distance = distanceY;
                        width = .26f + FRINGE[x];
                    } else {
                        distance = distanceX;
                        width = .26f + FRINGE[y];
                    }
                    float amount = edgeWeight(distance, width);
                    if (amount == 0) continue;
                    int color = texture[index];
                    total += amount;
                    grass = Math.max(grass, amount);
                    red += ((color >>> 16) & 255) * amount;
                    green += ((color >>> 8) & 255) * amount;
                    blue += (color & 255) * amount;
                }
                if (total > 0) {
                    int soil = dirt[index];
                    float soilWeight = 1 - grass;
                    float grassWeight = grass / total;
                    int r = Math.round(((soil >>> 16) & 255) * soilWeight + red * grassWeight);
                    int g = Math.round(((soil >>> 8) & 255) * soilWeight + green * grassWeight);
                    int b = Math.round((soil & 255) * soilWeight + blue * grassWeight);
                    pixels[index] = 0xff000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
        return pixels;
    }

    private static float edgeWeight(float distance, float width) {
        float t = Math.max(0, 1 - distance / width);
        return t * t * (3 - 2 * t);
    }
}
