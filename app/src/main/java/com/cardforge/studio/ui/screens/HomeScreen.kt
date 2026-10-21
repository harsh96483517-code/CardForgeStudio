package com.cardforge.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.GradientHeader
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.KeyValueRow
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.components.SectionHeader
import com.cardforge.studio.ui.navigation.Routes

@Composable
fun HomeScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    val project = state.project

    ScreenScaffold(
        title = "CardForge Studio",
        actions = {
            androidx.compose.material3.IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    ) { _ ->
        GradientHeader(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("Print studio", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Arrange your own documents on paper",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Import → Template → Crop → Size → Front/Back → Copies → A4 → Preview → Export",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        GlassCard {
            SectionHeader("Current project", project.name)
            Spacer(Modifier.height(8.dp))
            KeyValueRow("Cards", project.cards.size.toString())
            KeyValueRow("Copies (with quantity)", project.cards.sumOf { it.quantity }.toString())
            KeyValueRow("Sheet", "${project.sheet.paper.label} · ${project.sheet.orientation.label}")
            KeyValueRow("Pages", (state.layout?.pageCount ?: 1).toString())
        }

        Button(
            onClick = { nav.navigate(Routes.IMPORT) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start: import a document")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.navigate(Routes.GALLERY) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.GridView, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Templates")
            }
            OutlinedButton(onClick = { nav.navigate(Routes.LAYOUT) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Layout")
            }
        }
        OutlinedButton(onClick = { nav.navigate(Routes.PROJECTS) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Recent projects")
        }

        OutlinedButton(
            onClick = {
                vm.setTemplate("aadhaar")
                nav.navigate(Routes.IMPORT)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Aadhaar front + back → one A4") }

        if (project.cards.isEmpty()) {
            InfoBanner(
                "No cards yet. Import a PDF or image of your own document to begin. " +
                    "The app never generates official documents — you supply the images.",
                tone = com.cardforge.studio.ui.components.BannerTone.INFO
            )
        } else {
            InfoBanner(
                "Everything is processed on this device. Nothing is uploaded.",
                tone = com.cardforge.studio.ui.components.BannerTone.SUCCESS
            )
        }

        state.message?.let { msg ->
            InfoBanner(msg, tone = com.cardforge.studio.ui.components.BannerTone.WARN)
            OutlinedButton(onClick = { vm.clearMessage() }, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss")
            }
        }
    }
}
