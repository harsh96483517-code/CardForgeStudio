package com.cardforge.studio.core

import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.model.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun mmToPt_matchesPostScriptDefinition() {
        // 1 inch = 25.4 mm = 72 pt
        assertEquals(72.0, Units.mmToPt(25.4), 0.0001)
    }

    @Test
    fun ptToMm_roundTrips() {
        assertEquals(10.0, Units.ptToMm(Units.mmToPt(10.0)), 0.0001)
    }

    @Test
    fun mmToPx_at300dpi() {
        // 25.4 mm = 1 inch = 300 px at 300 dpi
        assertEquals(300.0, Units.mmToPx(25.4, 300), 0.0001)
    }

    @Test
    fun a4PortraitPoints() {
        val spec = SheetSpec(paper = PaperKind.A4, orientation = Orientation.PORTRAIT)
        val mm = com.cardforge.studio.core.template.TemplateRegistry.paperSizeMm(spec)
        assertEquals(210.0, mm.widthMm, 0.001)
        assertEquals(297.0, mm.heightMm, 0.001)
        // A4 in points is approximately 595 x 842
        assertEquals(595.0, Units.mmToPt(mm.widthMm), 1.0)
        assertEquals(842.0, Units.mmToPt(mm.heightMm), 1.0)
    }
}
