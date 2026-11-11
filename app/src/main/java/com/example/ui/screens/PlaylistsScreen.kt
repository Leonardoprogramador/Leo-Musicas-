package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
fun PlaylistsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val playlistsList by viewModel.allPlaylists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val rawSongs by viewModel.rawSongs.collectAsState()
    val sarcasmLevel by viewModel.sarcasmLevel.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    BackHandler(enabled = selectedPlaylist != null) {
        viewModel.selectPlaylist(null)
    }

    AnimatedContent(
        targetState = selectedPlaylist,
        transitionSpec = {
            slideInHorizontally { width -> if (targetState != null) width else -width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> if (targetState == null) width else -width } + fadeOut()
        },
        label = "PlaylistNavigation"
    ) { activePlaylist ->
        if (activePlaylist == null) {
            // Main Playlists List Screen
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = SarcasmManager.getPlaylistsTitle(sarcasmLevel),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = SarcasmManager.getPlaylistsSubtitle(sarcasmLevel, playlistsList.size),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showCreateDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Criar nova pasta para fingir organização")
                    }
                }

                if (playlistsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            title = "Tudo desorganizado por aqui...",
                            subtitle = "Nenhuma playlist criada. Você realmente ouve tudo misturado como uma salada sonora indigesta? Crie uma pasta para fingir que é uma pessoa organizada.",
                            buttonText = "Criar pasta (Falsa organização)",
                            onButtonClick = { showCreateDialog = true }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
                    ) {
                        items(playlistsList, key = { it.id }) { playlist ->
                            PlaylistItemVerticalCard(
                                playlist = playlist,
                                sarcasmLevel = sarcasmLevel,
                                onSelect = { viewModel.selectPlaylist(playlist) }
                            )
                        }
                    }
                }
            }
        } else {
            // Playlist Details View Screen
            PlaylistDetailView(
                playlist = activePlaylist,
                rawSongs = rawSongs,
                sarcasmLevel = sarcasmLevel,
                onBackClick = { viewModel.selectPlaylist(null) },
                onRenameClick = { showRenameDialog = activePlaylist },
                onDeleteClick = { playlistToDelete = activePlaylist },
                onPlayTrack = { playlistSongs, startIndex ->
                    viewModel.playerManager.setPlaylistAndPlay(
                        playlistSongs,
                        startIndex,
                        playImmediately = true,
                        source = com.example.data.MusicPlayerManager.PlaybackSource.PLAYLIST,
                        playlistId = activePlaylist.id
                    )
                },
                onRemoveTrack = { songId ->
                    viewModel.removeSongFromPlaylist(songId, activePlaylist)
                },
                onMoveTrack = { from, to ->
                    viewModel.reorderPlaylistSongs(activePlaylist, from, to)
                },
                modifier = modifier
            )
        }
    }

    // Creating playlist dialog
    if (showCreateDialog) {
        TextInputDialog(
            title = "Fingir Organização (Nova Playlist)",
            placeholder = "Dê um nome criativo (se conseguir)",
            confirmText = "Criar pasta",
            onConfirm = { name -> viewModel.createPlaylist(name) },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Renaming playlist dialog
    showRenameDialog?.let { playlist ->
        TextInputDialog(
            title = "Mudar Nome da Pasta",
            placeholder = "Dê um nome menos pior",
            initialValue = playlist.name,
            confirmText = "Mudar",
            onConfirm = { newName -> viewModel.renamePlaylist(playlist, newName) },
            onDismiss = { showRenameDialog = null }
        )
    }

    // Confirmation delete playlist dialog
    playlistToDelete?.let { playlist ->
        ConfirmationDialog(
            title = "Desintegrar playlist?",
            message = "Tem certeza que quer apagar a playlist '${playlist.name}' da minha memória? Seus fones de ouvido e arquivos de áudio originais sobreviverão intactos.",
            confirmText = "Apagar para sempre",
            onConfirm = {
                viewModel.deletePlaylist(playlist)
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null }
        )
    }
}

@Composable
fun PlaylistItemVerticalCard(
    playlist: PlaylistEntity,
    sarcasmLevel: SarcasmManager.SarcasmLevel,
    onSelect: () -> Unit
) {
    val trackCount = playlist.songIds.split(",").filter { it.isNotBlank() }.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• " + SarcasmManager.getPlaylistSongsSubtitle(sarcasmLevel, trackCount),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir pasta",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: PlaylistEntity,
    rawSongs: List<SongEntity>,
    sarcasmLevel: SarcasmManager.SarcasmLevel,
    onBackClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayTrack: (List<SongEntity>, Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolve order and content of playlist songs based on the IDs
    val songIds = remember(playlist.songIds) {
        playlist.songIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
    }
    val playlistSongs = remember(songIds, rawSongs) {
        songIds.mapNotNull { id -> rawSongs.find { it.id == id } }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = SarcasmManager.getPlaylistSongsSubtitle(sarcasmLevel, playlistSongs.size),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            IconButton(onClick = onRenameClick) {
                Icon(Icons.Default.Edit, contentDescription = "Renomear Playlist", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir Playlist", tint = MaterialTheme.colorScheme.error)
            }
        }

        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = "Uma pasta cheia de vento...",
                    subtitle = "Esta playlist não tem absolutamente nada. Vá na aba de Músicas e jogue algo útil aqui dentro usando o menu (...)."
                )
            }
        } else {
            // Track list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                itemsIndexed(playlistSongs, key = { index, song -> "${song.id}_$index" }) { index, song ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play Trigger
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPlayTrack(playlistSongs, index) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SongCoverArt(
                                    uriString = song.uriString,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = song.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${song.artist}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Reordering control buttons
                            IconButton(
                                onClick = { onMoveTrack(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Mover para cima",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMoveTrack(index, index + 1) },
                                enabled = index < playlistSongs.size - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Mover para baixo",
                                    tint = if (index < playlistSongs.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Remove from playlist
                            IconButton(
                                onClick = { onRemoveTrack(song.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remover da playlist",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
