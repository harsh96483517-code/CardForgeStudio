package com.cardforge.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.core.model.CardSide
import com.cardforge.studio.core.model.DocumentTemplate
import com.cardforge.studio.core.model.TemplateCategory
import com.cardforge.studio.core.template.TemplateRegistry
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.fmt
import com.cardforge.studio.ui.navigation.Routes

@Composable
fun TemplateGalleryScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<TemplateCategory?>(null) }
    val favourites = remember { mutableStateListOf<String>() }
    val recents = remember { mutableStateListOf<String>() }

    val results = remember(query, category) {
        TemplateRegistry.search(query).filter { category == null || it.category == category }
    }

    ScreenScaffold(title = "Template Gallery", onBack = { nav.popBackStack() }) { _ ->
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search templates") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = category == null,
                onClick = { category = null },
                label = { Text("All") }
            )
            TemplateCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = if (category == cat) null else cat },
                    label = { Text(cat.label) }
                )
            }
        }

        if (recents.isNotEmpty()) {
            InfoBanner("Recent: " + recents.take(3).joinToString(", ") { id ->
                TemplateRegistry.byId(id)?.name ?: id
            })
        }

        results.forEach { template ->
            TemplateRow(
                template = template,
                isFavourite = template.id in favourites,
                onFavourite = {
                    if (template.id in favourites) favourites.remove(template.id) else favourites.add(template.id)
                },
                onSelect = {
                    if (template.id !in recents) recents.add(0, template.id)
                    vm.setTemplate(template.id)
                    nav.navigate(Routes.IMPORT)
                }
            )
        }

        if (results.isEmpty()) {
            InfoBanner("No templates match \"$query\".")
        }

        InfoBanner(
            "Templates define crop area, aspect ratio, print size and orientation only. " +
                "Previews are neutral samples — never real identity information or official artwork.",
            tone = com.cardforge.studio.ui.components.BannerTone.INFO
        )
    }
}

@Composable
private fun TemplateRow(
    template: DocumentTemplate,
    isFavourite: Boolean,
    onFavourite: () -> Unit,
    onSelect: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Neutral sample thumbnail (no real document imagery).
            Box(
                modifier = Modifier
                    .size(width = 78.dp, height = 50.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Sample", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    template.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                val sides = when (template.sides) {
                    CardSide.FRONT -> "Front only"
                    CardSide.BACK -> "Back only"
                    CardSide.TWO_SIDED -> "Front & back"
                }
                val size = "${fmt(template.cardSizeMm.widthMm)} × ${fmt(template.cardSizeMm.heightMm)} mm"
                Text(
                    "$sides · ${template.orientation.label} · $size" +
                        if (!template.sizeVerified) " (preset)" else " (verified)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onFavourite) {
                Icon(
                    if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favourite"
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
            Text("Select template")
        }
    }
}
