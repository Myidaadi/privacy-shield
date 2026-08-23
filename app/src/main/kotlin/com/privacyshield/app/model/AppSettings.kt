package com.privacyshield.app.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_settings")

enum class OverlayStyle { BLUR, DARK_CURTAIN, MOSAIC }
enum class DetectionSensitivity(val frameThreshold: Int) {
    LOW(5), MEDIUM(3), HIGH(1)
}

data class AppSettings(
    val enabled: Boolean = false,
    val overlayStyle: OverlayStyle = OverlayStyle.BLUR,
    val sensitivity: DetectionSensitivity = DetectionSensitivity.MEDIUM,
    val autoLockSeconds: Int = 30,
    val requireBiometric: Boolean = false,
)

object AppSettingsKeys {
    val ENABLED          = booleanPreferencesKey("enabled")
    val OVERLAY_STYLE    = intPreferencesKey("overlay_style")
    val SENSITIVITY      = intPreferencesKey("sensitivity")
    val AUTO_LOCK_SECS   = intPreferencesKey("auto_lock_secs")
    val REQUIRE_BIOMETRIC = booleanPreferencesKey("require_biometric")
}

class AppSettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs -> prefs.toAppSettings() }

    private fun Preferences.toAppSettings() = AppSettings(
        enabled          = this[AppSettingsKeys.ENABLED]           ?: false,
        overlayStyle     = OverlayStyle.entries[this[AppSettingsKeys.OVERLAY_STYLE]   ?: 0],
        sensitivity      = DetectionSensitivity.entries[this[AppSettingsKeys.SENSITIVITY] ?: 1],
        autoLockSeconds  = this[AppSettingsKeys.AUTO_LOCK_SECS]   ?: 30,
        requireBiometric = this[AppSettingsKeys.REQUIRE_BIOMETRIC] ?: false,
    )

    suspend fun setEnabled(v: Boolean)                 = save(AppSettingsKeys.ENABLED, v)
    suspend fun setOverlayStyle(v: OverlayStyle)       = save(AppSettingsKeys.OVERLAY_STYLE, v.ordinal)
    suspend fun setSensitivity(v: DetectionSensitivity)= save(AppSettingsKeys.SENSITIVITY, v.ordinal)
    suspend fun setAutoLockSeconds(v: Int)             = save(AppSettingsKeys.AUTO_LOCK_SECS, v)
    suspend fun setRequireBiometric(v: Boolean)        = save(AppSettingsKeys.REQUIRE_BIOMETRIC, v)

    private suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
