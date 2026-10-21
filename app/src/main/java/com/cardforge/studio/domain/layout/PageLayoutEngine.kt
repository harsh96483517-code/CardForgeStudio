package com.cardforge.studio.domain.layout

import com.cardforge.studio.core.model.CardInstance
import com.cardforge.studio.core.model.LayoutResult
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.Placement
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.core.template.TemplateRegistry
import kotlin.math.floor

/**
 * Pure layout engine. Packs card copies onto one or more sheets.
 *
 * It never silently resizes a document to make it fit: if the requested copies
 * overflow the sheet it reports [LayoutResult.overflowCount] and a warning so
 * the UI can suggest more pages or a different layout.
 */
object PageLayoutEngine {

    /** Expand each card's `quantity` into that many copies. */
    fun expand(cards: List<CardInstance>): List<CardInstance> =
        cards.flatMap { c -> List(c.quantity.coerceAtLeast(1)) { c } }

    /**
     * Lay out [cards] (quantities respected) onto sheets described by [spec].
     * Cards are packed in a simple row-major grid; the grid cell size is the
     * largest card in the set so the result stays tidy and cuttable.
     */
    fun layout(cards: List<CardInstance>, spec: SheetSpec): LayoutResult {
        val copies = expand(cards)
        if (copies.isEmpty()) {
            return LayoutResult(emptyList(), pageCount = 1, overflowCount = 0, warnings = emptyList())
        }

        val printable = TemplateRegistry.printableAreaMm(spec)
        if (printable.widthMm <= 0.0 || printable.heightMm <= 0.0) {
            return LayoutResult(
                emptyList(), 0, copies.size,
                listOf("Margins leave no printable area. Reduce margins or choose a larger paper.")
            )
        }

        val warnings = mutableListOf<String>()
        val spacing = spec.spacingMm

        // Columns/rows that fit, using the first card's size as the grid cell.
        val cellW = copies.maxOf { it.sizeMm.widthMm }
        val cellH = copies.maxOf { it.sizeMm.heightMm }

        val cols = floor((printable.widthMm + spacing) / (cellW + spacing)).toInt().coerceAtLeast(1)
        val rows = floor((printable.heightMm + spacing) / (cellH + spacing)).toInt().coerceAtLeast(1)
        val perPage = cols * rows

        if (perPage < 1) {
            warnings += "A single card is larger than the printable area. Choose a bigger paper or smaller card."
        } else if (perPage < copies.size) {
            warnings += "Requested ${copies.size} cards; $perPage fit per sheet. " +
                "Extra pages were added — nothing was resized."
        }

        // Warn if a card's own footprint exceeds the cell (mixed sizes).
        val oversized = copies.filter { it.sizeMm.widthMm > printable.widthMm || it.sizeMm.heightMm > printable.heightMm }
        if (oversized.isNotEmpty()) {
            warnings += "${oversized.size} card(s) are larger than the printable area and may be clipped."
        }

        val placements = ArrayList<Placement>(copies.size)
        copies.forEachIndexed { index, card ->
            val page = index / perPage
            val slot = index % perPage
            val col = slot % cols
            val row = slot / cols
            val x = spec.marginMm + col * (cellW + spacing)
            val y = spec.marginMm + row * (cellH + spacing)
            placements += Placement(
                cardId = card.id,
                page = page,
                xMm = x,
                yMm = y,
                widthMm = card.sizeMm.widthMm,
                heightMm = card.sizeMm.heightMm
            )
        }

        val pages = (placements.maxOfOrNull { it.page } ?: 0) + 1
        val overflow = (copies.size - perPage * pages).coerceAtLeast(0)

        return LayoutResult(
            placements = placements,
            pageCount = pages,
            overflowCount = overflow,
            warnings = warnings
        )
    }

    /** How many copies of a given card size fit on one sheet. */
    fun capacityFor(cardSize: SizeMm, spec: SheetSpec): Int {
        val printable = TemplateRegistry.printableAreaMm(spec)
        val cols = floor((printable.widthMm + spec.spacingMm) / (cardSize.widthMm + spec.spacingMm)).toInt()
        val rows = floor((printable.heightMm + spec.spacingMm) / (cardSize.heightMm + spec.spacingMm)).toInt()
        return (cols.coerceAtLeast(0)) * (rows.coerceAtLeast(0))
    }

    /** Mirror the sheet orientation of [spec] for a quick 100 % preview. */
    fun previewAspect(spec: SheetSpec): Float {
        val p = TemplateRegistry.paperSizeMm(spec)
        return (p.widthMm / p.heightMm).toFloat()
    }

    fun effectiveOrientation(spec: SheetSpec): Orientation = spec.orientation
}
