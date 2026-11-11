package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Int): SongEntity?

    @Query("SELECT * FROM songs WHERE uriString = :uriString LIMIT 1")
    suspend fun getSongByUri(uriString: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Int)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
}
