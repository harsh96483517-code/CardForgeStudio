package com.cardforge.studio.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardforge.studio.core.model.FitMode
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import com.cardforge.studio.data.repo.SettingsRepository
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.components.BannerTone
import com.cardforge.studio.ui.components.GlassCard
import com.cardforge.studio.ui.components.InfoBanner
import com.cardforge.studio.ui.components.ScreenScaffold
import com.cardforge.studio.ui.components.SectionHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: WorkspaceViewModel, nav: NavController) {
    val settings by vm.settings.collectAsState(initial = SettingsRepository.Settings())
    val scope = rememberCoroutineScope()

    ScreenScaffold(title = "Settings", onBack = { nav.popBackStack() }) { _ ->
        GlassCard {
            SectionHeader("Appearance")
            Spacer(Modifier.height(8.dp))
            SwitchRow("Dark navy theme", settings.darkTheme) { v ->
                scope.launch { vm.settingsRepo.setDarkTheme(v) }
            }
            SwitchRow("Dynamic colour (Android 12+)", settings.dynamicColor) { v ->
                scope.launch { vm.settingsRepo.setDynamicColor(v) }
            }
        }

        GlassCard {
            SectionHeader("Default sheet")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperKind.entries.forEach { kind ->
                    FilterChip(
                        selected = settings.defaultPaper == kind,
                        onClick = { scope.launch { vm.settingsRepo.setDefaultPaper(kind) } },
                        label = { Text(kind.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Orientation.entries.forEach { o ->
                    FilterChip(
                        selected = settings.defaultOrientation == o,
                        onClick = { scope.launch { vm.settingsRepo.setDefaultOrientation(o) } },
                        label = { Text(o.label) }
                    )
                }
            }
        }

        GlassCard {
            SectionHeader("Crop defaults")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FitMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.defaultFit == mode,
                        onClick = { scope.launch { vm.settingsRepo.setDefaultFit(mode) } },
                        label = { Text(mode.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SwitchRow("Confirm low-confidence crops", settings.confirmLowConfidence) { v ->
                scope.launch { vm.settingsRepo.setConfirmLowConfidence(v) }
            }
            SwitchRow("Keep cut marks by default", settings.keepCutMarks) { v ->
                scope.launch { vm.settingsRepo.setKeepCutMarks(v) }
            }
        }

        GlassCard {
            SectionHeader("Privacy")
            Spacer(Modifier.height(8.dp))
            Text(
                "All processing happens on this device. The app requests no internet " +
                    "permission, sends no analytics, and never logs document contents or " +
                    "personal numbers. Card images live in private app storage.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.clearTempData() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear temporary image data") }
        }

        GlassCard {
            SectionHeader("About")
            Spacer(Modifier.height(6.dp))
            Text("CardForge Studio · v1.0.0", style = MaterialTheme.typography.bodyMedium)
            Text(
                "A print studio for your own documents. It never generates or imitates " +
                    "official government documents, seals, signatures or security features.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MessageBanner(vm)

        InfoBanner(
            "Templates define geometry only. Always use your own legitimate document images.",
            tone = BannerTone.INFO
        )
    }
}

@Composable
private fun MessageBanner(vm: WorkspaceViewModel) {
    val s by vm.state.collectAsState()
    s.message?.let { InfoBanner(it, tone = BannerTone.INFO) }
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
