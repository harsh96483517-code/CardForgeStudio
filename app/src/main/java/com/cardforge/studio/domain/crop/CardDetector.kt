package com.cardforge.studio.domain.crop

import com.cardforge.studio.core.model.CropQuad
import com.cardforge.studio.core.model.Pt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Result of automatic card detection.
 *
 * When [lowConfidence] is true the caller MUST ask the user to confirm the crop
 * rather than silently accepting it — document content is never discarded.
 */
data class DetectionResult(
    val quad: CropQuad,
    val confidence: Float,
    val lowConfidence: Boolean
)

/**
 * Automatic rectangular card detection.
 *
 * Pure Kotlin (no Android / no native OpenCV), operating on an ARGB pixel
 * array so it is unit-testable on the JVM. Strategy:
 *
 *  1. Downscale to at most [MAX_DIM] on the long edge for speed.
 *  2. Convert to luma and compute a Sobel gradient magnitude.
 *  3. Threshold to an edge map (mean + k·std).
 *  4. Project edge energy onto the x and y axes and take the span where the
 *     projection exceeds a fraction of its peak — this brackets the document.
 *  5. Confidence = fraction of total edge energy captured inside the span.
 *
 * To use the native OpenCV detector instead, implement [CardDetector] with
 * `org.opencv:opencv` and swap the binding — the interface is intentionally
 * narrow.
 */
interface CardDetector {
    fun detect(pixels: IntArray, width: Int, height: Int): DetectionResult
}

class GradientCardDetector(
    private val maxDim: Int = 512,
    private val sensitivity: Float = 0.5f
) : CardDetector {

    companion object {
        private const val MAX_DIM = 512
    }

    override fun detect(pixels: IntArray, width: Int, height: Int): DetectionResult {
        require(width > 0 && height > 0) { "Empty image" }
        val fullFrame = CropQuad(
            Pt(0f, 0f), Pt(width.toFloat(), 0f),
            Pt(width.toFloat(), height.toFloat()), Pt(0f, height.toFloat())
        )

        // 1. Downscale factor.
        val longEdge = max(width, height)
        val scale = if (longEdge > maxDim) maxDim.toFloat() / longEdge else 1f
        val sw = max(8, (width * scale).toInt())
        val sh = max(8, (height * scale).toInt())

        // 2. Luma plane.
        val luma = FloatArray(sw * sh)
        for (y in 0 until sh) {
            val sy = (y / scale).toInt().coerceIn(0, height - 1)
            for (x in 0 until sw) {
                val sx = (x / scale).toInt().coerceIn(0, width - 1)
                val p = pixels[sy * width + sx]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                luma[y * sw + x] = (0.299f * r + 0.587f * g + 0.114f * b)
            }
        }

        // Sobel gradient magnitude.
        val grad = FloatArray(sw * sh)
        var sum = 0.0
        var sumSq = 0.0
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
            }
        }
        val n = (sw - 2).toDouble() * (sh - 2).toDouble()
        val mean = if (n > 0) sum / n else 0.0
        val variance = if (n > 0) (sumSq / n) - mean * mean else 0.0
        val std = sqrt(max(0.0, variance))
        val threshold = (mean + (1.0 + sensitivity) * std).toFloat()

        // 3. Edge map projections.
        val colEnergy = FloatArray(sw)
        val rowEnergy = FloatArray(sh)
        var total = 0.0
        for (y in 1 until sh - 1) {
            for (x in 1 until sw - 1) {
                if (grad[y * sw + x] > threshold) {
                    colEnergy[x] += 1f
                    rowEnergy[y] += 1f
                    total += 1.0
                }
            }
        }

        val maxCol = colEnergy.maxOrNull() ?: 0f
        val maxRow = rowEnergy.maxOrNull() ?: 0f
        if (total < 1.0 || maxCol <= 0f || maxRow <= 0f) {
            return DetectionResult(fullFrame, confidence = 0f, lowConfidence = true)
        }

        val colCut = maxCol * 0.15f
        val rowCut = maxRow * 0.15f
        var left = -1; var right = -1; var top = -1; var bottom = -1
        for (x in 0 until sw) if (colEnergy[x] >= colCut) { if (left < 0) left = x; right = x }
        for (y in 0 until sh) if (rowEnergy[y] >= rowCut) { if (top < 0) top = y; bottom = y }
        if (left < 0 || top < 0) return DetectionResult(fullFrame, 0f, true)

        // Map back to source pixels.
        val inv = 1f / scale
        val l = left * inv
        val t = top * inv
        val r = (right + 1) * inv
        val b = (bottom + 1) * inv
        val quad = CropQuad(Pt(l, t), Pt(r, t), Pt(r, b), Pt(l, b))

        // 5. Confidence = how much of the frame the detected span covers.
        val spanArea = (right - left + 1).toDouble() * (bottom - top + 1).toDouble()
        val frameArea = sw.toDouble() * sh.toDouble()
        val coverage = (spanArea / frameArea).coerceIn(0.0, 1.0)

        // A plausible document occupies between ~8 % and ~98 % of the frame.
        val plausible = coverage in 0.08..0.98
        val aspectOk = run {
            val w = (r - l); val h = (b - t)
            val a = if (h <= 0f) 0f else w / h
            a in 0.4f..3.0f
        }
        val confidence = (coverage.coerceAtMost(1.0) * (if (aspectOk) 1.0 else 0.5)).toFloat()
        val low = !plausible || !aspectOk || confidence < 0.25f

        return DetectionResult(quad, confidence, low)
    }
}
