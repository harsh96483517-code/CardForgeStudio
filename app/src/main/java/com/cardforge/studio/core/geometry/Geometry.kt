package com.cardforge.studio.core.geometry

import com.cardforge.studio.core.model.CropQuad
import com.cardforge.studio.core.model.CropRect
import com.cardforge.studio.core.model.Pt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Framework-free geometry helpers shared by detection, crop and layout. */
object Geometry {

    fun distance(a: Pt, b: Pt): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /** Axis-aligned bounding box of a quad. */
    fun bounds(q: CropQuad): CropRect {
        val xs = q.points.map { it.x }
        val ys = q.points.map { it.y }
        return CropRect(xs.min(), ys.min(), xs.max(), ys.max())
    }

    /** Average of the two horizontal edge lengths. */
    fun widthOf(q: CropQuad): Float = (distance(q.topLeft, q.topRight) + distance(q.bottomLeft, q.bottomRight)) / 2f

    /** Average of the two vertical edge lengths. */
    fun heightOf(q: CropQuad): Float = (distance(q.topLeft, q.bottomLeft) + distance(q.topRight, q.bottomRight)) / 2f

    /** Aspect ratio (w/h) of a quad, guarding against a zero height. */
    fun aspectOf(q: CropQuad): Float {
        val h = heightOf(q)
        return if (h <= 0f) 0f else widthOf(q) / h
    }

    /**
     * Fit [src] (a w×h box) inside [dstW]×[dstH] preserving aspect ratio.
     * Returns the scale factor that makes the whole source visible.
     */
    fun fitScale(srcW: Double, srcH: Double, dstW: Double, dstH: Double): Double {
        if (srcW <= 0.0 || srcH <= 0.0) return 1.0
        return min(dstW / srcW, dstH / srcH)
    }

    /** Cover scale — the source fills the destination, cropping overflow. */
    fun fillScale(srcW: Double, srcH: Double, dstW: Double, dstH: Double): Double {
        if (srcW <= 0.0 || srcH <= 0.0) return 1.0
        return max(dstW / srcW, dstH / srcH)
    }

    /**
     * Inverse bilinear map: given normalised (u,v) in [0,1]² over the quad,
     * return the source-space point. Corners are TL, TR, BR, BL.
     * Used by the perspective corrector to sample a rectified rectangle.
     */
    fun bilinear(q: CropQuad, u: Float, v: Float): Pt {
        val top = lerp(q.topLeft, q.topRight, u)
        val bottom = lerp(q.bottomLeft, q.bottomRight, u)
        return lerp(top, bottom, v)
    }

    fun lerp(a: Pt, b: Pt, t: Float): Pt = Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    /** Order four arbitrary points into TL, TR, BR, BL. */
    fun orderCorners(points: List<Pt>): CropQuad {
        require(points.size == 4) { "Expected 4 corners" }
        // TL = smallest x+y, BR = largest x+y, TR = smallest y-x, BL = largest y-x.
        val tl = points.minBy { it.x + it.y }
        val br = points.maxBy { it.x + it.y }
        val tr = points.minBy { it.y - it.x }
        val bl = points.maxBy { it.y - it.x }
        return CropQuad(tl, tr, br, bl)
    }

    /** True if the quad is (approximately) axis-aligned. */
    fun isAxisAligned(q: CropQuad, toleranceDeg: Float = 2f): Boolean {
        fun angle(a: Pt, b: Pt): Float =
            Math.toDegrees(kotlin.math.atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
        val top = abs(angle(q.topLeft, q.topRight))
        val left = abs(angle(q.topLeft, q.bottomLeft) - 90f)
        return top <= toleranceDeg && left <= toleranceDeg
    }
}
