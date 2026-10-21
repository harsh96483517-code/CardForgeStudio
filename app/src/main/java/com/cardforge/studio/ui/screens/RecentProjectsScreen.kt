package com.cardforge.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.core.model.Project
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.BannerTone
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.navigation.Routes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecentProjectsScreen(vm: WorkspaceViewModel, nav: NavController) {
    val projects by produceState(initialValue = emptyList<Project>(), vm) {
        value = vm.allProjects()
    }
    val fmtDate = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

    ScreenScaffold(title = "Recent Projects", onBack = { nav.popBackStack() }) { _ ->
        if (projects.isEmpty()) {
            InfoBanner("No saved projects yet. Export or save a project to see it here.")
        }

        projects.forEach { p ->
            GlassCard(Modifier.fillMaxWidth()) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${p.cards.size} card(s) · ${fmtDate.format(Instant.ofEpochMilli(p.updatedAt))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            vm.loadProject(p.id)
                            nav.navigate(Routes.LAYOUT)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Open") }
                    OutlinedButton(
                        onClick = {
                            vm.deleteProject(
                                id = p.id,
                                cardImagePaths = p.cards.flatMap { listOfNotNull(it.frontImage, it.backImage) }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Delete") }
                }
            }
        }

        InfoBanner(
            "Deleting a project also removes its stored card images from this device.",
            tone = BannerTone.WARN
        )
    }
}
