package com.cardforge.studio.data.repo

import android.content.Context
import com.cardforge.studio.core.model.DocumentTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Persists user-created custom templates as JSON in private storage. */
class CustomTemplateStore(private val context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val file: File get() = File(context.filesDir, "custom_templates.json")

    suspend fun load(): List<DocumentTemplate> = withContext(Dispatchers.IO) {
        if (!file.exists()) emptyList()
        else runCatching {
            json.decodeFromString(ListSerializer(DocumentTemplate.serializer()), file.readText())
        }.getOrNull() ?: emptyList()
    }

    suspend fun add(template: DocumentTemplate): List<DocumentTemplate> = withContext(Dispatchers.IO) {
        val current = load().toMutableList()
        current.removeAll { it.id == template.id }
        current += template
        file.writeText(json.encodeToString(ListSerializer(DocumentTemplate.serializer()), current))
        current
    }

    suspend fun remove(id: String): List<DocumentTemplate> = withContext(Dispatchers.IO) {
        val current = load().filterNot { it.id == id }
        file.writeText(json.encodeToString(ListSerializer(DocumentTemplate.serializer()), current))
        current
    }
}
