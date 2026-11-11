package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MusicPlayerManager
import com.example.data.SarcasmManager
import com.example.data.SettingsManager
import com.example.data.database.PlaylistEntity
import com.example.data.database.SongEntity
import com.example.data.repository.MusicRepository
import com.example.util.MetadataHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(
    private val repository: MusicRepository,
    val settings: SettingsManager,
    val playerManager: MusicPlayerManager
) : ViewModel() {

    enum class SortOption(val label: String) {
        TITULO("Ordem Alfabética"),
        ARTISTA("Culpado (Artista)"),
        ALBUM("Coleção (Álbum)"),
        DATA_ADICIONADA("Erro Recente (Recentes)")
    }

    enum class ActiveTab {
        INICIO, PLAYLISTS, FAVORITOS, QR_CODE
    }

    // UI routing state
    private val _currentTab = MutableStateFlow(
        run {
            val savedTab = settings.lastActiveTab
            runCatching { ActiveTab.valueOf(savedTab) }.getOrDefault(ActiveTab.INICIO)
        }
    )
    val currentTab: StateFlow<ActiveTab> = _currentTab

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist

    // Search and Sort states
    private val _searchQuery = MutableStateFlow(settings.lastSearchQuery)
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOption = MutableStateFlow(
        runCatching { SortOption.valueOf(settings.lastSortOption) }.getOrDefault(SortOption.TITULO)
    )
    val sortOption: StateFlow<SortOption> = _sortOption

    // Filter by Artist state
    private val _filterArtist = MutableStateFlow<String?>(settings.lastFilterArtist.ifBlank { null })
    val filterArtist: StateFlow<String?> = _filterArtist

    // Live Settings state
    private val _sarcasmLevel = MutableStateFlow(settings.sarcasmLevel)
    val sarcasmLevel: StateFlow<SarcasmManager.SarcasmLevel> = _sarcasmLevel

    private val _appTheme = MutableStateFlow(settings.theme)
    val appTheme: StateFlow<SettingsManager.AppTheme> = _appTheme

    private val _apkDownloadUrl = MutableStateFlow(
        run {
            val cached = settings.apkDownloadUrl
            val lastVersion = settings.lastAppVersionCode
            if (lastVersion < com.example.util.QrCodeGenerator.CURRENT_VERSION_CODE ||
                cached.isBlank() ||
                cached.contains("kAdwrV7o") ||
                cached.contains("github.com/leomusicas") ||
                cached.contains("ais-pre-")
            ) {
                settings.apkDownloadUrl = com.example.util.QrCodeGenerator.DEFAULT_DOWNLOAD_URL
                settings.lastAppVersionCode = com.example.util.QrCodeGenerator.CURRENT_VERSION_CODE
                com.example.util.QrCodeGenerator.DEFAULT_DOWNLOAD_URL
            } else {
                cached
            }
        }
    )
    val apkDownloadUrl: StateFlow<String> = _apkDownloadUrl

    // Playlists & Songs from DB
    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered and Sorted Songs
    val songs: StateFlow<List<SongEntity>> = combine(
        rawSongs,
        _searchQuery,
        _sortOption,
        _filterArtist
    ) { songsList, query, sort, artistFilter ->
        var filtered = songsList

        // Apply Search Query
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        }

        // Apply Artist Filter
        if (artistFilter != null) {
            filtered = filtered.filter { it.artist.equals(artistFilter, ignoreCase = true) }
        }

        // Apply Sorting
        when (sort) {
            SortOption.TITULO -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTISTA -> filtered.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SortOption.DATA_ADICIONADA -> filtered.sortedByDescending { it.dateAdded }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique list of artists for filtering
    val artistsList: StateFlow<List<String>> = rawSongs
        .map { list -> list.map { it.artist }.distinct().sortedBy { it.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically restore previous playing song state, exact queue, and playlist selection
        viewModelScope.launch {
            combine(rawSongs, allPlaylists) { songs, playlists ->
                Pair(songs, playlists)
            }.collect { (songs, playlists) ->
                if (songs.isNotEmpty() && playerManager.currentSong.value == null) {
                    val mode = settings.lastPlaybackMode
                    val playlistId = settings.lastPlaybackPlaylistId

                    val queueToRestore = when (mode) {
                        MusicPlayerManager.PlaybackSource.PLAYLIST.name -> {
                            val targetPlaylist = playlists.find { it.id == playlistId }
                            if (targetPlaylist != null) {
                                val songIds = targetPlaylist.songIds.split(",")
                                    .mapNotNull { it.trim().toIntOrNull() }
                                val pSongs = songIds.mapNotNull { id -> songs.find { it.id == id } }
                                if (pSongs.isNotEmpty()) pSongs else songs
                            } else {
                                songs
                            }
                        }
                        MusicPlayerManager.PlaybackSource.FAVORITES.name -> {
                            val favs = songs.filter { it.isFavorite }
                            if (favs.isNotEmpty()) favs else songs
                        }
                        else -> songs
                    }

                    playerManager.restoreLastPlayedState(queueToRestore)

                    // Also restore selected playlist view if user was viewing one
                    val lastSelectedId = settings.lastSelectedPlaylistId
                    if (lastSelectedId != -1 && _selectedPlaylist.value == null) {
                        _selectedPlaylist.value = playlists.find { it.id == lastSelectedId }
                    }
                }
            }
        }
    }

    fun setTab(tab: ActiveTab) {
        _currentTab.value = tab
        settings.lastActiveTab = tab.name
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
        settings.lastSelectedPlaylistId = playlist?.id ?: -1
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        settings.lastSearchQuery = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        settings.lastSortOption = option.name
    }

    fun setFilterArtist(artist: String?) {
        _filterArtist.value = artist
        settings.lastFilterArtist = artist ?: ""
    }

    // Settings API
    fun updateSarcasmLevel(level: SarcasmManager.SarcasmLevel) {
        settings.sarcasmLevel = level
        _sarcasmLevel.value = level
    }

    fun updateTheme(theme: SettingsManager.AppTheme) {
        settings.theme = theme
        _appTheme.value = theme
    }

    fun updateApkDownloadUrl(url: String) {
        val trimmed = url.trim()
        settings.apkDownloadUrl = trimmed
        _apkDownloadUrl.value = trimmed.ifBlank { com.example.util.QrCodeGenerator.DEFAULT_DOWNLOAD_URL }
    }

    fun restoreSettings() {
        settings.reset()
        _sarcasmLevel.value = settings.sarcasmLevel
        _appTheme.value = settings.theme
        playerManager.setVolume(settings.volume)
        playerManager.setShuffle(settings.isShuffle)
        playerManager.setRepeatMode(settings.repeatMode)
    }

    // Music Importing
    fun importSongs(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            var successCount = 0

            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    // Get the file name
                    val originalName = getFileName(context, uri) ?: "imported_media_${System.currentTimeMillis()}"
                    
                    // Sanitize file name to avoid directory traversal or invalid characters
                    val cleanName = originalName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                    val uniqueName = "${System.currentTimeMillis()}_$cleanName"
                    val localFile = java.io.File(context.filesDir, uniqueName)

                    var copySuccess = false
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            localFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        copySuccess = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (!copySuccess) {
                        continue
                    }

                    val localUriString = Uri.fromFile(localFile).toString()

                    // Check if already in DB
                    val existing = repository.getSongByUri(localUriString)
                    if (existing != null) {
                        continue // already added
                    }

                    // Retrieve metadata from local file
                    val metadata = MetadataHelper.getSongMetadata(context, Uri.parse(localUriString))
                    val song = SongEntity(
                        uriString = localUriString,
                        title = metadata.title,
                        artist = metadata.artist,
                        album = metadata.album,
                        durationMs = metadata.durationMs,
                        dateAdded = System.currentTimeMillis()
                    )

                    repository.insertSong(song)
                    successCount++
                }
            }

            if (successCount > 0) {
                Toast.makeText(context, "$successCount arquivo(s) importado(s) com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) result = cursor.getString(idx)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    // Song CRUD
    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            val updated = song.copy(isFavorite = !song.isFavorite)
            repository.updateSong(updated)
            playerManager.updateSongInQueue(updated)
            // If current playing, update state
            if (playerManager.currentSong.value?.id == song.id) {
                playerManager.updateCurrentSongEntity(updated)
            }
        }
    }

    fun updateSongLyrics(song: SongEntity, lyrics: String) {
        viewModelScope.launch {
            val updated = song.copy(lyrics = lyrics)
            repository.updateSong(updated)
            playerManager.updateCurrentSongEntity(updated)
        }
    }

    fun deleteSong(context: Context, song: SongEntity) {
        viewModelScope.launch {
            // 1. If song is in player playback, safely remove and advance
            playerManager.removeSongFromPlayback(song.id)

            // 2. Remove from all playlists
            val playlists = allPlaylists.value
            for (playlist in playlists) {
                val ids = playlist.songIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
                if (ids.contains(song.id)) {
                    val updatedIds = ids.filter { it != song.id }.joinToString(",")
                    repository.updatePlaylist(playlist.copy(songIds = updatedIds))
                }
            }

            // Delete the copied local file from private filesDir
            try {
                if (song.uriString.startsWith("file://")) {
                    val fileUri = Uri.parse(song.uriString)
                    fileUri.path?.let { path ->
                        val localFile = java.io.File(path)
                        if (localFile.exists()) {
                            localFile.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Delete from DB
            repository.deleteSong(song)
            Toast.makeText(context, "Música removida da biblioteca", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteAllSongs(context: Context) {
        viewModelScope.launch {
            // 1. Clear player playback
            playerManager.clearPlayback()

            // 2. Clear song references from playlists
            val playlists = allPlaylists.value
            for (playlist in playlists) {
                repository.updatePlaylist(playlist.copy(songIds = ""))
            }

            // 3. Delete imported private media files
            val songsList = rawSongs.value
            for (song in songsList) {
                try {
                    if (song.uriString.startsWith("file://")) {
                        val fileUri = Uri.parse(song.uriString)
                        fileUri.path?.let { path ->
                            val localFile = java.io.File(path)
                            if (localFile.exists()) {
                                localFile.delete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. Delete from Room DB
            repository.deleteAllSongs()
            Toast.makeText(context, "Todas as músicas foram removidas da biblioteca", Toast.LENGTH_SHORT).show()
        }
    }

    // Playlist CRUD
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val playlist = PlaylistEntity(name = name)
            repository.insertPlaylist(playlist)
        }
    }

    fun renamePlaylist(playlist: PlaylistEntity, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) return@launch
            val updated = playlist.copy(name = newName)
            repository.updatePlaylist(updated)
            // Update details view if active
            if (selectedPlaylist.value?.id == playlist.id) {
                _selectedPlaylist.value = updated
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            if (selectedPlaylist.value?.id == playlist.id) {
                _selectedPlaylist.value = null
            }
        }
    }

    fun addSongToPlaylist(song: SongEntity, playlist: PlaylistEntity) {
        viewModelScope.launch {
            val ids = playlist.songIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableList()
            if (!ids.contains(song.id)) {
                ids.add(song.id)
                val updatedIds = ids.joinToString(",")
                val updatedPlaylist = playlist.copy(songIds = updatedIds)
                repository.updatePlaylist(updatedPlaylist)
                if (selectedPlaylist.value?.id == playlist.id) {
                    _selectedPlaylist.value = updatedPlaylist
                }
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, song: SongEntity) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val newPlaylist = PlaylistEntity(name = name)
            val id = repository.insertPlaylist(newPlaylist).toInt()
            val playlistWithId = newPlaylist.copy(id = id)
            addSongToPlaylist(song, playlistWithId)
        }
    }

    fun removeSongFromPlaylist(songId: Int, playlist: PlaylistEntity) {
        viewModelScope.launch {
            val ids = playlist.songIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
            val updatedIds = ids.filter { it != songId }.joinToString(",")
            val updatedPlaylist = playlist.copy(songIds = updatedIds)
            repository.updatePlaylist(updatedPlaylist)
            if (selectedPlaylist.value?.id == playlist.id) {
                _selectedPlaylist.value = updatedPlaylist
            }
        }
    }

    fun reorderPlaylistSongs(playlist: PlaylistEntity, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val ids = playlist.songIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableList()
            if (fromIndex in ids.indices && toIndex in ids.indices) {
                val id = ids.removeAt(fromIndex)
                ids.add(toIndex, id)
                val updatedPlaylist = playlist.copy(songIds = ids.joinToString(","))
                repository.updatePlaylist(updatedPlaylist)
                if (selectedPlaylist.value?.id == playlist.id) {
                    _selectedPlaylist.value = updatedPlaylist
                }
            }
        }
    }
}

// ViewModel Factory
class MusicViewModelFactory(
    private val repository: MusicRepository,
    private val settings: SettingsManager,
    private val playerManager: MusicPlayerManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(repository, settings, playerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
