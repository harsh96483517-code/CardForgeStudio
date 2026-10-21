package com.cardforge.studio.core.template

import com.cardforge.studio.core.model.CardSide
import com.cardforge.studio.core.model.DocumentTemplate
import com.cardforge.studio.core.model.FitMode
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.core.model.SheetSpec

/**
 * The single source of truth for Indian document card templates.
 *
 * Only geometry is defined here. No official artwork, seals, signatures,
 * numbers, QR codes or security features are ever produced by this app — the
 * user imports their own legitimate document images.
 *
 * Card sizes marked `sizeVerified = true` are well-known standards. Others are
 * editable presets because issuers differ; the app never forces a document
 * into an assumed universal size.
 */
object TemplateRegistry {

    /** ISO/IEC 7810 ID-1 — the credit-card form factor most Indian cards use. */
    private val ID1 = SizeMm(85.6, 54.0)

    val builtIn: List<DocumentTemplate> = listOf(
        // ---- Identity documents ----
        DocumentTemplate(
            id = "pan",
            name = "PAN Card",
            category = com.cardforge.studio.core.model.TemplateCategory.IDENTITY,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = true,
            defaultFit = FitMode.FIT,
            notes = "Standard ISO/IEC 7810 ID-1 card size."
        ),
        DocumentTemplate(
            id = "aadhaar",
            name = "Aadhaar Card",
            category = com.cardforge.studio.core.model.TemplateCategory.IDENTITY,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false,
            defaultFit = FitMode.FIT,
            notes = "Preset (ID-1). Editable — some issuances vary."
        ),
        DocumentTemplate(
            id = "voter-id",
            name = "Voter ID / EPIC",
            category = com.cardforge.studio.core.model.TemplateCategory.IDENTITY,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false,
            notes = "Preset (ID-1)."
        ),
        DocumentTemplate(
            id = "passport",
            name = "Passport Information Page",
            category = com.cardforge.studio.core.model.TemplateCategory.IDENTITY,
            cardSizeMm = SizeMm(125.0, 88.0),
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.FRONT,
            sizeVerified = false,
            defaultFit = FitMode.FIT,
            notes = "Editable preset for the data page area only."
        ),
        DocumentTemplate(
            id = "generic-id",
            name = "Generic Identity Card",
            category = com.cardforge.studio.core.model.TemplateCategory.IDENTITY,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false
        ),

        // ---- Driving & vehicle documents ----
        DocumentTemplate(
            id = "driving-licence",
            name = "Driving Licence (DL)",
            category = com.cardforge.studio.core.model.TemplateCategory.VEHICLE,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false,
            notes = "Preset (ID-1 card form)."
        ),
        DocumentTemplate(
            id = "vehicle-rc",
            name = "Vehicle Registration Certificate (RC)",
            category = com.cardforge.studio.core.model.TemplateCategory.VEHICLE,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false,
            notes = "Preset (ID-1 card form)."
        ),
        DocumentTemplate(
            id = "vehicle-insurance",
            name = "Vehicle Insurance Card",
            category = com.cardforge.studio.core.model.TemplateCategory.VEHICLE,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false
        ),
        DocumentTemplate(
            id = "vehicle-info",
            name = "Vehicle Information Document",
            category = com.cardforge.studio.core.model.TemplateCategory.VEHICLE,
            cardSizeMm = SizeMm(210.0, 99.0),
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.FRONT,
            sizeVerified = false,
            defaultFit = FitMode.FIT,
            notes = "Editable preset — sized for a half-page vehicle sheet."
        ),
        DocumentTemplate(
            id = "generic-vehicle",
            name = "Generic Vehicle Document",
            category = com.cardforge.studio.core.model.TemplateCategory.VEHICLE,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.FRONT,
            sizeVerified = false
        ),

        // ---- Other documents ----
        DocumentTemplate(
            id = "student-id",
            name = "Student ID Card",
            category = com.cardforge.studio.core.model.TemplateCategory.OTHER,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false
        ),
        DocumentTemplate(
            id = "employee-id",
            name = "Employee ID Card",
            category = com.cardforge.studio.core.model.TemplateCategory.OTHER,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false
        ),
        DocumentTemplate(
            id = "membership-card",
            name = "Membership Card",
            category = com.cardforge.studio.core.model.TemplateCategory.OTHER,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false
        ),
        DocumentTemplate(
            id = "business-card",
            name = "Business Card",
            category = com.cardforge.studio.core.model.TemplateCategory.OTHER,
            cardSizeMm = SizeMm(90.0, 54.0),
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false,
            notes = "Common 90 × 54 mm preset; editable."
        ),
        DocumentTemplate(
            id = "custom",
            name = "Custom Document Card",
            category = com.cardforge.studio.core.model.TemplateCategory.CUSTOM,
            cardSizeMm = ID1,
            orientation = Orientation.LANDSCAPE,
            sides = CardSide.TWO_SIDED,
            sizeVerified = false,
            notes = "Define your own width and height in millimetres."
        )
    )

    fun byId(id: String): DocumentTemplate? = builtIn.firstOrNull { it.id == id }

    fun byCategory(category: com.cardforge.studio.core.model.TemplateCategory): List<DocumentTemplate> =
        builtIn.filter { it.category == category }

    fun search(query: String): List<DocumentTemplate> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return builtIn
        return builtIn.filter {
            it.name.lowercase().contains(q) || it.category.label.lowercase().contains(q)
        }
    }

    /** Physical paper sizes in millimetres, portrait orientation. */
    fun paperSizeMm(spec: SheetSpec): SizeMm {
        val base = when (spec.paper) {
            PaperKind.A4 -> SizeMm(210.0, 297.0)
            PaperKind.A3 -> SizeMm(297.0, 420.0)
            PaperKind.LETTER -> SizeMm(215.9, 279.4)
            PaperKind.CUSTOM -> spec.customSize ?: SizeMm(210.0, 297.0)
        }
        return if (spec.orientation == Orientation.LANDSCAPE) base.swapped() else base
    }

    /** The printable area (paper minus margins), in millimetres. */
    fun printableAreaMm(spec: SheetSpec): SizeMm {
        val paper = paperSizeMm(spec)
        val m = spec.marginMm
        return SizeMm(
            (paper.widthMm - 2 * m).coerceAtLeast(0.0),
            (paper.heightMm - 2 * m).coerceAtLeast(0.0)
        )
    }
}
