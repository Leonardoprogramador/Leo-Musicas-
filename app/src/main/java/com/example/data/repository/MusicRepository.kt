package com.example.data.repository

import com.example.data.database.PlaylistDao
import com.example.data.database.PlaylistEntity
import com.example.data.database.SongDao
import com.example.data.database.SongEntity
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun getSongById(id: Int): SongEntity? = songDao.getSongById(id)

    suspend fun getSongByUri(uriString: String): SongEntity? = songDao.getSongByUri(uriString)

    suspend fun insertSong(song: SongEntity): Long = songDao.insertSong(song)

    suspend fun updateSong(song: SongEntity) = songDao.updateSong(song)

    suspend fun deleteSong(song: SongEntity) = songDao.deleteSong(song)

    suspend fun deleteSongById(id: Int) = songDao.deleteSongById(id)

    suspend fun deleteAllSongs() = songDao.deleteAllSongs()

    suspend fun getPlaylistById(id: Int): PlaylistEntity? = playlistDao.getPlaylistById(id)

    suspend fun insertPlaylist(playlist: PlaylistEntity): Long = playlistDao.insertPlaylist(playlist)

    suspend fun updatePlaylist(playlist: PlaylistEntity) = playlistDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlist: PlaylistEntity) = playlistDao.deletePlaylist(playlist)

    suspend fun deletePlaylistById(id: Int) = playlistDao.deletePlaylistById(id)
}
