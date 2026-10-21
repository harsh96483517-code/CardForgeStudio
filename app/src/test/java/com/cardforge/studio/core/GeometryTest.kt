package com.cardforge.studio.core

import com.cardforge.studio.core.geometry.Geometry
import com.cardforge.studio.core.model.CropQuad
import com.cardforge.studio.core.model.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {

    private fun quad() = CropQuad(
        Pt(0f, 0f), Pt(100f, 0f), Pt(100f, 50f), Pt(0f, 50f)
    )

    @Test
    fun distance_isEuclidean() {
        assertEquals(5f, Geometry.distance(Pt(0f, 0f), Pt(3f, 4f)), 0.0001f)
    }

    @Test
    fun bounds_ofQuad() {
        val b = Geometry.bounds(quad())
        assertEquals(0f, b.left, 0.001f)
        assertEquals(0f, b.top, 0.001f)
        assertEquals(100f, b.right, 0.001f)
        assertEquals(50f, b.bottom, 0.001f)
    }

    @Test
    fun widthHeightAndAspect() {
        val q = quad()
        assertEquals(100f, Geometry.widthOf(q), 0.001f)
        assertEquals(50f, Geometry.heightOf(q), 0.001f)
        assertEquals(2f, Geometry.aspectOf(q), 0.001f)
    }

    @Test
    fun fitScale_letterboxes() {
        // 100x50 into 50x50 -> scale 0.5
        assertEquals(0.5, Geometry.fitScale(100.0, 50.0, 50.0, 50.0), 0.0001)
    }

    @Test
    fun fillScale_covers() {
        // 100x50 into 50x50 -> scale 1.0 (covers height)
        assertEquals(1.0, Geometry.fillScale(100.0, 50.0, 50.0, 50.0), 0.0001)
    }

    @Test
    fun orderCorners_mapsToTlTrBrBl() {
        val unordered = listOf(Pt(100f, 0f), Pt(0f, 0f), Pt(0f, 50f), Pt(100f, 50f))
        val q = Geometry.orderCorners(unordered)
        assertEquals(Pt(0f, 0f), q.topLeft)
        assertEquals(Pt(100f, 0f), q.topRight)
        assertEquals(Pt(100f, 50f), q.bottomRight)
        assertEquals(Pt(0f, 50f), q.bottomLeft)
    }

    @Test
    fun bilinear_hitsCornersAndCentre() {
        val q = quad()
        assertEquals(Pt(0f, 0f), Geometry.bilinear(q, 0f, 0f))
        assertEquals(Pt(100f, 0f), Geometry.bilinear(q, 1f, 0f))
        assertEquals(Pt(100f, 50f), Geometry.bilinear(q, 1f, 1f))
        assertEquals(Pt(0f, 50f), Geometry.bilinear(q, 0f, 1f))
        assertEquals(Pt(50f, 25f), Geometry.bilinear(q, 0.5f, 0.5f))
    }

    @Test
    fun isAxisAligned_trueForRectangle_falseForSkew() {
        assertTrue(Geometry.isAxisAligned(quad()))
        val skewed = CropQuad(Pt(0f, 0f), Pt(100f, 20f), Pt(100f, 60f), Pt(0f, 50f))
        assertTrue(!Geometry.isAxisAligned(skewed))
    }
}
