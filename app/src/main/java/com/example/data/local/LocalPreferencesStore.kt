package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentPalette
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flexdesk_local_prefs")

data class LocalControlState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentPalette: AccentPalette = AccentPalette.CHROME_BLUE,
    val fontScale: Float = 1.0f,
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val largeTargets: Boolean = false,
    val liteMode: Boolean = false,
    val dndEnabled: Boolean = false,
    val screenBrightness: Float = -1f, // -1f = system default
    val developerModeEnabled: Boolean = false,
    val suppressDevBootWarning: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.AUTO,
    val lockPin: String = "",
    val requireLockOnBoot: Boolean = false,
    val pinnedApps: List<String> = listOf(
        "builtin:docs",
        "builtin:notes",
        "builtin:files",
        "web:google_photos",
        "builtin:calculator"
    )
)

class LocalPreferencesStore(private val context: Context) {

    val localStateFlow: Flow<LocalControlState> = context.dataStore.data.map { prefs ->
        val pinnedRaw = prefs[KEY_PINNED_APPS]
        val pinnedList = if (pinnedRaw.isNullOrBlank()) {
            LocalControlState().pinnedApps
        } else {
            pinnedRaw.split("|").filter { it.isNotBlank() }.take(20)
        }
        LocalControlState(
            themeMode = ThemeMode.fromKey(prefs[KEY_THEME_MODE]),
            accentPalette = AccentPalette.fromKey(prefs[KEY_ACCENT_PALETTE]),
            fontScale = (prefs[KEY_FONT_SCALE] ?: 1.0).toFloat().coerceIn(0.85f, 1.4f),
            reduceMotion = prefs[KEY_REDUCE_MOTION] ?: false,
            highContrast = prefs[KEY_HIGH_CONTRAST] ?: false,
            largeTargets = prefs[KEY_LARGE_TARGETS] ?: false,
            liteMode = prefs[KEY_LITE_MODE] ?: false,
            dndEnabled = prefs[KEY_DND_ENABLED] ?: false,
            screenBrightness = prefs[KEY_SCREEN_BRIGHTNESS] ?: -1f,
            developerModeEnabled = prefs[KEY_DEVELOPER_MODE_ENABLED] ?: false,
            suppressDevBootWarning = prefs[KEY_SUPPRESS_DEV_BOOT_WARNING] ?: false,
            appLanguage = AppLanguage.fromKey(prefs[KEY_APP_LANGUAGE]),
            lockPin = prefs[KEY_LOCK_PIN] ?: "",
            requireLockOnBoot = prefs[KEY_REQUIRE_LOCK_ON_BOOT] ?: false,
            pinnedApps = pinnedList
        )
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[KEY_DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val generated = "dev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        context.dataStore.edit { it[KEY_DEVICE_ID] = generated }
        return generated
    }

    suspend fun saveFromUserSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = settings.themeMode
            prefs[KEY_ACCENT_PALETTE] = settings.accentPalette
            prefs[KEY_FONT_SCALE] = settings.fontScale.coerceIn(0.85, 1.4)
            prefs[KEY_REDUCE_MOTION] = settings.reduceMotion
            prefs[KEY_HIGH_CONTRAST] = settings.highContrast
            prefs[KEY_LARGE_TARGETS] = settings.largeTargets
            prefs[KEY_LITE_MODE] = settings.liteMode
            prefs[KEY_PINNED_APPS] = settings.pinnedApps.take(20).joinToString("|")
        }
    }

    suspend fun updateLocalDeviceControls(dndEnabled: Boolean, screenBrightness: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DND_ENABLED] = dndEnabled
            prefs[KEY_SCREEN_BRIGHTNESS] = screenBrightness
        }
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVELOPER_MODE_ENABLED] = enabled
            // Re-arm WP boot warning whenever Developer Mode is toggled
            prefs[KEY_SUPPRESS_DEV_BOOT_WARNING] = false
        }
    }

    suspend fun setSuppressDevBootWarning(suppress: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUPPRESS_DEV_BOOT_WARNING] = suppress
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language.key
        }
    }

    suspend fun setLockSettings(pin: String, requireOnBoot: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCK_PIN] = pin.filter { it.isDigit() }.take(8)
            prefs[KEY_REQUIRE_LOCK_ON_BOOT] = requireOnBoot
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val KEY_FONT_SCALE = doublePreferencesKey("font_scale")
        val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val KEY_LARGE_TARGETS = booleanPreferencesKey("large_targets")
        val KEY_LITE_MODE = booleanPreferencesKey("lite_mode")
        val KEY_PINNED_APPS = stringPreferencesKey("pinned_apps")
        val KEY_DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val KEY_SCREEN_BRIGHTNESS = floatPreferencesKey("screen_brightness")
        val KEY_DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val KEY_SUPPRESS_DEV_BOOT_WARNING = booleanPreferencesKey("suppress_dev_boot_warning")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_LOCK_PIN = stringPreferencesKey("lock_pin")
        val KEY_REQUIRE_LOCK_ON_BOOT = booleanPreferencesKey("require_lock_on_boot")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    }
}
