package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SarcasmManager
import com.example.data.database.PlaylistEntity
import com.example.data.database.SongEntity
import com.example.ui.MusicViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TextInputDialog

@Composable
fun FavoritesScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawSongs by viewModel.rawSongs.collectAsState()
    val playlistsList by viewModel.allPlaylists.collectAsState()
    val sarcasmLevel by viewModel.sarcasmLevel.collectAsState()
    
    // Player State integration for the custom controller
    val currentSong by viewModel.playerManager.currentSong.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val isShuffle by viewModel.playerManager.shuffle.collectAsState()

    val favoriteSongs = remember(rawSongs) {
        rawSongs.filter { it.isFavorite }
    }

    var songToAddToPlaylist by remember { mutableStateOf<SongEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistInDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<SongEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // extra padding to clear player bar
    ) {
        // --- HEADER ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = SarcasmManager.getFavoritesTitle(sarcasmLevel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = SarcasmManager.getFavoritesSubtitle(sarcasmLevel, favoriteSongs.size),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- PREMIUM FAVORITES CONTROLLER HUB ---
        if (favoriteSongs.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top Row: Playing Status Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPlaying && favoriteSongs.any { it.id == currentSong?.id }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                val activeSongInFavs = favoriteSongs.find { it.id == currentSong?.id }
                                if (activeSongInFavs != null) {
                                    Text(
                                        text = activeSongInFavs.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = activeSongInFavs.artist,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                } else {
                                    Text(
                                        text = SarcasmManager.getPlayerDefaultTitle(sarcasmLevel),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = SarcasmManager.getPlayerDefaultArtist(sarcasmLevel),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Bottom Row: Dynamic playback controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle toggler
                            IconButton(
                                onClick = {
                                    viewModel.playerManager.setShuffle(!isShuffle)
                                    Toast.makeText(
                                        context, 
                                        if (!isShuffle) "Modo Aleatório Ativado! 🔀" else "Modo Aleatório Desativado! ➡️", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Aleatório",
                                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Prev button
                            IconButton(
                                onClick = {
                                    viewModel.playerManager.prev()
                                },
                                enabled = currentSong != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Anterior",
                                    tint = if (currentSong != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Play / Pause Circle
                            IconButton(
                                onClick = {
                                    val active = currentSong
                                    if (active == null || !favoriteSongs.any { it.id == active.id }) {
                                        // Start playing favorites list from beginning
                                        viewModel.playerManager.setPlaylistAndPlay(
                                            favoriteSongs,
                                            0,
                                            playImmediately = true,
                                            source = com.example.data.MusicPlayerManager.PlaybackSource.FAVORITES
                                        )
                                        viewModel.playerManager.setRepeatMode(com.example.data.SettingsManager.RepeatMode.REPETIR_PLAYLIST)
                                    } else {
                                        viewModel.playerManager.togglePlayPause()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            ) {
                                val isPlayingFav = isPlaying && favoriteSongs.any { it.id == currentSong?.id }
                                Icon(
                                    imageVector = if (isPlayingFav) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Tocar ou Pausar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Next button
                            IconButton(
                                onClick = {
                                    viewModel.playerManager.next()
                                },
                                enabled = currentSong != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Próxima",
                                    tint = if (currentSong != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Sequential play button
                            Button(
                                onClick = {
                                    viewModel.playerManager.setShuffle(false)
                                    viewModel.playerManager.setPlaylistAndPlay(
                                        favoriteSongs,
                                        0,
                                        playImmediately = true,
                                        source = com.example.data.MusicPlayerManager.PlaybackSource.FAVORITES
                                    )
                                    viewModel.playerManager.setRepeatMode(com.example.data.SettingsManager.RepeatMode.REPETIR_PLAYLIST)
                                    Toast.makeText(context, "Tocando Favoritas Sequencialmente! 🎧", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tudo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- LIST OF FAVORITE SONGS ---
        if (favoriteSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = "Pasta Vazia de Favoritos",
                        subtitle = "Sabe o ícone de ❤️ do lado das músicas na aba de Músicas? Clique nele para mandar elas pra cá!",
                        buttonText = "Ir para Músicas",
                        onButtonClick = {
                            viewModel.setTab(MusicViewModel.ActiveTab.INICIO)
                        }
                    )
                }
            }
        } else {
            itemsIndexed(favoriteSongs, key = { _, song -> song.id }) { index, song ->
                SongRowItem(
                    song = song,
                    onPlayClick = {
                        viewModel.playerManager.setPlaylistAndPlay(
                            favoriteSongs,
                            index,
                            playImmediately = true,
                            source = com.example.data.MusicPlayerManager.PlaybackSource.FAVORITES
                        )
                        viewModel.playerManager.setRepeatMode(com.example.data.SettingsManager.RepeatMode.REPETIR_PLAYLIST)
                        Toast.makeText(context, "Tocando: ${song.title} 🎧 (Apenas Favoritos)", Toast.LENGTH_SHORT).show()
                    },
                    onFavoriteClick = {
                        viewModel.toggleFavorite(song)
                    },
                    onMoreOptionsClick = { option ->
                        when (option) {
                            "ADD_PLAYLIST" -> {
                                songToAddToPlaylist = song
                                showAddToPlaylistDialog = true
                            }
                            "REMOVE" -> {
                                songToDelete = song
                            }
                        }
                    }
                )
            }
        }
    }

    // --- DIALOGS ---
    songToDelete?.let { song ->
        ConfirmationDialog(
            title = "Excluir música?",
            message = "Tem certeza que deseja remover '${song.title}' da biblioteca do aplicativo? O arquivo original no celular não será apagado.",
            confirmText = "Remover",
            onConfirm = {
                viewModel.deleteSong(context, song)
                songToDelete = null
            },
            onDismiss = { songToDelete = null }
        )
    }

    if (showAddToPlaylistDialog && songToAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Adicionar à Playlist", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Option to create playlist directly
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCreatePlaylistInDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Criar Nova Playlist...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    if (playlistsList.isEmpty()) {
                        Text(
                            text = "Nenhuma playlist criada ainda. Crie uma acima para organizar seu barulho!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            itemsIndexed(playlistsList) { _, playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addSongToPlaylist(songToAddToPlaylist!!, playlist)
                                            showAddToPlaylistDialog = false
                                            Toast.makeText(context, "Música adicionada à playlist '${playlist.name}'", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(playlist.name, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showCreatePlaylistInDialog && songToAddToPlaylist != null) {
        TextInputDialog(
            title = "Nova Playlist",
            placeholder = "Ex: Trabalho, Musculação, Choro...",
            confirmText = "Criar e Adicionar",
            onConfirm = { name ->
                viewModel.createPlaylistAndAddSong(name, songToAddToPlaylist!!)
                showCreatePlaylistInDialog = false
                showAddToPlaylistDialog = false
                Toast.makeText(context, "Música adicionada na nova playlist '$name'", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCreatePlaylistInDialog = false }
        )
    }
}
