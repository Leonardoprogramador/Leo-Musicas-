package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val songIds: String = "" // List of song IDs represented as a comma-separated string, e.g. "1,2,3"
)
