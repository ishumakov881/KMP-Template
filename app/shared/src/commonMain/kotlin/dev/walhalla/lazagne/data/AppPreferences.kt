package dev.walhalla.lazagne.data

import com.russhwolf.settings.Settings

/**
 * Thin typed facade over russhwolf [Settings].
 * Domain keys live in [Companion]; no one-key stores per feature.
 */
class AppPreferences(private val settings: Settings) {

    fun getStringOrNull(key: String): String? =
        settings.getStringOrNull(key)?.takeIf { it.isNotBlank() }

    fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    fun remove(key: String) {
        settings.remove(key)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        settings.getBoolean(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long =
        settings.getLong(key, defaultValue)

    fun putLong(key: String, value: Long) {
        settings.putLong(key, value)
    }

    fun getInt(key: String, defaultValue: Int = 0): Int =
        settings.getInt(key, defaultValue)

    fun putInt(key: String, value: Int) {
        settings.putInt(key, value)
    }

    companion object {
        const val KEY_PHPSESSID = "pref_nos_phpsessid"
    }
}
