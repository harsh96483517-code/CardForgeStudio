package com.cardforge.studio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.navigation.Routes
import java.io.File

@Composable
fun PdfPageBrowserScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    val pages = state.pages

    ScreenScaffold(title = "Page Browser", onBack = { nav.popBackStack() }) { _ ->
        if (pages.isEmpty()) {
            InfoBanner("No pages imported yet. Go back and import a PDF or image.")
            return@ScreenScaffold
        }

        Text(
            "Tap a page to select it for cropping. ${pages.size} page(s) available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Simple 3-column grid built from rows (keeps the outer column scrollable).
        pages.chunked(3).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEachIndexed { colIndex, page ->
                    val globalIndex = rowIndex * 3 + colIndex
                    val selected = globalIndex == state.activePage
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                vm.selectPage(globalIndex)
                                nav.navigate(Routes.AUTOCROP)
                            }
                    ) {
                        AsyncImage(
                            model = File(page.imagePath),
                            contentDescription = "Page ${globalIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Page ${globalIndex + 1}" + if (selected) " ✓" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Pad the last row so tiles keep their width.
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
