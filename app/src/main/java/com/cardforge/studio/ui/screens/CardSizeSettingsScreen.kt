package com.cardforge.studio.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.core.model.FitMode
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.core.template.TemplateRegistry
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.BannerTone
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.components.SectionHeader
import com.cardforge.studio.ui.fmt
import com.cardforge.studio.ui.navigation.Routes

@Composable
fun CardSizeSettingsScreen(vm: WorkspaceViewModel, nav: NavController) {
    val state by vm.state.collectAsState()
    val cards = state.project.cards

    var selectedId by remember { mutableStateOf(cards.lastOrNull()?.id) }
    LaunchedEffect(cards.size) {
        if (selectedId == null || cards.none { it.id == selectedId }) selectedId = cards.lastOrNull()?.id
    }
    val card = cards.firstOrNull { it.id == selectedId } ?: cards.lastOrNull()

    ScreenScaffold(title = "Card Size Settings", onBack = { nav.popBackStack() }) { _ ->
        if (card == null) {
            InfoBanner("No cards yet. Crop a document first.")
            OutlinedButton(onClick = { nav.navigate(Routes.AUTOCROP) }) { Text("Go to crop") }
            return@ScreenScaffold
        }

        val template = state.templates.firstOrNull { it.id == card.templateId }

        // Card selector chips.
        if (cards.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.forEachIndexed { i, c ->
                    FilterChip(
                        selected = c.id == selectedId,
                        onClick = { selectedId = c.id },
                        label = { Text("Card ${i + 1}") }
                    )
                }
            }
        }

        GlassCard {
            SectionHeader(card.displayName, template?.notes ?: "")
            Spacer(Modifier.height(8.dp))
            val verified = template?.sizeVerified == true
            InfoBanner(
                if (verified) "This is a verified standard size."
                else "This is an editable preset — official formats vary. Adjust as needed.",
                tone = if (verified) BannerTone.SUCCESS else BannerTone.WARN
            )
        }

        var wText by remember(card.id) { mutableStateOf(fmt(card.sizeMm.widthMm)) }
        var hText by remember(card.id) { mutableStateOf(fmt(card.sizeMm.heightMm)) }
        var lockAspect by remember(card.id) { mutableStateOf(true) }
        var fit by remember(card.id) { mutableStateOf(card.fit) }
        var customName by remember { mutableStateOf("") }

        GlassCard {
            SectionHeader("Dimensions (millimetres)")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = wText,
                    onValueChange = { v ->
                        wText = v
                        if (lockAspect) {
                            val w = v.toDoubleOrNull()
                            if (w != null && card.sizeMm.heightMm > 0) {
                                hText = fmt(w * card.sizeMm.heightMm / card.sizeMm.widthMm)
                            }
                        }
                    },
                    label = { Text("Width") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hText,
                    onValueChange = { v ->
                        hText = v
                        if (lockAspect) {
                            val h = v.toDoubleOrNull()
                            if (h != null && card.sizeMm.widthMm > 0) {
                                wText = fmt(h * card.sizeMm.widthMm / card.sizeMm.heightMm)
                            }
                        }
                    },
                    label = { Text("Height") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = lockAspect, onCheckedChange = { lockAspect = it })
                Spacer(Modifier.width(8.dp))
                Text("Lock aspect ratio", style = MaterialTheme.typography.bodyMedium)
            }
        }

        GlassCard {
            SectionHeader("Image fit")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FitMode.entries.forEach { mode ->
                    FilterChip(
                        selected = fit == mode,
                        onClick = { fit = mode },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        GlassCard {
            SectionHeader("Orientation")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val w = wText.toDoubleOrNull() ?: card.sizeMm.widthMm
                    val h = hText.toDoubleOrNull() ?: card.sizeMm.heightMm
                    wText = fmt(minOf(w, h)); hText = fmt(maxOf(w, h))
                }) { Text("Portrait") }
                OutlinedButton(onClick = {
                    val w = wText.toDoubleOrNull() ?: card.sizeMm.widthMm
                    val h = hText.toDoubleOrNull() ?: card.sizeMm.heightMm
                    wText = fmt(maxOf(w, h)); hText = fmt(minOf(w, h))
                }) { Text("Landscape") }
            }
        }

        Button(
            onClick = {
                val w = wText.toDoubleOrNull()
                val h = hText.toDoubleOrNull()
                if (w != null && h != null && w > 0 && h > 0) {
                    vm.updateCardSize(card.id, SizeMm(w, h), fit)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Apply size") }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val t = template ?: TemplateRegistry.byId("custom")!!
                    val s = vm.resolvedSizeMm(t)
                    wText = fmt(s.widthMm); hText = fmt(s.heightMm)
                    vm.updateCardSize(card.id, s, t.defaultFit)
                },
                modifier = Modifier.weight(1f)
            ) { Text("Reset to defaults") }
            OutlinedButton(
                onClick = { vm.setQuantity(card.id, 4) },
                modifier = Modifier.weight(1f)
            ) { Text("Set 4 copies") }
        }

        GlassCard {
            SectionHeader("Duplicate presets")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(2, 4, 6, 8).forEach { n ->
                    OutlinedButton(
                        onClick = { vm.setQuantity(card.id, n) },
                        modifier = Modifier.weight(1f)
                    ) { Text("$n") }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.applyQuantityToAll(4) }, modifier = Modifier.fillMaxWidth()) {
                Text("Apply 4 copies to all cards")
            }
        }

        GlassCard {
            SectionHeader("Save as custom template")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text("Template name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val w = wText.toDoubleOrNull() ?: card.sizeMm.widthMm
                    val h = hText.toDoubleOrNull() ?: card.sizeMm.heightMm
                    vm.saveCustomTemplate(
                        name = customName,
                        size = SizeMm(w, h),
                        orientation = if (w >= h) Orientation.LANDSCAPE else Orientation.PORTRAIT,
                        sides = com.cardforge.studio.core.model.CardSide.TWO_SIDED,
                        fit = fit
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save custom template") }
        }

        Button(onClick = { nav.navigate(Routes.LAYOUT) }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue to A4 layout")
        }

        state.message?.let { InfoBanner(it, tone = BannerTone.INFO) }
    }
}
