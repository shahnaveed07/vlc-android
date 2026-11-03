/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.model

enum class MediaType {
    VIDEO,
    AUDIO,
    STREAM
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit to screen"),
    FILL("Fill screen"),
    ZOOM("100% Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uri: String,
    val mediaType: MediaType = MediaType.VIDEO,
    val thumbnailUri: String? = null,
    val sizeBytes: Long = 0L,
    val resolution: String = "HD",
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L
) {
    val durationFormatted: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0) return ""
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.1f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

data class StreamBookmark(
    val title: String,
    val url: String,
    val category: String = "Public Stream",
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class EqualizerPreset(
    val name: String,
    val bandsDb: List<Int> // dB offset for 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (-12 to +12)
)

data class VlcSettings(
    val themeMode: String = "Dark", // "Dark", "Light", "System"
    val hardwareAcceleration: Boolean = true,
    val backgroundPlayback: Boolean = true,
    val resumePlayback: Boolean = true,
    val audioBoost: Boolean = false,
    val gesturesEnabled: Boolean = true,
    val subtitleSize: String = "Normal" // "Small", "Normal", "Large"
)
