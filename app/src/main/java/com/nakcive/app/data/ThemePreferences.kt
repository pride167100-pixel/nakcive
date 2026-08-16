package com.nakcive.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private const val PREFS_NAME = "nakcive_prefs"
private const val KEY_THEME_MODE = "theme_mode"

object ThemePreferences {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val saved = prefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        _themeMode.value = runCatching { ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        prefs(context).edit { putString(KEY_THEME_MODE, mode.name) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
