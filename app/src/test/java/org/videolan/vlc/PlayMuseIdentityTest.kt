/*
 * Copyright (C) 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * This file is part of PLayMuse, a fork of VideoLAN VLC for Android.
 * Original copyright: Copyright (C) 1996-2026 VideoLAN and VLC Authors.
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.videolan.vlc.data.MediaRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayMuseIdentityTest {

    @Test
    fun verifyBuildConfigApplicationIdentity() {
        assertEquals("com.dsc.playmuse", BuildConfig.APPLICATION_ID)
        assertEquals("PLayMuse", BuildConfig.APP_NAME)
        assertEquals("com.dsc.playmuse", BuildConfig.APP_ID)
        assertEquals("DarkSkullCorporation", BuildConfig.COPYRIGHT_HOLDER)
        assertEquals("Naveed Mushtaq", BuildConfig.DEVELOPER_NAME)
        assertTrue(BuildConfig.UPSTREAM_ATTRIBUTION.contains("VideoLAN"))
    }

    @Test
    fun verifyAndroidResourceIdentityAndCoexistence() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals("com.dsc.playmuse", context.packageName)
        assertFalse("Package must not collide with org.videolan.vlc", context.packageName == "org.videolan.vlc")

        assertEquals("PLayMuse", context.getString(R.string.app_name))
        assertEquals("com.dsc.playmuse", context.getString(R.string.app_package_id))
        assertEquals("Naveed Mushtaq", context.getString(R.string.developer_name))
        assertEquals("DarkSkullCorporation", context.getString(R.string.copyright_holder))
        assertEquals("Copyright © 2026 DarkSkullCorporation", context.getString(R.string.copyright_notice))
        assertTrue(context.getString(R.string.upstream_vlc_attribution).contains("VideoLAN"))
    }

    @Test
    fun verifyPlayMuseBrandingDrawablesExist() {
        val context = RuntimeEnvironment.getApplication()
        val logoDrawable = context.getDrawable(R.drawable.ic_playmuse_logo)
        val fgDrawable = context.getDrawable(R.drawable.ic_launcher_foreground)
        val bgDrawable = context.getDrawable(R.drawable.ic_launcher_background)
        assertNotNull("PLayMuse in-app logo vector must resolve", logoDrawable)
        assertNotNull("PLayMuse adaptive launcher foreground must resolve", fgDrawable)
        assertNotNull("PLayMuse adaptive launcher background must resolve", bgDrawable)
    }

    @Test
    fun verifyMediaRepositoryAndEqualizerFunctionalityPreserved() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repository = MediaRepository(context)
        val videos = repository.getVideos()
        val audioTracks = repository.getAudioTracks()
        val streams = repository.getStreamBookmarks()

        assertTrue("Sample video catalog must remain populated", videos.isNotEmpty())
        assertTrue("Sample audio catalog must remain populated", audioTracks.isNotEmpty())
        assertTrue(
            "PLayMuse Sound Lab artist attribution should be present in sample audio",
            audioTracks.any { it.artist == "PLayMuse Sound Lab" }
        )
        assertTrue("Default network streams must remain populated", streams.isNotEmpty())
        assertTrue("10-preset graphic equalizer presets must remain intact", repository.equalizerPresets.size >= 10)
    }
}
