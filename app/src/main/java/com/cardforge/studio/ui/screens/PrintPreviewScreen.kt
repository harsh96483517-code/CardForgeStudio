package com.cardforge.studio.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.core.template.TemplateRegistry
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

@Composable
fun PrintPreviewScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    val spec = state.project.sheet
    val layout = state.layout
    val paper = TemplateRegistry.paperSizeMm(spec)
    var page by remember { mutableIntStateOf(0) }
    var showBack by remember { mutableStateOf(false) }

    val images = remember(state.project.cards, showBack) {
        state.project.cards.associate { c ->
            val path = if (showBack) c.backImage else c.frontImage
            c.id to (path?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() })
        }
    }

    ScreenScaffold(title = "Print Preview", onBack = { nav.popBackStack() }) { _ ->
        if (layout == null || layout.placements.isEmpty()) {
            InfoBanner("Nothing to preview yet.")
            OutlinedButton(onClick = { nav.navigate(Routes.LAYOUT) }) { Text("Go to layout") }
            return@ScreenScaffold
        }

        GlassCard {
            SectionHeader("Sheet", "${spec.paper.label} · ${spec.orientation.label}")
            Spacer(Modifier.height(6.dp))
            KeyValueRow("Physical size", "${fmt(paper.widthMm)} × ${fmt(paper.heightMm)} mm")
            KeyValueRow("Scale", "100% (1 mm = 2.8346 pt)")
            KeyValueRow("Pages", layout.pageCount.toString())
            KeyValueRow("Cards", layout.placements.size.toString())
        }

        SheetPreview(
            spec = spec,
            layout = layout,
            page = page,
            imageFor = { id -> images[id] },
            showCutMarks = spec.showCutMarks
        )

        if (layout.pageCount > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0 until layout.pageCount).forEach { p ->
                    FilterChip(selected = page == p, onClick = { page = p }, label = { Text("Page ${p + 1}") })
                }
            }
        }

        OutlinedButton(onClick = { showBack = !showBack }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showBack) "Preview: back side" else "Preview: front side")
        }

        InfoBanner(
            "The PDF uses the true physical page size, so print at 100% / \"Actual size\" " +
                "for correct millimetre dimensions. Do not use \"Fit to page\".",
            tone = BannerTone.INFO
        )

        Button(onClick = { nav.navigate(Routes.EXPORT) }, modifier = Modifier.fillMaxWidth()) {
            Text("Export PDF")
        }
        OutlinedButton(onClick = { nav.navigate(Routes.LAYOUT) }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to layout")
        }
    }
}
