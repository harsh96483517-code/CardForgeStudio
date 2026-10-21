package com.cardforge.studio.domain.crop

import com.cardforge.studio.core.model.CropQuad
import com.cardforge.studio.core.model.Pt
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * One detected card-like region on a page.
 *
 * [score] blends how much of the page it covers with how close its aspect ratio
 * is to the expected card ratio, so a real card beats a denser block of text.
 */
data class CardRegion(
    val quad: CropQuad,
    val confidence: Float,
    val areaFraction: Float,
    val score: Float
) {
    val centerX: Float
        get() = (quad.topLeft.x + quad.topRight.x + quad.bottomRight.x + quad.bottomLeft.x) / 4f
    val centerY: Float
        get() = (quad.topLeft.y + quad.topRight.y + quad.bottomRight.y + quad.bottomLeft.y) / 4f
}

/** Detects several card-like regions on one page (e.g. front and back cards). */
interface MultiCardDetector {
    fun detect(
        pixels: IntArray,
        width: Int,
        height: Int,
        maxCards: Int = 4,
        targetAspect: Float = 1.585f
    ): List<CardRegion>
}

/**
 * Pure-Kotlin multi-card detector.
 *
 * Strategy: Sobel edge map → column/row energy projection → segment the page
 * into bands → in every band cell take the bounding box of edges, keep the
 * cells that look like a card, and rank them by a score that rewards both
 * coverage and a card-like aspect ratio. Regions are returned best-first.
 *
 * This is what powers "auto-split an Aadhaar layout page into its front and
 * back cards". No content is generated — only geometry is detected.
 */
class GradientMultiCardDetector(private val maxDim: Int = 512) : MultiCardDetector {

    override fun detect(
        pixels: IntArray,
        width: Int,
        height: Int,
        maxCards: Int,
        targetAspect: Float
    ): List<CardRegion> {
        require(width > 0 && height > 0) { "Empty image" }

        val longEdge = max(width, height)
        val scale = if (longEdge > maxDim) maxDim.toFloat() / longEdge else 1f
        val sw = max(8, (width * scale).toInt())
        val sh = max(8, (height * scale).toInt())

        // Luma plane (downscaled).
        val luma = FloatArray(sw * sh)
        for (y in 0 until sh) {
            val sy = (y / scale).toInt().coerceIn(0, height - 1)
            for (x in 0 until sw) {
                val sx = (x / scale).toInt().coerceIn(0, width - 1)
                val p = pixels[sy * width + sx]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                luma[y * sw + x] = 0.299f * r + 0.587f * g + 0.114f * b
            }
        }

        // Sobel gradient magnitude.
        val grad = FloatArray(sw * sh)
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until sh - 1) {
            for (x in 1 until sw - 1) {
                val gx = -luma[(y - 1) * sw + (x - 1)] + luma[(y - 1) * sw + (x + 1)] +
                    -2f * luma[y * sw + (x - 1)] + 2f * luma[y * sw + (x + 1)] +
                    -luma[(y + 1) * sw + (x - 1)] + luma[(y + 1) * sw + (x + 1)]
                val gy = -luma[(y - 1) * sw + (x - 1)] - 2f * luma[(y - 1) * sw + x] - luma[(y - 1) * sw + (x + 1)] +
                    luma[(y + 1) * sw + (x - 1)] + 2f * luma[(y + 1) * sw + x] + luma[(y + 1) * sw + (x + 1)]
                val m = sqrt(gx * gx + gy * gy)
                grad[y * sw + x] = m
                sum += m
                sumSq += m * m
                count++
            }
        }
        if (count == 0) return emptyList()
        val mean = sum / count
        val std = sqrt(max(0.0, sumSq / count - mean * mean))
        val threshold = (mean + 1.0 * std).toFloat()

        val edge = BooleanArray(sw * sh)
        var total = 0
        for (y in 1 until sh - 1) {
            for (x in 1 until sw - 1) {
                if (grad[y * sw + x] > threshold) {
                    edge[y * sw + x] = true
                    total++
                }
            }
        }
        if (total == 0) return emptyList()

        val colEnergy = FloatArray(sw)
        val rowEnergy = FloatArray(sh)
        for (y in 1 until sh - 1) {
            for (x in 1 until sw - 1) {
                if (edge[y * sw + x]) {
                    colEnergy[x] += 1f
                    rowEnergy[y] += 1f
                }
            }
        }

        val colBands = bands(colEnergy)
        val rowBands = bands(rowEnergy)
        val minEdges = max(20, (total * 0.01).toInt())

        val regions = ArrayList<CardRegion>()
        for (colBand in colBands) {
            val x0 = colBand.first
            val x1 = colBand.last
            for (rowBand in rowBands) {
                val y0 = rowBand.first
                val y1 = rowBand.last
                // Bounding box of edges inside this band cell.
                var bx0 = Int.MAX_VALUE; var by0 = Int.MAX_VALUE
                var bx1 = -1; var by1 = -1; var inCell = 0
                for (y in y0..y1) {
                    for (x in x0..x1) {
                        if (edge[y * sw + x]) {
                            if (x < bx0) bx0 = x
                            if (x > bx1) bx1 = x
                            if (y < by0) by0 = y
                            if (y > by1) by1 = y
                            inCell++
                        }
                    }
                }
                if (inCell < minEdges || bx1 < bx0 || by1 < by0) continue

                val bw = bx1 - bx0 + 1
                val bh = by1 - by0 + 1
                val areaFraction = (bw.toDouble() * bh.toDouble()) / (sw.toDouble() * sh.toDouble())
                if (areaFraction < 0.02 || areaFraction > 0.98) continue
                val aspect = bw.toFloat() / bh.toFloat()
                if (aspect < 0.3f || aspect > 3.5f) continue

                // Pad slightly so the card's border is included.
                val pad = max(2, (0.02f * min(bw, bh)).toInt())
                val px0 = max(0, bx0 - pad)
                val py0 = max(0, by0 - pad)
                val px1 = min(sw - 1, bx1 + pad)
                val py1 = min(sh - 1, by1 + pad)

                val dev = (aspect - targetAspect) / targetAspect
                val plausibility = exp(-(dev * dev) * 8.0).toFloat()
                val score = (areaFraction.toFloat() * plausibility)

                val inv = 1f / scale
                val quad = CropQuad(
                    Pt(px0 * inv, py0 * inv),
                    Pt((px1 + 1) * inv, py0 * inv),
                    Pt((px1 + 1) * inv, (py1 + 1) * inv),
                    Pt(px0 * inv, (py1 + 1) * inv)
                )
                regions += CardRegion(
                    quad = quad,
                    confidence = inCell.toFloat() / total,
                    areaFraction = areaFraction.toFloat(),
                    score = score
                )
            }
        }

        return regions.sortedByDescending { it.score }.take(maxCards.coerceAtLeast(1))
    }

    /**
     * Contiguous runs of columns/rows carrying a meaningful share of the peak
     * energy. A low fraction (5 %) keeps a card's own interior together while
     * still treating the truly empty page margins/gaps as separators.
     */
    private fun bands(energy: FloatArray): List<IntRange> {
        val peak = energy.maxOrNull() ?: 0f
        if (peak <= 0f) return emptyList()
        val cut = max(1f, 0.05f * peak)
        val runs = ArrayList<IntRange>()
        var start = -1
        for (i in energy.indices) {
            val active = energy[i] >= cut
            if (active && start < 0) start = i
            if (!active && start >= 0) {
                runs += start until i
                start = -1
            }
        }
        if (start >= 0) runs += start until energy.size

        // Merge runs separated by a tiny gap.
        val merged = ArrayList<IntRange>()
        for (r in runs) {
            val last = merged.lastOrNull()
            if (last != null && r.first - last.last <= 3) {
                merged[merged.lastIndex] = last.first..r.last
            } else {
                merged += r
            }
        }
        return merged
    }
}

/** Picks which detected regions form a front/back pair. */
object CardRegionSelector {

    /**
     * Choose the best-scoring region, then the best region on the *opposite*
     * side of the page whose vertical centre is closest — i.e. the matching
     * front/back card. The pair is returned left-to-right (front, back).
     */
    fun pickFrontBack(regions: List<CardRegion>, pageWidthPx: Float): List<CardRegion> {
        if (regions.size < 2) return regions
        val first = regions.first()
        val mid = pageWidthPx / 2f
        val firstOnLeft = first.centerX < mid
        val opposite = regions.drop(1).filter { (it.centerX < mid) != firstOnLeft }
        val pool = opposite.ifEmpty { regions.drop(1) }
        val second = pool.minByOrNull { abs(it.centerY - first.centerY) } ?: return listOf(first)
        return listOf(first, second).sortedBy { it.centerX }
    }
}
