package com.example

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.MusicPlayerManager
import com.example.data.SettingsManager
import com.example.data.database.AppDatabase
import com.example.data.repository.MusicRepository
import com.example.ui.MusicViewModel
import com.example.ui.MusicViewModelFactory
import com.example.ui.components.BottomFavoritesPanel
import com.example.ui.components.FullPlayerPanel
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SarcasmSplash
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.QrCodeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ApkHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize dependencies
        val database = AppDatabase.getInstance(this)
        val repository = MusicRepository(database.songDao(), database.playlistDao())
        val settings = SettingsManager(this)
        val playerManager = MusicPlayerManager.getInstance(this)

        // ViewModel Factory
        val factory = MusicViewModelFactory(repository, settings, playerManager)
        val viewModel = ViewModelProvider(this, factory)[MusicViewModel::class.java]

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val sarcasmLevel by viewModel.sarcasmLevel.collectAsState()

            MyApplicationTheme(
                darkTheme = (appTheme == SettingsManager.AppTheme.ESCURO),
                sarcasmLevel = sarcasmLevel
            ) {
                // Request Multiple Permissions (Notifications, Location, Recording, Media Storage)
                val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        val allGranted = permissions.values.all { it }
                        if (allGranted) {
                            Toast.makeText(this, "Todas as permissões concedidas! Aproveite a sua música sem limites! 🎉", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Algumas permissões não foram concedidas, mas faremos o melhor possível!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    val permissionsList = mutableListOf(
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(android.Manifest.permission.POST_NOTIFICATIONS)
                            add(android.Manifest.permission.READ_MEDIA_AUDIO)
                            add(android.Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                    multiplePermissionsLauncher.launch(permissionsList.toTypedArray())
                }

                // Splash vs Player View controller (shows once on initial launch, avoids restarting from scratch on reopen)
                var showSplash by remember { mutableStateOf(!viewModel.settings.hasSeenSplash) }

                if (showSplash) {
                    SarcasmSplash(
                        sarcasmLevel = sarcasmLevel,
                        onSplashFinished = {
                            viewModel.settings.hasSeenSplash = true
                            showSplash = false
                        }
                    )
                } else {
                    AppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppContent(viewModel: MusicViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val playerManager = viewModel.playerManager
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    var isPlayerExpanded by remember { mutableStateOf(viewModel.settings.isPlayerExpanded) }
    var isPlayerHidden by remember { mutableStateOf(false) }
    var isFavoritesPanelHidden by remember { mutableStateOf(false) }
    var isRestoreFabHidden by remember { mutableStateOf(false) }

    LaunchedEffect(isPlayerExpanded) {
        viewModel.settings.isPlayerExpanded = isPlayerExpanded
    }

    // Automatically reset all swiped/hidden panels and buttons back to normal when changing tabs/pages
    LaunchedEffect(currentTab) {
        isPlayerHidden = false
        isFavoritesPanelHidden = false
        isRestoreFabHidden = false
    }

    // Automatically show the player bar when a new song starts playing!
    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            isPlayerHidden = false
            isRestoreFabHidden = false
        }
    }

    // Intercept Back Button when the full player sheet is active to minimize it
    if (isPlayerExpanded) {
        BackHandler {
            isPlayerExpanded = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (isPlayerHidden && currentSong != null && !isRestoreFabHidden) {
                var fabOffsetX by remember { mutableStateOf(0f) }
                val animatedFabOffsetX by animateFloatAsState(
                    targetValue = fabOffsetX,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "fab_swipe"
                )
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = animatedFabOffsetX
                            alpha = (1f - (kotlin.math.abs(animatedFabOffsetX) / 300f)).coerceIn(0f, 1f)
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(fabOffsetX) > 120f) {
                                        isRestoreFabHidden = true
                                    }
                                    fabOffsetX = 0f
                                },
                                onDragCancel = {
                                    fabOffsetX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    fabOffsetX += dragAmount.x
                                }
                            )
                        },
                    text = { Text("Restaurar Player 🎵", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Restaurar Player") },
                    onClick = { isPlayerHidden = false },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Persistent miniplayer hovering over navigation bar
                if (currentSong != null && !isPlayerHidden) {
                    MiniPlayer(
                        playerManager = playerManager,
                        onExpandClick = { isPlayerExpanded = true },
                        onSwipeToHide = { 
                            isPlayerHidden = true 
                            isRestoreFabHidden = true
                        },
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Clean Navigation bar in Portuguese
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBarItem(
                        selected = currentTab == MusicViewModel.ActiveTab.INICIO,
                        onClick = { viewModel.setTab(MusicViewModel.ActiveTab.INICIO) },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        label = { Text("Músicas", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MusicViewModel.ActiveTab.PLAYLISTS,
                        onClick = { viewModel.setTab(MusicViewModel.ActiveTab.PLAYLISTS) },
                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        label = { Text("Playlists", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MusicViewModel.ActiveTab.FAVORITOS,
                        onClick = { viewModel.setTab(MusicViewModel.ActiveTab.FAVORITOS) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Favoritos", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MusicViewModel.ActiveTab.QR_CODE,
                        onClick = { viewModel.setTab(MusicViewModel.ActiveTab.QR_CODE) },
                        icon = { Icon(Icons.Default.QrCode2, contentDescription = "QR Code") },
                        label = { Text("QR Code", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { ApkHelper.shareApkFile(context) },
                        icon = { Icon(Icons.Default.Share, contentDescription = "Compartilhar APK") },
                        label = { Text("Compartilhar", fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (currentTab) {
                MusicViewModel.ActiveTab.INICIO -> LibraryScreen(viewModel = viewModel)
                MusicViewModel.ActiveTab.PLAYLISTS -> PlaylistsScreen(viewModel = viewModel)
                MusicViewModel.ActiveTab.FAVORITOS -> FavoritesScreen(viewModel = viewModel)
                MusicViewModel.ActiveTab.QR_CODE -> QrCodeScreen(viewModel = viewModel)
            }
        }

        // Animated Sliding Overlay Full-screen Player Sheet
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FullPlayerPanel(
                viewModel = viewModel,
                onMinimizeClick = { isPlayerExpanded = false }
            )
        }
    }
}
