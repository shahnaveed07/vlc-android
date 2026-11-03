/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.videolan.vlc.theme.VLCTheme
import org.videolan.vlc.theme.VlcOrangePrimary
import org.videolan.vlc.ui.audio.AudioScreen
import org.videolan.vlc.ui.audio.NowPlayingScreen
import org.videolan.vlc.ui.browse.BrowseScreen
import org.videolan.vlc.ui.components.VlcMiniPlayer
import org.videolan.vlc.ui.components.VlcTopBar
import org.videolan.vlc.ui.equalizer.EqualizerScreen
import org.videolan.vlc.ui.modals.*
import org.videolan.vlc.ui.settings.SettingsScreen
import org.videolan.vlc.ui.stream.StreamScreen
import org.videolan.vlc.ui.video.VideoListScreen
import org.videolan.vlc.ui.video.VideoPlayerScreen
import org.videolan.vlc.viewmodel.ActiveModal
import org.videolan.vlc.viewmodel.VlcTab
import org.videolan.vlc.viewmodel.VlcViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VlcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDark = when (settings.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> true
            }

            VLCTheme(darkTheme = isDark) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: VlcViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isFullScreenVideo by viewModel.isFullScreenVideo.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val activeModal by viewModel.activeModal.collectAsState()
    val selectedMediaForInfo by viewModel.selectedMediaForInfo.collectAsState()

    val currentPlayingMedia by viewModel.playerController.currentMedia.collectAsState()
    val isPlaying by viewModel.playerController.isPlaying.collectAsState()
    val currentPosition by viewModel.playerController.currentPosition.collectAsState()
    val duration by viewModel.playerController.duration.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridMode by viewModel.isGridMode.collectAsState()
    val selectedCategory by viewModel.selectedAudioCategory.collectAsState()
    val aspectRatioMode by viewModel.aspectRatioMode.collectAsState()

    val videos by viewModel.filteredVideos.collectAsState()
    val audioTracks by viewModel.filteredAudioTracks.collectAsState()
    val streamBookmarks by viewModel.streamBookmarks.collectAsState()
    val currentBrowsePath by viewModel.currentBrowsePath.collectAsState()
    val browseFiles by viewModel.browseFiles.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Request permissions for local media
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadMedia()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isFullScreenVideo && !isNowPlayingExpanded && activeModal == ActiveModal.NONE) {
                VlcTopBar(
                    title = when (currentTab) {
                        VlcTab.VIDEO -> "Videos"
                        VlcTab.AUDIO -> "Audio"
                        VlcTab.STREAM -> "Streams"
                        VlcTab.BROWSE -> "Browse"
                        VlcTab.MORE -> "More"
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    isGridMode = isGridMode,
                    onToggleGridMode = { viewModel.toggleGridMode() },
                    onOpenEqualizer = { viewModel.showModal(ActiveModal.EQUALIZER) },
                    onOpenAbout = { viewModel.showModal(ActiveModal.ABOUT_VLC) }
                )
            }
        },
        bottomBar = {
            if (!isFullScreenVideo && !isNowPlayingExpanded && activeModal == ActiveModal.NONE) {
                Column {
                    // Mini Player bar if media loaded
                    if (currentPlayingMedia != null) {
                        VlcMiniPlayer(
                            mediaItem = currentPlayingMedia!!,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosition,
                            durationMs = duration,
                            onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                            onPlayNext = { viewModel.playerController.playNext() },
                            onExpand = {
                                if (currentPlayingMedia!!.mediaType == org.videolan.vlc.model.MediaType.AUDIO) {
                                    viewModel.setNowPlayingExpanded(true)
                                } else {
                                    viewModel.setFullScreenVideo(true)
                                }
                            }
                        )
                    }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 4.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == VlcTab.VIDEO,
                            onClick = { viewModel.setTab(VlcTab.VIDEO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == VlcTab.VIDEO) Icons.Default.Movie else Icons.Outlined.Movie,
                                    contentDescription = "Videos"
                                )
                            },
                            label = { Text("Video") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VlcOrangePrimary
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == VlcTab.AUDIO,
                            onClick = { viewModel.setTab(VlcTab.AUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == VlcTab.AUDIO) Icons.Default.Audiotrack else Icons.Outlined.Audiotrack,
                                    contentDescription = "Audio"
                                )
                            },
                            label = { Text("Audio") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VlcOrangePrimary
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == VlcTab.STREAM,
                            onClick = { viewModel.setTab(VlcTab.STREAM) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == VlcTab.STREAM) Icons.Default.Podcasts else Icons.Outlined.Podcasts,
                                    contentDescription = "Streams"
                                )
                            },
                            label = { Text("Streams") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VlcOrangePrimary
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == VlcTab.BROWSE,
                            onClick = { viewModel.setTab(VlcTab.BROWSE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == VlcTab.BROWSE) Icons.Default.Folder else Icons.Outlined.Folder,
                                    contentDescription = "Browse"
                                )
                            },
                            label = { Text("Browse") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VlcOrangePrimary
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == VlcTab.MORE,
                            onClick = { viewModel.setTab(VlcTab.MORE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == VlcTab.MORE) Icons.Default.MoreHoriz else Icons.Outlined.MoreHoriz,
                                    contentDescription = "More"
                                )
                            },
                            label = { Text("More") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                indicatorColor = VlcOrangePrimary
                            )
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
            when (currentTab) {
                VlcTab.VIDEO -> VideoListScreen(
                    videos = videos,
                    isGridMode = isGridMode,
                    onVideoClick = { viewModel.playVideo(it) },
                    onPlayAsAudio = { viewModel.playAudio(it) },
                    onShowInfo = { viewModel.showModal(ActiveModal.MEDIA_INFO, it) }
                )

                VlcTab.AUDIO -> AudioScreen(
                    tracks = audioTracks,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setAudioCategory(it) },
                    onTrackClick = { viewModel.playAudio(it) },
                    onShuffleAll = {
                        if (audioTracks.isNotEmpty()) {
                            viewModel.playerController.playQueue(audioTracks.shuffled())
                            viewModel.setNowPlayingExpanded(true)
                        }
                    },
                    onShowInfo = { viewModel.showModal(ActiveModal.MEDIA_INFO, it) }
                )

                VlcTab.STREAM -> StreamScreen(
                    bookmarks = streamBookmarks,
                    onPlayStream = { url, title -> viewModel.playStream(url, title) },
                    onAddBookmark = { title, url -> viewModel.addStreamBookmark(title, url) },
                    onRemoveBookmark = { viewModel.removeStream(it) }
                )

                VlcTab.BROWSE -> BrowseScreen(
                    currentPath = currentBrowsePath,
                    files = browseFiles,
                    onFolderClick = { viewModel.browsePath(it) },
                    onMediaClick = {
                        if (it.mediaType == org.videolan.vlc.model.MediaType.AUDIO) {
                            viewModel.playAudio(it)
                        } else {
                            viewModel.playVideo(it)
                        }
                    },
                    onNavigateUp = { viewModel.navigateUpBrowse() }
                )

                VlcTab.MORE -> SettingsScreen(
                    settings = settings,
                    onSettingsChanged = { viewModel.updateSettings(it) },
                    onOpenEqualizer = { viewModel.showModal(ActiveModal.EQUALIZER) },
                    onOpenAbout = { viewModel.showModal(ActiveModal.ABOUT_VLC) },
                    onRefreshMedia = { viewModel.loadMedia() }
                )
            }
        }
    }

    // Full Screen Video Player Overlay
    if (isFullScreenVideo) {
        VideoPlayerScreen(
            playerController = viewModel.playerController,
            aspectRatioMode = aspectRatioMode,
            onCycleAspectRatio = { viewModel.cycleAspectRatio() },
            onOpenSpeed = { viewModel.showModal(ActiveModal.PLAYBACK_SPEED) },
            onOpenEqualizer = { viewModel.showModal(ActiveModal.EQUALIZER) },
            onClose = { viewModel.setFullScreenVideo(false) }
        )
    }

    // Now Playing Audio Overlay Sheet
    if (isNowPlayingExpanded) {
        NowPlayingScreen(
            playerController = viewModel.playerController,
            onOpenSpeed = { viewModel.showModal(ActiveModal.PLAYBACK_SPEED) },
            onOpenSleepTimer = { viewModel.showModal(ActiveModal.SLEEP_TIMER) },
            onOpenEqualizer = { viewModel.showModal(ActiveModal.EQUALIZER) },
            onClose = { viewModel.setNowPlayingExpanded(false) }
        )
    }

    // Equalizer Screen Modal
    if (activeModal == ActiveModal.EQUALIZER) {
        EqualizerScreen(
            playerController = viewModel.playerController,
            presets = viewModel.presets,
            selectedPresetName = viewModel.selectedPresetName.collectAsState().value,
            onSelectPreset = { viewModel.applyPreset(it) },
            onClose = { viewModel.closeModal() }
        )
    }

    // Speed Modal
    if (activeModal == ActiveModal.PLAYBACK_SPEED) {
        SpeedDialog(
            currentSpeed = viewModel.playerController.playbackSpeed.collectAsState().value,
            onSpeedSelected = { viewModel.playerController.setSpeed(it) },
            onDismiss = { viewModel.closeModal() }
        )
    }

    // Sleep Timer Modal
    if (activeModal == ActiveModal.SLEEP_TIMER) {
        SleepTimerDialog(
            currentRemainingSec = viewModel.playerController.sleepTimerSeconds.collectAsState().value,
            onSetMinutes = { viewModel.playerController.setSleepTimer(it) },
            onCancelTimer = { viewModel.playerController.cancelSleepTimer() },
            onDismiss = { viewModel.closeModal() }
        )
    }

    // Media Info Modal
    if (activeModal == ActiveModal.MEDIA_INFO && selectedMediaForInfo != null) {
        MediaInfoDialog(
            mediaItem = selectedMediaForInfo!!,
            onDismiss = { viewModel.closeModal() }
        )
    }

    // About PLayMuse Modal
    if (activeModal == ActiveModal.ABOUT_VLC) {
        AboutVlcDialog(
            onDismiss = { viewModel.closeModal() }
        )
    }
}
