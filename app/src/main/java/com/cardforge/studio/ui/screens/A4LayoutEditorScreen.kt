package com.cardforge.studio.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.core.layout.LayoutPresets
import com.cardforge.studio.core.model.CardSide
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.core.model.SidesMode
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.BannerTone
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.KeyValueRow
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.components.SectionHeader
import com.cardforge.studio.ui.components.SheetPreview
import com.cardforge.studio.ui.fmt
import com.cardforge.studio.ui.navigation.Routes
import kotlin.math.roundToInt

@Composable
fun A4LayoutEditorScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    val spec = state.project.sheet
    val layout = state.layout
    var page by remember { mutableIntStateOf(0) }

    // Decode the front and back images per card for the sheet preview.
    val images = remember(state.project.cards) {
        state.project.cards.associate { c ->
            val f = c.frontImage?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
            val b = c.backImage?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
            c.id to (f to b)
        }
    }

    ScreenScaffold(title = "A4 Layout Studio", onBack = { nav.popBackStack() }) { _ ->
        if (state.project.cards.isEmpty()) {
            InfoBanner("Add at least one card before arranging the sheet.")
            OutlinedButton(onClick = { nav.navigate(Routes.AUTOCROP) }) { Text("Go to crop") }
            return@ScreenScaffold
        }

        GlassCard {
            SectionHeader("Paper")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperKind.entries.forEach { kind ->
                    FilterChip(
                        selected = spec.paper == kind,
                        onClick = { vm.setSheet(spec.copy(paper = kind)) },
                        label = { Text(kind.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.setSheet(spec.copy(orientation = Orientation.PORTRAIT)) }) {
                    Text("Portrait")
                }
                OutlinedButton(onClick = { vm.setSheet(spec.copy(orientation = Orientation.LANDSCAPE)) }) {
                    Text("Landscape")
                }
            }
        }

        GlassCard {
            SectionHeader("Card sides", "Print the front and back together, or one at a time")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SidesMode.entries.forEach { mode ->
                    FilterChip(
                        selected = spec.sidesMode == mode,
                        onClick = { vm.setSidesMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        GlassCard {
            SectionHeader("Quick presets", "Centred grids on A4")
            Spacer(Modifier.height(8.dp))
            val cardSize = state.project.cards.firstOrNull()?.sizeMm ?: SizeMm(85.6, 54.0)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "2-up" to (2 to 1),
                    "4-up" to (2 to 2),
                    "6-up" to (2 to 3),
                    "8-up" to (2 to 4)
                ).forEach { (label, grid) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            vm.applySheetPreset(LayoutPresets.gridSpec(cardSize, grid.first, grid.second))
                        },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = spec.sidesMode == SidesMode.SIDE_BY_SIDE && spec.marginMm < 20.0,
                    onClick = {
                        vm.setSidesMode(SidesMode.SIDE_BY_SIDE)
                        vm.applySheetPreset(LayoutPresets.twoUp(cardSize))
                    },
                    label = { Text("Front + back") }
                )
            }
        }

        GlassCard {
            SectionHeader("Spacing")
            Spacer(Modifier.height(6.dp))
            Text("Margin: ${fmt(spec.marginMm)} mm", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = spec.marginMm.toFloat(),
                onValueChange = { vm.setSheet(spec.copy(marginMm = it.toDouble())) },
                valueRange = 0f..30f
            )
            Text("Card spacing: ${fmt(spec.spacingMm)} mm", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = spec.spacingMm.toFloat(),
                onValueChange = { vm.setSheet(spec.copy(spacingMm = it.toDouble())) },
                valueRange = 0f..20f
            )
        }

        GlassCard {
            SectionHeader("Guides & snapping")
            Spacer(Modifier.height(8.dp))
            SwitchRow("Cut marks", spec.showCutMarks) { vm.setSheet(spec.copy(showCutMarks = it)) }
            SwitchRow("Dashed cutting lines", spec.showDashedCutLines) { vm.setSheet(spec.copy(showDashedCutLines = it)) }
            SwitchRow("Snap to grid", spec.snapToGrid) { vm.setSheet(spec.copy(snapToGrid = it)) }
        }

        if (layout != null) {
            GlassCard {
                SectionHeader("Sheet preview", "Drag a card to position it manually")
                Spacer(Modifier.height(10.dp))
                SheetPreview(
                    spec = spec,
                    layout = layout,
                    page = page,
                    imageFor = { id, side ->
                        val p = images[id]
                        if (side == CardSide.BACK) p?.second else p?.first
                    },
                    showCutMarks = spec.showCutMarks,
                    onMove = { index, x, y -> vm.movePlacement(index, x, y) }
                )
                Spacer(Modifier.height(10.dp))
                KeyValueRow("Cards placed", layout.placements.size.toString())
                KeyValueRow("Pages", layout.pageCount.toString())
                KeyValueRow("Per sheet", (layout.placements.count { it.page == 0 }).toString())
                if (layout.pageCount > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0 until layout.pageCount).forEach { p ->
                            FilterChip(
                                selected = page == p,
                                onClick = { page = p },
                                label = { Text("Page ${p + 1}") }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.resetManualPositions() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset manual positions")
                }
            }

            layout.warnings.forEach { InfoBanner(it, tone = BannerTone.WARN) }
            if (layout.warnings.isEmpty()) {
                InfoBanner("All requested copies fit. Nothing was resized.", tone = BannerTone.SUCCESS)
            }
        }

        Button(onClick = { nav.navigate(Routes.PREVIEW) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview")
        }
        OutlinedButton(onClick = { nav.navigate(Routes.EXPORT) }, modifier = Modifier.fillMaxWidth()) {
            Text("Export PDF")
        }

        state.message?.let { InfoBanner(it, tone = BannerTone.INFO) }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
