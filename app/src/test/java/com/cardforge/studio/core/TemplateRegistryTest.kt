package com.cardforge.studio.core

import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.template.TemplateRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRegistryTest {

    @Test
    fun allTemplatesHavePositiveSizes() {
        TemplateRegistry.builtIn.forEach { t ->
            assertTrue("${t.name} width", t.cardSizeMm.widthMm > 0)
            assertTrue("${t.name} height", t.cardSizeMm.heightMm > 0)
        }
    }

    @Test
    fun idsAreUnique() {
        val ids = TemplateRegistry.builtIn.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun panCardIsVerifiedId1() {
        val pan = TemplateRegistry.byId("pan")!!
        assertTrue(pan.sizeVerified)
        assertEquals(85.6, pan.cardSizeMm.widthMm, 0.001)
        assertEquals(54.0, pan.cardSizeMm.heightMm, 0.001)
    }

    @Test
    fun everyCategoryIsRepresented() {
        val cats = TemplateRegistry.builtIn.map { it.category }.toSet()
        assertTrue(cats.contains(com.cardforge.studio.core.model.TemplateCategory.IDENTITY))
        assertTrue(cats.contains(com.cardforge.studio.core.model.TemplateCategory.VEHICLE))
        assertTrue(cats.contains(com.cardforge.studio.core.model.TemplateCategory.OTHER))
        assertTrue(cats.contains(com.cardforge.studio.core.model.TemplateCategory.CUSTOM))
    }

    @Test
    fun searchFindsByPartialName() {
        val results = TemplateRegistry.search("aadhaar")
        assertEquals(1, results.size)
        assertEquals("aadhaar", results.first().id)
    }

    @Test
    fun paperSizeLandscapeIsSwapped() {
        val portrait = TemplateRegistry.paperSizeMm(SheetSpec(paper = PaperKind.A4, orientation = Orientation.PORTRAIT))
        val landscape = TemplateRegistry.paperSizeMm(SheetSpec(paper = PaperKind.A4, orientation = Orientation.LANDSCAPE))
        assertEquals(portrait.widthMm, landscape.heightMm, 0.001)
        assertEquals(portrait.heightMm, landscape.widthMm, 0.001)
    }

    @Test
    fun printableAreaSubtractsMargins() {
        val spec = SheetSpec(paper = PaperKind.A4, orientation = Orientation.PORTRAIT, marginMm = 10.0)
        val area = TemplateRegistry.printableAreaMm(spec)
        assertEquals(190.0, area.widthMm, 0.001)
        assertEquals(277.0, area.heightMm, 0.001)
    }

    @Test
    fun letterSize() {
        val spec = SheetSpec(paper = PaperKind.LETTER, orientation = Orientation.PORTRAIT)
        val mm = TemplateRegistry.paperSizeMm(spec)
        assertEquals(215.9, mm.widthMm, 0.01)
        assertEquals(279.4, mm.heightMm, 0.01)
    }
}
