# CardForge Studio

An Android app that turns **your own** PDFs and images of Indian documents into
neatly cropped cards arranged on A4 (or A3 / Letter) sheets, exported as
print-ready PDFs.

Import → Select template → Auto-crop → Adjust size → Front/Back → Copies →
Arrange on A4 → Preview → Export.

> **What this app is *not*.** It does not generate, invent or imitate official
> government documents, seals, signatures, QR codes or security features, and it
> never fabricates identity data. Templates describe **geometry only** — crop
> area, aspect ratio, physical size and orientation. Every document pixel comes
> from a file you supply. All processing is on-device; the app requests **no
> internet permission** and sends no analytics.

---

## Features

- **Template library** for Indian documents across Identity, Vehicle and Other
  categories (PAN, Aadhaar, Voter ID/EPIC, Passport data page, DL, RC, insurance,
  student/employee/membership/business cards, custom). Search, category chips,
  favourites, recents.
- **Import** multi-page PDFs (rasterised with `PdfRenderer`) and JPG/JPEG/PNG.
- **Automatic rectangular card detection** with a low-confidence flag that asks
  for confirmation instead of silently discarding content.
- **Auto-split a layout page** (e.g. the Aadhaar print sheet) into its **front
  and back card** in one tap, or into every card on the page — then drop the
  pair straight onto one A4, side by side.
- **Front + back side by side**, or **duplex** (front and back on separate
  pages) with configurable flip direction.
- **Centred presets**: 2-up / 4-up / 6-up / 8-up grids on A4.
- **Manual crop** with draggable corners and **perspective correction** for
  photographed documents.
- **Front/back** support: assign both sides to one card, side-by-side preview.
- **Custom sizes** in millimetres, orientation, Fit / Fill / Original-Ratio,
  aspect lock, reset-to-defaults, and save-as-custom-template.
- **A4 Layout Studio**: A4/A3/Letter/custom, portrait/landscape, margins,
  spacing, cut marks, dashed cut lines, drag-to-position, page & card counts,
  overflow warnings.
- **Duplicate presets**: 2 / 4 / 6 / 8 / custom, and multiple different cards per
  sheet.
- **Export**: print-ready multi-page PDF at true physical page size
  (1 mm = 2.8346 pt, so print at 100 %), optional cutting guides, optional
  duplex (front+back) layout with configurable flip direction, plus individual
  cropped PNGs. Uses the Storage Access Framework.
- **Premium UI**: dark-navy and light themes, refined blue accent, liquid-glass
  surfaces, Material 3, responsive phone/tablet layouts.

## Screens

1. Home Dashboard
2. Document Template Gallery
3. File Import
4. PDF Page Browser
5. Auto Crop Review
6. Front/Back Editor
7. Card Size Settings
8. A4 Layout Editor
9. Print Preview
10. Export PDF
11. Recent Projects
12. Settings

## Tech stack

- Kotlin, Jetpack Compose, Material 3, MVVM + clean layering
- `PdfRenderer` for PDF input, `android.graphics.pdf.PdfDocument` for output
- Coroutines for all background work
- kotlinx.serialization for projects/templates, DataStore for settings
- Pure-Kotlin crop detection & perspective warp (JVM unit-tested). The
  `CardDetector` interface lets you drop in the native OpenCV detector
  (`org.opencv:opencv`) — see the commented dependency in `app/build.gradle.kts`.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full design.

## Build & run

Requirements: Android Studio (Ladybug or newer) or JDK 17 + Android SDK 35.

```bash
# From the project root
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # pure-JVM unit tests
./gradlew installDebug       # install on a connected device
```

Or open the folder in Android Studio and press Run.

### Prebuilt APK via CI

Every push to `main` triggers [`.github/workflows/android.yml`](.github/workflows/android.yml),
which runs the unit tests, builds `app-debug.apk`, and attaches it to the
**`latest`** GitHub Release. Download the APK from the repo's *Releases* page and
open it on your device (enable "Install unknown apps" first).

## Privacy

- On-device only; no `INTERNET` permission is declared.
- No logging of document contents or personal numbers.
- Temporary render files are cleared via Settings → *Clear temporary image data*.
- Projects can be deleted from Recent Projects; deleting a project also removes
  its stored card images.

## Legal note

Use this app only with documents you are entitled to reproduce, and only for
lawful purposes. It is a printing/organising tool, not a document issuer.
