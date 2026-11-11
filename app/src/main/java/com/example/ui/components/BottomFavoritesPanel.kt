package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.SongEntity
import com.example.ui.MusicViewModel

@Composable
fun BottomFavoritesPanel(
    viewModel: MusicViewModel,
    onSwipeToHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawSongs by viewModel.rawSongs.collectAsState()
    val favoriteSongs = remember(rawSongs) { rawSongs.filter { it.isFavorite } }
    
    // Expandable state of the panel
    var isExpanded by remember { mutableStateOf(true) }
    
    // Lyrics Dialog state
    var selectedLyricsSong by remember { mutableStateOf<SongEntity?>(null) }

    // Gesture state for swipe-to-dismiss
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "favorites_panel_swipe"
    )

    if (favoriteSongs.isEmpty()) {
        return
    }

    // Main Favorites Panel (when favorites exist)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .graphicsLayer {
                translationX = animatedOffsetX
                alpha = (1f - (kotlin.math.abs(animatedOffsetX) / 400f)).coerceIn(0f, 1f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 150f) {
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
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFB0A1E14) // Deep high contrast green/black
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favoritos",
                        tint = Color(0xFF00FF66), // Vibrant Neon Green
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Favoritos do Léo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = Color(0xFF00FF66),
                        contentColor = Color.Black,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(text = favoriteSongs.size.toString(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    // horizontal list of favorites
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(favoriteSongs) { index, song ->
                            FavoriteQuickCard(
                                song = song,
                                onListenClick = {
                                    viewModel.playerManager.setPlaylistAndPlay(
                                        favoriteSongs,
                                        index,
                                        playImmediately = true,
                                        source = com.example.data.MusicPlayerManager.PlaybackSource.FAVORITES
                                    )
                                    viewModel.playerManager.setRepeatMode(com.example.data.SettingsManager.RepeatMode.REPETIR_PLAYLIST)
                                    Toast.makeText(context, "Tocando: ${song.title} 🎧 (Repetindo Favoritos)", Toast.LENGTH_SHORT).show()
                                },
                                onReadClick = {
                                    selectedLyricsSong = song
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog showing scrollable lyrics for "Só Ler"
    if (selectedLyricsSong != null) {
        val song = selectedLyricsSong!!
        var showEditLyrics by remember { mutableStateOf(false) }
        var currentLyricsText by remember { mutableStateOf(song.lyrics ?: "") }

        AlertDialog(
            onDismissRequest = { selectedLyricsSong = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = "Leitor de Letra - Léo Música 📖",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    if (showEditLyrics) {
                        OutlinedTextField(
                            value = currentLyricsText,
                            onValueChange = { currentLyricsText = it },
                            placeholder = { Text("Escreva a letra horizontal ou vertical da música...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF66),
                                unfocusedBorderColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            if (currentLyricsText.isNotBlank()) {
                                Text(
                                    text = currentLyricsText,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Nenhuma letra salva para essa música favorita.\nClique em 'Editar Letra' abaixo para inventar ou colar a letra! 🎤",
                                    fontSize = 13.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (showEditLyrics) {
                    TextButton(
                        onClick = {
                            viewModel.updateSongLyrics(song, currentLyricsText)
                            // Update the state so the main view has it
                            val updatedSong = song.copy(lyrics = currentLyricsText)
                            selectedLyricsSong = updatedSong
                            showEditLyrics = false
                            Toast.makeText(context, "Letra salva com sucesso! 🎵", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = Color(0xFF00FF66))
                    }
                } else {
                    TextButton(
                        onClick = { showEditLyrics = true }
                    ) {
                        Text("Editar Letra", color = Color(0xFF00FF66))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (showEditLyrics) {
                            showEditLyrics = false
                        } else {
                            selectedLyricsSong = null
                        }
                    }
                ) {
                    Text(text = if (showEditLyrics) "Voltar" else "Fechar", color = MaterialTheme.colorScheme.tertiary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun FavoriteQuickCard(
    song: SongEntity,
    onListenClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .height(105.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14241B) // Slightly lighter green-black
        ),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Song Title & Artist
            Column {
                Text(
                    text = song.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = song.artist,
                    fontSize = 10.sp,
                    color = Color(0xFF00FF66),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            // Row of Actions: Só Escutar & Só Ler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // "Só Escutar" Button
                Button(
                    onClick = onListenClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF66),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "Escutar", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // "Só Ler" Button
                Button(
                    onClick = onReadClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF263238),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = Color(0xFF00FF66)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "Ler", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
