package com.cardforge.studio.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cardforge.studio.core.model.FitMode
import com.cardforge.studio.core.model.Orientation
import com.cardforge.studio.core.model.PaperKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cardforge_settings")

/** User preferences: theme, default paper and default crop behaviour. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_PAPER = stringPreferencesKey("default_paper")
        val DEFAULT_ORIENTATION = stringPreferencesKey("default_orientation")
        val DEFAULT_FIT = stringPreferencesKey("default_fit")
        val CONFIRM_LOW_CONFIDENCE = booleanPreferencesKey("confirm_low_confidence")
        val KEEP_CUT_MARKS = booleanPreferencesKey("keep_cut_marks")
    }

    data class Settings(
        val darkTheme: Boolean = true,
        val dynamicColor: Boolean = false,
        val defaultPaper: PaperKind = PaperKind.A4,
        val defaultOrientation: Orientation = Orientation.PORTRAIT,
        val defaultFit: FitMode = FitMode.FIT,
        val confirmLowConfidence: Boolean = true,
        val keepCutMarks: Boolean = true
    )

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            darkTheme = p[Keys.DARK_THEME] ?: true,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: false,
            defaultPaper = p[Keys.DEFAULT_PAPER]?.let { runCatching { PaperKind.valueOf(it) }.getOrNull() } ?: PaperKind.A4,
            defaultOrientation = p[Keys.DEFAULT_ORIENTATION]?.let { runCatching { Orientation.valueOf(it) }.getOrNull() } ?: Orientation.PORTRAIT,
            defaultFit = p[Keys.DEFAULT_FIT]?.let { runCatching { FitMode.valueOf(it) }.getOrNull() } ?: FitMode.FIT,
            confirmLowConfidence = p[Keys.CONFIRM_LOW_CONFIDENCE] ?: true,
            keepCutMarks = p[Keys.KEEP_CUT_MARKS] ?: true
        )
    }

    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[Keys.DARK_THEME] = value }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setDefaultPaper(value: PaperKind) = context.dataStore.edit { it[Keys.DEFAULT_PAPER] = value.name }
    suspend fun setDefaultOrientation(value: Orientation) = context.dataStore.edit { it[Keys.DEFAULT_ORIENTATION] = value.name }
    suspend fun setDefaultFit(value: FitMode) = context.dataStore.edit { it[Keys.DEFAULT_FIT] = value.name }
    suspend fun setConfirmLowConfidence(value: Boolean) = context.dataStore.edit { it[Keys.CONFIRM_LOW_CONFIDENCE] = value }
    suspend fun setKeepCutMarks(value: Boolean) = context.dataStore.edit { it[Keys.KEEP_CUT_MARKS] = value }
}
