package com.cardforge.studio.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.cardforge.studio.ui.fmt
import com.cardforge.studio.ui.navigation.Routes
import java.io.File

@Composable
fun FrontBackEditorScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    var assign by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // cardId to asBack

    ScreenScaffold(title = "Front / Back Editor", onBack = { nav.popBackStack() }) { _ ->
        if (state.project.cards.isEmpty()) {
            InfoBanner("No cards yet. Crop a document first.")
            OutlinedButton(onClick = { nav.navigate(Routes.AUTOCROP) }) { Text("Go to crop") }
            return@ScreenScaffold
        }

        InfoBanner(
            "Assign the two sides of your own document to the same card. " +
                "Nothing is generated — both images come from files you imported.",
            tone = BannerTone.INFO
        )

        state.project.cards.forEach { card ->
            GlassCard(Modifier.fillMaxWidth()) {
                SectionHeader(card.displayName, "${fmt(card.sizeMm.widthMm)} × ${fmt(card.sizeMm.heightMm)} mm")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SideThumb("Front", card.frontImage, Modifier.weight(1f))
                    SideThumb("Back", card.backImage, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { assign = card.id to false },
                        modifier = Modifier.weight(1f),
                        enabled = state.pages.isNotEmpty()
                    ) { Text("Set front") }
                    OutlinedButton(
                        onClick = { assign = card.id to true },
                        modifier = Modifier.weight(1f),
                        enabled = state.pages.isNotEmpty()
                    ) { Text("Set back") }
                }
                if (state.pages.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Import pages first to assign sides.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        InfoBanner(
            "Duplex printing: align a test sheet before printing many copies. " +
                "Exact front/back alignment cannot be guaranteed across all printers.",
            tone = BannerTone.WARN
        )

        Button(onClick = { nav.navigate(Routes.LAYOUT) }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue to layout")
        }
    }

    assign?.let { (cardId, asBack) ->
        AlertDialog(
            onDismissRequest = { assign = null },
            title = { Text(if (asBack) "Choose back page" else "Choose front page") },
            text = {
                Column {
                    state.pages.forEachIndexed { index, page ->
                        TextButton(onClick = {
                            vm.assignPageToCard(cardId, index, asBack)
                            assign = null
                        }) {
                            Text(if (page.isPdf) "PDF page ${page.pageIndex + 1}" else "Image ${index + 1}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { assign = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SideThumb(label: String, path: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (path != null) {
                AsyncImage(
                    model = File(path),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 140.dp, height = 96.dp)
                )
            } else {
                Text("—", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
