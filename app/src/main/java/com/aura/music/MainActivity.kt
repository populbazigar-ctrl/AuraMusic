package com.aura.music

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage

private val AuraCyan = Color(0xFF00E5FF)
private val AuraViolet = Color(0xFF9D4EDD)
private val DeepBlack = Color(0xFF090A0F)
private val DeepCharcoal = Color(0xFF151824)

private val AuraDarkScheme = darkColorScheme(
    primary = AuraCyan,
    secondary = AuraViolet,
    background = DeepBlack,
    surface = DeepCharcoal,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF2F5FA),
    onSurface = Color(0xFFE5E9F0)
)

private val AuraLightScheme = lightColorScheme(
    primary = Color(0xFF008394),
    secondary = Color(0xFF7B2CBF),
    background = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF12141C),
    onSurface = Color(0xFF1B1F2B)
)

class MainActivity : ComponentActivity() {

    private lateinit var playerController: MusicPlayerController
    private var generatedAudioSessionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        generatedAudioSessionId = audioManager.generateAudioSessionId()

        playerController = MusicPlayerController(applicationContext, lifecycleScope)
        playerController.connectController()

        setContent {
            val isDark by playerController.isDarkTheme.collectAsState()
            MaterialTheme(colorScheme = if (isDark) AuraDarkScheme else AuraLightScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuraMusicApp(
                        controller = playerController,
                        fallbackSessionId = generatedAudioSessionId
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        playerController.release()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraMusicApp(
    controller: MusicPlayerController,
    fallbackSessionId: Int
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val allTracks by controller.allTracks.collectAsState()
    val queue by controller.queue.collectAsState()
    val currentTrack by controller.currentTrack.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val positionMs by controller.positionMs.collectAsState()
    val durationMs by controller.durationMs.collectAsState()
    val favorites by controller.favorites.collectAsState()
    val recentIds by controller.recentIds.collectAsState()
    val playlists by controller.playlists.collectAsState()
    val sortOrder by controller.sortOrder.collectAsState()
    val searchQuery by controller.searchQuery.collectAsState()
    val isDark by controller.isDarkTheme.collectAsState()
    val isLoading by controller.isLoading.collectAsState()
    val statusMessage by controller.statusMessage.collectAsState()
    val lyrics by controller.lyrics.collectAsState()
    val eqState by controller.equalizerState.collectAsState()
    val sleepSeconds by controller.sleepTimer.remainingSeconds.collectAsState()

    var selectedTab by remember { mutableStateOf(LibraryTab.HOME) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var activeModal by remember { mutableIntStateOf(0) } // 1=Queue, 2=EQ, 3=Lyrics, 4=Sleep, 5=AddToPlaylist
    var trackForPlaylist by remember { mutableStateOf<AuraTrack?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<AuraPlaylist?>(null) }

    val requiredAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredAudioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[requiredAudioPermission] == true
        hasAudioPermission = granted
        if (granted) {
            controller.scanLibrary()
        }
    }

    val safAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        controller.importSafAudioFiles(uris)
    }

    val safLyricsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) controller.loadExternalLyrics(uri)
    }

    LaunchedEffect(Unit) {
        if (hasAudioPermission) {
            controller.scanLibrary()
        } else {
            val perms = buildList {
                add(requiredAudioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            controller.clearStatusMessage()
        }
    }

    val filteredTracks = remember(allTracks, searchQuery, selectedTab, selectedCategoryFilter) {
        val base = if (searchQuery.isBlank()) {
            allTracks
        } else {
            val q = searchQuery.trim().lowercase()
            allTracks.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
        when {
            selectedTab == LibraryTab.ALBUMS && selectedCategoryFilter != null ->
                base.filter { it.album == selectedCategoryFilter }
            selectedTab == LibraryTab.ARTISTS && selectedCategoryFilter != null ->
                base.filter { it.artist == selectedCategoryFilter }
            selectedTab == LibraryTab.FOLDERS && selectedCategoryFilter != null ->
                base.filter { it.folderName == selectedCategoryFilter }
            else -> base
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AuraTopHeader(
                searchQuery = searchQuery,
                onSearchChange = controller::updateSearch,
                sortOrder = sortOrder,
                onSortChange = controller::updateSortOrder,
                isDark = isDark,
                onToggleTheme = controller::toggleTheme,
                onPickFiles = { safAudioPicker.launch(arrayOf("audio/*")) },
                onRescan = {
                    if (hasAudioPermission) controller.scanLibrary()
                    else permissionLauncher.launch(arrayOf(requiredAudioPermission))
                }
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = currentTrack != null && !showNowPlaying) {
                    currentTrack?.let { track ->
                        MiniPlayerCard(
                            track = track,
                            isPlaying = isPlaying,
                            onTogglePlay = controller::togglePlayPause,
                            onNext = controller::next,
                            onOpenFull = { showNowPlaying = true }
                        )
                    }
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    LibraryTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            LibraryTab.HOME -> Icons.Default.Home
                            LibraryTab.SONGS -> Icons.Default.MusicNote
                            LibraryTab.ALBUMS -> Icons.Default.Album
                            LibraryTab.ARTISTS -> Icons.Default.Person
                            LibraryTab.FOLDERS -> Icons.Default.Folder
                            LibraryTab.PLAYLISTS -> Icons.Default.PlaylistPlay
                        }
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                                selectedCategoryFilter = null
                            },
                            icon = { Icon(icon, contentDescription = tab.title) },
                            label = { Text(tab.title, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AuraCyan)
                }
            } else if (allTracks.isEmpty()) {
                EmptyLibraryView(
                    hasPermission = hasAudioPermission,
                    onGrantOrScan = {
                        if (hasAudioPermission) controller.scanLibrary()
                        else permissionLauncher.launch(arrayOf(requiredAudioPermission))
                    },
                    onSelectFiles = { safAudioPicker.launch(arrayOf("audio/*")) }
                )
            } else {
                when (selectedTab) {
                    LibraryTab.HOME -> HomeDashboardScreen(
                        allTracks = allTracks,
                        favorites = favorites,
                        recentIds = recentIds,
                        sleepSeconds = sleepSeconds,
                        onPlayTracks = controller::playQueue,
                        onToggleFavorite = controller::toggleFavorite,
                        onAddToPlaylist = { trackForPlaylist = it; activeModal = 5 }
                    )

                    LibraryTab.SONGS -> TrackListScreen(
                        tracks = filteredTracks,
                        favorites = favorites,
                        headerTitle = "All Songs (${filteredTracks.size})",
                        onBackFilter = null,
                        onPlayTrack = { idx -> controller.playQueue(filteredTracks, idx) },
                        onToggleFavorite = controller::toggleFavorite,
                        onAddToPlaylist = { trackForPlaylist = it; activeModal = 5 }
                    )

                    LibraryTab.ALBUMS -> {
                        if (selectedCategoryFilter == null) {
                            val groups = allTracks.groupBy { it.album }
                            CategoryGridList(
                                title = "Albums (${groups.size})",
                                groups = groups,
                                onSelectGroup = { selectedCategoryFilter = it }
                            )
                        } else {
                            TrackListScreen(
                                tracks = filteredTracks,
                                favorites = favorites,
                                headerTitle = "Album: $selectedCategoryFilter",
                                onBackFilter = { selectedCategoryFilter = null },
                                onPlayTrack = { idx -> controller.playQueue(filteredTracks, idx) },
                                onToggleFavorite = controller::toggleFavorite,
                                onAddToPlaylist = { trackForPlaylist = it; activeModal = 5 }
                            )
                        }
                    }

                    LibraryTab.ARTISTS -> {
                        if (selectedCategoryFilter == null) {
                            val groups = allTracks.groupBy { it.artist }
                            CategoryGridList(
                                title = "Artists (${groups.size})",
                                groups = groups,
                                onSelectGroup = { selectedCategoryFilter = it }
                            )
                        } else {
                            TrackListScreen(
                                tracks = filteredTracks,
                                favorites = favorites,
                                headerTitle = "Artist: $selectedCategoryFilter",
                                onBackFilter = { selectedCategoryFilter = null },
                                onPlayTrack = { idx -> controller.playQueue(filteredTracks, idx) },
                                onToggleFavorite = controller::toggleFavorite,
                                onAddToPlaylist = { trackForPlaylist = it; activeModal = 5 }
                            )
                        }
                    }

                    LibraryTab.FOLDERS -> {
                        if (selectedCategoryFilter == null) {
                            val groups = allTracks.groupBy { it.folderName }
                            CategoryGridList(
                                title = "Folders (${groups.size})",
                                groups = groups,
                                onSelectGroup = { selectedCategoryFilter = it }
                            )
                        } else {
                            TrackListScreen(
                                tracks = filteredTracks,
                                favorites = favorites,
                                headerTitle = "Folder: $selectedCategoryFilter",
                                onBackFilter = { selectedCategoryFilter = null },
                                onPlayTrack = { idx -> controller.playQueue(filteredTracks, idx) },
                                onToggleFavorite = controller::toggleFavorite,
                                onAddToPlaylist = { trackForPlaylist = it; activeModal = 5 }
                            )
                        }
                    }

                    LibraryTab.PLAYLISTS -> PlaylistsScreen(
                        playlists = playlists,
                        allTracks = allTracks,
                        onCreateClick = { showCreatePlaylistDialog = true },
                        onRenameClick = { playlistToRename = it },
                        onDeleteClick = controller::deletePlaylist,
                        onPlayPlaylist = { pl ->
                            val plTracks = pl.trackIds.mapNotNull { id -> allTracks.find { it.id == id } }
                            controller.playQueue(plTracks, 0)
                        }
                    )
                }
            }
        }
    }

    if (showNowPlaying && currentTrack != null) {
        NowPlayingOverlay(
            track = currentTrack!!,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            isFavorite = favorites.contains(currentTrack!!.id),
            sleepTimerLabel = controller.sleepTimer.formatRemaining(sleepSeconds),
            onClose = { showNowPlaying = false },
            onPlayPause = controller::togglePlayPause,
            onNext = controller::next,
            onPrevious = controller::previous,
            onSeek = controller::seekTo,
            onToggleFavorite = { controller.toggleFavorite(currentTrack!!.id) },
            onOpenQueue = { activeModal = 1 },
            onOpenEq = {
                controller.refreshEqualizer(fallbackSessionId)
                activeModal = 2
            },
            onOpenLyrics = { activeModal = 3 },
            onOpenSleepTimer = { activeModal = 4 }
        )
    }

    if (activeModal != 0) {
        ModalBottomSheet(onDismissRequest = { activeModal = 0 }) {
            when (activeModal) {
                1 -> QueueSheetContent(
                    queue = queue,
                    currentTrack = currentTrack,
                    onSelectTrack = { idx -> controller.playQueue(queue, idx) },
                    onRemoveTrack = controller::removeFromQueue
                )

                2 -> EqualizerSheetContent(
                    state = eqState,
                    onToggleEnabled = controller::setEqualizerEnabled,
                    onBandChange = controller::setEqualizerBand,
                    onBassChange = controller::setBassBoost
                )

                3 -> LyricsSheetContent(
                    lyrics = lyrics,
                    positionMs = positionMs,
                    onLoadFile = { safLyricsPicker.launch(arrayOf("text/*", "application/octet-stream", "*/*")) }
                )

                4 -> SleepTimerSheetContent(
                    remainingSeconds = sleepSeconds,
                    onSelectMinutes = { mins ->
                        controller.sleepTimer.startMinutes(mins)
                        activeModal = 0
                    },
                    onCancelTimer = {
                        controller.sleepTimer.cancel()
                        activeModal = 0
                    }
                )

                5 -> AddToPlaylistSheetContent(
                    playlists = playlists,
                    onSelectPlaylist = { pl ->
                        trackForPlaylist?.let { controller.addTrackToPlaylist(pl.id, it.id) }
                        activeModal = 0
                    },
                    onNewPlaylist = {
                        activeModal = 0
                        showCreatePlaylistDialog = true
                    }
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        TextInputDialog(
            title = "Create Playlist",
            initialValue = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                controller.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }

    playlistToRename?.let { target ->
        TextInputDialog(
            title = "Rename Playlist",
            initialValue = target.name,
            confirmLabel = "Save",
            onConfirm = { newName ->
                controller.renamePlaylist(target.id, newName)
                playlistToRename = null
            },
            onDismiss = { playlistToRename = null }
        )
    }
}

@Composable
private fun AuraTopHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sortOrder: AuraSortOrder,
    onSortChange: (AuraSortOrder) -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onPickFiles: () -> Unit,
    onRescan: () -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AuraCyan, AuraViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AURA Music",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPickFiles) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Select Audio Files")
                }
                IconButton(onClick = onRescan) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan Library")
                }
                Box {
                    IconButton(onClick = { sortExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        AuraSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = {
                                    onSortChange(order)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Switch Theme"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Search songs or artists...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        )
    }
}

@Composable
private fun HomeDashboardScreen(
    allTracks: List<AuraTrack>,
    favorites: Set<Long>,
    recentIds: List<Long>,
    sleepSeconds: Long,
    onPlayTracks: (List<AuraTrack>, Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (AuraTrack) -> Unit
) {
    val favTracks = remember(allTracks, favorites) {
        allTracks.filter { favorites.contains(it.id) }
    }
    val recentTracks = remember(allTracks, recentIds) {
        recentIds.mapNotNull { id -> allTracks.find { it.id == id } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = "Total Tracks",
                    value = allTracks.size.toString(),
                    accent = AuraCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onPlayTracks(allTracks, 0) }
                )
                DashboardStatCard(
                    title = "Favorites",
                    value = favTracks.size.toString(),
                    accent = AuraViolet,
                    modifier = Modifier.weight(1f),
                    onClick = { if (favTracks.isNotEmpty()) onPlayTracks(favTracks, 0) }
                )
            }
        }

        if (sleepSeconds > 0L) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = AuraCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sleep Timer Active: ${sleepSeconds / 60}m ${sleepSeconds % 60}s remaining")
                    }
                }
            }
        }

        if (recentTracks.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Played",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(recentTracks) { idx, track ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onPlayTracks(recentTracks, idx) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                TrackArtwork(uri = track.albumArtUri, sizeDp = 120)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = track.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = if (favTracks.isNotEmpty()) "Favorite Songs" else "Quick Library Picks",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        val displayList = if (favTracks.isNotEmpty()) favTracks else allTracks.take(15)
        itemsIndexed(displayList) { idx, track ->
            TrackRowItem(
                track = track,
                isFavorite = favorites.contains(track.id),
                onClick = { onPlayTracks(displayList, idx) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onAddToPlaylist = { onAddToPlaylist(track) }
            )
        }
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            )
        }
    }
}

@Composable
private fun TrackListScreen(
    tracks: List<AuraTrack>,
    favorites: Set<Long>,
    headerTitle: String,
    onBackFilter: (() -> Unit)?,
    onPlayTrack: (Int) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (AuraTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (onBackFilter != null) {
                    TextButton(onClick = onBackFilter) {
                        Text("Back to Categories")
                    }
                }
            }
        }

        itemsIndexed(tracks) { index, track ->
            TrackRowItem(
                track = track,
                isFavorite = favorites.contains(track.id),
                onClick = { onPlayTrack(index) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onAddToPlaylist = { onAddToPlaylist(track) }
            )
        }
    }
}

@Composable
private fun TrackRowItem(
    track: AuraTrack,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(uri = track.albumArtUri, sizeDp = 50)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${track.artist} • ${formatTime(track.durationMs)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) AuraCyan else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist")
            }
        }
    }
}

@Composable
private fun CategoryGridList(
    title: String,
    groups: Map<String, List<AuraTrack>>,
    onSelectGroup: (String) -> Unit
) {
    val entries = remember(groups) { groups.entries.sortedBy { it.key.lowercase() } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        items(entries) { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectGroup(entry.key) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(uri = entry.value.firstOrNull()?.albumArtUri, sizeDp = 46)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.key, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            "${entry.value.size} tracks",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsScreen(
    playlists: List<AuraPlaylist>,
    allTracks: List<AuraTrack>,
    onCreateClick: () -> Unit,
    onRenameClick: (AuraPlaylist) -> Unit,
    onDeleteClick: (String) -> Unit,
    onPlayPlaylist: (AuraPlaylist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Playlists", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Playlist")
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Text(
                    "No playlists yet. Create one and add your favorite tracks!",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        items(playlists) { pl ->
            val validCount = pl.trackIds.count { id -> allTracks.any { it.id == id } }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (validCount > 0) onPlayPlaylist(pl) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pl.name, fontWeight = FontWeight.Bold)
                        Text("$validCount songs", fontSize = 12.sp)
                    }
                    IconButton(onClick = { onRenameClick(pl) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename")
                    }
                    IconButton(onClick = { onDeleteClick(pl.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerCard(
    track: AuraTrack,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenFull: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable { onOpenFull() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(uri = track.albumArtUri, sizeDp = 44)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(
                    track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = AuraCyan
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause"
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
        }
    }
}

@Composable
private fun NowPlayingOverlay(
    track: AuraTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    sleepTimerLabel: String,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEq: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", modifier = Modifier.size(30.dp))
                }
                Text("NOW PLAYING", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AuraCyan else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            TrackArtwork(uri = track.albumArtUri, sizeDp = 250)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${track.artist} — ${track.album}",
                    color = AuraCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AuraVisualizerBar(
                isPlaying = isPlaying,
                progressMs = positionMs,
                cyanAccent = AuraCyan,
                violetAccent = AuraViolet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                val safeTotal = durationMs.coerceAtLeast(1L)
                Slider(
                    value = positionMs.coerceIn(0L, safeTotal).toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..safeTotal.toFloat()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(positionMs), fontSize = 12.sp)
                    Text(formatTime(durationMs), fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AuraCyan, AuraViolet)))
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue")
                }
                IconButton(onClick = onOpenEq) {
                    Icon(Icons.Default.Equalizer, contentDescription = "Equalizer")
                }
                IconButton(onClick = onOpenLyrics) {
                    Icon(Icons.Default.Lyrics, contentDescription = "Lyrics")
                }
                TextButton(onClick = onOpenSleepTimer) {
                    Icon(Icons.Default.Timer, contentDescription = "Sleep Timer")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(sleepTimerLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TrackArtwork(uri: android.net.Uri?, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(DeepCharcoal, AuraViolet.copy(alpha = 0.45f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = AuraCyan,
            modifier = Modifier.size((sizeDp / 2.2f).dp)
        )
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun EmptyLibraryView(
    hasPermission: Boolean,
    onGrantOrScan: () -> Unit,
    onSelectFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = AuraCyan,
            modifier = Modifier.size(68.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (hasPermission) "No Music Found on Device" else "Storage Permission Needed",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your phone's local storage or manually pick audio files using Android Storage Access Framework.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onGrantOrScan,
            colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
        ) {
            Text(if (hasPermission) "Scan Phone Storage" else "Grant Audio Permission")
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = onSelectFiles) {
            Text("Select Audio Files Manually")
        }
    }
}

@Composable
private fun QueueSheetContent(
    queue: List<AuraTrack>,
    currentTrack: AuraTrack?,
    onSelectTrack: (Int) -> Unit,
    onRemoveTrack: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Playback Queue (${queue.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(queue) { idx, track ->
                val active = currentTrack?.id == track.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTrack(idx) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.title,
                        modifier = Modifier.weight(1f),
                        color = if (active) AuraCyan else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                    IconButton(onClick = { onRemoveTrack(track.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerSheetContent(
    state: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onBandChange: (Short, Short) -> Unit,
    onBassChange: (Short) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Audio Equalizer & Bass", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            if (state.isSupported) {
                Switch(checked = state.isEnabled, onCheckedChange = onToggleEnabled)
            }
        }
        Text(state.message, fontSize = 12.sp, color = AuraCyan)

        if (state.isSupported) {
            state.bands.forEach { band ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${band.centerFreqHz} Hz", fontSize = 12.sp)
                        Text("${band.currentLevelMb / 100} dB", fontSize = 12.sp)
                    }
                    Slider(
                        value = band.currentLevelMb.toFloat(),
                        onValueChange = { onBandChange(band.bandIndex, it.toInt().toShort()) },
                        valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat()
                    )
                }
            }

            if (state.bassBoostSupported) {
                Text("Bass Boost (${state.bassStrength / 10}%)", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = state.bassStrength.toFloat(),
                    onValueChange = { onBassChange(it.toInt().toShort()) },
                    valueRange = 0f..1000f
                )
            }
        }
    }
}

@Composable
private fun LyricsSheetContent(
    lyrics: List<LyricLine>,
    positionMs: Long,
    onLoadFile: () -> Unit
) {
    val activeIdx = remember(lyrics, positionMs) {
        AuraLyricsEngine.activeLineIndex(lyrics, positionMs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Synchronized Lyrics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedButton(onClick = onLoadFile) {
                Text("Load .lrc / .txt")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (lyrics.isEmpty()) {
            Text(
                "No local lyrics loaded. Place a matching .lrc/.txt file next to your song or tap 'Load .lrc / .txt'.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(lyrics) { idx, line ->
                    val highlighted = idx == activeIdx
                    Text(
                        text = line.text,
                        color = if (highlighted) AuraCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontWeight = if (highlighted) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = if (highlighted) 17.sp else 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepTimerSheetContent(
    remainingSeconds: Long,
    onSelectMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    val presets = listOf(10, 20, 30, 45, 60)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sleep Timer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { mins ->
                FilterChip(
                    selected = false,
                    onClick = { onSelectMinutes(mins) },
                    label = { Text("${mins}m") }
                )
            }
        }
        if (remainingSeconds > 0L) {
            Button(
                onClick = onCancelTimer,
                colors = ButtonDefaults.buttonColors(containerColor = AuraViolet)
            ) {
                Text("Turn Off Timer")
            }
        }
    }
}

@Composable
private fun AddToPlaylistSheetContent(
    playlists: List<AuraPlaylist>,
    onSelectPlaylist: (AuraPlaylist) -> Unit,
    onNewPlaylist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Track to Playlist", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onNewPlaylist) {
                Text("+ Create New")
            }
        }
        playlists.forEach { pl ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPlaylist(pl) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = "${pl.name} (${pl.trackIds.size} tracks)",
                    modifier = Modifier.padding(14.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Enter playlist name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%02d:%02d".format(min, sec)
}
