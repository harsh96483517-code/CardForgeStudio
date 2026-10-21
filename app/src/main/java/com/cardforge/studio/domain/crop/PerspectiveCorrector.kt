package com.cardforge.studio.domain.crop

import com.cardforge.studio.core.geometry.Geometry
import com.cardforge.studio.core.model.CropQuad
import kotlin.math.roundToInt

/**
 * Rectifies a photographed (trapezoidal) document quad into an axis-aligned
 * rectangle using inverse-bilinear sampling.
 *
 * The pixel transform is pure and JVM-testable; the Android Bitmap wrapper
 * lives in the data layer.
 */
object PerspectiveCorrector {

    /**
     * Warp [srcPixels] (srcW × srcH, ARGB) so that [quad] maps onto a
     * [outW] × [outH] rectangle. Nearest-neighbour sampling keeps this fast and
     * deterministic; bilinear is used when [smooth] is true.
     */
    fun warp(
        srcPixels: IntArray,
        srcW: Int,
        srcH: Int,
        quad: CropQuad,
        outW: Int,
        outH: Int,
        smooth: Boolean = true
    ): IntArray {
        require(srcW > 0 && srcH > 0 && outW > 0 && outH > 0) { "Bad dimensions" }
        val out = IntArray(outW * outH)
        for (y in 0 until outH) {
            val v = if (outH == 1) 0f else y.toFloat() / (outH - 1)
            for (x in 0 until outW) {
                val u = if (outW == 1) 0f else x.toFloat() / (outW - 1)
                val src = Geometry.bilinear(quad, u, v)
                out[y * outW + x] = if (smooth) {
                    sampleBilinear(srcPixels, srcW, srcH, src.x, src.y)
                } else {
                    sampleNearest(srcPixels, srcW, srcH, src.x, src.y)
                }
            }
        }
        return out
    }

    private fun sampleNearest(px: IntArray, w: Int, h: Int, x: Float, y: Float): Int {
        val xi = x.roundToInt().coerceIn(0, w - 1)
        val yi = y.roundToInt().coerceIn(0, h - 1)
        return px[yi * w + xi]
    }

    private fun sampleBilinear(px: IntArray, w: Int, h: Int, x: Float, y: Float): Int {
        val x0 = x.toInt().coerceIn(0, w - 1)
        val y0 = y.toInt().coerceIn(0, h - 1)
        val x1 = (x0 + 1).coerceAtMost(w - 1)
        val y1 = (y0 + 1).coerceAtMost(h - 1)
        val fx = x - x0
        val fy = y - y0

        val c00 = px[y0 * w + x0]; val c10 = px[y0 * w + x1]
        val c01 = px[y1 * w + x0]; val c11 = px[y1 * w + x1]

        fun ch(c: Int, shift: Int) = (c shr shift) and 0xFF
        fun blend(shift: Int): Int {
            val top = ch(c00, shift) * (1 - fx) + ch(c10, shift) * fx
            val bot = ch(c01, shift) * (1 - fx) + ch(c11, shift) * fx
            return (top * (1 - fy) + bot * fy).roundToInt().coerceIn(0, 255)
        }
        val a = blend(24); val r = blend(16); val g = blend(8); val b = blend(0)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
