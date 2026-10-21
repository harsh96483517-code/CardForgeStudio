package com.cardforge.studio.core.layout

import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.core.template.TemplateRegistry

/**
 * Ready-made sheet setups. A "2-up" sheet places two ID-1 cards side by side
 * (e.g. the front and back of an Aadhaar card) on one A4 page.
 */
object LayoutPresets {

    const val DEFAULT_SPACING_MM = 6.0

    /**
     * A centred grid of [cols] × [rows] cards on [paper]. The margin is chosen
     * so the block is centred on the short axis; extra room is left on the long
     * axis. Always leaves at least a 5 mm printable margin.
     */
    fun gridSpec(
        card: SizeMm,
        cols: Int,
        rows: Int,
        paper: PaperKind = PaperKind.A4,
        orientation: Orientation = Orientation.PORTRAIT,
        spacingMm: Double = DEFAULT_SPACING_MM,
        cutMarks: Boolean = true
    ): SheetSpec {
        val page = TemplateRegistry.paperSizeMm(
            SheetSpec(paper = paper, orientation = orientation)
        )
        val blockW = cols * card.widthMm + (cols - 1).coerceAtLeast(0) * spacingMm
        val blockH = rows * card.heightMm + (rows - 1).coerceAtLeast(0) * spacingMm
        val marginX = ((page.widthMm - blockW) / 2.0).coerceAtLeast(5.0)
        val marginY = ((page.heightMm - blockH) / 2.0).coerceAtLeast(5.0)
        return SheetSpec(
            paper = paper,
            orientation = orientation,
            marginMm = minOf(marginX, marginY),
            spacingMm = spacingMm,
            showCutMarks = cutMarks
        )
    }

    /** Two ID-1 cards side by side on one A4 (the "front + back" sheet). */
    fun twoUp(card: SizeMm = SizeMm(85.6, 54.0)): SheetSpec =
        gridSpec(card, cols = 2, rows = 1)

    /** Four cards (2 × 2) on one A4. */
    fun fourUp(card: SizeMm = SizeMm(85.6, 54.0)): SheetSpec =
        gridSpec(card, cols = 2, rows = 2)

    /** Eight cards (2 × 4) on one A4. */
    fun eightUp(card: SizeMm = SizeMm(85.6, 54.0)): SheetSpec =
        gridSpec(card, cols = 2, rows = 4)

    /** Six cards (2 × 3) on one A4. */
    fun sixUp(card: SizeMm = SizeMm(85.6, 54.0)): SheetSpec =
        gridSpec(card, cols = 2, rows = 3)
}
