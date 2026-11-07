/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.videolan.vlc.data.FileItem
import org.videolan.vlc.data.MediaRepository
import org.videolan.vlc.model.AspectRatioMode
import org.videolan.vlc.model.EqualizerPreset
import org.videolan.vlc.model.MediaDeleteResult
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.VlcSettings
import org.videolan.vlc.model.isEligibleLocalMediaStoreItem
import org.videolan.vlc.player.VlcPlayerController

enum class VlcTab {
    VIDEO,
    AUDIO,
    BROWSE,
    MORE
}

enum class ActiveModal {
    NONE,
    EQUALIZER,
    SLEEP_TIMER,
    PLAYBACK_SPEED,
    MEDIA_INFO,
    ABOUT_VLC
}

class VlcViewModel(application: Application) : AndroidViewModel(application) {

    var repository = MediaRepository(application)
        internal set
    val playerController = VlcPlayerController(application)

    // Navigation & Screen States
    private val _currentTab = MutableStateFlow(VlcTab.VIDEO)
    val currentTab: StateFlow<VlcTab> = _currentTab.asStateFlow()

    private val _isFullScreenVideo = MutableStateFlow(false)
    val isFullScreenVideo: StateFlow<Boolean> = _isFullScreenVideo.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _activeModal = MutableStateFlow(ActiveModal.NONE)
    val activeModal: StateFlow<ActiveModal> = _activeModal.asStateFlow()

    private val _selectedMediaForInfo = MutableStateFlow<MediaItem?>(null)
    val selectedMediaForInfo: StateFlow<MediaItem?> = _selectedMediaForInfo.asStateFlow()

    // Media deletion states
    private val _pendingDeleteMedia = MutableStateFlow<MediaItem?>(null)
    val pendingDeleteMedia: StateFlow<MediaItem?> = _pendingDeleteMedia.asStateFlow()

    private val _isDeleteInProgress = MutableStateFlow(false)
    val isDeleteInProgress: StateFlow<Boolean> = _isDeleteInProgress.asStateFlow()

    private val _pendingConsentEvent = MutableStateFlow<MediaDeleteResult.RequiresSystemConsent?>(null)
    val pendingConsentEvent: StateFlow<MediaDeleteResult.RequiresSystemConsent?> = _pendingConsentEvent.asStateFlow()

    private val _pendingLegacyPermissionItem = MutableStateFlow<MediaItem?>(null)
    val pendingLegacyPermissionItem: StateFlow<MediaItem?> = _pendingLegacyPermissionItem.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Search and display
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridMode = MutableStateFlow(true)
    val isGridMode: StateFlow<Boolean> = _isGridMode.asStateFlow()

    private val _selectedAudioCategory = MutableStateFlow("Tracks")
    val selectedAudioCategory: StateFlow<String> = _selectedAudioCategory.asStateFlow()

    // Video aspect ratio
    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    // Raw media lists
    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<MediaItem>>(emptyList())
    val audioTracks: StateFlow<List<MediaItem>> = _audioTracks.asStateFlow()

    // Browsing
    private val _currentBrowsePath = MutableStateFlow<String?>(null)
    val currentBrowsePath: StateFlow<String?> = _currentBrowsePath.asStateFlow()

    private val _browseFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val browseFiles: StateFlow<List<FileItem>> = _browseFiles.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(VlcSettings())
    val settings: StateFlow<VlcSettings> = _settings.asStateFlow()

    // Equalizer presets
    val presets: List<EqualizerPreset> = repository.equalizerPresets
    private val _selectedPresetName = MutableStateFlow("Flat")
    val selectedPresetName: StateFlow<String> = _selectedPresetName.asStateFlow()

    // Filtered Video List
    val filteredVideos: StateFlow<List<MediaItem>> = combine(_videos, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Audio List
    val filteredAudioTracks: StateFlow<List<MediaItem>> = combine(_audioTracks, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadMedia()
        browsePath(null)
    }

    fun loadMedia() {
        viewModelScope.launch {
            _videos.value = repository.getVideos()
            _audioTracks.value = repository.getAudioTracks()
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _videos.value = repository.getVideos()
        }
    }

    fun loadAudioTracks() {
        viewModelScope.launch {
            _audioTracks.value = repository.getAudioTracks()
        }
    }

    fun requestDeleteMedia(item: MediaItem) {
        if (_isDeleteInProgress.value) return
        if (!isEligibleLocalMediaStoreItem(item)) {
            _snackbarMessage.value = "Only local device media files can be deleted."
            return
        }
        _pendingDeleteMedia.value = item
    }

    fun cancelDeleteMedia() {
        _pendingDeleteMedia.value = null
    }

    fun confirmDeleteMedia(
        sdkInt: Int = Build.VERSION.SDK_INT,
        hasLegacyWritePermission: Boolean = repository.hasLegacyWriteStoragePermission()
    ) {
        val item = _pendingDeleteMedia.value ?: return
        if (_isDeleteInProgress.value) return
        _pendingDeleteMedia.value = null
        _isDeleteInProgress.value = true

        viewModelScope.launch {
            val result = repository.deleteMediaItem(
                item = item,
                sdkInt = sdkInt,
                hasLegacyWritePermission = hasLegacyWritePermission
            )
            handleMediaDeleteResult(result)
        }
    }

    fun onSystemDeleteConsentResult(granted: Boolean) {
        val consent = _pendingConsentEvent.value ?: return
        _pendingConsentEvent.value = null

        if (!granted) {
            _isDeleteInProgress.value = false
            _snackbarMessage.value = "Deletion cancelled."
            return
        }

        viewModelScope.launch {
            val result = repository.completePostConsentDelete(
                item = consent.mediaItem,
                requiresFollowUpDelete = consent.requiresFollowUpDelete
            )
            handleMediaDeleteResult(result)
        }
    }

    fun onLegacyWritePermissionResult(
        granted: Boolean,
        sdkInt: Int = Build.VERSION.SDK_INT
    ) {
        val item = _pendingLegacyPermissionItem.value ?: return
        _pendingLegacyPermissionItem.value = null

        if (!granted) {
            _isDeleteInProgress.value = false
            _snackbarMessage.value = "Storage permission is required to delete media files."
            return
        }

        viewModelScope.launch {
            val result = repository.deleteMediaItem(
                item = item,
                sdkInt = sdkInt,
                hasLegacyWritePermission = true
            )
            handleMediaDeleteResult(result)
        }
    }

    private suspend fun handleMediaDeleteResult(result: MediaDeleteResult) {
        when (result) {
            is MediaDeleteResult.Deleted -> {
                val deletedItem = result.mediaItem
                val wasPlayingDeleted = playerController.onMediaItemDeleted(deletedItem)
                if (wasPlayingDeleted) {
                    _isFullScreenVideo.value = false
                    _isNowPlayingExpanded.value = false
                }
                if (_selectedMediaForInfo.value?.id == deletedItem.id ||
                    _selectedMediaForInfo.value?.uri == deletedItem.uri
                ) {
                    closeModal()
                }

                // Targeted library refresh: only refresh the affected library
                val matchesDeleted: (MediaItem) -> Boolean = { candidate ->
                    candidate.id == deletedItem.id || candidate.uri == deletedItem.uri
                }
                if (deletedItem.mediaType == MediaType.VIDEO) {
                    _videos.value = _videos.value.filterNot(matchesDeleted)
                    _videos.value = repository.getVideos().filterNot(matchesDeleted)
                } else {
                    _audioTracks.value = _audioTracks.value.filterNot(matchesDeleted)
                    _audioTracks.value = repository.getAudioTracks().filterNot(matchesDeleted)
                }

                _isDeleteInProgress.value = false
                _snackbarMessage.value = "Deleted \"${deletedItem.title}\""
            }

            is MediaDeleteResult.RequiresSystemConsent -> {
                _pendingConsentEvent.value = result
            }

            is MediaDeleteResult.RequiresLegacyWritePermission -> {
                _pendingLegacyPermissionItem.value = result.mediaItem
            }

            is MediaDeleteResult.Cancelled -> {
                _isDeleteInProgress.value = false
                _snackbarMessage.value = "Deletion cancelled."
            }

            is MediaDeleteResult.PermissionDenied -> {
                _isDeleteInProgress.value = false
                _snackbarMessage.value = result.message
            }

            is MediaDeleteResult.NotFoundOrIneligible -> {
                _isDeleteInProgress.value = false
                _snackbarMessage.value = result.message
            }

            is MediaDeleteResult.Error -> {
                _isDeleteInProgress.value = false
                _snackbarMessage.value = result.message
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun setTab(tab: VlcTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleGridMode() {
        _isGridMode.value = !_isGridMode.value
    }

    fun setAudioCategory(category: String) {
        _selectedAudioCategory.value = category
    }

    fun setFullScreenVideo(fullScreen: Boolean) {
        _isFullScreenVideo.value = fullScreen
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun showModal(modal: ActiveModal, mediaItem: MediaItem? = null) {
        _selectedMediaForInfo.value = mediaItem
        _activeModal.value = modal
    }

    fun closeModal() {
        _activeModal.value = ActiveModal.NONE
        _selectedMediaForInfo.value = null
    }

    fun cycleAspectRatio() {
        val next = when (_aspectRatioMode.value) {
            AspectRatioMode.FIT -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.ZOOM
            AspectRatioMode.ZOOM -> AspectRatioMode.RATIO_16_9
            AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_4_3
            AspectRatioMode.RATIO_4_3 -> AspectRatioMode.FIT
        }
        _aspectRatioMode.value = next
    }

    fun playVideo(item: MediaItem) {
        playerController.playMedia(item, _videos.value)
        _isFullScreenVideo.value = true
    }

    fun playAudio(item: MediaItem) {
        playerController.playMedia(item, _audioTracks.value)
        _isNowPlayingExpanded.value = true
    }

    fun browsePath(path: String?) {
        viewModelScope.launch {
            _currentBrowsePath.value = path
            _browseFiles.value = repository.browseDirectory(path)
        }
    }

    fun navigateUpBrowse() {
        val current = _currentBrowsePath.value ?: return
        val parent = java.io.File(current).parent
        browsePath(parent)
    }

    fun applyPreset(preset: EqualizerPreset) {
        _selectedPresetName.value = preset.name
        playerController.applyPreset(preset.bandsDb)
    }

    fun updateSettings(newSettings: VlcSettings) {
        _settings.value = newSettings
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
