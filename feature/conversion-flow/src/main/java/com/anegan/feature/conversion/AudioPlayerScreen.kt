/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.audiofx.Equalizer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController
import android.content.ComponentName
import com.anegan.core.database.AneganDatabase
import com.anegan.core.database.AudioTrackEntity
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.PlaylistEntity
import com.anegan.core.database.PlaylistTrackEntity
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    initialAudioPath: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val db = remember { DatabaseProvider.getDatabase(context) }
    
    // Playback state variables
    val sessionToken = remember {
        SessionToken(context, ComponentName(context, "com.anegan.app.AneganAudioService"))
    }
    val controllerFuture = remember {
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    var exoPlayer by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(controllerFuture) {
        controllerFuture.addListener(
            {
                try {
                    exoPlayer = controllerFuture.get()
                } catch (e: Exception) {
                    // Ignore controller connection failure
                }
            },
            android.os.AsyncTask.THREAD_POOL_EXECUTOR
        )
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }
    var currentTrack by remember { mutableStateOf<AudioTrackEntity?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    
    // Equalizer
    var equalizer by remember { mutableStateOf<Equalizer?>(null) }
    var showEqDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<Short>(0) }
    var numPresets by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        try {
            val eq = Equalizer(0, 0)
            equalizer = eq
            numPresets = eq.numberOfPresets.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Active tabs: "LIBRARY", "PLAYLISTS", "ALBUMS", "FAVORITES", "STATS"
    var activeTab by remember { mutableStateOf("LIBRARY") }
    
    // Library lists
    var allTracks by remember { mutableStateOf(listOf<AudioTrackEntity>()) }
    var playlists by remember { mutableStateOf(listOf<PlaylistEntity>()) }
    var favoriteTracks by remember { mutableStateOf(listOf<AudioTrackEntity>()) }
    var statsTracks by remember { mutableStateOf(listOf<AudioTrackEntity>()) }
    
    // Dialogs
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showAddToPlaylistDialog by remember { mutableStateOf<AudioTrackEntity?>(null) }
    
    // Initialize list database
    fun refreshLibrary() {
        scope.launch(Dispatchers.IO) {
            val tracks = db.audioTrackDao().getAll()
            val listPlay = db.playlistDao().getAll()
            val favs = db.audioTrackDao().getFavorites()
            val stats = db.audioTrackDao().getMostPlayed(10)
            
            withContext(Dispatchers.Main) {
                allTracks = tracks
                playlists = listPlay
                favoriteTracks = favs
                statsTracks = stats
            }
        }
    }
    
    // Load metadata and add new track into database dynamically
    fun loadFileIntoDb(file: File) {
        scope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dur = durStr?.toLongOrNull() ?: 0L
                
                val track = AudioTrackEntity(
                    filePath = file.absolutePath,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = dur
                )
                db.audioTrackDao().upsert(track)
                refreshLibrary()
                
                withContext(Dispatchers.Main) {
                    currentTrack = track
                }
            } catch (e: Exception) {
                // Ignore metadata failures
            } finally {
                retriever.release()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshLibrary()
        
        // Scan typical device downloads / music directories for audio files to build library
        scope.launch(Dispatchers.IO) {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            val audioFiles = mutableListOf<File>()
            listOf(musicDir, downloadsDir).forEach { dir ->
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension.lowercase(Locale.ROOT) in listOf("mp3", "m4a", "flac", "wav", "ogg")) {
                        audioFiles.add(file)
                    }
                }
            }
            
            audioFiles.forEach { file ->
                val existing = db.audioTrackDao().getByPath(file.absolutePath)
                if (existing == null) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val dur = durStr?.toLongOrNull() ?: 0L
                        
                        db.audioTrackDao().upsert(
                            AudioTrackEntity(
                                filePath = file.absolutePath,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = dur
                            )
                        )
                    } catch (e: Exception) {
                        // ignore
                    } finally {
                        retriever.release()
                    }
                }
            }
            refreshLibrary()
        }
    }

    // Playback logic initial file
    LaunchedEffect(initialAudioPath) {
        if (initialAudioPath != null) {
            val file = File(initialAudioPath)
            if (file.exists()) {
                val existing = db.audioTrackDao().getByPath(initialAudioPath)
                if (existing != null) {
                    currentTrack = existing
                } else {
                    loadFileIntoDb(file)
                }
            }
        }
    }

    // Play track when currentTrack changes
    LaunchedEffect(exoPlayer, currentTrack) {
        val player = exoPlayer ?: return@LaunchedEffect
        val track = currentTrack ?: return@LaunchedEffect
        val trackUriStr = Uri.fromFile(File(track.filePath)).toString()
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem?.mediaId != trackUriStr) {
            val mediaItem = MediaItem.Builder()
                .setMediaId(trackUriStr)
                .setUri(Uri.fromFile(File(track.filePath)))
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }
    }

    // Listener for state updates
    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlay: Boolean) {
                isPlaying = isPlay
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = player.duration
                } else if (state == Player.STATE_ENDED) {
                    scope.launch(Dispatchers.IO) {
                        val track = currentTrack ?: return@launch
                        val updatedTrack = track.copy(
                            playCount = track.playCount + 1,
                            lastPlayedAt = System.currentTimeMillis()
                        )
                        db.audioTrackDao().upsert(updatedTrack)
                        refreshLibrary()
                        
                        withContext(Dispatchers.Main) {
                            val idx = allTracks.indexOfFirst { it.filePath == track.filePath }
                            if (idx != -1 && idx < allTracks.size - 1) {
                                currentTrack = allTracks[idx + 1]
                            }
                        }
                    }
                }
            }
        }
        player.addListener(listener)
        isPlaying = player.isPlaying
        if (player.playbackState == Player.STATE_READY) {
            duration = player.duration
        }

        onDispose {
            player.removeListener(listener)
        }
    }

    // Position Tracker Loop
    LaunchedEffect(exoPlayer) {
        while (true) {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPosition = player.currentPosition
                }
            }
            delay(500)
        }
    }

    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Music Library",
                    onBack = onBack,
                    neonAccent = NeonMagenta,
                    actions = {
                        IconButton(onClick = { showEqDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Equalizer",
                                tint = NeonMagenta
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // Tab Selectors
            val tabs = remember { listOf("LIBRARY", "PLAYLISTS", "ALBUMS", "FAVORITES", "STATS") }
            val selectedIdx = when (activeTab) {
                "LIBRARY" -> 0
                "PLAYLISTS" -> 1
                "ALBUMS" -> 2
                "FAVORITES" -> 3
                else -> 4
            }
            NovaSegmentedControl(
                items = tabs,
                selectedIndex = selectedIdx,
                onIndexSelected = { idx ->
                    activeTab = tabs[idx]
                },
                neonColor = NeonMagenta,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Playlist Creation Option
            if (activeTab == "PLAYLISTS") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    NovaPrimaryButton(
                        text = "Create Custom Playlist",
                        neonColor = NeonMagenta,
                        icon = Icons.Rounded.Add,
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Library Main Scroll Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val currentList = when (activeTab) {
                    "LIBRARY" -> allTracks
                    "FAVORITES" -> favoriteTracks
                    "STATS" -> statsTracks
                    else -> emptyList() // Custom handling for albums/playlists below
                }

                if (activeTab == "ALBUMS") {
                    val albumGroups = remember(allTracks) { allTracks.groupBy { it.album }.toList() }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(albumGroups) { index, (albumName, albumTracks) ->
                            NovaAnimatedItem(index = index) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    neonAccent = NeonMagenta.copy(alpha = 0.2f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = albumName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${albumTracks.size} Audio tracks",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        albumTracks.forEach { track ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { currentTrack = track }
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(track.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text(track.artist, fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Icon(Icons.Rounded.PlayArrow, null, tint = NeonMagenta, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (activeTab == "PLAYLISTS") {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (playlists.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No playlists created yet.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                        itemsIndexed(playlists) { index, playlist ->
                            var count by remember { mutableStateOf(0) }
                            LaunchedEffect(playlist.id) {
                                withContext(Dispatchers.IO) {
                                    count = db.playlistTrackDao().getTracksForPlaylist(playlist.id).size
                                }
                            }

                            NovaAnimatedItem(index = index) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    neonAccent = NeonMagenta.copy(alpha = 0.3f),
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            val pTracks = db.playlistTrackDao().getTracksForPlaylist(playlist.id)
                                            if (pTracks.isNotEmpty()) {
                                                val t = db.audioTrackDao().getByPath(pTracks.first().trackPath)
                                                if (t != null) {
                                                    withContext(Dispatchers.Main) {
                                                        currentTrack = t
                                                    }
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Playlist is empty! Add tracks first.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(playlist.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                            Text("$count tracks", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    db.playlistDao().delete(playlist)
                                                    db.playlistTrackDao().deletePlaylistTracks(playlist.id)
                                                    refreshLibrary()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Delete, null, tint = NovaError)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentList.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No audio tracks found. Put MP3/M4A files in Downloads.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        itemsIndexed(currentList) { index, track ->
                            val isSelected = currentTrack?.filePath == track.filePath
                            
                            NovaAnimatedItem(index = index) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    neonAccent = if (isSelected) NeonMagenta else Color.Transparent,
                                    onClick = { currentTrack = track }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Extract art preview
                                        val art = remember(track.filePath) { getEmbeddedAlbumArt(track.filePath) }
                                        if (art != null) {
                                            Image(
                                                bitmap = art.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NeonMagenta.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Rounded.MusicNote, null, tint = NeonMagenta)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${track.artist} • ${track.album}",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (activeTab == "STATS") {
                                                Text(
                                                    text = "Listened: ${track.playCount} times",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Add to Playlist Button
                                        IconButton(onClick = { showAddToPlaylistDialog = track }) {
                                            Icon(Icons.Rounded.PlaylistAdd, null, tint = NeonMagenta)
                                        }

                                        // Fav Star toggle
                                        IconButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    val t = track.copy(isFavorite = !track.isFavorite)
                                                    db.audioTrackDao().upsert(t)
                                                    refreshLibrary()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (track.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                contentDescription = null,
                                                tint = if (track.isFavorite) Color(0xFFFFD54F) else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Floating Audio Player Widget Panel ─────────────────────────────
            currentTrack?.let { track ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    neonAccent = NeonMagenta,
                    enableGlow = true
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(56.dp)
                            ) {
                                if (isPlaying) {
                                    NovaPulseRing(
                                        neonColor = NeonMagenta,
                                        isActive = true,
                                        baseRadius = 24f,
                                        pulseAmplitude = 6f
                                    )
                                }
                                val art = remember(track.filePath) { getEmbeddedAlbumArt(track.filePath) }
                                if (art != null) {
                                    Image(
                                        bitmap = art.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NeonMagenta.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = NeonMagenta)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatPosition(currentPosition),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            NovaSlider(
                                value = currentPosition.toFloat(),
                                onValueChange = {
                                    currentPosition = it.toLong()
                                    exoPlayer?.seekTo(currentPosition)
                                },
                                valueRange = 0f..maxOf(1f, duration.toFloat()),
                                neonColor = NeonMagenta,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatPosition(duration),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        // Playback Action Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back 10 seconds
                            IconButton(onClick = { exoPlayer?.seekTo(maxOf(0, currentPosition - 10000)) }) {
                                Icon(Icons.Rounded.Replay10, null, tint = NeonMagenta, modifier = Modifier.size(26.dp))
                            }
                            
                            // Previous track
                            IconButton(
                                onClick = {
                                    val idx = allTracks.indexOfFirst { it.filePath == track.filePath }
                                    if (idx > 0) {
                                        currentTrack = allTracks[idx - 1]
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, null, tint = NeonMagenta, modifier = Modifier.size(28.dp))
                            }

                            // Play / Pause
                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(NeonMagenta, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = PureWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Next track
                            IconButton(
                                onClick = {
                                    val idx = allTracks.indexOfFirst { it.filePath == track.filePath }
                                    if (idx != -1 && idx < allTracks.size - 1) {
                                        currentTrack = allTracks[idx + 1]
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.SkipNext, null, tint = NeonMagenta, modifier = Modifier.size(28.dp))
                            }

                            // Forward 10 seconds
                            IconButton(onClick = { exoPlayer?.seekTo(minOf(duration, currentPosition + 10000)) }) {
                                Icon(Icons.Rounded.Forward10, null, tint = NeonMagenta, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Equalizer preset overlay dialog ───────────────────────────────
    if (showEqDialog && numPresets > 0) {
        val isDark = isSystemInDarkTheme()
        AlertDialog(
            onDismissRequest = { showEqDialog = false },
            containerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.95f) else NovaPureWhite.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Sound Equalizer Presets", fontWeight = FontWeight.Bold, color = NeonMagenta) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(numPresets) { presetIdx ->
                        val presetName = equalizer?.getPresetName(presetIdx.toShort()) ?: "Preset $presetIdx"
                        val isSelected = selectedPreset == presetIdx.toShort()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        equalizer?.usePreset(presetIdx.toShort())
                                        selectedPreset = presetIdx.toShort()
                                    } catch (e: Exception) {
                                        // Preset use failed
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = presetName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonMagenta else if (isDark) NovaFrostWhite else NovaDeepInk
                                )
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, null, tint = NeonMagenta, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Divider(color = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqDialog = false }) {
                    Text("Done", color = NeonMagenta)
                }
            }
        )
    }

    // ── Create Playlist Dialog ──────────────────────────────────────
    if (showCreatePlaylistDialog) {
        val isDark = isSystemInDarkTheme()
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.95f) else NovaPureWhite.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            title = { Text("New Playlist", fontWeight = FontWeight.Bold, color = NeonMagenta) },
            text = {
                NovaTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = "Playlist Name",
                    neonColor = NeonMagenta
                )
            },
            confirmButton = {
                NovaPrimaryButton(
                    text = "Create",
                    neonColor = NeonMagenta,
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                db.playlistDao().upsert(
                                    PlaylistEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = newPlaylistName
                                    )
                                )
                                newPlaylistName = ""
                                withContext(Dispatchers.Main) {
                                    showCreatePlaylistDialog = false
                                }
                                refreshLibrary()
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = NeonMagenta)
                }
            }
        )
    }

    // ── Add Track to Playlist dialog ─────────────────────────────────
    showAddToPlaylistDialog?.let { track ->
        val isDark = isSystemInDarkTheme()
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            containerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.95f) else NovaPureWhite.copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp),
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold, color = NeonMagenta) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists created yet. Create a playlist first.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(playlists) { playlist ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch(Dispatchers.IO) {
                                            val currentTracks = db.playlistTrackDao().getTracksForPlaylist(playlist.id)
                                            val position = currentTracks.size
                                            db.playlistTrackDao().upsert(
                                                PlaylistTrackEntity(
                                                    playlistId = playlist.id,
                                                    trackPath = track.filePath,
                                                    position = position
                                                )
                                            )
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                showAddToPlaylistDialog = null
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                Text(playlist.name, fontSize = 14.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk)
                            }
                            Divider(color = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Close", color = NeonMagenta)
                }
            }
        )
    }
}
}

// Extract Embedded Album Art Bitmap from audio file using MediaMetadataRetriever
fun getEmbeddedAlbumArt(path: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val art = retriever.embeddedPicture
        if (art != null) {
            BitmapFactory.decodeByteArray(art, 0, art.size)
        } else null
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

// Convert position in MS to MM:SS format
private fun formatPosition(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private val softLightBg = Color(0xFFF8FAFC)
