/*
 * Copyright (C) 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * This file is part of PLayMuse, a fork of VideoLAN VLC for Android.
 * Original copyright: Copyright (C) 1996-2026 VideoLAN and VLC Authors.
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.app.RemoteAction
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.IntentSender
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.videolan.vlc.data.MediaRepository
import org.videolan.vlc.model.MediaDeleteResult
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.isEligibleLocalMediaStoreItem
import org.videolan.vlc.model.parseValidMediaStoreUri
import org.videolan.vlc.viewmodel.VlcViewModel
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AboutAndMediaDeleteTest {

    class FakeMediaStoreContentProvider : ContentProvider() {
        companion object {
            val videoRows = mutableMapOf<Long, Triple<String, String, Long>>()
            val audioRows = mutableMapOf<Long, Triple<String, String, Long>>()
            var deleteCallCount = 0
            var videoQueryCount = 0
            var audioQueryCount = 0
            var securityExceptionToThrowOnDelete: SecurityException? = null

            fun reset() {
                videoRows.clear()
                audioRows.clear()
                deleteCallCount = 0
                videoQueryCount = 0
                audioQueryCount = 0
                securityExceptionToThrowOnDelete = null
            }
        }

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val path = uri.path.orEmpty()
            return if (path.contains("/video/media")) {
                videoQueryCount++
                val cursor = MatrixCursor(
                    arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.TITLE,
                        MediaStore.Video.Media.ARTIST,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.RESOLUTION
                    )
                )
                videoRows.forEach { (id, data) ->
                    cursor.addRow(arrayOf<Any?>(id, data.first, data.second, data.third, 12_500_000L, "1080p HD"))
                }
                cursor
            } else if (path.contains("/audio/media")) {
                audioQueryCount++
                val cursor = MatrixCursor(
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.SIZE
                    )
                )
                audioRows.forEach { (id, data) ->
                    cursor.addRow(arrayOf<Any?>(id, data.first, data.second, "Local Album", data.third, 4_200_000L))
                }
                cursor
            } else {
                MatrixCursor(emptyArray())
            }
        }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            deleteCallCount++
            securityExceptionToThrowOnDelete?.let { throw it }

            val id = try {
                ContentUris.parseId(uri)
            } catch (_: Exception) {
                -1L
            }
            if (id <= 0L) return 0

            val path = uri.path.orEmpty()
            return when {
                path.contains("/video/media/") -> if (videoRows.remove(id) != null) 1 else 0
                path.contains("/audio/media/") -> if (audioRows.remove(id) != null) 1 else 0
                else -> 0
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }

    private lateinit var viewModel: VlcViewModel

    private val localVideoItem = MediaItem(
        id = "device_vid_101",
        title = "Camera Holiday Clip",
        artist = "Device Camera",
        durationMs = 45_000L,
        uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 101L).toString(),
        mediaType = MediaType.VIDEO,
        sizeBytes = 12_500_000L,
        resolution = "1080p HD"
    )

    private val secondLocalVideoItem = MediaItem(
        id = "device_vid_102",
        title = "Evening Timelapse",
        artist = "Device Camera",
        durationMs = 30_000L,
        uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 102L).toString(),
        mediaType = MediaType.VIDEO,
        sizeBytes = 8_000_000L,
        resolution = "1080p HD"
    )

    private val localAudioItem = MediaItem(
        id = "device_aud_202",
        title = "Voice Memo Acoustic",
        artist = "Local Artist",
        album = "Local Album",
        durationMs = 120_000L,
        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 202L).toString(),
        mediaType = MediaType.AUDIO,
        sizeBytes = 4_200_000L,
        resolution = "Local Audio"
    )

    private val remoteSampleVideo = MediaItem(
        id = "sample_video_1",
        title = "Big Buck Bunny (1080p)",
        artist = "Blender Foundation",
        durationMs = 596_000L,
        uri = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        mediaType = MediaType.VIDEO
    )

    private val remoteSampleAudio = MediaItem(
        id = "sample_audio_1",
        title = "Midnight Jazz & Chill",
        artist = "PLayMuse Sound Lab",
        durationMs = 214_000L,
        uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        mediaType = MediaType.AUDIO
    )

    private fun createDummyIntentSender(): IntentSender {
        val context = RuntimeEnvironment.getApplication()
        val pendingIntent = PendingIntent.getActivity(
            context,
            99,
            Intent(Intent.ACTION_VIEW),
            PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent.intentSender
    }

    private fun drainMainThread() {
        repeat(4) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(15)
        }
        ShadowLooper.idleMainLooper()
    }

    @Before
    fun setUp() {
        FakeMediaStoreContentProvider.reset()
        FakeMediaStoreContentProvider.videoRows[101L] = Triple(localVideoItem.title, localVideoItem.artist, localVideoItem.durationMs)
        FakeMediaStoreContentProvider.videoRows[102L] = Triple(secondLocalVideoItem.title, secondLocalVideoItem.artist, secondLocalVideoItem.durationMs)
        FakeMediaStoreContentProvider.audioRows[202L] = Triple(localAudioItem.title, localAudioItem.artist, localAudioItem.durationMs)

        Robolectric.setupContentProvider(FakeMediaStoreContentProvider::class.java, MediaStore.AUTHORITY)

        val app = RuntimeEnvironment.getApplication()
        viewModel = VlcViewModel(app)
        drainMainThread()
        viewModel.repository = MediaRepository(
            context = app,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Main,
            systemDeleteIntentCreator = { _, _ -> createDummyIntentSender() }
        )
        viewModel.loadMedia()
        drainMainThread()
        FakeMediaStoreContentProvider.deleteCallCount = 0
        FakeMediaStoreContentProvider.videoQueryCount = 0
        FakeMediaStoreContentProvider.audioQueryCount = 0
    }

    @After
    fun tearDown() {
        viewModel.playerController.release()
        FakeMediaStoreContentProvider.reset()
    }

    // =========================================================================
    // PART A: Simplified About Screen & Legal Preservation Tests
    // =========================================================================

    @Test
    fun verifyAboutScreenDisplaysOnlyPlayMuseLogoNameAndVersionWithoutMisleadingCredits() {
        val context = RuntimeEnvironment.getApplication()

        // Verify application identity and version
        assertEquals("PLayMuse", BuildConfig.APP_NAME)
        assertEquals("3.6.0-playmuse", BuildConfig.VERSION_NAME)
        assertEquals("PLayMuse", context.getString(R.string.app_name))

        // Verify misleading "LibVLC Engine" is removed from BuildConfig and resources
        assertFalse(
            "BuildConfig.UPSTREAM_ATTRIBUTION must not claim LibVLC Engine is used",
            BuildConfig.UPSTREAM_ATTRIBUTION.contains("LibVLC Engine")
        )
        assertFalse(
            "R.string.upstream_vlc_attribution must not claim LibVLC Engine is used",
            context.getString(R.string.upstream_vlc_attribution).contains("LibVLC Engine")
        )

        // Verify AboutVlcDialog implementation in VlcModals.kt keeps only logo, name, and version
        val modalsFileCandidates = listOf(
            File("src/main/java/org/videolan/vlc/ui/modals/VlcModals.kt"),
            File("app/src/main/java/org/videolan/vlc/ui/modals/VlcModals.kt")
        )
        val modalsFile = modalsFileCandidates.firstOrNull { it.exists() }
        assertNotNull("VlcModals.kt must exist for source verification", modalsFile)
        val modalsSource = modalsFile!!.readText()
        val aboutDialogSection = modalsSource.substringAfter("fun AboutVlcDialog")

        assertTrue("AboutVlcDialog must reference R.drawable.ic_playmuse_logo", aboutDialogSection.contains("R.drawable.ic_playmuse_logo"))
        assertTrue("AboutVlcDialog must display PLayMuse application name", aboutDialogSection.contains("\"PLayMuse\""))
        assertTrue("AboutVlcDialog must display BuildConfig.VERSION_NAME", aboutDialogSection.contains("BuildConfig.VERSION_NAME"))

        // Verify forbidden credits and misleading engine strings are absent from AboutVlcDialog
        assertFalse("AboutVlcDialog must not display LibVLC Engine", aboutDialogSection.contains("LibVLC Engine"))
        assertFalse("AboutVlcDialog must not display VideoLAN / VLC Authors", aboutDialogSection.contains("VideoLAN / VLC Authors"))
        assertFalse("AboutVlcDialog must not display upstream_vlc_attribution", aboutDialogSection.contains("upstream_vlc_attribution"))
        assertFalse("AboutVlcDialog must not display Developer: Naveed Mushtaq", aboutDialogSection.contains("Developer: Naveed Mushtaq"))
        assertFalse("AboutVlcDialog must not display Open Source Licenses & Legal Notices", aboutDialogSection.contains("Open Source Licenses & Legal Notices"))
        assertFalse("AboutVlcDialog must not display Copyright © 2026 DarkSkullCorporation", aboutDialogSection.contains("Copyright © 2026 DarkSkullCorporation"))

        // Verify mandatory repository legal preservation (COPYING and source header notices)
        val copyingFile = listOf(File("../COPYING"), File("COPYING")).firstOrNull { it.exists() }
        assertNotNull("Repository COPYING license file must be preserved", copyingFile)
        assertTrue("Source file GPLv2+ header must be preserved", modalsSource.contains("GNU General Public License v2 or later"))
    }

    // =========================================================================
    // PART B, C, D.1: Media Delete Eligibility Validation Tests
    // =========================================================================

    @Test
    fun verifyEligibleLocalMediaStoreVideoAndAudioItemsAreRecognizedAsDeletable() {
        assertTrue("Local MediaStore video item must be eligible for deletion", isEligibleLocalMediaStoreItem(localVideoItem))
        assertTrue("Local MediaStore audio item must be eligible for deletion", isEligibleLocalMediaStoreItem(localAudioItem))
        assertNotNull(parseValidMediaStoreUri(localVideoItem.uri, MediaType.VIDEO))
        assertNotNull(parseValidMediaStoreUri(localAudioItem.uri, MediaType.AUDIO))
    }

    @Test
    fun verifyRemoteHttpHttpsSamplesAndInvalidUrisAreNeverDeletable() {
        assertFalse("Remote HLS/HTTPS sample video must NOT be eligible for deletion", isEligibleLocalMediaStoreItem(remoteSampleVideo))
        assertFalse("Remote HTTPS sample audio must NOT be eligible for deletion", isEligibleLocalMediaStoreItem(remoteSampleAudio))

        val rtspItem = remoteSampleVideo.copy(uri = "rtsp://192.168.1.10:554/live.sdp")
        assertFalse("RTSP stream must NOT be eligible for deletion", isEligibleLocalMediaStoreItem(rtspItem))

        val thirdPartyContentItem = localVideoItem.copy(uri = "content://com.android.providers.downloads.documents/document/42")
        assertFalse("Non-MediaStore content authority must NOT be eligible for deletion", isEligibleLocalMediaStoreItem(thirdPartyContentItem))

        val tableLevelUriItem = localVideoItem.copy(uri = "content://media/external/video/media")
        assertFalse("Table-level MediaStore URI without numeric ID must NOT be eligible for deletion", isEligibleLocalMediaStoreItem(tableLevelUriItem))

        val mismatchedTypeItem = localVideoItem.copy(mediaType = MediaType.AUDIO)
        assertFalse("Video MediaStore URI on an AUDIO item must be rejected", isEligibleLocalMediaStoreItem(mismatchedTypeItem))
    }

    // =========================================================================
    // PART D.2: Confirmation Cancel Path Tests
    // =========================================================================

    @Test
    fun verifyConfirmDeleteCancelPathLeavesMediaListUnchanged() {
        val initialVideoCount = viewModel.videos.value.size
        val initialAudioCount = viewModel.audioTracks.value.size
        assertTrue(viewModel.videos.value.any { it.id == localVideoItem.id })

        viewModel.requestDeleteMedia(localVideoItem)
        assertEquals(localVideoItem, viewModel.pendingDeleteMedia.value)

        // User taps Cancel in confirmation dialog
        viewModel.cancelDeleteMedia()
        assertNull(viewModel.pendingDeleteMedia.value)
        assertEquals(initialVideoCount, viewModel.videos.value.size)
        assertEquals(initialAudioCount, viewModel.audioTracks.value.size)
        assertTrue(viewModel.videos.value.any { it.id == localVideoItem.id })
        assertEquals(0, FakeMediaStoreContentProvider.deleteCallCount)
    }

    // =========================================================================
    // PART D.3, E, G: Successful Video & Audio Deletion + Targeted Refresh Tests
    // =========================================================================

    @Test
    fun verifySuccessfulVideoDeleteOnApi30PlusRemovesVideoWithoutExtraContentResolverDeleteAndPreservesAudioList() {
        assertTrue(viewModel.videos.value.any { it.id == localVideoItem.id })
        val audioSnapshotBefore = viewModel.audioTracks.value
        val audioQueriesBefore = FakeMediaStoreContentProvider.audioQueryCount

        // 1. User requests delete and confirms dialog on API 35 (Android 11+ createDeleteRequest path)
        viewModel.requestDeleteMedia(localVideoItem)
        viewModel.confirmDeleteMedia(sdkInt = Build.VERSION_CODES.VANILLA_ICE_CREAM, hasLegacyWritePermission = false)
        drainMainThread()

        val consentEvent = viewModel.pendingConsentEvent.value
        assertNotNull("API 30+ must emit RequiresSystemConsent via MediaStore.createDeleteRequest", consentEvent)
        assertEquals(localVideoItem.id, consentEvent!!.mediaItem.id)
        assertFalse("API 30+ createDeleteRequest must not require a second ContentResolver.delete", consentEvent.requiresFollowUpDelete)

        // Simulate MediaStore system dialog deleting the row upon user approval (RESULT_OK)
        FakeMediaStoreContentProvider.videoRows.remove(101L)
        viewModel.onSystemDeleteConsentResult(granted = true)
        drainMainThread()

        // Verify no redundant second ContentResolver.delete call was made on API 30+
        assertEquals(0, FakeMediaStoreContentProvider.deleteCallCount)

        // Verify deleted video is removed from video list, while second video and sample videos remain
        assertFalse("Deleted video must be removed from videos state", viewModel.videos.value.any { it.id == localVideoItem.id })
        assertTrue("Other local videos must remain intact", viewModel.videos.value.any { it.id == secondLocalVideoItem.id })
        assertEquals("Deleted \"${localVideoItem.title}\"", viewModel.snackbarMessage.value)

        // Verify audio library was NOT re-queried or modified during video deletion
        assertEquals(audioQueriesBefore, FakeMediaStoreContentProvider.audioQueryCount)
        assertEquals(audioSnapshotBefore, viewModel.audioTracks.value)
    }

    @Test
    fun verifySuccessfulAudioDeleteOnApi28RemovesAudioAndDoesNotAffectVideoList() {
        assertTrue(viewModel.audioTracks.value.any { it.id == localAudioItem.id })
        val videoSnapshotBefore = viewModel.videos.value
        val videoQueriesBefore = FakeMediaStoreContentProvider.videoQueryCount

        viewModel.requestDeleteMedia(localAudioItem)
        viewModel.confirmDeleteMedia(sdkInt = Build.VERSION_CODES.P, hasLegacyWritePermission = true)
        drainMainThread()

        assertEquals(1, FakeMediaStoreContentProvider.deleteCallCount)
        assertFalse("Deleted audio track must be removed from audioTracks state", viewModel.audioTracks.value.any { it.id == localAudioItem.id })
        assertEquals("Deleted \"${localAudioItem.title}\"", viewModel.snackbarMessage.value)

        // Verify video library was NOT re-queried or modified during audio deletion
        assertEquals(videoQueriesBefore, FakeMediaStoreContentProvider.videoQueryCount)
        assertEquals(videoSnapshotBefore, viewModel.videos.value)
    }

    @Test
    fun verifyApi29RecoverableSecurityExceptionLaunchesConsentAndCompletesDeleteOnApproval() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val remoteAction = RemoteAction(
            Icon.createWithResource(context, R.drawable.ic_playmuse_logo),
            "Delete",
            "Confirm delete",
            PendingIntent.getActivity(context, 10, Intent(), PendingIntent.FLAG_IMMUTABLE)
        )
        FakeMediaStoreContentProvider.securityExceptionToThrowOnDelete =
            RecoverableSecurityException(SecurityException("Need user approval"), "Approve delete", remoteAction)

        viewModel.requestDeleteMedia(localVideoItem)
        viewModel.confirmDeleteMedia(sdkInt = Build.VERSION_CODES.Q, hasLegacyWritePermission = false)
        drainMainThread()

        val consentEvent = viewModel.pendingConsentEvent.value
        assertNotNull("API 29 RecoverableSecurityException must emit RequiresSystemConsent", consentEvent)
        assertTrue("API 29 RecoverableSecurityException requires follow-up ContentResolver.delete after approval", consentEvent!!.requiresFollowUpDelete)

        // Now user approves the system dialog and security exception clears
        FakeMediaStoreContentProvider.securityExceptionToThrowOnDelete = null
        viewModel.onSystemDeleteConsentResult(granted = true)
        drainMainThread()

        assertFalse("Video must be removed after API 29 consent approval", viewModel.videos.value.any { it.id == localVideoItem.id })
        assertEquals("Deleted \"${localVideoItem.title}\"", viewModel.snackbarMessage.value)
    }

    // =========================================================================
    // PART D.3 & E: Permission Denial & System Consent Cancellation Tests
    // =========================================================================

    @Test
    fun verifySystemConsentDenialAndLegacyPermissionDenialKeepMediaItemAndNeverReportSuccess() {
        // 1. System consent dialog cancelled/denied on API 30+
        viewModel.requestDeleteMedia(localVideoItem)
        viewModel.confirmDeleteMedia(sdkInt = Build.VERSION_CODES.R, hasLegacyWritePermission = false)
        drainMainThread()

        assertNotNull(viewModel.pendingConsentEvent.value)
        viewModel.onSystemDeleteConsentResult(granted = false)
        drainMainThread()

        assertTrue("Cancelled system consent must keep video in library", viewModel.videos.value.any { it.id == localVideoItem.id })
        assertEquals("Deletion cancelled.", viewModel.snackbarMessage.value)
        assertFalse(viewModel.isDeleteInProgress.value)

        // 2. Legacy WRITE_EXTERNAL_STORAGE denied on API 28
        viewModel.requestDeleteMedia(localAudioItem)
        viewModel.confirmDeleteMedia(sdkInt = Build.VERSION_CODES.P, hasLegacyWritePermission = false)
        drainMainThread()

        assertEquals(localAudioItem, viewModel.pendingLegacyPermissionItem.value)
        viewModel.onLegacyWritePermissionResult(granted = false, sdkInt = Build.VERSION_CODES.P)
        drainMainThread()

        assertTrue("Denied legacy storage permission must keep audio track in library", viewModel.audioTracks.value.any { it.id == localAudioItem.id })
        assertEquals("Storage permission is required to delete media files.", viewModel.snackbarMessage.value)
        assertFalse(viewModel.isDeleteInProgress.value)
    }

    // =========================================================================
    // PART F: Active Playback & Queue Safety Tests
    // =========================================================================

    @Test
    fun verifyDeletingCurrentlyPlayingItemStopsAndClearsPlaybackWhileUnrelatedPlaybackContinues() {
        // 1. Start playing localVideoItem in full-screen video player with a 2-item queue
        viewModel.playVideo(localVideoItem)
        assertTrue(viewModel.isFullScreenVideo.value)
        assertEquals(localVideoItem.id, viewModel.playerController.currentMedia.value?.id)
        assertTrue(viewModel.playerController.playlist.value.any { it.id == localVideoItem.id })

        // Delete the currently playing video
        viewModel.requestDeleteMedia(localVideoItem)
        viewModel.confirmDeleteMedia(sdkInt = Build.VERSION_CODES.P, hasLegacyWritePermission = true)
        drainMainThread()

        // Verify playback stopped, currentMedia cleared, full-screen closed, and deleted item removed from playlist
        assertNull("Current media must be cleared when active item is deleted", viewModel.playerController.currentMedia.value)
        assertFalse("Player must stop playing when active item is deleted", viewModel.playerController.isPlaying.value)
        assertFalse("Full-screen video player must close when active video is deleted", viewModel.isFullScreenVideo.value)
        assertFalse("Deleted item must be removed from active playlist", viewModel.playerController.playlist.value.any { it.id == localVideoItem.id })
        assertTrue("Other playlist items must remain intact", viewModel.playerController.playlist.value.any { it.id == secondLocalVideoItem.id })

        // 2. Play remoteSampleAudio and delete localAudioItem (unrelated item)
        viewModel.playAudio(remoteSampleAudio)
        assertTrue(viewModel.isNowPlayingExpanded.value)
        assertEquals(remoteSampleAudio.id, viewModel.playerController.currentMedia.value?.id)

        viewModel.requestDeleteMedia(localAudioItem)
        viewModel.confirmDeleteMedia(sdkInt = Build.VERSION_CODES.P, hasLegacyWritePermission = true)
        drainMainThread()

        // Verify unrelated playback and NowPlaying screen remain active and uninterrupted
        assertEquals(remoteSampleAudio.id, viewModel.playerController.currentMedia.value?.id)
        assertTrue("Now Playing screen must remain open when a different track is deleted", viewModel.isNowPlayingExpanded.value)
        assertFalse("Deleted audio track must be removed from playlist", viewModel.playerController.playlist.value.any { it.id == localAudioItem.id })
    }
}
