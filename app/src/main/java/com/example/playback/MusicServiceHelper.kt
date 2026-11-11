package com.example.data

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.playback.MusicService

object MusicServiceHelper {
    fun startService(context: Context) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun updateServiceState(context: Context) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_UPDATE
        }
        context.startService(intent)
    }

    fun stopService(context: Context) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_STOP
        }
        context.stopService(intent)
    }
}
