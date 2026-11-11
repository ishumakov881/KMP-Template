package online.lds.nos.data

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

    fun hasKey(key: String): Boolean = settings.hasKey(key)

    /**
     * Map legend: layers the user unchecked. `null` = never saved (show all).
     * New catalog layers not in this set stay visible after restore.
     */
    fun getMapHiddenLayers(): Set<String>? {
        if (!hasKey(KEY_MAP_HIDDEN_LAYERS)) return null
        val raw = settings.getString(KEY_MAP_HIDDEN_LAYERS, "")
        if (raw.isBlank()) return emptySet()
        return raw.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun putMapHiddenLayers(hidden: Set<String>) {
        putString(KEY_MAP_HIDDEN_LAYERS, hidden.sorted().joinToString("\n"))
    }

    fun isOfflineCacheEnabled(): Boolean =
        getBoolean(KEY_OFFLINE_CACHE, defaultValue = false)

    fun setOfflineCacheEnabled(enabled: Boolean) {
        putBoolean(KEY_OFFLINE_CACHE, enabled)
    }

    companion object {
        const val KEY_PHPSESSID = "pref_nos_phpsessid"
        /** When true: UI reads Room Flow, network writes DAO. When false: network-only. */
        const val KEY_OFFLINE_CACHE = "pref_nos_offline_cache"
        /** Newline-separated leaflet `layer` keys hidden on the map legend. */
        const val KEY_MAP_HIDDEN_LAYERS = "pref_nos_map_hidden_layers"
    }
}
