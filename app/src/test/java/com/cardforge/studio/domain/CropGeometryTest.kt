package com.cardforge.studio.domain

import com.cardforge.studio.core.model.CropQuad
import com.cardforge.studio.core.model.CropRect
import com.cardforge.studio.core.model.Pt
import com.cardforge.studio.domain.crop.GradientCardDetector
import com.cardforge.studio.domain.crop.PerspectiveCorrector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {

    @Test
    fun cropRectClampsToBounds() {
        val r = CropRect(-10f, -5f, 120f, 130f).clampTo(100f, 100f)
        assertEquals(0f, r.left, 0.001f)
        assertEquals(0f, r.top, 0.001f)
        assertEquals(100f, r.right, 0.001f)
        assertEquals(100f, r.bottom, 0.001f)
    }

    @Test
    fun cropRectWidthHeight() {
        val r = CropRect(10f, 20f, 60f, 70f)
        assertEquals(50f, r.width, 0.001f)
        assertEquals(50f, r.height, 0.001f)
    }

    @Test
    fun warpProducesRequestedSizeAndUniformColour() {
        val w = 40; val h = 20
        val colour = 0xFF3366FF.toInt()
        val src = IntArray(w * h) { colour }
        val quad = CropQuad(Pt(0f, 0f), Pt(w.toFloat(), 0f), Pt(w.toFloat(), h.toFloat()), Pt(0f, h.toFloat()))
        val out = PerspectiveCorrector.warp(src, w, h, quad, 80, 40)
        assertEquals(80 * 40, out.size)
        assertTrue(out.all { it == colour })
    }

    @Test
    fun warpOfSubQuadPicksSourceColour() {
        // Left half red, right half green; crop the right half.
        val w = 40; val h = 20
        val src = IntArray(w * h) { i -> if (i % w < w / 2) 0xFFFF0000.toInt() else 0xFF00FF00.toInt() }
        val quad = CropQuad(
            Pt(w / 2f, 0f), Pt(w.toFloat(), 0f), Pt(w.toFloat(), h.toFloat()), Pt(w / 2f, h.toFloat())
        )
        val out = PerspectiveCorrector.warp(src, w, h, quad, 20, 20, smooth = false)
        // Interior samples should be green.
        assertTrue(out[5 * 20 + 5] == 0xFF00FF00.toInt())
    }

    @Test
    fun detectorReturnsFullFrameForUniformImage() {
        val w = 120; val h = 120
        val flat = IntArray(w * h) { 0xFF202020.toInt() }
        val result = GradientCardDetector().detect(flat, w, h)
        assertTrue(result.lowConfidence)
        assertEquals(0f, result.confidence, 0.001f)
    }

    @Test
    fun detectorFindsBrightRectangle() {
        val w = 200; val h = 200
        val px = IntArray(w * h) { 0xFF000000.toInt() }
        for (y in 40 until 160) for (x in 40 until 160) px[y * w + x] = 0xFFFFFFFF.toInt()
        val result = GradientCardDetector().detect(px, w, h)
        // Corners should bracket the rectangle, not the whole frame.
        assertTrue(result.quad.topLeft.x in 25f..55f)
        assertTrue(result.quad.topLeft.y in 25f..55f)
        assertTrue(result.quad.bottomRight.x in 145f..175f)
        assertTrue(result.quad.bottomRight.y in 145f..175f)
    }
}
