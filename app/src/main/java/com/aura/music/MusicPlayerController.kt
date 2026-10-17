package com.aura.music

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MusicPlayerController(
    private val appContext: Context,
    private val scope: CoroutineScope
) {
    private val storage = AuraLocalStorage(appContext)
    val equalizerManager = AuraEqualizerManager()
    val sleepTimer = AuraSleepTimer(scope) { pause() }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    private val _allTracks = MutableStateFlow<List<AuraTrack>>(emptyList())
    val allTracks: StateFlow<List<AuraTrack>> = _allTracks.asStateFlow()

    private val _queue = MutableStateFlow<List<AuraTrack>>(emptyList())
    val queue: StateFlow<List<AuraTrack>> = _queue.asStateFlow()

    private val _currentTrack = MutableStateFlow<AuraTrack?>(null)
    val currentTrack: StateFlow<AuraTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _favorites = MutableStateFlow(storage.getFavorites())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _recentIds = MutableStateFlow(storage.getRecentIds())
    val recentIds: StateFlow<List<Long>> = _recentIds.asStateFlow()

    private val _playlists = MutableStateFlow(storage.getPlaylists())
    val playlists: StateFlow<List<AuraPlaylist>> = _playlists.asStateFlow()

    private val _sortOrder = MutableStateFlow(storage.getSortOrder())
    val sortOrder: StateFlow<AuraSortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(storage.isDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    fun connectController() {
        if (controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, AuraPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future

        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
            mediaController = controller
            controller.addListener(playerListener)
            _isPlaying.value = controller.isPlaying
            _durationMs.value = controller.duration.coerceAtLeast(0L)
            startProgressTicker()
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            val matched = _queue.value.find { it.id == id } ?: _allTracks.value.find { it.id == id }
            _currentTrack.value = matched
            _durationMs.value = (mediaController?.duration ?: matched?.durationMs ?: 0L).coerceAtLeast(0L)
            if (matched != null) {
                storage.pushRecentId(matched.id)
                _recentIds.value = storage.getRecentIds()
                _lyrics.value = AuraLyricsEngine.findSidecarLyrics(matched)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _durationMs.value = (mediaController?.duration ?: 0L).coerceAtLeast(0L)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _statusMessage.value = "Skipped unreadable or unsupported audio file."
            mediaController?.let {
                if (it.hasNextMediaItem()) {
                    it.seekToNextMediaItem()
                    it.prepare()
                    it.play()
                }
            }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val ctrl = mediaController
                if (ctrl != null && ctrl.isPlaying) {
                    _positionMs.value = ctrl.currentPosition.coerceAtLeast(0L)
                    val dur = ctrl.duration
                    if (dur > 0L) _durationMs.value = dur
                }
                delay(400L)
            }
        }
    }

    fun scanLibrary() {
        scope.launch {
            _isLoading.value = true
            val scanned = withContext(Dispatchers.IO) {
                val list = mutableListOf<AuraTrack>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

                runCatching {
                    appContext.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        null,
                        null
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                        val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                        val artworkBase = Uri.parse("content://media/external/audio/albumart")

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val duration = cursor.getLong(durCol)
                            if (duration < 1000L) continue
                            val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown Title"
                            val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist"
                            val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown Album"
                            val albumId = cursor.getLong(albumIdCol)
                            val path = cursor.getString(dataCol) ?: ""
                            val folder = if (path.isNotBlank()) File(path).parentFile?.name ?: "InternalAudio" else "Music"
                            val dateAdded = cursor.getLong(dateCol)

                            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            val artUri = ContentUris.withAppendedId(artworkBase, albumId)

                            list.add(
                                AuraTrack(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    durationMs = duration,
                                    contentUri = contentUri,
                                    albumArtUri = artUri,
                                    folderName = folder,
                                    filePath = path,
                                    dateAddedSec = dateAdded
                                )
                            )
                        }
                    }
                }

                storage.getSavedSafUris().forEach { uriStr ->
                    parseSafTrack(Uri.parse(uriStr))?.let { safTrack ->
                        if (list.none { it.contentUri == safTrack.contentUri }) {
                            list.add(safTrack)
                        }
                    }
                }

                list
            }

            _allTracks.value = applySorting(scanned, _sortOrder.value)
            _isLoading.value = false
        }
    }

    fun importSafAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        appContext.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    storage.addSafUri(uri.toString())
                    parseSafTrack(uri)
                }
            }
            if (imported.isNotEmpty()) {
                val combined = (_allTracks.value + imported).distinctBy { it.contentUri.toString() }
                _allTracks.value = applySorting(combined, _sortOrder.value)
                playQueue(imported, 0)
            } else {
                _statusMessage.value = "Could not load the selected file."
            }
        }
    }

    private fun parseSafTrack(uri: Uri): AuraTrack? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(appContext, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "Selected Audio"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Local File"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Picked Files"
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()

            AuraTrack(
                id = (uri.toString().hashCode().toLong() and 0x7FFFFFFFL) + 1_000_000_000L,
                title = title,
                artist = artist,
                album = album,
                durationMs = dur,
                contentUri = uri,
                albumArtUri = null,
                folderName = "SAF Picked",
                filePath = "",
                dateAddedSec = System.currentTimeMillis() / 1000L
            )
        }.getOrNull()
    }

    fun playQueue(tracks: List<AuraTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.value = tracks
        val target = tracks[safeIndex]
        _currentTrack.value = target
        storage.pushRecentId(target.id)
        _recentIds.value = storage.getRecentIds()
        _lyrics.value = AuraLyricsEngine.findSidecarLyrics(target)

        val controller = mediaController ?: return
        val items = tracks.map { track ->
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.albumArtUri)
                .build()

            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.contentUri)
                .setMediaMetadata(metadata)
                .build()
        }

        controller.setMediaItems(items, safeIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.mediaItemCount == 0 && _allTracks.value.isNotEmpty()) {
                playQueue(_allTracks.value, 0)
            } else {
                controller.play()
            }
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun next() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekTo(0, 0L)
        }
    }

    fun previous() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _positionMs.value = positionMs.coerceAtLeast(0L)
    }

    fun removeFromQueue(trackId: Long) {
        val idx = _queue.value.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            val updated = _queue.value.toMutableList().apply { removeAt(idx) }
            _queue.value = updated
            mediaController?.removeMediaItem(idx)
        }
    }

    fun toggleFavorite(trackId: Long) {
        val nextSet = _favorites.value.toMutableSet()
        if (!nextSet.add(trackId)) {
            nextSet.remove(trackId)
        }
        _favorites.value = nextSet
        storage.saveFavorites(nextSet)
    }

    fun createPlaylist(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val newList = _playlists.value + AuraPlaylist(
            id = UUID.randomUUID().toString(),
            name = clean,
            trackIds = emptyList()
        )
        _playlists.value = newList
        storage.savePlaylists(newList)
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val updated = _playlists.value.map {
            if (it.id == playlistId) it.copy(name = clean) else it
        }
        _playlists.value = updated
        storage.savePlaylists(updated)
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _playlists.value.filterNot { it.id == playlistId }
        _playlists.value = updated
        storage.savePlaylists(updated)
    }

    fun addTrackToPlaylist(playlistId: String, trackId: Long) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId && !pl.trackIds.contains(trackId)) {
                pl.copy(trackIds = pl.trackIds + trackId)
            } else pl
        }
        _playlists.value = updated
        storage.savePlaylists(updated)
        _statusMessage.value = "Track added to playlist."
    }

    fun loadExternalLyrics(uri: Uri) {
        val loaded = AuraLyricsEngine.loadFromUri(appContext, uri)
        _lyrics.value = loaded
        if (loaded.isEmpty()) {
            _statusMessage.value = "No readable lyrics found in selected file."
        }
    }

    fun refreshEqualizer(sessionId: Int) {
        _equalizerState.value = equalizerManager.bindToAudioSession(sessionId)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.value = equalizerManager.setEffectsEnabled(enabled)
    }

    fun setEqualizerBand(bandIndex: Short, levelMb: Short) {
        _equalizerState.value = equalizerManager.setBandLevel(bandIndex, levelMb)
    }

    fun setBassBoost(strength: Short) {
        _equalizerState.value = equalizerManager.setBassStrength(strength)
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: AuraSortOrder) {
        _sortOrder.value = order
        storage.setSortOrder(order)
        _allTracks.value = applySorting(_allTracks.value, order)
    }

    fun toggleTheme() {
        val next = !_isDarkTheme.value
        _isDarkTheme.value = next
        storage.setDarkTheme(next)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun applySorting(list: List<AuraTrack>, order: AuraSortOrder): List<AuraTrack> {
        return when (order) {
            AuraSortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
            AuraSortOrder.ARTIST -> list.sortedBy { it.artist.lowercase() }
            AuraSortOrder.RECENT -> list.sortedByDescending { it.dateAddedSec }
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimer.cancel()
        equalizerManager.releaseEffects()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }
}
