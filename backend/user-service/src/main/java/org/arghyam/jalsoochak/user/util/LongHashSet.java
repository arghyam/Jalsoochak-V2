package org.arghyam.jalsoochak.user.util;

/**
 * Minimal primitive long hash set to avoid boxing overhead for large uploads.
 * Open addressing with linear probing.
 *
 * Note: This is intentionally small and purpose-built (add/contains only).
 */
public final class LongHashSet {

    private static final float LOAD_FACTOR = 0.7f;
    // We store value+1 so 0 can be used as the "empty" sentinel.
    private long[] table;
    private int size;
    private int mask;
    private int maxFill;

    public LongHashSet(int expectedSize) {
        int cap = arraySize(Math.max(2, expectedSize), LOAD_FACTOR);
        this.table = new long[cap];
        this.mask = cap - 1;
        this.maxFill = maxFill(cap, LOAD_FACTOR);
    }

    public boolean add(long value) {
        long k = value + 1;
        int pos = mix64To32(k) & mask;
        while (true) {
            long cur = table[pos];
            if (cur == 0) {
                table[pos] = k;
                if (++size >= maxFill) {
                    rehash(table.length * 2);
                }
                return true;
            }
            if (cur == k) {
                return false;
            }
            pos = (pos + 1) & mask;
        }
    }

    public boolean contains(long value) {
        long k = value + 1;
        int pos = mix64To32(k) & mask;
        while (true) {
            long cur = table[pos];
            if (cur == 0) {
                return false;
            }
            if (cur == k) {
                return true;
            }
            pos = (pos + 1) & mask;
        }
    }

    public int size() {
        return size;
    }

    private void rehash(int newCap) {
        int cap = 1;
        while (cap < newCap) {
            cap <<= 1;
        }

        long[] old = this.table;
        long[] next = new long[cap];
        int nextMask = cap - 1;
        for (long k : old) {
            if (k == 0) {
                continue;
            }
            int pos = mix64To32(k) & nextMask;
            while (next[pos] != 0) {
                pos = (pos + 1) & nextMask;
            }
            next[pos] = k;
        }
        this.table = next;
        this.mask = nextMask;
        this.maxFill = maxFill(cap, LOAD_FACTOR);
    }

    private static int mix64To32(long x) {
        int h = (int) (x ^ (x >>> 32));
        // A couple of avalanching steps (cheap + good enough here).
        h ^= (h >>> 16);
        h *= 0x7feb352d;
        h ^= (h >>> 15);
        h *= 0x846ca68b;
        h ^= (h >>> 16);
        return h;
    }

    private static int arraySize(int expected, float f) {
        long s = Math.max(2L, nextPowerOfTwo((long) Math.ceil(expected / (double) f)));
        if (s > (1 << 30)) {
            throw new IllegalArgumentException("Too large");
        }
        return (int) s;
    }

    private static int maxFill(int n, float f) {
        return Math.min(n - 1, (int) Math.ceil(n * (double) f));
    }

    private static long nextPowerOfTwo(long x) {
        if (x == 0) {
            return 1;
        }
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        x |= x >> 32;
        return x + 1;
    }
}

