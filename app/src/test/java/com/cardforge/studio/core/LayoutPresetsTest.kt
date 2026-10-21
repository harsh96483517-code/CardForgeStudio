package com.cardforge.studio.core

import com.cardforge.studio.core.layout.LayoutPresets
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.core.model.SidesMode
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.core.template.TemplateRegistry
import com.cardforge.studio.domain.layout.PageLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutPresetsTest {

    private val id1 = SizeMm(85.6, 54.0)

    @Test
    fun twoUpCentresTwoCardsOnA4() {
        val spec = LayoutPresets.twoUp(id1)
        assertEquals(PaperKind.A4, spec.paper)
        // (210 - (85.6*2 + 6)) / 2 = 16.4 mm
        assertEquals(16.4, spec.marginMm, 0.2)
        assertEquals(6.0, spec.spacingMm, 0.001)
    }

    @Test
    fun twoUpFitsAtLeastTwoCards() {
        val spec = LayoutPresets.twoUp(id1)
        assertTrue(PageLayoutEngine.capacityFor(id1, spec) >= 2)
    }

    @Test
    fun fourUpFitsAtLeastFour() {
        val spec = LayoutPresets.fourUp(id1)
        assertTrue(PageLayoutEngine.capacityFor(id1, spec) >= 4)
    }

    @Test
    fun eightUpFitsAtLeastEight() {
        val spec = LayoutPresets.eightUp(id1)
        assertTrue(PageLayoutEngine.capacityFor(id1, spec) >= 8)
    }

    @Test
    fun gridSpecNeverDropsBelowPrintableMargin() {
        // A card almost as big as the page must still leave >= 5 mm margin.
        val spec = LayoutPresets.gridSpec(SizeMm(200.0, 280.0), cols = 1, rows = 1)
        assertTrue(spec.marginMm >= 5.0)
    }

    @Test
    fun twoUpLayoutProducesSideBySidePlacements() {
        val spec = LayoutPresets.twoUp(id1).copy(sidesMode = SidesMode.SIDE_BY_SIDE)
        val card = com.cardforge.studio.core.model.CardInstance(
            id = "c1", templateId = "aadhaar", displayName = "Aadhaar", sizeMm = id1, quantity = 1
        )
        val result = PageLayoutEngine.layout(listOf(card), spec)
        assertEquals(2, result.placements.size)
        assertEquals(1, result.pageCount)
        // Same row, different columns, no overlap.
        val a = result.placements[0]
        val b = result.placements[1]
        assertEquals(a.yMm, b.yMm, 0.001)
        assertTrue(b.xMm > a.xMm + a.widthMm)
        assertEquals(TemplateRegistry.paperSizeMm(spec).widthMm, 210.0, 0.001)
    }
}
