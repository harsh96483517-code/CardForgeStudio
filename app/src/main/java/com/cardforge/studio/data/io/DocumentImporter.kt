package com.cardforge.studio.data.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.IOException
import kotlin.math.max

/** Typed error surfaced to the UI when a file cannot be read. */
class ImportException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Reads user-supplied PDFs and images. Nothing leaves the device.
 *
 * PDF pages are rasterised with the platform [PdfRenderer]; images are decoded
 * with a downsample pass so large camera photos do not exhaust memory. Corrupt
 * or unsupported files raise [ImportException] instead of crashing.
 */
class DocumentImporter(private val context: Context) {

    /** Number of pages in a PDF, or 1 for a single-page document. */
    fun pdfPageCount(uri: Uri): Int {
        openRenderer(uri).use { renderer -> return renderer.pageCount }
    }

    /** Rasterise one PDF page at (approximately) [targetWidthPx] wide. */
    fun renderPdfPage(uri: Uri, pageIndex: Int, targetWidthPx: Int = 1600): Bitmap {
        openRenderer(uri).use { renderer ->
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                throw ImportException("Page $pageIndex is out of range (0..${renderer.pageCount - 1}).")
            }
            renderer.openPage(pageIndex).use { page ->
                val scale = if (page.width > 0) targetWidthPx.toFloat() / page.width else 1f
                val w = max(1, (page.width * scale).toInt())
                val h = max(1, (page.height * scale).toInt())
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bmp
            }
        }
    }

    /** Decode an image, downsampled so its long edge is at most [maxDim]. */
    fun loadImage(uri: Uri, maxDim: Int = 2600): Bitmap {
        // Pass 1: bounds only.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw ImportException("Could not open the selected image.")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw ImportException("The selected file is not a supported image.")
        }
        // Pass 2: decode with a power-of-two sample size.
        var sample = 1
        var longEdge = max(bounds.outWidth, bounds.outHeight)
        while (longEdge / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw ImportException("Could not decode the selected image.")
        return bmp
    }

    /** True when the MIME type looks like a PDF. */
    fun isPdf(uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri) ?: return false
        return type == "application/pdf"
    }

    private fun openRenderer(uri: Uri): PdfRenderer {
        val pfd: ParcelFileDescriptor = try {
            context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw ImportException("Could not open the PDF.")
        } catch (e: Exception) {
            throw ImportException("Could not open the PDF.", e)
        }
        return try {
            PdfRenderer(pfd)
        } catch (e: Exception) {
            pfd.close()
            throw ImportException("The PDF appears to be corrupt or password-protected.", e)
        }
    }
}
