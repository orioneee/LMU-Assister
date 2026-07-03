package com.orioooneee.lmuasister.ui.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class RatingProgressionCardTest {

    @Test
    fun scoreLabelsMatchBackendRatingHistoryBuckets() {
        assertEquals("B0", scoreLabel(-100.0))
        assertEquals("B0", scoreLabel(-1.0))
        assertEquals("B1", scoreLabel(0.0))
        assertEquals("B1", scoreLabel(99.9))
        assertEquals("B2", scoreLabel(100.0))
        assertEquals("B3", scoreLabel(200.0))
        assertEquals("S1", scoreLabel(300.0))
        assertEquals("S3", scoreLabel(500.0))
        assertEquals("G1", scoreLabel(600.0))
        assertEquals("G2", scoreLabel(737.909992))
        assertEquals("P1", scoreLabel(900.0))
        assertEquals("P2", scoreLabel(1000.0))
        assertEquals("P3", scoreLabel(1100.0))
        assertEquals("P3", scoreLabel(1200.0))
    }
}
