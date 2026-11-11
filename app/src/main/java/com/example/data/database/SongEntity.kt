package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lyrics: String? = null
)
