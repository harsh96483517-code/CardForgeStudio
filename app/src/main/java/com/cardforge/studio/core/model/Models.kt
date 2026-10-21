package com.cardforge.studio.core.model

import kotlinx.serialization.Serializable

/** Broad groupings used by the template gallery's category chips. */
@Serializable
enum class TemplateCategory(val label: String) {
    IDENTITY("Identity Documents"),
    VEHICLE("Driving & Vehicle Documents"),
    OTHER("Other Documents"),
    CUSTOM("Custom")
}

/** Which physical sides a document has. */
@Serializable
enum class CardSide { FRONT, BACK, TWO_SIDED }

@Serializable
enum class Orientation(val label: String) { PORTRAIT("Portrait"), LANDSCAPE("Landscape") }

/** How an imported image is mapped into the card's frame. */
@Serializable
enum class FitMode(val label: String) {
    FIT("Fit"),           // whole image visible, letterboxed
    FILL("Fill"),         // cover, may crop edges
    ORIGINAL("Original Ratio") // no scaling beyond the crop's own ratio
}

@Serializable
enum class PaperKind(val label: String) { A4("A4"), A3("A3"), LETTER("Letter"), CUSTOM("Custom") }

/** A physical size in millimetres. */
@Serializable
data class SizeMm(val widthMm: Double, val heightMm: Double) {
    val areaMm2: Double get() = widthMm * heightMm
    fun swapped(): SizeMm = SizeMm(heightMm, widthMm)
}

/** A point in a bitmap's pixel space (kept JVM-safe: no android.graphics). */
@Serializable
data class Pt(val x: Float, val y: Float)

/** Four crop corners in source-pixel space, ordered TL, TR, BR, BL. */
@Serializable
data class CropQuad(val topLeft: Pt, val topRight: Pt, val bottomRight: Pt, val bottomLeft: Pt) {
    val points: List<Pt> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)
}

/** An axis-aligned crop rectangle in source-pixel space. */
@Serializable
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun clampTo(w: Float, h: Float): CropRect = CropRect(
        left.coerceIn(0f, w), top.coerceIn(0f, h),
        right.coerceIn(0f, w), bottom.coerceIn(0f, h)
    )

    companion object {
        fun full(w: Float, h: Float) = CropRect(0f, 0f, w, h)
    }
}

/**
 * A template describes only GEOMETRY — crop area, aspect ratio, physical size
 * and orientation. It never describes, generates or imitates official artwork,
 * seals, signatures, QR codes or security features.
 */
@Serializable
data class DocumentTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val cardSizeMm: SizeMm,
    val orientation: Orientation,
    val sides: CardSide,
    val sizeVerified: Boolean,
    val defaultFit: FitMode = FitMode.FIT,
    val notes: String? = null
) {
    val aspect: Double get() = cardSizeMm.widthMm / cardSizeMm.heightMm
    val isBuiltIn: Boolean get() = !id.startsWith("custom-")
}

/** One card placed into a project (front/back images + resolved geometry). */
@Serializable
data class CardInstance(
    val id: String,
    val templateId: String,
    val displayName: String,
    val frontImage: String? = null,
    val backImage: String? = null,
    val cropFront: CropRect? = null,
    val cropBack: CropRect? = null,
    val sizeMm: SizeMm,
    val fit: FitMode = FitMode.FIT,
    val quantity: Int = 1
)

/** Paper + margins + guides for the sheet the cards are arranged on. */
@Serializable
data class SheetSpec(
    val paper: PaperKind = PaperKind.A4,
    val customSize: SizeMm? = null,
    val orientation: Orientation = Orientation.PORTRAIT,
    val marginMm: Double = 10.0,
    val spacingMm: Double = 4.0,
    val showCutMarks: Boolean = true,
    val showDashedCutLines: Boolean = false,
    val snapToGrid: Boolean = true
)

/** A concrete placement of one card copy on one sheet, in millimetres. */
@Serializable
data class Placement(
    val cardId: String,
    val page: Int,
    val xMm: Double,
    val yMm: Double,
    val widthMm: Double,
    val heightMm: Double
)

/** The full in-progress or saved project. */
@Serializable
data class Project(
    val id: String,
    val name: String,
    val templateId: String? = null,
    val cards: List<CardInstance> = emptyList(),
    val sheet: SheetSpec = SheetSpec(),
    val createdAt: Long,
    val updatedAt: Long
)

/** Result of laying cards out on a sheet. */
data class LayoutResult(
    val placements: List<Placement>,
    val pageCount: Int,
    val overflowCount: Int,
    val warnings: List<String>
)
