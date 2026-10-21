# CardForge Studio — Architecture & Implementation Plan

CardForge Studio is an Android app that lets a user import their **own** PDFs or
images of Indian documents, crop them into cards, apply size presets, duplicate
them, arrange them on an A4 (or other) sheet, and export a print-ready PDF.

> **Legal / ethical boundary (enforced in code and UI):** the app never
> generates, invents, or imitates official government documents, seals,
> signatures, QR codes, or security features. Templates define only *geometry*
> (crop area, aspect ratio, physical size, orientation). All document pixels
> come from files the user supplies. Nothing is uploaded; processing is local.

---

## 1. High-level architecture

Clean-ish layered MVVM, single-module Gradle project (`:app`).

```
ui/          Compose screens + ViewModels (presentation)
domain/      Pure logic: crop detection, perspective, layout engine, export
data/        Importers (PDF/image), repositories, persistence
core/        Models, template registry, units & geometry utilities
```

* **Presentation (`ui/`)** — Jetpack Compose + Material 3. One ViewModel per
  screen, exposing a single immutable `UiState` via `StateFlow`. Screens are
  stateless and receive state + lambdas.
* **Domain (`domain/`)** — no Android UI dependencies. Pure Kotlin where
  possible so it is unit-testable on the JVM:
  * `CardDetector` — automatic rectangular card detection (edge/contour scan).
  * `PerspectiveCorrector` — 4-point → rectangle warp for photographed docs.
  * `PageLayoutEngine` — packs `CardInstance`s onto sheets with margins,
    spacing, grid snap, overflow detection.
  * `PdfExporter` — renders pages to a print-ready PDF at true physical size.
* **Data (`data/`)** — `PdfImporter` (Android `PdfRenderer`), `ImageImporter`,
  `ProjectRepository` (JSON projects on disk), `SettingsRepository` (DataStore).
* **Core (`core/`)** — `DocumentTemplate`, `TemplateRegistry`, `Units`
  (mm↔pt↔px), `Geometry`.

### Dependency rule
`ui → domain → core`, `ui → data → core`. Domain never depends on data or ui.

---

## 2. Navigation

Single-activity Compose app (`MainActivity` → `CardForgeNavHost`) using
Navigation-Compose. Routes:

| # | Route | Screen |
|---|-------|--------|
| 1 | `home` | Home Dashboard |
| 2 | `gallery` | Document Template Gallery |
| 3 | `import` | File Import |
| 4 | `pdf/{uri}` | PDF Page Browser |
| 5 | `autocrop` | Auto Crop Review |
| 6 | `frontback` | Front/Back Editor |
| 7 | `size` | Card Size Settings |
| 8 | `layout` | A4 Layout Editor |
| 9 | `preview` | Print Preview |
| 10 | `export` | Export PDF |
| 11 | `projects` | Recent Projects |
| 12 | `settings` | Settings |

Canonical workflow:

```
Import → Select template → Auto-crop → Adjust size → Front/Back
       → Choose copies → Arrange on A4 → Preview → Export PDF
```

A single `ProjectViewModel` (activity-scoped) holds the in-progress `Project`
so state survives navigation; each screen reads/writes it.

---

## 3. Data models (`core/model`)

```kotlin
enum class TemplateCategory { IDENTITY, VEHICLE, OTHER, CUSTOM }

enum class CardSide { FRONT, BACK, TWO_SIDED }

enum class Orientation { PORTRAIT, LANDSCAPE }

enum class FitMode { FIT, FILL, ORIGINAL }        // how an image fills a card

enum class PaperKind { A4, A3, LETTER, CUSTOM }

data class SizeMm(val widthMm: Double, val heightMm: Double)

data class DocumentTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val cardSizeMm: SizeMm,          // official physical size when verified
    val orientation: Orientation,
    val sides: CardSide,
    val sizeVerified: Boolean,       // false => editable preset, not official
    val defaultFit: FitMode,
    val notes: String? = null
)

data class CropQuad(val points: List<PointF>)     // 4 crop corners (source px)
data class CropRect(val left: Float, val top: Float,
                    val right: Float, val bottom: Float)

data class CardInstance(
    val id: String,
    val templateId: String,
    val frontImage: String?,          // local file path
    val backImage: String?,
    val cropFront: CropRect?,
    val cropBack: CropRect?,
    val sizeMm: SizeMm,               // resolved size (template or custom)
    val fit: FitMode,
    val quantity: Int = 1
)

data class SheetSpec(
    val paper: PaperKind,
    val customSize: SizeMm?,
    val orientation: Orientation,
    val marginMm: Double,
    val spacingMm: Double,
    val showCutMarks: Boolean,
    val showDashedCutLines: Boolean,
    val snapToGrid: Boolean
)

data class Placement(val cardId: String, val page: Int,
                     val xMm: Double, val yMm: Double,
                     val widthMm: Double, val heightMm: Double)

data class Project(
    val id: String,
    val name: String,
    val templateId: String?,
    val cards: List<CardInstance>,
    val sheet: SheetSpec,
    val createdAt: Long,
    val updatedAt: Long
)
```

`SizeMm`/`CropRect`/`PointF` (our own, JVM-safe) live in `core` so the layout
and geometry code is unit-testable without the Android framework.

---

## 4. Template registry (verified dimensions)

`TemplateRegistry` is a single source of truth. Sizes below are the *card*
dimensions in millimetres; where a document has multiple official formats we
ship an **editable preset** (`sizeVerified = false`) plus a custom option
rather than asserting one universal size.

| Category | Template | Card size (mm) | Verified |
|----------|----------|----------------|----------|
| Identity | PAN Card | 85.6 × 54 | yes (ISO/IEC 7810 ID-1) |
| Identity | Aadhaar Card | 85.6 × 54 | preset |
| Identity | Voter ID (EPIC) | 85.6 × 54 | preset |
| Identity | Passport info page | 125 × 88 | preset |
| Identity | Generic identity card | 85.6 × 54 | preset |
| Vehicle | Driving Licence | 85.6 × 54 | preset |
| Vehicle | Vehicle RC | 85.6 × 54 | preset |
| Vehicle | Vehicle insurance card | 85.6 × 54 | preset |
| Vehicle | Vehicle information doc | 210 × 99 | no |
| Vehicle | Generic vehicle doc | 85.6 × 54 | preset |
| Other | Student ID | 85.6 × 54 | preset |
| Other | Employee ID | 85.6 × 54 | preset |
| Other | Membership card | 85.6 × 54 | preset |
| Other | Business card | 90 × 54 | preset |
| Other | Custom document | user-defined | — |

**Why mostly 85.6 × 54:** most Indian smart cards conform to ISO/IEC 7810
ID-1 (the credit-card form factor). Where an issuer deviates, the user can
edit the preset or create a custom size — the app never forces a document into
a wrong size and never stretches/alters printed information.

---

## 5. Import & cropping

* **PDF** — `PdfRenderer` renders each page to a `Bitmap`; a thumbnail grid
  lets the user pick pages. Corrupt PDFs are caught and surfaced as an error,
  never a crash.
* **Images** — JPG/JPEG/PNG decoded with a downsampled `BitmapFactory`.
* **Auto-detect** — `CardDetector` runs a luminance-gradient edge scan, then
  finds the largest quadrilateral that keeps a plausible aspect ratio. When
  confidence is low it returns the full page and flags `lowConfidence = true`,
  so the UI asks the user to confirm — content is never silently discarded.
* **Manual crop** — draggable corner handles with aspect-ratio locking.
* **Perspective** — `PerspectiveCorrector` computes a homography from the 4
  corners to an axis-aligned rectangle (bilinear warp; the OpenCV-backed
  variant can be dropped in behind the same interface).
* **Undo/redo** — an in-memory stack of crop states per card.

---

## 6. Layout engine

`PageLayoutEngine` is pure Kotlin:

1. Resolve the printable area = paper − margins.
2. For each card compute placement cells; support both **auto grid** (columns
   = floor(usableWidth / (cardWidth+spacing))) and **manual** positions.
3. Duplicate presets (2/4/6/8/custom) expand `quantity` into instances.
4. Detect overflow → return `LayoutResult(pages, placements, overflow, warnings)`
   so the UI can *suggest* more pages or a different layout — it never silently
   resizes a document.
5. Optional cut marks / dashed lines drawn at export time.

---

## 7. Export

* `PdfExporter` creates a PDF whose page size equals the real paper size in
  PostScript points (1 mm = 72/25.4 pt ≈ 2.8346 pt), so prints come out at
  **100 % scale**.
* Emits: multi-page PDF, optional cut guides, optional front/back (duplex)
  layout with configurable flip direction.
* Also exports individual cropped card images (PNG) when requested.
* Uses the **Storage Access Framework** (`ACTION_CREATE_DOCUMENT`) for output.
* Aspect ratios and image quality are preserved; nothing is stretched.

---

## 8. Privacy

* All processing is on-device. No network permission is requested.
* No analytics, no logging of document contents or personal numbers.
* Temp render files are cleared when a project is closed/deleted.
* Projects can be deleted from Recent Projects and Settings.

---

## 9. Testing

Pure-JVM unit tests (no device needed) for:

* `GeometryTest` — mm↔pt↔px conversion, aspect-ratio math, homography solve.
* `TemplateRegistryTest` — every template has a valid size; ISO ID-1 checks.
* `PageLayoutEngineTest` — grid packing, overflow detection, duplication,
  margins/spacing.
* `CropGeometryTest` — crop-quad → rect normalisation and clamping.

Run with `./gradlew test`.

---

## 10. Implementation plan (stages)

1. Scaffold: Gradle, version catalog, manifest, theme, navigation graph. ✔
2. Core models + template registry + units/geometry. ✔
3. Data layer: importers + repositories. ✔
4. Domain engines: detector, perspective, layout, export. ✔
5. UI: 12 screens + ViewModels, wired to the engines. ✔
6. Tests + README + CI (GitHub Actions → APK). ✔
