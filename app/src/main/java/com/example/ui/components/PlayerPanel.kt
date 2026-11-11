package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MusicPlayerManager
import com.example.data.SarcasmManager
import com.example.data.SettingsManager
import com.example.data.database.SongEntity
import com.example.ui.MusicViewModel
import com.example.ui.screens.SongCoverArt
import com.example.ui.screens.formatDuration
import kotlinx.coroutines.launch

@Composable
fun AddToPlaylistModalDialog(
    song: SongEntity,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playlistsList by viewModel.allPlaylists.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar à Playlist 📁", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreatePlaylistDialog = true }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
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
                        text = "Nenhuma playlist criada ainda. Crie uma acima para guardar essa música!",
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
                                        viewModel.addSongToPlaylist(song, playlist)
                                        Toast.makeText(context, "Música '${song.title}' adicionada à playlist '${playlist.name}'! 🎶", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.tertiary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )

    if (showCreatePlaylistDialog) {
        TextInputDialog(
            title = "Nova Playlist",
            placeholder = "Ex: Minhas Favoritas, Treino...",
            confirmText = "Criar e Adicionar",
            onConfirm = { name ->
                viewModel.createPlaylistAndAddSong(name, song)
                showCreatePlaylistDialog = false
                Toast.makeText(context, "Música adicionada na nova playlist '$name'!", Toast.LENGTH_SHORT).show()
                onDismiss()
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }
}

@Composable
fun MiniPlayer(
    playerManager: MusicPlayerManager,
    onExpandClick: () -> Unit,
    onSwipeToHide: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel? = null
) {
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val position by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()

    if (currentSong == null) return

    val progressFraction = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val context = LocalContext.current
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "miniplayer_swipe"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .graphicsLayer {
                translationX = animatedOffsetX
                alpha = (1f - (kotlin.math.abs(animatedOffsetX) / 500f)).coerceIn(0f, 1f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 200f) {
                            onSwipeToHide()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
            .clickable(onClick = onExpandClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Line Progress Indicator at the very top
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
                    .padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover Art
                SongCoverArt(
                    uriString = currentSong!!.uriString,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong!!.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = currentSong!!.artist,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Controls: Previous Song
                IconButton(
                    onClick = { playerManager.prev() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Controls: Play / Pause
                IconButton(
                    onClick = { playerManager.togglePlayPause() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Tocar/Pausar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Controls: Next Song
                IconButton(
                    onClick = { playerManager.next() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Controls: Repeat Mode (none, list, single)
                val repeatMode by playerManager.repeatMode.collectAsState()
                IconButton(
                    onClick = {
                        val nextMode = when (repeatMode) {
                            SettingsManager.RepeatMode.NENHUM -> SettingsManager.RepeatMode.REPETIR_PLAYLIST
                            SettingsManager.RepeatMode.REPETIR_PLAYLIST -> SettingsManager.RepeatMode.REPETIR_UMA
                            SettingsManager.RepeatMode.REPETIR_UMA -> SettingsManager.RepeatMode.NENHUM
                        }
                        playerManager.setRepeatMode(nextMode)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    val repeatIcon = if (repeatMode == SettingsManager.RepeatMode.REPETIR_UMA) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    val isRepeatActive = repeatMode != SettingsManager.RepeatMode.NENHUM
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repetir",
                        tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Controls: Add to Playlist
                if (viewModel != null) {
                    IconButton(
                        onClick = { showAddToPlaylistDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Adicionar a Playlist",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddToPlaylistDialog && viewModel != null && currentSong != null) {
        AddToPlaylistModalDialog(
            song = currentSong!!,
            viewModel = viewModel,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerPanel(
    viewModel: com.example.ui.MusicViewModel,
    onMinimizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerManager = viewModel.playerManager
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val position by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val shuffle by playerManager.shuffle.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val volume by playerManager.volume.collectAsState()
    val activeTimerOption by playerManager.activeTimerOption.collectAsState()
    val timerRemainingSeconds by playerManager.timerRemainingSeconds.collectAsState()
    val sarcasmLevel by viewModel.sarcasmLevel.collectAsState()

    if (currentSong == null) return

    val context = LocalContext.current
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteSongDialog by remember { mutableStateOf(false) }

    // Album Art Rotate Animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotate")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "discRotation"
    )

    // Animated seek position so moving slider is responsive
    var sliderValueOverride by remember { mutableStateOf<Float?>(null) }
    val currentSliderValue = sliderValueOverride ?: position.toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinimizeClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimizar",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = SarcasmManager.getPlayerTitleHeader(sarcasmLevel),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )

                IconButton(onClick = { showTimerDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Temporizador",
                        tint = if (activeTimerOption != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 2. Spinning Vinyl Disk Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentRotation = if (isPlaying) discRotation else 0f
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer(rotationZ = currentRotation)
                        .clip(CircleShape)
                ) {
                    SongCoverArt(
                        uriString = currentSong!!.uriString,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Metadata & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong!!.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong!!.artist,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleFavorite(currentSong!!) }) {
                        Icon(
                            imageVector = if (currentSong!!.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = if (currentSong!!.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { showAddToPlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Adicionar a Playlist",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = { showDeleteSongDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir Música da Biblioteca",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // 3.5. Horizontal Lyrics Marquee / Viewer Panel
            var showLyricsEditDialog by remember { mutableStateOf(false) }
            val currentLyrics = currentSong!!.lyrics ?: ""

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLyricsEditDialog = true }
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Letra da Música",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (currentLyrics.isNotBlank()) {
                            "Letra Horizontal: $currentLyrics"
                        } else {
                            "Nenhuma letra salva. Clique aqui para inventar uma mentira horizontal para cantar junto! 🎤"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Letra",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (showLyricsEditDialog) {
                var tempLyrics by remember { mutableStateOf(currentLyrics) }
                AlertDialog(
                    onDismissRequest = { showLyricsEditDialog = false },
                    title = {
                        Text(
                            text = "Escrever Letra Horizontal 📝",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Toda a letra ficará na horizontal (em uma única linha) para manter o visual limpo, fácil de achar e sem poluição vertical.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = tempLyrics,
                                onValueChange = {
                                    tempLyrics = it.replace("\n", " ").replace("\r", " ")
                                },
                                placeholder = { Text("Insira a letra horizontal aqui...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updateSongLyrics(currentSong!!, tempLyrics)
                                showLyricsEditDialog = false
                                Toast.makeText(context, "Letra horizontal salva com sucesso! 🎵", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLyricsEditDialog = false }) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.tertiary)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Interactive Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentSliderValue,
                    onValueChange = { sliderValueOverride = it },
                    onValueChangeFinished = {
                        sliderValueOverride?.let {
                            playerManager.seekTo(it.toInt())
                        }
                        sliderValueOverride = null
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentSliderValue.toLong()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = formatDuration(duration.toLong()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Volume Slider Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VolumeMute, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Slider(
                    value = volume,
                    onValueChange = { playerManager.setVolume(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Media Control Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = { playerManager.setShuffle(!shuffle) }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatório",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }

                // Prev Button
                IconButton(onClick = { playerManager.prev() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Large central Play/Pause circular FAB
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { playerManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Tocar/Pausar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Button
                IconButton(onClick = { playerManager.next() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(
                    onClick = {
                        val nextMode = when (repeatMode) {
                            SettingsManager.RepeatMode.NENHUM -> SettingsManager.RepeatMode.REPETIR_PLAYLIST
                            SettingsManager.RepeatMode.REPETIR_PLAYLIST -> SettingsManager.RepeatMode.REPETIR_UMA
                            SettingsManager.RepeatMode.REPETIR_UMA -> SettingsManager.RepeatMode.NENHUM
                        }
                        playerManager.setRepeatMode(nextMode)
                    }
                ) {
                    val repeatIcon = if (repeatMode == SettingsManager.RepeatMode.REPETIR_UMA) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    val isRepeatActive = repeatMode != SettingsManager.RepeatMode.NENHUM
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Modo de repetição",
                        tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Sleep Timer indicator display
            if (activeTimerOption != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val timerLabel = if (activeTimerOption == MusicPlayerManager.TimerOption.FIM_DA_MUSICA) {
                    "Ativo: Fim da música atual"
                } else {
                    val mins = (timerRemainingSeconds ?: 0) / 60
                    val secs = (timerRemainingSeconds ?: 0) % 60
                    String.format("Ativo: %02d:%02d", mins, secs)
                }
                Text(
                    text = timerLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Sleep timer configuration sheet
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = {
                Text(
                    text = SarcasmManager.getTimerDialogTitle(sarcasmLevel),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = SarcasmManager.getTimerDialogSubtitle(sarcasmLevel),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    MusicPlayerManager.TimerOption.values().forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.setTimer(option)
                                    showTimerDialog = false
                                    Toast.makeText(context, "Timer ativado: ${option.label}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeTimerOption == option,
                                onClick = {
                                    playerManager.setTimer(option)
                                    showTimerDialog = false
                                    Toast.makeText(context, "Timer ativado: ${option.label}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option.label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    if (activeTimerOption != null) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.clearTimer()
                                    showTimerDialog = false
                                    Toast.makeText(context, "Fica acordado aí então...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TimerOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Ficar acordado ouvindo música para sempre", fontSize = 15.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Deixa pra lá", color = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showAddToPlaylistDialog && currentSong != null) {
        AddToPlaylistModalDialog(
            song = currentSong!!,
            viewModel = viewModel,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showDeleteSongDialog && currentSong != null) {
        val songToDelete = currentSong!!
        ConfirmationDialog(
            title = "Excluir Música",
            message = "Tem certeza que deseja excluir permanentemente a música \"${songToDelete.title}\"? Ela será removida da biblioteca e das suas playlists.",
            confirmText = "Excluir Música",
            dismissText = "Cancelar",
            isDestructive = true,
            onConfirm = {
                showDeleteSongDialog = false
                viewModel.deleteSong(context, songToDelete)
                if (playerManager.playbackQueue.value.size <= 1) {
                    onMinimizeClick()
                }
            },
            onDismiss = { showDeleteSongDialog = false }
        )
    }
}
