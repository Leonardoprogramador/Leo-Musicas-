package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SarcasmManager
import com.example.data.SettingsManager
import com.example.ui.MusicViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.util.SarcasmTts
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val sarcasmLevel by viewModel.sarcasmLevel.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val rawSongs by viewModel.rawSongs.collectAsState()
    val playlistsList by viewModel.allPlaylists.collectAsState()

    val favoritesCount = remember(rawSongs) { rawSongs.count { it.isFavorite } }

    var showResetConfirmation by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "Configurações & Humores ⚙️",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Group 1: Humor & Sarcasm
        SettingsGroupHeader(title = "Nível de Sarcasmo (Nossa Relação)", icon = Icons.Default.Mood)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SarcasmOptionRow(
                    title = "Sem Humor (Robô Sem Alma) 😐",
                    description = "Eu finjo que sou um aplicativo normal e sem graça para não magoar seus sentimentos.",
                    selected = sarcasmLevel == SarcasmManager.SarcasmLevel.SEM,
                    onClick = { viewModel.updateSarcasmLevel(SarcasmManager.SarcasmLevel.SEM) }
                )

                Divider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 8.dp))

                SarcasmOptionRow(
                    title = "Irônico 😏",
                    description = "Vou soltar algumas piadinhas leves sobre o seu gosto musical questionável para alegrar seu dia.",
                    selected = sarcasmLevel == SarcasmManager.SarcasmLevel.MODERADO,
                    onClick = { viewModel.updateSarcasmLevel(SarcasmManager.SarcasmLevel.MODERADO) }
                )

                Divider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 8.dp))

                SarcasmOptionRow(
                    title = "Ácido Total 😈",
                    description = "Prepare o psicológico para ser julgado a cada segundo de uso deste reprodutor.",
                    selected = sarcasmLevel == SarcasmManager.SarcasmLevel.MUITO,
                    onClick = { viewModel.updateSarcasmLevel(SarcasmManager.SarcasmLevel.MUITO) }
                )

                Divider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 12.dp))

                // Speech Bubble showing live preview of the sarcasm level
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
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
                                text = "Comentário Teste do Nível:",
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
                            // Hear Voice Button
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

                            // Regenerate comment
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
                                        contentDescription = "Trocar frase",
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

        // Group 2: Design and Appearance
        SettingsGroupHeader(title = "Aparência Visual", icon = Icons.Default.Palette)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val nextTheme = if (appTheme == SettingsManager.AppTheme.ESCURO) {
                            SettingsManager.AppTheme.CLARO
                        } else {
                            SettingsManager.AppTheme.ESCURO
                        }
                        viewModel.updateTheme(nextTheme)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (appTheme == SettingsManager.AppTheme.ESCURO) "Modo Escuro (Vampiro)" else "Modo Claro (Cega-Olho)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Alternar entre economizar sua bateria ou fritar sua retina sob a luz do sol.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Switch(
                    checked = appTheme == SettingsManager.AppTheme.ESCURO,
                    onCheckedChange = { isDark ->
                        val nextTheme = if (isDark) SettingsManager.AppTheme.ESCURO else SettingsManager.AppTheme.CLARO
                        viewModel.updateTheme(nextTheme)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        // Group 3: Library Info & Management
        SettingsGroupHeader(title = "Estatísticas de Poluição Sonora", icon = Icons.Default.LibraryMusic)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Info Songs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Músicas entulhadas:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${rawSongs.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info Favorites
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Favoritadas (Amor platônico):", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$favoritesCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info Playlists
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tentativas de organização:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${playlistsList.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Group 4: Reset & Restore Defaults
        SettingsGroupHeader(title = "Botão de Pânico", icon = Icons.Default.SettingsBackupRestore)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp) // extra padding to clear player sheet
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetConfirmation = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Restaurar Minha Sanidade",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Esquecer todas as suas preferências de tema e humor e voltar ao modo original sem graça.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }

    // Confirmation dialog
    if (showResetConfirmation) {
        ConfirmationDialog(
            title = "Redefinir minha personalidade?",
            message = "Tem certeza que deseja apagar minhas memórias de tema e humor? Eu vou esquecer temporariamente suas piadas preferidas, mas suas músicas e playlists continuarão a salvo (infelizmente).",
            confirmText = "Mandar ver",
            onConfirm = {
                viewModel.restoreSettings()
                Toast.makeText(context, "Voltei ao meu eu comportado sem personalidade.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showResetConfirmation = false }
        )
    }
}

@Composable
fun SettingsGroupHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun SarcasmOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.tertiary,
                lineHeight = 16.sp
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.tertiary
            )
        )
    }
}
