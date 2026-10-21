package com.cardforge.studio.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cardforge.studio.CardForgeApp
import com.cardforge.studio.core.geometry.Geometry
import com.cardforge.studio.core.model.CardInstance
import com.cardforge.studio.core.model.CropQuad
import com.cardforge.studio.core.model.DocumentTemplate
import com.cardforge.studio.core.model.LayoutResult
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.Project
import com.cardforge.studio.core.model.SheetSpec
import com.cardforge.studio.core.model.SizeMm
import com.cardforge.studio.core.model.Units
import com.cardforge.studio.core.template.TemplateRegistry
import com.cardforge.studio.domain.crop.PerspectiveCorrector
import com.cardforge.studio.domain.export.PdfExporter
import com.cardforge.studio.domain.export.PrintSide
import com.cardforge.studio.domain.layout.PageLayoutEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** One source page/image the user has imported (front or back material). */
data class ImportedPage(
    val sourceUri: String,
    val pageIndex: Int,
    val imagePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val isPdf: Boolean
)

data class WorkspaceState(
    val project: Project,
    val templates: List<DocumentTemplate> = TemplateRegistry.builtIn,
    val selectedTemplateId: String? = null,
    val pages: List<ImportedPage> = emptyList(),
    val activePage: Int = 0,
    val quad: CropQuad? = null,
    val confidence: Float = 0f,
    val lowConfidence: Boolean = false,
    val layout: LayoutResult? = null,
    val manualPositions: Map<Int, Pair<Double, Double>> = emptyMap(),
    val busy: Boolean = false,
    val message: String? = null
) {
    val selectedTemplate: DocumentTemplate?
        get() = templates.firstOrNull { it.id == selectedTemplateId }
}

/**
 * The shared, activity-scoped workspace. Holds the in-progress project so that
 * state survives navigation between the 12 screens.
 */
class WorkspaceViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CardForgeApp).container
    private val importer = container.importer
    private val imageStore = container.imageStore
    private val projectRepo = container.projectRepository

    val settings = container.settingsRepository.settings

    /** Exposed so the Settings screen can persist preferences. */
    val settingsRepo get() = container.settingsRepository

    private val _state = MutableStateFlow(WorkspaceState(project = blankProject()))
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()

    init {
        // Load any user-saved custom templates alongside the built-ins.
        viewModelScope.launch {
            val custom = container.customTemplateStore.load()
            if (custom.isNotEmpty()) {
                _state.update { it.copy(templates = TemplateRegistry.builtIn + custom) }
            }
        }
    }

    /** Persist a user-defined template so it appears in the gallery. */
    fun saveCustomTemplate(
        name: String,
        size: SizeMm,
        orientation: Orientation,
        sides: com.cardforge.studio.core.model.CardSide,
        fit: com.cardforge.studio.core.model.FitMode
    ) {
        viewModelScope.launch {
            val t = DocumentTemplate(
                id = "custom-${UUID.randomUUID()}",
                name = name.ifBlank { "Custom template" },
                category = com.cardforge.studio.core.model.TemplateCategory.CUSTOM,
                cardSizeMm = size,
                orientation = orientation,
                sides = sides,
                sizeVerified = false,
                defaultFit = fit,
                notes = "User-saved custom template."
            )
            val list = container.customTemplateStore.add(t)
            _state.update {
                it.copy(
                    templates = TemplateRegistry.builtIn + list,
                    selectedTemplateId = t.id,
                    message = "Custom template saved."
                )
            }
        }
    }

    // ---------------------------------------------------------------- project

    private fun blankProject(): Project {
        val now = System.currentTimeMillis()
        return Project(
            id = UUID.randomUUID().toString(),
            name = "Untitled Project",
            cards = emptyList(),
            sheet = SheetSpec(),
            createdAt = now,
            updatedAt = now
        )
    }

    fun newProject(name: String = "Untitled Project") {
        _state.update { it.copy(project = blankProject().copy(name = name), manualPositions = emptyMap()) }
        recomputeLayout()
    }

    fun renameProject(name: String) {
        _state.update { it.copy(project = it.project.copy(name = name)) }
    }

    // --------------------------------------------------------------- template

    fun setTemplate(id: String) {
        _state.update { it.copy(selectedTemplateId = id) }
    }

    /** Effective physical size of a template, honouring its orientation. */
    fun resolvedSizeMm(t: DocumentTemplate): SizeMm {
        val s = t.cardSizeMm
        val wantsLandscape = t.orientation == Orientation.LANDSCAPE
        val isLandscape = s.widthMm >= s.heightMm
        return if (wantsLandscape == isLandscape) s else s.swapped()
    }

    // ----------------------------------------------------------------- import

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val newPages = mutableListOf<ImportedPage>()
            val errors = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    try {
                        if (importer.isPdf(uri)) {
                            val count = importer.pdfPageCount(uri)
                            val limit = minOf(count, 30)
                            for (p in 0 until limit) {
                                val bmp = importer.renderPdfPage(uri, p, 1600)
                                val path = imageStore.savePage(bmp)
                                newPages += ImportedPage(
                                    sourceUri = uri.toString(), pageIndex = p,
                                    imagePath = path, widthPx = bmp.width, heightPx = bmp.height, isPdf = true
                                )
                                bmp.recycle()
                            }
                        } else {
                            val bmp = importer.loadImage(uri, 2600)
                            val path = imageStore.savePage(bmp)
                            newPages += ImportedPage(
                                sourceUri = uri.toString(), pageIndex = 0,
                                imagePath = path, widthPx = bmp.width, heightPx = bmp.height, isPdf = false
                            )
                            bmp.recycle()
                        }
                    } catch (e: Exception) {
                        errors += (e.message ?: "Unreadable file")
                    }
                }
            }
            _state.update {
                it.copy(
                    busy = false,
                    pages = it.pages + newPages,
                    activePage = if (it.pages.isEmpty() && newPages.isNotEmpty()) it.pages.size else it.activePage,
                    message = if (errors.isEmpty()) null else errors.joinToString("\n")
                )
            }
            if (newPages.isNotEmpty()) runDetectionForActivePage()
        }
    }

    fun selectPage(index: Int) {
        if (index !in _state.value.pages.indices) return
        _state.update { it.copy(activePage = index) }
        runDetectionForActivePage()
    }

    /** Run automatic detection on the active page and publish the result. */
    fun runDetectionForActivePage() {
        val s = _state.value
        val page = s.pages.getOrNull(s.activePage) ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = withContext(Dispatchers.Default) {
                val bmp = imageStore.load(page.imagePath)
                if (bmp == null) null else {
                    val px = IntArray(bmp.width * bmp.height)
                    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                    val det = container.detector.detect(px, bmp.width, bmp.height)
                    bmp.recycle()
                    det
                }
            }
            _state.update {
                it.copy(
                    busy = false,
                    quad = result?.quad,
                    confidence = result?.confidence ?: 0f,
                    lowConfidence = result?.lowConfidence ?: true
                )
            }
        }
    }

    fun setQuad(quad: CropQuad) {
        _state.update { it.copy(quad = quad, lowConfidence = false) }
    }

    fun setConfidenceLow(value: Boolean) = _state.update { it.copy(lowConfidence = value) }

    /**
     * Warp the active page by the current quad and attach it to the project as a
     * new card (front) — or as the back of the most recent card when [asBack].
     */
    fun acceptCrop(asBack: Boolean = false) {
        val s = _state.value
        val page = s.pages.getOrNull(s.activePage) ?: return
        val quad = s.quad ?: return
        val template = s.selectedTemplate ?: TemplateRegistry.byId("custom")!!
        val size = resolvedSizeMm(template)

        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val savedPath = withContext(Dispatchers.Default) {
                val bmp = imageStore.load(page.imagePath) ?: return@withContext null
                val px = IntArray(bmp.width * bmp.height)
                bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                val outW = Units.mmToPx(size.widthMm, 300).toInt().coerceAtLeast(32)
                val outH = Units.mmToPx(size.heightMm, 300).toInt().coerceAtLeast(32)
                val warped = PerspectiveCorrector.warp(px, bmp.width, bmp.height, quad, outW, outH)
                bmp.recycle()
                val outBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                outBmp.setPixels(warped, 0, outW, 0, 0, outW, outH)
                val path = imageStore.save(outBmp)
                outBmp.recycle()
                path
            }

            if (savedPath == null) {
                _state.update { it.copy(busy = false, message = "Could not crop that page.") }
                return@launch
            }

            _state.update { st ->
                val cards = st.project.cards.toMutableList()
                if (asBack && cards.isNotEmpty()) {
                    val last = cards.last()
                    cards[cards.lastIndex] = last.copy(backImage = savedPath)
                } else {
                    cards += CardInstance(
                        id = UUID.randomUUID().toString(),
                        templateId = template.id,
                        displayName = template.name,
                        frontImage = savedPath,
                        cropFront = null,
                        sizeMm = size,
                        fit = template.defaultFit
                    )
                }
                st.copy(busy = false, project = st.project.copy(cards = cards), message = "Card added.")
            }
            recomputeLayout()
        }
    }

    /**
     * Warp an imported page (using its auto-detected quad) and assign it to a
     * specific card as front or back.
     */
    fun assignPageToCard(cardId: String, pageIndex: Int, asBack: Boolean) {
        val page = _state.value.pages.getOrNull(pageIndex) ?: return
        val card = _state.value.project.cards.firstOrNull { it.id == cardId } ?: return
        val size = card.sizeMm
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val savedPath = withContext(Dispatchers.Default) {
                val bmp = imageStore.load(page.imagePath) ?: return@withContext null
                val px = IntArray(bmp.width * bmp.height)
                bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                val det = container.detector.detect(px, bmp.width, bmp.height)
                val quad = if (det.lowConfidence) {
                    CropQuad(
                        com.cardforge.studio.core.model.Pt(0f, 0f),
                        com.cardforge.studio.core.model.Pt(bmp.width.toFloat(), 0f),
                        com.cardforge.studio.core.model.Pt(bmp.width.toFloat(), bmp.height.toFloat()),
                        com.cardforge.studio.core.model.Pt(0f, bmp.height.toFloat())
                    )
                } else det.quad
                val outW = Units.mmToPx(size.widthMm, 300).toInt().coerceAtLeast(32)
                val outH = Units.mmToPx(size.heightMm, 300).toInt().coerceAtLeast(32)
                val warped = PerspectiveCorrector.warp(px, bmp.width, bmp.height, quad, outW, outH)
                bmp.recycle()
                val outBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                outBmp.setPixels(warped, 0, outW, 0, 0, outW, outH)
                val path = imageStore.save(outBmp)
                outBmp.recycle()
                path
            }
            if (savedPath == null) {
                _state.update { it.copy(busy = false, message = "Could not read that page.") }
                return@launch
            }
            _state.update { st ->
                val cards = st.project.cards.map { c ->
                    if (c.id == cardId) {
                        if (asBack) c.copy(backImage = savedPath) else c.copy(frontImage = savedPath)
                    } else c
                }
                st.copy(busy = false, project = st.project.copy(cards = cards), message = "Updated card.")
            }
        }
    }

    fun removeCard(id: String) {
        _state.update { st ->
            val card = st.project.cards.firstOrNull { it.id == id }
            imageStore.delete(card?.frontImage)
            imageStore.delete(card?.backImage)
            st.copy(project = st.project.copy(cards = st.project.cards.filterNot { it.id == id }))
        }
        recomputeLayout()
    }

    fun setQuantity(id: String, q: Int) {
        _state.update { st ->
            val cards = st.project.cards.map { if (it.id == id) it.copy(quantity = q.coerceIn(1, 200)) else it }
            st.copy(project = st.project.copy(cards = cards))
        }
        recomputeLayout()
    }

    fun applyQuantityToAll(q: Int) {
        _state.update { st ->
            val cards = st.project.cards.map { it.copy(quantity = q.coerceIn(1, 200)) }
            st.copy(project = st.project.copy(cards = cards))
        }
        recomputeLayout()
    }

    fun updateCardSize(id: String, size: SizeMm, fit: com.cardforge.studio.core.model.FitMode) {
        _state.update { st ->
            val cards = st.project.cards.map { if (it.id == id) it.copy(sizeMm = size, fit = fit) else it }
            st.copy(project = st.project.copy(cards = cards))
        }
        recomputeLayout()
    }

    // ------------------------------------------------------------------ sheet

    fun setSheet(spec: SheetSpec) {
        _state.update { it.copy(project = it.project.copy(sheet = spec), manualPositions = emptyMap()) }
        recomputeLayout()
    }

    /** Manual drag: move the placement at [index] to (xMm, yMm). */
    fun movePlacement(index: Int, xMm: Double, yMm: Double) {
        _state.update { it.copy(manualPositions = it.manualPositions + (index to (xMm to yMm))) }
        recomputeLayout()
    }

    fun resetManualPositions() {
        _state.update { it.copy(manualPositions = emptyMap()) }
        recomputeLayout()
    }

    fun recomputeLayout() {
        val s = _state.value
        val result = PageLayoutEngine.layout(s.project.cards, s.project.sheet)
        // Apply manual overrides on top of the auto layout.
        val adjusted = result.placements.mapIndexed { i, p ->
            val ov = s.manualPositions[i]
            if (ov != null) p.copy(xMm = ov.first, yMm = ov.second) else p
        }
        _state.update { it.copy(layout = result.copy(placements = adjusted)) }
    }

    // -------------------------------------------------------------- persistence

    fun saveProject() {
        viewModelScope.launch {
            val saved = projectRepo.save(_state.value.project)
            _state.update { it.copy(project = saved, message = "Project saved.") }
        }
    }

    fun loadProject(id: String) {
        viewModelScope.launch {
            val p = projectRepo.load(id)
            if (p == null) {
                _state.update { it.copy(message = "Project not found.") }
            } else {
                _state.update {
                    it.copy(
                        project = p,
                        selectedTemplateId = p.templateId ?: it.selectedTemplateId,
                        manualPositions = emptyMap(),
                        message = "Opened ${p.name}."
                    )
                }
                recomputeLayout()
            }
        }
    }

    fun deleteProject(id: String, cardImagePaths: List<String> = emptyList()) {
        viewModelScope.launch {
            projectRepo.delete(id, cardImagePaths)
            _state.update { it.copy(message = "Project deleted.") }
        }
    }

    suspend fun allProjects(): List<Project> = projectRepo.list()

    // ------------------------------------------------------------------ export

    fun bitmapFor(cardId: String, side: PrintSide): Bitmap? {
        val card = _state.value.project.cards.firstOrNull { it.id == cardId } ?: return null
        return imageStore.load(if (side == PrintSide.BACK) card.backImage else card.frontImage)
    }

    fun exportPdf(
        uri: Uri,
        options: PdfExporter.Options,
        includeBack: Boolean = false
    ) {
        val s = _state.value
        val layout = s.layout ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val ok = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        container.exporter.export(layout, s.project.sheet, out, options) { cardId, side ->
                            val effectiveSide = if (side == PrintSide.BACK && !includeBack) PrintSide.FRONT else side
                            bitmapFor(cardId, effectiveSide)
                        }
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            _state.update {
                it.copy(busy = false, message = if (ok) "PDF exported." else "Export failed.")
            }
        }
    }

    fun clearTempData() {
        imageStore.clearAll()
        _state.update { it.copy(pages = emptyList(), message = "Temporary data cleared.") }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    override fun onCleared() {
        super.onCleared()
        // Leave stored images on disk; they are managed per-project.
    }

    // Small helper so screens can query capacity.
    fun capacityFor(size: SizeMm): Int =
        PageLayoutEngine.capacityFor(size, _state.value.project.sheet)

    fun aspectOfQuad(q: CropQuad): Float = Geometry.aspectOf(q)
}
