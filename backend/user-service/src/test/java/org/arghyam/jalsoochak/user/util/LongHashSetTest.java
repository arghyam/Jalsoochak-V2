package org.arghyam.jalsoochak.user.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LongHashSetTest {

    @Test
    void supportsMinusOne() {
        LongHashSet set = new LongHashSet(2);

        assertTrue(set.add(-1L));
        assertTrue(set.contains(-1L));
        assertEquals(1, set.size());

        assertFalse(set.add(-1L));
        assertTrue(set.contains(-1L));
        assertEquals(1, set.size());
    }

    @Test
    void minusOneSurvivesRehash() {
        LongHashSet set = new LongHashSet(2);

        assertTrue(set.add(-1L));
        for (long i = 0; i < 10_000; i++) {
            assertTrue(set.add(i));
        }

        assertTrue(set.contains(-1L));
        assertTrue(set.contains(0L));
        assertTrue(set.contains(9_999L));
        assertEquals(10_001, set.size());
    }
}

