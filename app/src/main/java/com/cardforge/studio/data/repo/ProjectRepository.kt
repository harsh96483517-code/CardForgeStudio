package com.cardforge.studio.data.repo

import android.content.Context
import com.cardforge.studio.core.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists projects as JSON files in the app's private storage.
 * Deleting a project also removes its stored card images.
 */
class ProjectRepository(private val context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dir: File get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    suspend fun list(): List<Project> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { runCatching { json.decodeFromString<Project>(it.readText()) }.getOrNull() }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    suspend fun load(id: String): Project? = withContext(Dispatchers.IO) {
        val f = File(dir, "$id.json")
        if (!f.exists()) null else runCatching { json.decodeFromString<Project>(f.readText()) }.getOrNull()
    }

    suspend fun save(project: Project): Project = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        File(dir, "${updated.id}.json").writeText(json.encodeToString(Project.serializer(), updated))
        updated
    }

    suspend fun delete(id: String, cardImagePaths: List<String> = emptyList()): Unit = withContext(Dispatchers.IO) {
        File(dir, "$id.json").delete()
        cardImagePaths.forEach { runCatching { File(it).delete() } }
    }
}
