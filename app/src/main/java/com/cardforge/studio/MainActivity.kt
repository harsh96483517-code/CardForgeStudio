package com.cardforge.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardforge.studio.data.repo.SettingsRepository
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.navigation.CardForgeNavHost
import com.cardforge.studio.ui.theme.CardForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: WorkspaceViewModel = viewModel()
            val settings by vm.settings.collectAsState(initial = SettingsRepository.Settings())
            CardForgeTheme(
                darkTheme = settings.darkTheme,
                dynamicColor = settings.dynamicColor
            ) {
                CardForgeNavHost(vm)
            }
        }
    }
}
