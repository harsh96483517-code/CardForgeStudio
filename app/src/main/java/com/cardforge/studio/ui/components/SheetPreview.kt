package com.cardforge.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cardforge.studio.core.model.LayoutResult
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.template.TemplateRegistry

/**
 * Draws one sheet with its card placements, scaled to fit the available space.
 * When [onMove] is provided the cards can be dragged (millimetre coordinates).
 */
@Composable
fun SheetPreview(
    spec: SheetSpec,
    layout: LayoutResult,
    page: Int = 0,
    imageFor: (String) -> ImageBitmap? = { null },
    showCutMarks: Boolean = true,
    onMove: ((index: Int, xMm: Double, yMm: Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val paper = TemplateRegistry.paperSizeMm(spec)
    val pagePlacements = layout.placements.filter { it.page == page }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(Color(0xFF0A1424)),
        contentAlignment = Alignment.Center
    ) {
        val availW = with(density) { maxWidth.toPx() }
        val availH = with(density) { maxHeight.toPx() }
        val scale = minOf(availW / paper.widthMm, availH / paper.heightMm).toFloat()
        val sheetW = (paper.widthMm * scale).toFloat()
        val sheetH = (paper.heightMm * scale).toFloat()

        var dragIndex by remember { mutableStateOf(-1) }

        Box(Modifier.size(with(density) { sheetW.toDp() }, with(density) { sheetH.toDp() })) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (onMove != null) {
                            Modifier.pointerInput(layout, page, scale) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        dragIndex = pagePlacements.indexOfLast { p ->
                                            val l = p.xMm * scale; val t = p.yMm * scale
                                            val r = (p.xMm + p.widthMm) * scale
                                            val b = (p.yMm + p.heightMm) * scale
                                            start.x in l..r && start.y in t..b
                                        }
                                    },
                                    onDragEnd = { dragIndex = -1 },
                                    onDragCancel = { dragIndex = -1 }
                                ) { change, drag ->
                                    val p = pagePlacements.getOrNull(dragIndex)
                                    if (p != null) {
                                        val globalIndex = layout.placements.indexOf(p)
                                        val nx = (p.xMm + drag.x / scale).coerceIn(0.0, (paper.widthMm - p.widthMm).coerceAtLeast(0.0))
                                        val ny = (p.yMm + drag.y / scale).coerceIn(0.0, (paper.heightMm - p.heightMm).coerceAtLeast(0.0))
                                        onMove.invoke(globalIndex, nx, ny)
                                    }
                                    change.consume()
                                }
                            }
                        } else Modifier
                    )
            ) {
                // Paper.
                drawRect(Color.White, topLeft = Offset.Zero, size = Size(size.width, size.height))

                pagePlacements.forEach { p ->
                    val l = (p.xMm * scale).toFloat()
                    val t = (p.yMm * scale).toFloat()
                    val w = (p.widthMm * scale).toFloat()
                    val h = (p.heightMm * scale).toFloat()
                    val bmp = imageFor(p.cardId)
                    if (bmp != null) {
                        drawImage(
                            image = bmp,
                            dstOffset = IntOffset(l.toInt(), t.toInt()),
                            dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))
                        )
                    } else {
                        drawRect(Color(0xFFDCE4F0), topLeft = Offset(l, t), size = Size(w, h))
                    }
                    if (showCutMarks) {
                        drawRect(
                            Color(0xFF3D7EFF),
                            topLeft = Offset(l, t),
                            size = Size(w, h),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }
        }
    }
}
