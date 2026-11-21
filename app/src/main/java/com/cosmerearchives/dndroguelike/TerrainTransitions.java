package com.cosmerearchives.dndroguelike;

/** Composites the tile and its eight neighbors using matching weights at shared edges. */
final class TerrainTransitions {
    static final int SIZE = 128;
    static final int WATER = 10;
    static final int BRIDGE = 11;
    private static final int LAST = SIZE - 1;
    private static final int[] X = {0, 1, 0, -1, 1, 1, -1, -1};
    private static final int[] Y = {-1, 0, 1, 0, -1, 1, 1, -1};
    private static final float[] FRINGE = new float[SIZE];

    static {
        for (int i = 1; i < LAST; i++) {
            double phase = 2 * Math.PI * i / LAST;
            FRINGE[i] = (float) (.022 * Math.sin(phase * 5) + .012 * Math.sin(phase * 11));
        }
    }

    private TerrainTransitions() {}

    static int dx(int slot) { return X[slot]; }
    static int dy(int slot) { return Y[slot]; }
    static int shift(int slot) { return 4 + slot * 5; }
    static int neighbor(long key, int slot) { return (int) ((key >>> shift(slot)) & 31) - 1; }

    static int[] blend(long key, int[][] textures) {
        int center = (int) (key & 15);
        int[] materials = new int[8];
        for (int slot = 0; slot < materials.length; slot++) {
            int material = neighbor(key, slot);
            materials[slot] = material < 0 ? center : material;
        }
        int[] base = textures[center];
        int[] output = base.clone();
        for (int y = 0; y < SIZE; y++) {
            boolean north = y < SIZE / 2;
            int vertical = north ? 0 : 2;
            for (int x = 0; x < SIZE; x++) {
                boolean west = x < SIZE / 2;
                int horizontal = west ? 3 : 1;
                int corner = north ? (west ? 7 : 4) : (west ? 6 : 5);
                int side = materials[horizontal], end = materials[vertical], diagonal = materials[corner];
                // Reuse one width on both sides of a bank, including along
                // adjacent tiles where the shoreline turns across a corner.
                boolean shore = center == WATER || side == WATER || end == WATER || diagonal == WATER;
                float width = shore ? .16f : .28f;
                float u = edgeWeight(west ? x : LAST - x, width + FRINGE[y]);
                float v = edgeWeight(north ? y : LAST - y, width + FRINGE[x]);
                if (u == 0 && v == 0) continue;
                int i = y * SIZE + x;
                output[i] = mix(base[i], textures[side][i], textures[end][i], textures[diagonal][i], u, v);
            }
        }
        return output;
    }

    private static float edgeWeight(int distance, float width) {
        float t = Math.max(0, 1 - distance / (LAST * width));
        return .5f * t * t * (3 - 2 * t);
    }

    private static int mix(int base, int side, int end, int corner, float u, float v) {
        float a = (1 - u) * (1 - v), b = u * (1 - v), c = (1 - u) * v, d = u * v;
        int rgb = 0xff000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = Math.round(((base >>> shift) & 255) * a + ((side >>> shift) & 255) * b
                + ((end >>> shift) & 255) * c + ((corner >>> shift) & 255) * d);
            rgb |= channel << shift;
        }
        return rgb;
    }
}
