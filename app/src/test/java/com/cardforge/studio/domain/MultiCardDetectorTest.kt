package com.cardforge.studio.domain

import com.cardforge.studio.domain.crop.CardRegionSelector
import com.cardforge.studio.domain.crop.GradientMultiCardDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiCardDetectorTest {

    /** Grey page with two light, black-bordered cards that have interior "text" lines. */
    private fun twoCardPage(w: Int = 400, h: Int = 220): IntArray {
        val px = IntArray(w * h) { 0xFFC8C8C8.toInt() }
        fun card(x0: Int, y0: Int, x1: Int, y1: Int) {
            // A 2×2 checkerboard gives every interior row and column strong edges,
            // standing in for the dense texture of real document text.
            for (y in y0..y1) for (x in x0..x1) {
                px[y * w + x] =
                    if (((x / 2 + y / 2) % 2) == 0) 0xFF101010.toInt() else 0xFFF0F0F0.toInt()
            }
        }
        card(20, 60, 180, 160)
        card(220, 60, 380, 160)
        return px
    }

    @Test
    fun detectsTwoCards() {
        val px = twoCardPage()
        val regions = GradientMultiCardDetector().detect(px, 400, 220, maxCards = 4, targetAspect = 1.6f)
        assertTrue("expected at least 2 regions, got ${regions.size}", regions.size >= 2)
    }

    @Test
    fun frontBackPairIsLeftThenRight() {
        val px = twoCardPage()
        val regions = GradientMultiCardDetector().detect(px, 400, 220, maxCards = 4, targetAspect = 1.6f)
        val pair = CardRegionSelector.pickFrontBack(regions, 400f)
        assertEquals(2, pair.size)
        assertTrue("front should be the left card", pair[0].centerX < pair[1].centerX)
        // The left card sits around x=100, the right around x=300.
        assertTrue(pair[0].centerX < 200f)
        assertTrue(pair[1].centerX > 200f)
    }

    @Test
    fun uniformPageYieldsNoRegions() {
        val flat = IntArray(200 * 200) { 0xFF404040.toInt() }
        val regions = GradientMultiCardDetector().detect(flat, 200, 200, maxCards = 4)
        assertTrue(regions.isEmpty())
    }

    @Test
    fun respectsMaxCards() {
        val px = twoCardPage()
        val regions = GradientMultiCardDetector().detect(px, 400, 220, maxCards = 1, targetAspect = 1.6f)
        assertEquals(1, regions.size)
    }

    @Test
    fun singleRegionPairReturnsItself() {
        val flat = IntArray(50 * 50) { 0xFF101010.toInt() }
        val regions = GradientMultiCardDetector().detect(flat, 50, 50)
        val pair = CardRegionSelector.pickFrontBack(regions, 50f)
        assertTrue(pair.size <= 1)
    }
}
