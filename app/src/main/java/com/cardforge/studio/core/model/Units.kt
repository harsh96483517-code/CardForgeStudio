package com.cardforge.studio.core.model

/**
 * Unit conversions. A "point" here is the PostScript point used by PDF
 * (1 inch = 72 pt), so a PDF page created in points prints at 100 % scale.
 *
 * 1 inch = 25.4 mm, so 1 mm = 72 / 25.4 pt ≈ 2.8346456693 pt.
 */
object Units {
    const val MM_PER_INCH = 25.4
    const val PT_PER_INCH = 72.0
    const val PT_PER_MM = PT_PER_INCH / MM_PER_INCH

    fun mmToPt(mm: Double): Double = mm * PT_PER_MM
    fun ptToMm(pt: Double): Double = pt / PT_PER_MM
    fun mmToPx(mm: Double, dpi: Int): Double = mm / MM_PER_INCH * dpi
    fun pxToMm(px: Double, dpi: Int): Double = px / dpi * MM_PER_INCH
}
