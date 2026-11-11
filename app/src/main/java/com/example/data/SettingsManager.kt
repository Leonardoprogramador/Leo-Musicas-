package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SARCASM_LEVEL = "sarcasm_level"
        private const val KEY_THEME = "app_theme"
        private const val KEY_VOLUME = "playback_volume"
        private const val KEY_SHUFFLE = "playback_shuffle"
        private const val KEY_REPEAT_MODE = "playback_repeat_mode"
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_POSITION = "last_position"
    }

    enum class AppTheme {
        ESCURO, CLARO
    }

    enum class RepeatMode {
        NENHUM, REPETIR_UMA, REPETIR_PLAYLIST
    }

    var sarcasmLevel: SarcasmManager.SarcasmLevel
        get() = SarcasmManager.SarcasmLevel.valueOf(prefs.getString(KEY_SARCASM_LEVEL, SarcasmManager.SarcasmLevel.MUITO.name) ?: SarcasmManager.SarcasmLevel.MUITO.name)
        set(value) = prefs.edit().putString(KEY_SARCASM_LEVEL, value.name).apply()

    var theme: AppTheme
        get() = AppTheme.valueOf(prefs.getString(KEY_THEME, AppTheme.ESCURO.name) ?: AppTheme.ESCURO.name)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var volume: Float
        get() = prefs.getFloat(KEY_VOLUME, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOLUME, value).apply()

    var isShuffle: Boolean
        get() = prefs.getBoolean(KEY_SHUFFLE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHUFFLE, value).apply()

    var repeatMode: RepeatMode
        get() = RepeatMode.valueOf(prefs.getString(KEY_REPEAT_MODE, RepeatMode.NENHUM.name) ?: RepeatMode.NENHUM.name)
        set(value) = prefs.edit().putString(KEY_REPEAT_MODE, value.name).apply()

    var lastSongId: Int
        get() = prefs.getInt(KEY_LAST_SONG_ID, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_SONG_ID, value).apply()

    var lastPosition: Int
        get() = prefs.getInt(KEY_LAST_POSITION, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_POSITION, value).apply()

    var apkDownloadUrl: String
        get() = prefs.getString("apk_download_url", "") ?: ""
        set(value) = prefs.edit().putString("apk_download_url", value).apply()

    var lastActiveTab: String
        get() = prefs.getString("last_active_tab", "INICIO") ?: "INICIO"
        set(value) = prefs.edit().putString("last_active_tab", value).apply()

    var lastSelectedPlaylistId: Int
        get() = prefs.getInt("last_selected_playlist_id", -1)
        set(value) = prefs.edit().putInt("last_selected_playlist_id", value).apply()

    var lastPlaybackMode: String
        get() = prefs.getString("last_playback_mode", "ALL_SONGS") ?: "ALL_SONGS"
        set(value) = prefs.edit().putString("last_playback_mode", value).apply()

    var lastPlaybackPlaylistId: Int
        get() = prefs.getInt("last_playback_playlist_id", -1)
        set(value) = prefs.edit().putInt("last_playback_playlist_id", value).apply()

    var lastFilterArtist: String
        get() = prefs.getString("last_filter_artist", "") ?: ""
        set(value) = prefs.edit().putString("last_filter_artist", value).apply()

    var lastSearchQuery: String
        get() = prefs.getString("last_search_query", "") ?: ""
        set(value) = prefs.edit().putString("last_search_query", value).apply()

    var lastSortOption: String
        get() = prefs.getString("last_sort_option", "TITULO") ?: "TITULO"
        set(value) = prefs.edit().putString("last_sort_option", value).apply()

    var hasSeenSplash: Boolean
        get() = prefs.getBoolean("has_seen_splash", false)
        set(value) = prefs.edit().putBoolean("has_seen_splash", value).apply()

    var isPlayerExpanded: Boolean
        get() = prefs.getBoolean("is_player_expanded", false)
        set(value) = prefs.edit().putBoolean("is_player_expanded", value).apply()

    var lastAppVersionCode: Int
        get() = prefs.getInt("last_app_version_code", 1)
        set(value) = prefs.edit().putInt("last_app_version_code", value).apply()

    fun reset() {
        val seenSplash = hasSeenSplash
        prefs.edit().clear().apply()
        hasSeenSplash = seenSplash
    }
}
