package com.cardforge.studio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cardforge.studio.ui.WorkspaceViewModel
import com.cardforge.studio.ui.screens.A4LayoutEditorScreen
import com.cardforge.studio.ui.screens.AutoCropReviewScreen
import com.cardforge.studio.ui.screens.CardSizeSettingsScreen
import com.cardforge.studio.ui.screens.ExportPdfScreen
import com.cardforge.studio.ui.screens.FileImportScreen
import com.cardforge.studio.ui.screens.FrontBackEditorScreen
import com.cardforge.studio.ui.screens.HomeScreen
import com.cardforge.studio.ui.screens.PdfPageBrowserScreen
import com.cardforge.studio.ui.screens.PrintPreviewScreen
import com.cardforge.studio.ui.screens.RecentProjectsScreen
import com.cardforge.studio.ui.screens.SettingsScreen
import com.cardforge.studio.ui.screens.TemplateGalleryScreen

@Composable
fun CardForgeNavHost(vm: WorkspaceViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(vm, nav) }
        composable(Routes.GALLERY) { TemplateGalleryScreen(vm, nav) }
        composable(Routes.IMPORT) { FileImportScreen(vm, nav) }
        composable(Routes.PDF_BROWSER) { PdfPageBrowserScreen(vm, nav) }
        composable(Routes.AUTOCROP) { AutoCropReviewScreen(vm, nav) }
        composable(Routes.FRONT_BACK) { FrontBackEditorScreen(vm, nav) }
        composable(Routes.SIZE) { CardSizeSettingsScreen(vm, nav) }
        composable(Routes.LAYOUT) { A4LayoutEditorScreen(vm, nav) }
        composable(Routes.PREVIEW) { PrintPreviewScreen(vm, nav) }
        composable(Routes.EXPORT) { ExportPdfScreen(vm, nav) }
        composable(Routes.PROJECTS) { RecentProjectsScreen(vm, nav) }
        composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
    }
}
