package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.data.database.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileNotFoundException

class MusicPlayerManager private constructor(private val context: Context) {

    enum class TimerOption(val label: String, val minutes: Int) {
        MIN_15("15 minutos", 15),
        MIN_30("30 minutos", 30),
        MIN_45("45 minutos", 45),
        MIN_60("60 minutos", 60),
        MIN_90("90 minutos", 90),
        FIM_DA_MUSICA("Fim da música atual", 0)
    }

    enum class PlaybackSource {
        ALL_SONGS, PLAYLIST, FAVORITES
    }

    private val settings = SettingsManager(context)
    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var timerJob: Job? = null

    // State flows for UI observations
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _shuffle = MutableStateFlow(settings.isShuffle)
    val shuffle: StateFlow<Boolean> = _shuffle

    private val _repeatMode = MutableStateFlow(settings.repeatMode)
    val repeatMode: StateFlow<SettingsManager.RepeatMode> = _repeatMode

    private val _volume = MutableStateFlow(settings.volume)
    val volume: StateFlow<Float> = _volume

    private val _playbackQueue = MutableStateFlow<List<SongEntity>>(emptyList())
    val playbackQueue: StateFlow<List<SongEntity>> = _playbackQueue

    private var originalQueue: List<SongEntity> = emptyList()
    private var currentQueueIndex = -1

    // Timer status
    private val _activeTimerOption = MutableStateFlow<TimerOption?>(null)
    val activeTimerOption: StateFlow<TimerOption?> = _activeTimerOption

    private val _timerRemainingSeconds = MutableStateFlow<Int?>(null)
    val timerRemainingSeconds: StateFlow<Int?> = _timerRemainingSeconds

    // Error communication
    private val _errorFlow = MutableSharedFlow<String>(replay = 0)
    val errorFlow: SharedFlow<String> = _errorFlow

    init {
        initMediaPlayer()
        // Start tracking position
        startProgressTracking()
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    handleSongCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    // Recover from media errors
                    _isPlaying.value = false
                    stopProgressTracking()
                    true
                }
            }
            // Restore previous volume
            val savedVolume = _volume.value
            mediaPlayer?.setVolume(savedVolume, savedVolume)
        }
    }

    private fun setDataSourceRobust(player: MediaPlayer, uriString: String) {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                val file = java.io.File(path)
                if (file.exists()) {
                    val fis = java.io.FileInputStream(file)
                    try {
                        player.setDataSource(fis.fd)
                        return
                    } finally {
                        try { fis.close() } catch (e: Exception) {}
                    }
                }
            }
        } else if (uri.scheme == "content") {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    player.setDataSource(pfd.fileDescriptor)
                    return
                }
            } catch (e: Exception) {
                // Fallback below
            }
        }
        player.setDataSource(context, uri)
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPosition.value = mp.currentPosition
                        _duration.value = mp.duration
                        // Auto-save position to settings
                        settings.lastPosition = mp.currentPosition
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun setPlaylistAndPlay(
        songs: List<SongEntity>,
        startIndex: Int,
        playImmediately: Boolean = true,
        source: PlaybackSource = PlaybackSource.ALL_SONGS,
        playlistId: Int = -1
    ) {
        if (songs.isEmpty()) return

        // Persist playback mode and playlist ID for seamless app restart
        settings.lastPlaybackMode = source.name
        settings.lastPlaybackPlaylistId = playlistId
        
        val targetSong = if (startIndex in songs.indices) songs[startIndex] else null
        val finalSongs = if (source == PlaybackSource.FAVORITES || (targetSong != null && targetSong.isFavorite && songs.all { it.isFavorite })) {
            songs.filter { it.isFavorite }
        } else {
            songs
        }
        
        val finalIndex = finalSongs.indexOf(targetSong).let { if (it != -1) it else 0 }
        
        originalQueue = finalSongs
        currentQueueIndex = if (finalIndex in finalSongs.indices) finalIndex else 0

        if (_shuffle.value) {
            val currentSongItem = finalSongs[currentQueueIndex]
            val shuffledList = finalSongs.toMutableList().apply {
                removeAt(currentQueueIndex)
                shuffle()
                add(0, currentSongItem)
            }
            _playbackQueue.value = shuffledList
            currentQueueIndex = 0
        } else {
            _playbackQueue.value = finalSongs
        }

        playActiveIndex(playImmediately)
    }

    private fun playActiveIndex(playImmediately: Boolean) {
        val queue = _playbackQueue.value
        if (currentQueueIndex !in queue.indices) return

        val song = queue[currentQueueIndex]
        _currentSong.value = song
        settings.lastSongId = song.id

        try {
            initMediaPlayer()
            mediaPlayer?.apply {
                reset()
                setDataSourceRobust(this, song.uriString)
                setVolume(_volume.value, _volume.value)
                prepare()
                _duration.value = duration
                _currentPosition.value = 0
                settings.lastPosition = 0
                if (playImmediately) {
                    start()
                    _isPlaying.value = true
                    // Notify Service
                    MusicServiceHelper.startService(context)
                } else {
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle deleted file
            coroutineScope.launch {
                _errorFlow.emit("O arquivo '${song.title}' não está mais disponível no celular.")
            }
            // Toast fallback on UI
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "O arquivo não está mais disponível no aparelho.", Toast.LENGTH_LONG).show()
            }
            // Try to play next
            if (queue.size > 1) {
                next()
            } else {
                _isPlaying.value = false
                _currentSong.value = null
            }
        }
    }

    fun updateCurrentSongEntity(updatedSong: SongEntity) {
        if (_currentSong.value?.id == updatedSong.id) {
            _currentSong.value = updatedSong
        }
    }

    fun updateSongInQueue(updatedSong: SongEntity) {
        val updatedOriginal = originalQueue.map { 
            if (it.id == updatedSong.id) updatedSong else it 
        }
        
        val isCurrentlyPlayingFavoritesOnly = originalQueue.all { it.isFavorite } && originalQueue.isNotEmpty()
        
        val finalOriginal = if (isCurrentlyPlayingFavoritesOnly && !updatedSong.isFavorite) {
            updatedOriginal.filter { it.id != updatedSong.id }
        } else {
            updatedOriginal
        }
        
        originalQueue = finalOriginal
        
        val updatedPlayback = _playbackQueue.value.map {
            if (it.id == updatedSong.id) updatedSong else it
        }
        val finalPlayback = if (isCurrentlyPlayingFavoritesOnly && !updatedSong.isFavorite) {
            updatedPlayback.filter { it.id != updatedSong.id }
        } else {
            updatedPlayback
        }
        _playbackQueue.value = finalPlayback
        
        if (isCurrentlyPlayingFavoritesOnly && !updatedSong.isFavorite && _currentSong.value?.id == updatedSong.id) {
            if (finalPlayback.isNotEmpty()) {
                next()
            } else {
                pause()
                _currentSong.value = null
            }
        }
    }

    /**
     * Safely removes a deleted song from the playback queues and handles advancing
     * to the next song or stopping playback if the currently playing song was deleted.
     */
    fun removeSongFromPlayback(deletedSongId: Int) {
        val isDeletingCurrent = _currentSong.value?.id == deletedSongId
        val wasPlaying = _isPlaying.value

        originalQueue = originalQueue.filter { it.id != deletedSongId }

        val currentQueue = _playbackQueue.value.toMutableList()
        val removeIndex = currentQueue.indexOfFirst { it.id == deletedSongId }
        if (removeIndex != -1) {
            currentQueue.removeAt(removeIndex)
            _playbackQueue.value = currentQueue
        }

        if (isDeletingCurrent) {
            if (currentQueue.isNotEmpty()) {
                // If deleted song was at the end of the queue, wrap around to 0
                val nextIndex = if (removeIndex in currentQueue.indices) removeIndex else 0
                currentQueueIndex = nextIndex
                playActiveIndex(wasPlaying)
            } else {
                // All songs deleted from queue
                clearPlayback()
            }
        } else {
            val curSong = _currentSong.value
            if (curSong != null) {
                currentQueueIndex = currentQueue.indexOfFirst { it.id == curSong.id }
            }
        }
    }

    fun play() {
        val song = _currentSong.value ?: return
        try {
            initMediaPlayer()
            val mp = mediaPlayer ?: return
            if (!mp.isPlaying) {
                try {
                    mp.start()
                } catch (e: Exception) {
                    mp.reset()
                    setDataSourceRobust(mp, song.uriString)
                    mp.setVolume(_volume.value, _volume.value)
                    mp.prepare()
                    if (_currentPosition.value > 0) {
                        mp.seekTo(_currentPosition.value)
                    }
                    mp.start()
                }
                _isPlaying.value = true
                MusicServiceHelper.startService(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    _isPlaying.value = false
                    settings.lastPosition = mp.currentPosition
                    MusicServiceHelper.updateServiceState(context)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun next() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return

        currentQueueIndex++
        if (currentQueueIndex >= queue.size) {
            if (_repeatMode.value == SettingsManager.RepeatMode.REPETIR_PLAYLIST) {
                currentQueueIndex = 0
            } else {
                // Stop playing
                currentQueueIndex = queue.size - 1
                pause()
                seekTo(0)
                return
            }
        }
        playActiveIndex(true)
    }

    fun prev() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return

        // If played more than 3 seconds, restart song
        if (_currentPosition.value > 3000) {
            seekTo(0)
            return
        }

        currentQueueIndex--
        if (currentQueueIndex < 0) {
            if (_repeatMode.value == SettingsManager.RepeatMode.REPETIR_PLAYLIST) {
                currentQueueIndex = queue.size - 1
            } else {
                currentQueueIndex = 0
            }
        }
        playActiveIndex(true)
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { mp ->
            mp.seekTo(positionMs)
            _currentPosition.value = positionMs
            settings.lastPosition = positionMs
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        settings.volume = clamped
        mediaPlayer?.setVolume(clamped, clamped)
    }

    fun setTemporaryDucking(ducked: Boolean) {
        val targetVolume = if (ducked) {
            (_volume.value * 0.12f).coerceAtLeast(0.04f)
        } else {
            _volume.value
        }
        mediaPlayer?.setVolume(targetVolume, targetVolume)
    }

    fun setShuffle(enabled: Boolean) {
        _shuffle.value = enabled
        settings.isShuffle = enabled

        val queue = _playbackQueue.value
        val song = _currentSong.value

        if (queue.isEmpty() || song == null) return

        if (enabled) {
            // Shuffle and put current song at index 0
            val shuffled = originalQueue.toMutableList().apply {
                remove(song)
                shuffle()
                add(0, song)
            }
            _playbackQueue.value = shuffled
            currentQueueIndex = 0
        } else {
            // Restore original queue, find current index
            _playbackQueue.value = originalQueue
            currentQueueIndex = originalQueue.indexOf(song)
        }
    }

    fun setRepeatMode(mode: SettingsManager.RepeatMode) {
        _repeatMode.value = mode
        settings.repeatMode = mode
    }

    private fun handleSongCompletion() {
        // Check if sleep timer was set to "Fim da música atual"
        if (_activeTimerOption.value == TimerOption.FIM_DA_MUSICA) {
            pause()
            clearTimer()
            return
        }

        when (_repeatMode.value) {
            SettingsManager.RepeatMode.REPETIR_UMA -> {
                // Loop same song
                playActiveIndex(true)
            }
            SettingsManager.RepeatMode.REPETIR_PLAYLIST -> {
                next()
            }
            SettingsManager.RepeatMode.NENHUM -> {
                val queue = _playbackQueue.value
                if (currentQueueIndex < queue.size - 1) {
                    next()
                } else {
                    // Reached end, stop
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    settings.lastPosition = 0
                    MusicServiceHelper.updateServiceState(context)
                }
            }
        }
    }

    // Timer API
    fun setTimer(option: TimerOption?) {
        _activeTimerOption.value = option
        timerJob?.cancel()

        if (option == null) {
            _timerRemainingSeconds.value = null
            return
        }

        if (option == TimerOption.FIM_DA_MUSICA) {
            _timerRemainingSeconds.value = null // Will trigger on natural song complete
            return
        }

        val totalSeconds = option.minutes * 60
        _timerRemainingSeconds.value = totalSeconds

        timerJob = coroutineScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _timerRemainingSeconds.value = remaining
            }
            // Timer expired, stop playing music
            pause()
            clearTimer()
        }
    }

    fun clearTimer() {
        timerJob?.cancel()
        _activeTimerOption.value = null
        _timerRemainingSeconds.value = null
    }

    // Try to load last played song on launch
    fun restoreLastPlayedState(allSongs: List<SongEntity>) {
        if (allSongs.isEmpty() || _currentSong.value != null) return
        originalQueue = allSongs

        val lastId = settings.lastSongId
        var index = allSongs.indexOfFirst { it.id == lastId }
        if (index == -1) {
            index = 0
        }

        if (_shuffle.value) {
            val songItem = allSongs[index]
            val shuffled = allSongs.toMutableList().apply {
                remove(songItem)
                shuffle()
                add(0, songItem)
            }
            _playbackQueue.value = shuffled
            currentQueueIndex = 0
        } else {
            _playbackQueue.value = allSongs
            currentQueueIndex = index
        }

        val song = _playbackQueue.value[currentQueueIndex]
        _currentSong.value = song
        _duration.value = song.durationMs.toInt()

        val savedPos = settings.lastPosition
        _currentPosition.value = savedPos

        // Prepare the media player with the restored song in a paused state
        try {
            initMediaPlayer()
            mediaPlayer?.apply {
                reset()
                setDataSourceRobust(this, song.uriString)
                setVolume(_volume.value, _volume.value)
                prepare()
                seekTo(savedPos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // File might have been deleted, ignore silently at startup
        }
    }

    fun clearPlayback() {
        pause()
        stopProgressTracking()
        try {
            mediaPlayer?.reset()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _currentSong.value = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = 0
        originalQueue = emptyList()
        _playbackQueue.value = emptyList()
        currentQueueIndex = -1
        settings.lastSongId = -1
        settings.lastPosition = 0
        MusicServiceHelper.updateServiceState(context)
    }

    fun release() {
        stopProgressTracking()
        timerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        @Volatile
        private var INSTANCE: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MusicPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
