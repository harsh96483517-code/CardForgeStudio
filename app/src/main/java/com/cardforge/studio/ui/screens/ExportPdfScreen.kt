package com.cardforge.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.cardforge.studio.domain.export.PdfExporter
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.BannerTone
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.components.SectionHeader
import com.cardforge.studio.ui.navigation.Routes
import java.io.File

@Composable
fun ExportPdfScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var cutMarks by remember { mutableStateOf(state.project.sheet.showCutMarks) }
    var dashed by remember { mutableStateOf(state.project.sheet.showDashedCutLines) }
    var duplex by remember { mutableStateOf(false) }
    var includeBack by remember { mutableStateOf(true) }
    var longEdge by remember { mutableStateOf(true) }

    val createPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            vm.exportPdf(
                uri = uri,
                options = PdfExporter.Options(
                    includeCutMarks = cutMarks,
                    includeDashedCutLines = dashed,
                    duplex = duplex,
                    flipEdge = if (longEdge) PdfExporter.FlipEdge.LONG_EDGE else PdfExporter.FlipEdge.SHORT_EDGE
                ),
                includeBack = includeBack
            )
        }
    }

    val pickFolder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val dir = DocumentFile.fromTreeUri(context, uri)
            state.project.cards.forEachIndexed { index, card ->
                listOfNotNull(
                    card.frontImage?.let { "card${index + 1}-front.png" to it },
                    card.backImage?.let { "card${index + 1}-back.png" to it }
                ).forEach { (name, path) ->
                    val src = File(path)
                    if (src.exists()) {
                        val doc = dir?.createFile("image/png", name)
                        doc?.uri?.let { outUri ->
                            runCatching {
                                context.contentResolver.openOutputStream(outUri)?.use { out ->
                                    src.inputStream().use { it.copyTo(out) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ScreenScaffold(title = "Export", onBack = { nav.popBackStack() }) { _ ->
        val layout = state.layout
        if (layout == null || layout.placements.isEmpty()) {
            InfoBanner("Nothing to export yet. Arrange cards first.")
            OutlinedButton(onClick = { nav.navigate(Routes.LAYOUT) }) { Text("Go to layout") }
            return@ScreenScaffold
        }

        GlassCard {
            SectionHeader("PDF options")
            Spacer(Modifier.height(8.dp))
            SwitchRow("Cutting guides", cutMarks) { cutMarks = it }
            SwitchRow("Dashed cutting lines", dashed) { dashed = it }
            SwitchRow("Include back-side images", includeBack) { includeBack = it }
            SwitchRow("Duplex (front + back passes)", duplex) { duplex = it }
            if (duplex) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Flip direction: ${if (longEdge) "Long edge" else "Short edge"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { longEdge = true }) { Text("Long edge") }
                    OutlinedButton(onClick = { longEdge = false }) { Text("Short edge") }
                }
            }
        }

        Button(
            onClick = {
                vm.saveProject()
                createPdf.launch("${state.project.name.ifBlank { "cardforge" }}.pdf")
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy
        ) { Text(if (state.busy) "Exporting…" else "Export print-ready PDF") }

        OutlinedButton(
            onClick = { pickFolder.launch(null) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export cropped images (PNG)") }

        OutlinedButton(
            onClick = { nav.navigate(Routes.PROJECTS) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save & view projects") }

        InfoBanner(
            "Export uses the Storage Access Framework — you choose exactly where the " +
                "file goes. Nothing is uploaded anywhere.",
            tone = BannerTone.INFO
        )

        if (duplex) {
            InfoBanner(
                "Test alignment on one sheet before printing many copies. " +
                    "Exact duplex alignment depends on your printer.",
                tone = BannerTone.WARN
            )
        }

        state.message?.let { InfoBanner(it, tone = BannerTone.SUCCESS) }
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
