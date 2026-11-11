package com.example.playback

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import com.example.MainActivity
import com.example.data.MusicPlayerManager
import com.example.data.database.SongEntity
import com.example.util.MetadataHelper
import kotlinx.coroutines.*
import java.io.File

class MusicService : Service() {

    companion object {
        const val ACTION_START = "com.example.playback.START"
        const val ACTION_UPDATE = "com.example.playback.UPDATE"
        const val ACTION_PLAY = "com.example.playback.PLAY"
        const val ACTION_PAUSE = "com.example.playback.PAUSE"
        const val ACTION_NEXT = "com.example.playback.NEXT"
        const val ACTION_PREV = "com.example.playback.PREV"
        const val ACTION_STOP = "com.example.playback.STOP"

        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 404
    }

    private var mediaSession: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var playerManager: MusicPlayerManager
    private var updateJob: Job? = null

    // Listen to headphone unplugging (Becomes noisy)
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                playerManager.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = MusicPlayerManager.getInstance(this)

        // Register noisy receiver
        val filter = IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)

        // Create notification channel
        createNotificationChannel()

        // Setup MediaSession
        setupMediaSession()

        // Observe player changes to update metadata and notifications
        observePlayerState()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Controle de Reprodução",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra os controles do reprodutor de música na barra de notificações."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "ReprodutorDeMusica").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    playerManager.play()
                }

                override fun onPause() {
                    playerManager.pause()
                }

                override fun onSkipToNext() {
                    playerManager.next()
                }

                override fun onSkipToPrevious() {
                    playerManager.prev()
                }

                override fun onSeekTo(pos: Long) {
                    playerManager.seekTo(pos.toInt())
                }
            })
            isActive = true
        }
    }

    private fun observePlayerState() {
        serviceScope.launch {
            playerManager.currentSong.collect { song ->
                updateSessionAndNotification()
            }
        }
        serviceScope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                updateSessionAndNotification()
            }
        }
    }

    private fun updateSessionAndNotification() {
        val song = playerManager.currentSong.value ?: return
        val isPlaying = playerManager.isPlaying.value

        updateJob?.cancel()
        updateJob = serviceScope.launch {
            val artBitmap = withContext(Dispatchers.IO) {
                MetadataHelper.getArtwork(applicationContext, song.uriString)
            }
            updateMediaSessionState(song, isPlaying, artBitmap)
            showNotification(song, isPlaying, artBitmap)
        }
    }

    private fun updateMediaSessionState(song: SongEntity, isPlaying: Boolean, artwork: Bitmap?) {
        mediaSession?.let { session ->
            // Update metadata
            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, song.durationMs)

            if (artwork != null) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
            }
            session.setMetadata(metadataBuilder.build())

            // Update playback state
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val stateBuilder = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE or
                            PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackState.ACTION_SEEK_TO or
                            PlaybackState.ACTION_STOP
                )
                .setState(state, playerManager.currentPosition.value.toLong(), 1.0f)
            session.setPlaybackState(stateBuilder.build())
        }
    }

    private fun showNotification(song: SongEntity, isPlaying: Boolean, artwork: Bitmap?) {
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Action PendingIntents
        val playIntent = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).setAction(ACTION_PLAY), pendingFlags)
        val pauseIntent = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).setAction(ACTION_PAUSE), pendingFlags)
        val nextIntent = PendingIntent.getService(this, 3, Intent(this, MusicService::class.java).setAction(ACTION_NEXT), pendingFlags)
        val prevIntent = PendingIntent.getService(this, 4, Intent(this, MusicService::class.java).setAction(ACTION_PREV), pendingFlags)
        val stopIntent = PendingIntent.getService(this, 5, Intent(this, MusicService::class.java).setAction(ACTION_STOP), pendingFlags)

        // Open App Intent
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            pendingFlags
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val playPauseAction = if (isPlaying) {
            Notification.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pausar",
                pauseIntent
            ).build()
        } else {
            Notification.Action.Builder(
                android.R.drawable.ic_media_play,
                "Tocar",
                playIntent
            ).build()
        }

        val sessionToken = mediaSession?.sessionToken

        val notification = builder
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setContentIntent(openAppIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .apply {
                if (artwork != null) {
                    setLargeIcon(artwork)
                }
            }
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_previous, "Anterior", prevIntent).build())
            .addAction(playPauseAction)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_next, "Próxima", nextIntent).build())
            .build()

        if (isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY -> playerManager.play()
            ACTION_PAUSE -> playerManager.pause()
            ACTION_NEXT -> playerManager.next()
            ACTION_PREV -> playerManager.prev()
            ACTION_STOP -> {
                playerManager.pause()
                stopSelf()
            }
            ACTION_START, ACTION_UPDATE -> {
                updateSessionAndNotification()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {
            // ignore
        }
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
