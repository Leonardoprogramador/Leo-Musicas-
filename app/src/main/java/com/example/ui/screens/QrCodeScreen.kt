package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SarcasmManager
import com.example.ui.MusicViewModel
import com.example.util.ApkHelper
import com.example.util.ApkSharingServer
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class QrSharingMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ONLINE("Link Online", Icons.Default.CloudDownload),
    LOCAL_WIFI("Rede Local / Wi-Fi", Icons.Default.Wifi),
    DIRECT_APK("Enviar APK", Icons.Default.Share)
}

@Composable
fun QrCodeScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sarcasmLevel by viewModel.sarcasmLevel.collectAsState()
    val scrollState = rememberScrollState()

    // Handle back button to return to songs library
    BackHandler {
        viewModel.setTab(MusicViewModel.ActiveTab.INICIO)
    }

    var selectedMode by remember { mutableStateOf(QrSharingMode.DIRECT_APK) }
    val onlineUrl by viewModel.apkDownloadUrl.collectAsState()
    var localServerUrl by remember { mutableStateOf<String?>(null) }
    var showEditUrlDialog by remember { mutableStateOf(false) }

    // Start local server when in LOCAL_WIFI mode, stop when leaving
    DisposableEffect(selectedMode) {
        if (selectedMode == QrSharingMode.LOCAL_WIFI) {
            ApkSharingServer.start(context) { url ->
                localServerUrl = url
            }
        }
        onDispose {
            if (selectedMode == QrSharingMode.LOCAL_WIFI) {
                ApkSharingServer.stop()
            }
        }
    }

    // Determine target URL for QR code
    val activeUrl = when (selectedMode) {
        QrSharingMode.ONLINE -> onlineUrl
        QrSharingMode.LOCAL_WIFI -> localServerUrl ?: "http://192.168.1.1:8080/LeoMusicas.apk"
        QrSharingMode.DIRECT_APK -> onlineUrl
    }

    // Generate QR code bitmap asynchronously
    var qrBitmap by remember(activeUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(activeUrl) {
        isLoading = true
        val bmp = withContext(Dispatchers.Default) {
            QrCodeGenerator.generate(activeUrl, size = 600)
        }
        qrBitmap = bmp
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. SCREEN HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setTab(MusicViewModel.ActiveTab.INICIO) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar para músicas",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Instalar ou Atualizar App",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Baixe a versão completa ou atualize para a mais recente",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. MODE SELECTOR (ONLINE vs LOCAL WI-FI vs DIRETO) ---
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            QrSharingMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = QrSharingMode.entries.size),
                    onClick = { selectedMode = mode },
                    selected = selectedMode == mode,
                    icon = {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. QR CODE DISPLAY CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // High contrast white container for 100% reliable camera scanning
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code para baixar ou atualizar o aplicativo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pronto para Gerar QR Code",
                                color = Color.DarkGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status chip: Current Version
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Versão ${QrCodeGenerator.CURRENT_APP_VERSION} (Nova Versão Atualizada) • APK Completo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Subtitle explaining scanning behavior
                Text(
                    text = when (selectedMode) {
                        QrSharingMode.ONLINE -> "Aponte a câmera para baixar o instalador APK ou atualizar a versão do app."
                        QrSharingMode.LOCAL_WIFI -> "Conecte outro aparelho no mesmo Wi-Fi e aponte a câmera para baixar o APK direto deste celular!"
                        QrSharingMode.DIRECT_APK -> "Compartilhe o arquivo .apk completo por WhatsApp, Bluetooth ou Quick Share."
                    },
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Display active URL with edit icon
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeUrl,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedMode == QrSharingMode.ONLINE) {
                            IconButton(
                                onClick = { showEditUrlDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar link",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. QUICK ACTION BUTTONS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ações para Baixar ou Atualizar 🚀",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Button 1: Share real .apk file directly (WhatsApp, Telegram, Bluetooth, Quick Share)
                Button(
                    onClick = {
                        ApkHelper.shareApkFile(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar Arquivo APK Direto (${ApkHelper.getApkSizeMb(context)})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 2: Open link directly in browser to download APK / Update
                FilledTonalButton(
                    onClick = {
                        ApkHelper.openUrl(context, activeUrl)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Abrir Link / Baixar no Navegador", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 3: Copy link
                    OutlinedButton(
                        onClick = {
                            ApkHelper.copyToClipboard(context, activeUrl, "Link do APK copiado! 📋")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copiar Link", fontSize = 12.sp)
                    }

                    // Button 4: Edit link
                    OutlinedButton(
                        onClick = {
                            showEditUrlDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Editar Link", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. HOW TO INSTALL OR UPDATE GUIDE CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Como funciona a Instalação ou Atualização:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                InstructionStep(
                    stepNumber = "1",
                    title = "Para quem NÃO tem o app (Instalação)",
                    description = "Aponte a câmera para o QR Code ou receba o APK. Ao baixar, toque no arquivo e escolha 'Instalar' para obter o aplicativo completo."
                )
                Spacer(modifier = Modifier.height(8.dp))
                InstructionStep(
                    stepNumber = "2",
                    title = "Para quem JÁ tem o app (Atualização)",
                    description = "Abra o link ou baixe o APK atualizado. O Android reconhece o app e pergunta: 'Deseja atualizar?'. Suas músicas e playlists continuam salvas!"
                )
                Spacer(modifier = Modifier.height(8.dp))
                InstructionStep(
                    stepNumber = "3",
                    title = "Compartilhamento Instantâneo",
                    description = "Use 'Enviar Arquivo APK Direto' para transferir o instalador por WhatsApp ou Bluetooth em segundos para qualquer pessoa!"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. SARCASM COMMENT BANNER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "😏", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (sarcasmLevel) {
                        SarcasmManager.SarcasmLevel.SEM -> "Compartilhe este aplicativo com amigos e familiares de forma simples."
                        SarcasmManager.SarcasmLevel.MODERADO -> "Escaneie o QR Code ou compartilhe o APK antes que sua bateria acabe."
                        SarcasmManager.SarcasmLevel.MUITO -> "Atualize ou instale logo. Não me faça ter que desenhar outro QR Code à mão."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // --- DIALOG: EDIT DOWNLOAD / UPDATE URL ---
    if (showEditUrlDialog) {
        var tempUrl by remember { mutableStateOf(onlineUrl) }

        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = {
                Text(text = "Configurar Link do APK", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Cole o link onde o arquivo .apk do aplicativo está hospedado (Google Drive, GitHub Releases, MediaFire, servidor próprio, etc.):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://exemplo.com/LeoMusicas.apk") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "O QR Code e o botão de baixar serão atualizados na hora com este link.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempUrl.isNotBlank()) {
                            viewModel.updateApkDownloadUrl(tempUrl)
                            Toast.makeText(context, "Link de download atualizado!", Toast.LENGTH_SHORT).show()
                        }
                        showEditUrlDialog = false
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun InstructionStep(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
