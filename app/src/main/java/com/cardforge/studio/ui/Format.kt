package com.cardforge.studio.ui

/** Compact millimetre formatting: 85.6 -> "85.6", 54.0 -> "54". */
internal fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

/** Two-decimal formatting for layout coordinates. */
internal fun fmt2(v: Double): String = String.format("%.1f", v)
