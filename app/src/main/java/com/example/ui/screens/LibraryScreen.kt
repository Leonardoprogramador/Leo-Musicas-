package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SarcasmManager
import com.example.data.SettingsManager
import androidx.compose.ui.draw.scale
import com.example.data.database.PlaylistEntity
import com.example.data.database.SongEntity
import com.example.ui.MusicViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TextInputDialog
import com.example.util.MetadataHelper
import com.example.util.SarcasmTts
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val songsList by viewModel.songs.collectAsState()
    val allSongsCount by viewModel.rawSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterArtist by viewModel.filterArtist.collectAsState()
    val artistsList by viewModel.artistsList.collectAsState()
    val playlistsList by viewModel.allPlaylists.collectAsState()
    val sarcasmLevel by viewModel.sarcasmLevel.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val shuffle by viewModel.playerManager.shuffle.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<SongEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<SongEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistInDialog by remember { mutableStateOf(false) }
    var isSongsListExpanded by remember { mutableStateOf(true) }

    val sarcasmTts = remember { SarcasmTts(context) }
    DisposableEffect(Unit) {
        onDispose {
            sarcasmTts.shutdown()
        }
    }

    var activePhrase by remember { mutableStateOf("") }

    LaunchedEffect(sarcasmLevel) {
        activePhrase = SarcasmManager.getRandomPhrase(sarcasmLevel)
    }

    // Official System File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importSongs(context, uris)
            }
        }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // --- 1. APP HEADER & EASILY ACCESSIBLE IMPORT BUTTON ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = SarcasmManager.getLibraryTitle(sarcasmLevel),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = SarcasmManager.getLibrarySubtitle(sarcasmLevel, allSongsCount.size),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Highly visible button to import files from device storage easily
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = SarcasmManager.getImportButtonText(sarcasmLevel),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = SarcasmManager.getImportButtonText(sarcasmLevel),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- NEW: HORIZONTAL PASTAS & ORDEM (SEM MENUS) ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = SarcasmManager.getFoldersAndOrderTitle(sarcasmLevel),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(end = 16.dp, bottom = 4.dp)
                ) {
                    // 1. Shuffle/Ordem Aleatória Toggle Button (In text, horizontal!)
                    item {
                        FilterChip(
                            selected = shuffle,
                            onClick = { viewModel.playerManager.setShuffle(!shuffle) },
                            label = {
                                Text(
                                    text = if (shuffle) "Ordem Aleatória: Ativada 🔀" else "Ordem Aleatória: Desativada ➡️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    // 2. Playlists / Folders items ("quando clicar abre numa outra pasta")
                    if (playlistsList.isNotEmpty()) {
                        items(playlistsList) { playlist ->
                            val trackCount = playlist.songIds.split(",").filter { it.isNotBlank() }.size
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.selectPlaylist(playlist)
                                    viewModel.setTab(MusicViewModel.ActiveTab.PLAYLISTS)
                                    Toast.makeText(context, "Abrindo pasta: ${playlist.name} 📂", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Text(
                                        text = "📁 ${playlist.name} ($trackCount)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    } else {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.setTab(MusicViewModel.ActiveTab.PLAYLISTS)
                                },
                                label = {
                                    Text(
                                        text = SarcasmManager.getPlaylistCreateLabel(sarcasmLevel),
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    // Divider
                    item {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    // 3. Sort options ("Organizar por" direct text tags, sem menu!)
                    MusicViewModel.SortOption.values().forEach { option ->
                        val isSelected = sortOption == option
                        item {
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSortOption(option) },
                                label = {
                                    Text(text = option.label, fontSize = 12.sp)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- 2. SARCASM LEVEL SELECTOR & COMMENTS ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = when (sarcasmLevel) {
                        SarcasmManager.SarcasmLevel.SEM -> MaterialTheme.colorScheme.outlineVariant
                        SarcasmManager.SarcasmLevel.MODERADO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        SarcasmManager.SarcasmLevel.MUITO -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Humor & Sarcasmo ⚙️",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (sarcasmLevel) {
                                SarcasmManager.SarcasmLevel.SEM -> MaterialTheme.colorScheme.onSurfaceVariant
                                SarcasmManager.SarcasmLevel.MODERADO -> MaterialTheme.colorScheme.primary
                                SarcasmManager.SarcasmLevel.MUITO -> MaterialTheme.colorScheme.error
                            }
                        )
                        
                        val levelBadge = when (sarcasmLevel) {
                            SarcasmManager.SarcasmLevel.SEM -> "Paz e Amor 😐"
                            SarcasmManager.SarcasmLevel.MODERADO -> "Ironia Leve 😏"
                            SarcasmManager.SarcasmLevel.MUITO -> "Ácido Crítico 😈"
                        }
                        Text(
                            text = levelBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SarcasmManager.SarcasmLevel.values().forEach { level ->
                            val isSelected = sarcasmLevel == level
                            val label = when (level) {
                                SarcasmManager.SarcasmLevel.SEM -> "Sem Sarcasmo 😐"
                                SarcasmManager.SarcasmLevel.MODERADO -> "Irônico 😏"
                                SarcasmManager.SarcasmLevel.MUITO -> "Ácido Total 😈"
                            }
                            
                            val containerColor = if (isSelected) {
                                when (level) {
                                    SarcasmManager.SarcasmLevel.SEM -> MaterialTheme.colorScheme.secondary
                                    SarcasmManager.SarcasmLevel.MODERADO -> MaterialTheme.colorScheme.primary
                                    SarcasmManager.SarcasmLevel.MUITO -> MaterialTheme.colorScheme.error
                                }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            
                            val contentColor = if (isSelected) {
                                when (level) {
                                    SarcasmManager.SarcasmLevel.SEM -> MaterialTheme.colorScheme.onSecondary
                                    SarcasmManager.SarcasmLevel.MODERADO -> MaterialTheme.colorScheme.onPrimary
                                    SarcasmManager.SarcasmLevel.MUITO -> MaterialTheme.colorScheme.onError
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Button(
                                onClick = { viewModel.updateSarcasmLevel(level) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = containerColor,
                                    contentColor = contentColor
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Speech Bubble / Sarcasm text container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = SarcasmManager.getAssistantCommentLabel(sarcasmLevel),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Text(
                                text = "“$activePhrase”",
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // BUTTON 1: Hear Voice (TTS)
                                FilledTonalButton(
                                    onClick = { sarcasmTts.speak(activePhrase) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Ouvir Voz",
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ouvir Voz 🗣️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                // BUTTON 2: Get another phrase (Randomize)
                                FilledTonalButton(
                                    onClick = { activePhrase = SarcasmManager.getRandomPhrase(sarcasmLevel) },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Gerar outra frase",
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Novo Deboche 🎲", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. FILTERING & SEARCH CONTROLS (ONLY SHOW IF SONGS EXIST) ---
        if (allSongsCount.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSongsListExpanded = !isSongsListExpanded }
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSongsListExpanded) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = SarcasmManager.getSongsListExpandedLabel(sarcasmLevel, isSongsListExpanded),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isSongsListExpanded) "Toque para esconder a lista de músicas" else "Toque para revelar as músicas e vídeos importados",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isSongsListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isSongsListExpanded) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Pesquise alguma faixa repetitiva...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Desistir da busca")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SarcasmManager.getArtistFilterLabel(sarcasmLevel, filterArtist),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )

                        Text(
                            text = "Ordem: ${sortOption.label}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (artistsList.isNotEmpty()) {
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = filterArtist == null,
                                    onClick = { viewModel.setFilterArtist(null) },
                                    label = { Text("Todos os culpados") }
                                )
                            }
                            items(artistsList) { artist ->
                                FilterChip(
                                    selected = filterArtist == artist,
                                    onClick = { viewModel.setFilterArtist(artist) },
                                    label = { Text(artist) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. SONGS LIST OR EMPTY STATE CORES ---
        if (allSongsCount.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = "Silêncio constrangedor por aqui...",
                        subtitle = "Sua biblioteca de músicas está tão vazia quanto o vácuo espacial. Por que não importa alguns arquivos de som para eu poder julgar seu péssimo gosto?",
                        buttonText = "Alimentar o algoritmo (Importar)",
                        onButtonClick = { filePickerLauncher.launch("*/*") }
                    )
                }
            }
        } else {
            if (isSongsListExpanded) {
                if (songsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = SarcasmManager.getEmptySearchLabel(sarcasmLevel),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${songsList.size} música(s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            TextButton(
                                onClick = { showDeleteAllDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Excluir todas as músicas",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Excluir Todas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    itemsIndexed(songsList, key = { _, song -> song.id }) { index, song ->
                        SongRowItem(
                            song = song,
                            onPlayClick = {
                                viewModel.playerManager.setPlaylistAndPlay(
                                    songsList,
                                    index,
                                    playImmediately = true,
                                    source = com.example.data.MusicPlayerManager.PlaybackSource.ALL_SONGS
                                )
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
        }

        // --- 5. BOTTOM SETTINGS PANEL (FLOWS NATURALLY IN SCROLL!) ---
        item {
            BottomSettingsPanel(
                viewModel = viewModel,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }

    // Confirmation dialogs
    songToDelete?.let { song ->
        ConfirmationDialog(
            title = "Excluir música?",
            message = "Tem certeza que deseja remover '${song.title}' da biblioteca do aplicativo? O arquivo original no celular não será apagado.",
            confirmText = "Remover",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSong(context, song)
                songToDelete = null
            },
            onDismiss = { songToDelete = null }
        )
    }

    if (showDeleteAllDialog) {
        ConfirmationDialog(
            title = "Excluir todas as músicas?",
            message = "Tem certeza que deseja excluir todas as ${allSongsCount.size} músicas da biblioteca do aplicativo? Essa ação não pode ser desfeita.",
            confirmText = "Excluir Todas",
            dismissText = "Cancelar",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAllSongs(context)
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false }
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
                            text = SarcasmManager.getEmptyPlaylistLabel(sarcasmLevel),
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
                            items(playlistsList) { playlist ->
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

@Composable
fun SongRowItem(
    song: SongEntity,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreOptionsClick: (String) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dynamic Cover Artwork
            SongCoverArt(
                uriString = song.uriString,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            // Duration display
            Text(
                text = formatDuration(song.durationMs),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Favorite Icon Button directly horizontal!
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Fingir que ama",
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Add to Playlist Button directly horizontal!
            IconButton(
                onClick = { onMoreOptionsClick("ADD_PLAYLIST") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Jogar em alguma playlist",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Button directly horizontal!
            IconButton(
                onClick = { onMoreOptionsClick("REMOVE") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Expulsar do aplicativo",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SongCoverArt(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uriString) {
        withContext(Dispatchers.IO) {
            bitmap = MetadataHelper.getArtwork(context, uriString)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Capa do Álbum",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Fallback gorgeous musical icon with colorful background
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

@Composable
fun BottomSettingsPanel(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appTheme by viewModel.appTheme.collectAsState()
    val rawSongs by viewModel.rawSongs.collectAsState()
    val playlistsList by viewModel.allPlaylists.collectAsState()
    val favoritesCount = remember(rawSongs) { rawSongs.count { it.isFavorite } }
    var showResetConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 80.dp), // make sure it clears the miniplayer beautifully
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Configurações Gerais (Escondidas)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Audio stats / general stats
                Text(
                    text = "${rawSongs.size} 🎵 | ${playlistsList.size} 📁 | $favoritesCount ❤️",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                // Theme Toggle Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextTheme = if (appTheme == SettingsManager.AppTheme.ESCURO) {
                                SettingsManager.AppTheme.CLARO
                            } else {
                                SettingsManager.AppTheme.ESCURO
                            }
                            viewModel.updateTheme(nextTheme)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Escuro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (appTheme == SettingsManager.AppTheme.ESCURO) "Ativo (Fugindo da luz)" else "Inativo (Fritando a retina)", 
                                fontSize = 10.sp, 
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = appTheme == SettingsManager.AppTheme.ESCURO,
                            onCheckedChange = { isDark ->
                                val nextTheme = if (isDark) SettingsManager.AppTheme.ESCURO else SettingsManager.AppTheme.CLARO
                                viewModel.updateTheme(nextTheme)
                            },
                            modifier = Modifier.scale(0.8f) // Keep it beautifully compact!
                        )
                    }
                }
            }
        }
    }
}
