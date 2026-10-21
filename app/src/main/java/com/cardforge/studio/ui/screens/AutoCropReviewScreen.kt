package com.cardforge.studio.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.core.model.CropQuad
import com.cardforge.studio.core.model.Pt
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.BannerTone
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.navigation.Routes
import kotlin.math.sqrt

@Composable
fun AutoCropReviewScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    val page = state.pages.getOrNull(state.activePage)

    ScreenScaffold(title = "Auto Crop Review", onBack = { nav.popBackStack() }) { _ ->
        if (page == null) {
            InfoBanner("No page selected. Import a document first.")
            OutlinedButton(onClick = { nav.navigate(Routes.IMPORT) }) { Text("Go to import") }
            return@ScreenScaffold
        }

        Text(
            "Page ${state.activePage + 1} · ${page.widthPx} × ${page.heightPx} px",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val bitmap: ImageBitmap? = remember(page.imagePath) {
            runCatching { BitmapFactory.decodeFile(page.imagePath)?.asImageBitmap() }.getOrNull()
        }

        if (bitmap != null) {
            CropCanvas(
                image = bitmap,
                quad = state.quad,
                onQuadChange = { vm.setQuad(it) }
            )
        }

        val confidencePct = (state.confidence * 100).toInt()
        if (state.lowConfidence) {
            InfoBanner(
                "Detection is uncertain (confidence $confidencePct%). " +
                    "Drag the corner handles to set the crop, then confirm.",
                tone = BannerTone.WARN
            )
        } else {
            InfoBanner("Detected crop · confidence $confidencePct%.", tone = BannerTone.SUCCESS)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { vm.runDetectionForActivePage() },
                modifier = Modifier.weight(1f),
                enabled = !state.busy
            ) { Text("Re-detect") }
            OutlinedButton(
                onClick = { nav.navigate(Routes.FRONT_BACK) },
                modifier = Modifier.weight(1f)
            ) { Text("Front/Back") }
        }

        Button(
            onClick = {
                vm.acceptCrop(asBack = false)
                nav.navigate(Routes.SIZE)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.quad != null && !state.busy
        ) { Text("Accept crop as front") }

        OutlinedButton(
            onClick = { vm.acceptCrop(asBack = true) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.project.cards.isNotEmpty() && !state.busy
        ) { Text("Accept crop as back of last card") }

        GlassCard {
            Text("Tip", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "For photographed documents, align the four handles with the card's " +
                    "corners — the crop is perspective-corrected to a rectangle. " +
                    "Content is never discarded silently.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        state.message?.let { InfoBanner(it, tone = BannerTone.INFO) }
    }
}

/** Draws the page with a draggable 4-corner crop overlay. */
@Composable
private fun CropCanvas(
    image: ImageBitmap,
    quad: CropQuad?,
    onQuadChange: (CropQuad) -> Unit
) {
    val density = LocalDensity.current
    val imgW = image.width.toFloat()
    val imgH = image.height.toFloat()

    var localQuad by remember(image) {
        mutableStateOf(
            quad ?: CropQuad(Pt(0f, 0f), Pt(imgW, 0f), Pt(imgW, imgH), Pt(0f, imgH))
        )
    }
    LaunchedEffect(quad) { if (quad != null) localQuad = quad }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0xFF0A1424)),
        contentAlignment = Alignment.Center
    ) {
        val availW = with(density) { maxWidth.toPx() }
        val availH = with(density) { maxHeight.toPx() }
        val scale = minOf(availW / imgW, availH / imgH)
        val dispW = imgW * scale
        val dispH = imgH * scale

        Box(Modifier.size(with(density) { dispW.toDp() }, with(density) { dispH.toDp() })) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(image, scale) {
                        var active = -1
                        detectDragGestures(
                            onDragStart = { start ->
                                val pts = localQuad.points.map { Offset(it.x * scale, it.y * scale) }
                                active = nearestIndex(pts, start)
                            },
                            onDragEnd = { active = -1 },
                            onDragCancel = { active = -1 }
                        ) { change, drag ->
                            if (active in 0..3) {
                                val pts = localQuad.points.toMutableList()
                                val p = pts[active]
                                val nx = (p.x + drag.x / scale).coerceIn(0f, imgW)
                                val ny = (p.y + drag.y / scale).coerceIn(0f, imgH)
                                pts[active] = Pt(nx, ny)
                                localQuad = CropQuad(pts[0], pts[1], pts[2], pts[3])
                                onQuadChange(localQuad)
                            }
                            change.consume()
                        }
                    }
            ) {
                drawImage(
                    image = image,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
                val pts = localQuad.points.map { Offset(it.x * scale, it.y * scale) }
                val lineColor = Color(0xFF3D7EFF)
                for (i in pts.indices) {
                    val a = pts[i]
                    val b = pts[(i + 1) % pts.size]
                    drawLine(lineColor, a, b, strokeWidth = 3f)
                }
                pts.forEach { p ->
                    drawCircle(Color.White, radius = 11f, center = p)
                    drawCircle(lineColor, radius = 11f, center = p, style = Stroke(width = 4f))
                }
            }
        }
    }
}

private fun nearestIndex(points: List<Offset>, target: Offset): Int {
    var best = -1
    var bestD = Float.MAX_VALUE
    points.forEachIndexed { i, p ->
        val dx = p.x - target.x
        val dy = p.y - target.y
        val d = sqrt(dx * dx + dy * dy)
        if (d < bestD) { bestD = d; best = i }
    }
    return if (bestD <= 90f) best else -1
}
