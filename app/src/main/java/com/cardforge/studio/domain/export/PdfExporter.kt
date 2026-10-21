package com.cardforge.studio.domain.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.cardforge.studio.core.model.CardSide
import com.cardforge.studio.core.model.LayoutResult
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.Placement
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.model.Units
import com.cardforge.studio.core.template.TemplateRegistry
import java.io.OutputStream

/** Which side of the cards a given output page should render. */
enum class PrintSide { FRONT, BACK }

/**
 * Renders the laid-out cards to a print-ready, multi-page PDF.
 *
 * Page dimensions equal the true physical paper size in PostScript points, so
 * printing at 100 % scale reproduces real millimetre sizes. Aspect ratios are
 * preserved; images are never stretched.
 */
class PdfExporter {

    data class Options(
        val includeCutMarks: Boolean = true,
        val includeDashedCutLines: Boolean = false,
        val duplex: Boolean = false,
        val flipEdge: FlipEdge = FlipEdge.LONG_EDGE
    )

    enum class FlipEdge { LONG_EDGE, SHORT_EDGE }

    /**
     * @param imageFor returns the bitmap for a card copy on a given side, or
     *   null if that side has no image (a neutral placeholder is drawn).
     */
    fun export(
        layout: LayoutResult,
        spec: SheetSpec,
        out: OutputStream,
        options: Options = Options(),
        imageFor: (cardId: String, side: PrintSide) -> Bitmap?
    ) {
        val paper = TemplateRegistry.paperSizeMm(spec)
        val pageWpt = Units.mmToPt(paper.widthMm).toInt().coerceAtLeast(1)
        val pageHpt = Units.mmToPt(paper.heightMm).toInt().coerceAtLeast(1)

        val doc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        val byPage = layout.placements.groupBy { it.page }
        val totalPages = (byPage.keys.maxOrNull() ?: 0) + 1

        for (pageIndex in 0 until totalPages) {
            // For duplex we emit a front pass then a back pass.
            val passes = if (options.duplex) listOf(PrintSide.FRONT, PrintSide.BACK) else listOf(PrintSide.FRONT)
            for (side in passes) {
                val info = PdfDocument.PageInfo.Builder(pageWpt, pageHpt, doc.pages.size + 1).create()
                val page = doc.startPage(info)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                val placements = byPage[pageIndex].orEmpty()
                for (p in placements) {
                    // In duplex the pass decides the side; otherwise each placement
                    // carries its own side (front+back side-by-side sheets).
                    val effectiveSide = when {
                        options.duplex -> side
                        p.side == CardSide.BACK -> PrintSide.BACK
                        else -> PrintSide.FRONT
                    }
                    val mapped = if (effectiveSide == PrintSide.BACK) {
                        mirror(p, paper.widthMm, paper.heightMm, options.flipEdge)
                    } else p
                    drawCard(canvas, mapped, imageFor(p.cardId, effectiveSide), paint, spec, options)
                }
                if (options.includeCutMarks) drawCutMarks(canvas, placements, spec, options)
                doc.finishPage(page)
            }
        }

        doc.writeTo(out)
        doc.close()
    }

    private fun drawCard(
        canvas: Canvas,
        p: Placement,
        bmp: Bitmap?,
        paint: Paint,
        spec: SheetSpec,
        options: Options
    ) {
        val rect = RectF(
            Units.mmToPt(p.xMm).toFloat(),
            Units.mmToPt(p.yMm).toFloat(),
            Units.mmToPt(p.xMm + p.widthMm).toFloat(),
            Units.mmToPt(p.yMm + p.heightMm).toFloat()
        )
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, rect, paint)
        } else {
            // Neutral placeholder — never fake document content.
            val border = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawRect(rect, border)
        }
        if (options.includeCutMarks) {
            val mark = Paint().apply { color = Color.BLACK; strokeWidth = 0.75f }
            canvas.drawRect(rect, mark)
        }
    }

    private fun drawCutMarks(canvas: Canvas, placements: List<Placement>, spec: SheetSpec, options: Options) {
        val dash = Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            if (options.includeDashedCutLines) {
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 4f), 0f)
            }
        }
        for (p in placements) {
            val rect = RectF(
                Units.mmToPt(p.xMm).toFloat(),
                Units.mmToPt(p.yMm).toFloat(),
                Units.mmToPt(p.xMm + p.widthMm).toFloat(),
                Units.mmToPt(p.yMm + p.heightMm).toFloat()
            )
            val len = 6f
            // Corner tick marks extending outward.
            canvas.drawLine(rect.left, rect.top, rect.left + len, rect.top, dash)
            canvas.drawLine(rect.left, rect.top, rect.left, rect.top + len, dash)
            canvas.drawLine(rect.right, rect.top, rect.right - len, rect.top, dash)
            canvas.drawLine(rect.right, rect.top, rect.right, rect.top + len, dash)
            canvas.drawLine(rect.left, rect.bottom, rect.left + len, rect.bottom, dash)
            canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - len, dash)
            canvas.drawLine(rect.right, rect.bottom, rect.right - len, rect.bottom, dash)
            canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - len, dash)
        }
    }

    /** Mirror a placement across the sheet for the duplex back pass. */
    private fun mirror(p: Placement, pageWmm: Double, pageHmm: Double, edge: FlipEdge): Placement {
        return when (edge) {
            FlipEdge.LONG_EDGE -> p.copy(xMm = pageWmm - p.xMm - p.widthMm) // flip horizontally
            FlipEdge.SHORT_EDGE -> p.copy(yMm = pageHmm - p.yMm - p.heightMm) // flip vertically
        }
    }
}
