package com.cosmerearchives.dndroguelike;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Each 8-by-8 chunk is stored in one long, including chunks at negative coordinates. */
final class Exploration {
    private final Map<Long, Long> chunks = new HashMap<>();
    private final Set<Long> dirty = new HashSet<>();

    private static long key(int x, int y) {
        return ((long) (x >> 3) << 32) | ((y >> 3) & 0xffffffffL);
    }

    private static long mask(int x, int y) {
        return 1L << ((x & 7) + 8 * (y & 7));
    }

    boolean known(int x, int y) {
        return (chunks.getOrDefault(key(x, y), 0L) & mask(x, y)) != 0;
    }

    void discover(int x, int y) {
        long key = key(x, y);
        long old = chunks.getOrDefault(key, 0L), next = old | mask(x, y);
        if (old != next) {
            chunks.put(key, next);
            dirty.add(key);
        }
    }

    void restore(long key, long value) { chunks.put(key, value); }

    interface Writer { void chunk(long key, long value); }

    void save(Writer writer) {
        for (long key : dirty) writer.chunk(key, chunks.get(key));
        dirty.clear();
    }
}
