package com.cardforge.studio.domain.layout

import com.cardforge.studio.core.model.CardInstance
import com.cardforge.studio.core.model.CardSide
import com.cardforge.studio.core.model.LayoutResult
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.Placement
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.model.SidesMode
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

    /** One printable item: a card copy plus which side it prints. */
    data class SheetItem(val card: CardInstance, val side: CardSide)

    /** Expand each card's `quantity` into that many copies (front side). */
    fun expand(cards: List<CardInstance>): List<CardInstance> =
        cards.flatMap { c -> List(c.quantity.coerceAtLeast(1)) { c } }

    /**
     * Expand into (card, side) items, honouring the sheet's [SidesMode]:
     *  - FRONT_ONLY   : one front item per copy.
     *  - SIDE_BY_SIDE : a front item *and* a back item per copy, printed together.
     *  - DUPLEX       : one front item per copy; the exporter emits the back pass.
     */
    fun expandWithSides(cards: List<CardInstance>, spec: SheetSpec): List<SheetItem> {
        val out = ArrayList<SheetItem>()
        cards.forEach { c ->
            repeat(c.quantity.coerceAtLeast(1)) {
                out += SheetItem(c, CardSide.FRONT)
                if (spec.sidesMode == SidesMode.SIDE_BY_SIDE) out += SheetItem(c, CardSide.BACK)
            }
        }
        return out
    }

    /**
     * Lay out [cards] (quantities and sides respected) onto sheets described by
     * [spec]. Cards are packed in a simple row-major grid; the grid cell size is
     * the largest card in the set so the result stays tidy and cuttable.
     */
    fun layout(cards: List<CardInstance>, spec: SheetSpec): LayoutResult {
        val items = expandWithSides(cards, spec)
        if (items.isEmpty()) {
            return LayoutResult(emptyList(), pageCount = 1, overflowCount = 0, warnings = emptyList())
        }

        val printable = TemplateRegistry.printableAreaMm(spec)
        if (printable.widthMm <= 0.0 || printable.heightMm <= 0.0) {
            return LayoutResult(
                emptyList(), 0, items.size,
                listOf("Margins leave no printable area. Reduce margins or choose a larger paper.")
            )
        }

        val warnings = mutableListOf<String>()
        val spacing = spec.spacingMm

        // Columns/rows that fit, using the largest card as the grid cell.
        val cellW = items.maxOf { it.card.sizeMm.widthMm }
        val cellH = items.maxOf { it.card.sizeMm.heightMm }

        val cols = floor((printable.widthMm + spacing) / (cellW + spacing)).toInt().coerceAtLeast(1)
        val rows = floor((printable.heightMm + spacing) / (cellH + spacing)).toInt().coerceAtLeast(1)
        val perPage = cols * rows

        if (perPage < 1) {
            warnings += "A single card is larger than the printable area. Choose a bigger paper or smaller card."
        } else if (perPage < items.size) {
            warnings += "Requested ${items.size} card side(s); $perPage fit per sheet. " +
                "Extra pages were added — nothing was resized."
        }

        // Warn if a card's own footprint exceeds the sheet.
        val oversized = items.filter {
            it.card.sizeMm.widthMm > printable.widthMm || it.card.sizeMm.heightMm > printable.heightMm
        }
        if (oversized.isNotEmpty()) {
            warnings += "${oversized.size} card(s) are larger than the printable area and may be clipped."
        }

        val placements = ArrayList<Placement>(items.size)
        items.forEachIndexed { index, item ->
            val page = index / perPage
            val slot = index % perPage
            val col = slot % cols
            val row = slot / cols
            val x = spec.marginMm + col * (cellW + spacing)
            val y = spec.marginMm + row * (cellH + spacing)
            placements += Placement(
                cardId = item.card.id,
                page = page,
                xMm = x,
                yMm = y,
                widthMm = item.card.sizeMm.widthMm,
                heightMm = item.card.sizeMm.heightMm,
                side = item.side
            )
        }

        val pages = (placements.maxOfOrNull { it.page } ?: 0) + 1
        val overflow = (items.size - perPage * pages).coerceAtLeast(0)

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
