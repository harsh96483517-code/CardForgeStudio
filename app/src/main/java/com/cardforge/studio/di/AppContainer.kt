package com.cardforge.studio.di

import android.content.Context
import com.cardforge.studio.data.io.CardImageStore
import com.cardforge.studio.data.io.DocumentImporter
import com.cardforge.studio.data.repo.CustomTemplateStore
import com.cardforge.studio.data.repo.ProjectRepository
import com.cardforge.studio.data.repo.SettingsRepository
import com.cardforge.studio.domain.crop.CardDetector
import com.cardforge.studio.domain.crop.GradientCardDetector
import com.cardforge.studio.domain.crop.GradientMultiCardDetector
import com.cardforge.studio.domain.crop.MultiCardDetector
import com.cardforge.studio.domain.export.PdfExporter

/**
 * Minimal manual dependency container. Keeps the app free of a DI framework
 * while still giving a single, testable wiring point.
 */
class AppContainer(context: Context) {
    val importer: DocumentImporter = DocumentImporter(context)
    val imageStore: CardImageStore = CardImageStore(context)
    val projectRepository: ProjectRepository = ProjectRepository(context)
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val customTemplateStore: CustomTemplateStore = CustomTemplateStore(context)
    val detector: CardDetector = GradientCardDetector()
    val multiDetector: MultiCardDetector = GradientMultiCardDetector()
    val exporter: PdfExporter = PdfExporter()
}
