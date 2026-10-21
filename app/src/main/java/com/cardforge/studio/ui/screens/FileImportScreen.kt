package com.cardforge.studio.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.BannerTone
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.components.SectionHeader
import com.cardforge.studio.ui.navigation.Routes
import java.io.File

@Composable
fun FileImportScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) vm.importUris(uris) }

    ScreenScaffold(title = "Import", onBack = { nav.popBackStack() }) { _ ->
        val template = state.selectedTemplate
        GlassCard {
            SectionHeader("Selected template", template?.name ?: "None")
            Spacer(Modifier.height(4.dp))
            if (template != null) {
                Text(
                    "Crop area and print size come from this template. " +
                        "You supply the document image.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedButton(onClick = { nav.navigate(Routes.GALLERY) }) {
                    Text("Choose a template first")
                }
            }
        }

        Button(
            onClick = { picker.launch(arrayOf("application/pdf", "image/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy
        ) {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.busy) "Importing…" else "Choose PDF or image files")
        }

        if (state.pages.isNotEmpty()) {
            SectionHeader("Imported pages", "${state.pages.size} page(s)")
            state.pages.forEachIndexed { index, page ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = File(page.imagePath),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(width = 64.dp, height = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (page.isPdf) "PDF page ${page.pageIndex + 1}" else "Image",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${page.widthPx} × ${page.heightPx} px",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = {
                            vm.selectPage(index)
                            nav.navigate(Routes.AUTOCROP)
                        }) { Text("Crop") }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { nav.navigate(Routes.PDF_BROWSER) },
                    modifier = Modifier.weight(1f)
                ) { Text("Page browser") }
                Button(
                    onClick = {
                        vm.selectPage(0)
                        nav.navigate(Routes.AUTOCROP)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Auto crop") }
            }
        }

        if (state.busy) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Text("Working…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        state.message?.let { InfoBanner(it, tone = BannerTone.ERROR) }

        InfoBanner(
            "Supported: multi-page PDF, JPG, JPEG and PNG. Corrupt or " +
                "password-protected files are reported, never silently dropped.",
            tone = BannerTone.INFO
        )
    }
}
