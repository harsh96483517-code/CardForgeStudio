package com.cardforge.studio.domain

import com.cardforge.studio.core.model.CardInstance
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.domain.layout.PageLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageLayoutEngineTest {

    private val id1 = SizeMm(85.6, 54.0)
    private val a4 = SheetSpec(paper = PaperKind.A4, orientation = Orientation.PORTRAIT, marginMm = 10.0, spacingMm = 4.0)

    private fun card(qty: Int, size: SizeMm = id1) = CardInstance(
        id = "c1", templateId = "pan", displayName = "PAN Card", sizeMm = size, quantity = qty
    )

    @Test
    fun expand_repeatsByQuantity() {
        val copies = PageLayoutEngine.expand(listOf(card(3)))
        assertEquals(3, copies.size)
    }

    @Test
    fun capacityFor_id1OnA4IsEight() {
        // printable 190 x 277; cols=2, rows=4
        assertEquals(8, PageLayoutEngine.capacityFor(id1, a4))
    }

    @Test
    fun fourCopiesFitOnOnePage() {
        val result = PageLayoutEngine.layout(listOf(card(4)), a4)
        assertEquals(4, result.placements.size)
        assertEquals(1, result.pageCount)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun overflow_addsPagesAndWarns_butNeverResizes() {
        val result = PageLayoutEngine.layout(listOf(card(20)), a4)
        assertEquals(20, result.placements.size)
        assertEquals(3, result.pageCount) // 8 per page -> 3 pages
        assertTrue(result.warnings.any { it.contains("Extra pages") })
        // Every placement keeps the requested physical size (no silent resize).
        result.placements.forEach {
            assertEquals(85.6, it.widthMm, 0.001)
            assertEquals(54.0, it.heightMm, 0.001)
        }
    }

    @Test
    fun placementsRespectMargins() {
        val result = PageLayoutEngine.layout(listOf(card(1)), a4)
        val p = result.placements.first()
        assertEquals(10.0, p.xMm, 0.001)
        assertEquals(10.0, p.yMm, 0.001)
    }

    @Test
    fun oversizedCardWarns() {
        val huge = SizeMm(500.0, 500.0)
        val result = PageLayoutEngine.layout(listOf(card(1, huge)), a4)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun emptyCardsYieldSingleEmptyPage() {
        val result = PageLayoutEngine.layout(emptyList(), a4)
        assertEquals(0, result.placements.size)
        assertEquals(1, result.pageCount)
    }

    @Test
    fun zeroPrintableAreaWarns() {
        val badSpec = a4.copy(marginMm = 500.0)
        val result = PageLayoutEngine.layout(listOf(card(1)), badSpec)
        assertTrue(result.warnings.isNotEmpty())
        assertEquals(0, result.placements.size)
    }
}
