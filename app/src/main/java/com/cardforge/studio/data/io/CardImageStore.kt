package com.cardforge.studio.data.io

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Stores cropped card images and rendered source pages on the device's private
 * storage only. Temporary data is cleared when a project or card is deleted.
 */
class CardImageStore(private val context: Context) {

    private fun dirFor(kind: String): File =
        File(context.filesDir, kind).apply { if (!exists()) mkdirs() }

    /** Persist [bitmap] as a PNG and return the absolute file path. */
    fun save(bitmap: Bitmap, prefix: String = "card"): String {
        val dir = dirFor("cards")
        val file = File(dir, "$prefix-${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file.absolutePath
    }

    /** Persist a rendered source page (kept separate from cropped cards). */
    fun savePage(bitmap: Bitmap): String {
        val dir = dirFor("pages")
        val file = File(dir, "page-${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file.absolutePath
    }

    fun load(path: String?): Bitmap? {
        if (path == null) return null
        val f = File(path)
        if (!f.exists()) return null
        return try {
            android.graphics.BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path == null) return
        runCatching { File(path).delete() }
    }

    /** Remove every stored image (used by "clear temporary data"). */
    fun clearAll() {
        runCatching { dirFor("cards").listFiles()?.forEach { it.delete() } }
        runCatching { dirFor("pages").listFiles()?.forEach { it.delete() } }
    }

    /** Best-effort removal of orphaned card images not referenced by [keep]. */
    fun pruneOrphans(keep: Set<String>) {
        runCatching {
            dirFor("cards").listFiles()?.forEach { f -> if (f.absolutePath !in keep) f.delete() }
        }
    }
}
