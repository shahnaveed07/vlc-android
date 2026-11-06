/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.vlc.model.EqualizerPreset
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import java.io.File

class MediaRepository(private val context: Context) {

    // Default built-in sample videos (High quality open source media for immediate playback)
    private val sampleVideos = listOf(
        MediaItem(
            id = "sample_video_1",
            title = "Big Buck Bunny (1080p)",
            artist = "Blender Foundation",
            album = "Open Movie",
            durationMs = 596000L,
            uri = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            mediaType = MediaType.VIDEO,
            thumbnailUri = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 158000000L,
            resolution = "1080p Full HD"
        ),
        MediaItem(
            id = "sample_video_2",
            title = "Sintel Animated Trailer",
            artist = "Durian Open Movie Project",
            album = "Blender Animation",
            durationMs = 52000L,
            uri = "https://media.w3.org/2010/05/sintel/trailer.mp4",
            mediaType = MediaType.VIDEO,
            thumbnailUri = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 42000000L,
            resolution = "1080p HD"
        ),
        MediaItem(
            id = "sample_video_3",
            title = "Big Buck Bunny Trailer",
            artist = "Blender Foundation",
            album = "Blender HD",
            durationMs = 33000L,
            uri = "https://media.w3.org/2010/05/bunny/trailer.mp4",
            mediaType = MediaType.VIDEO,
            thumbnailUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 25000000L,
            resolution = "720p HD"
        ),
        MediaItem(
            id = "sample_video_4",
            title = "Tears of Steel Preview",
            artist = "Mango Open Movie Project",
            album = "Sci-Fi Cinema",
            durationMs = 32000L,
            uri = "https://media.w3.org/2010/05/video/movie_300.mp4",
            mediaType = MediaType.VIDEO,
            thumbnailUri = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 38000000L,
            resolution = "720p HD"
        ),
        MediaItem(
            id = "sample_video_5",
            title = "Car & Traffic Cinema Demo",
            artist = "Open Motion Project",
            album = "HD Showreel",
            durationMs = 28000L,
            uri = "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4",
            mediaType = MediaType.VIDEO,
            thumbnailUri = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 18000000L,
            resolution = "1080p HD"
        )
    )

    // Default sample audio tracks
    private val sampleAudioTracks = listOf(
        MediaItem(
            id = "sample_audio_1",
            title = "Midnight Jazz & Chill",
            artist = "PLayMuse Sound Lab",
            album = "Late Night Sessions",
            durationMs = 214000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            mediaType = MediaType.AUDIO,
            thumbnailUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 6800000L,
            resolution = "320 kbps MP3"
        ),
        MediaItem(
            id = "sample_audio_2",
            title = "Acoustic Horizon Symphony",
            artist = "PLayMuse Acoustic Ensemble",
            album = "Acoustic Journeys",
            durationMs = 188000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            mediaType = MediaType.AUDIO,
            thumbnailUri = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 5900000L,
            resolution = "256 kbps AAC"
        ),
        MediaItem(
            id = "sample_audio_3",
            title = "Electric Pulse Groove",
            artist = "Synthesizer Society",
            album = "Future Wave",
            durationMs = 345000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            mediaType = MediaType.AUDIO,
            thumbnailUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 9400000L,
            resolution = "320 kbps MP3"
        ),
        MediaItem(
            id = "sample_audio_4",
            title = "Deep Ambient Atmosphere",
            artist = "Cosmic Traveler",
            album = "Starlight Soundscapes",
            durationMs = 260000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            mediaType = MediaType.AUDIO,
            thumbnailUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            sizeBytes = 7200000L,
            resolution = "FLAC Lossless"
        )
    )

    // Equalizer presets
    val equalizerPresets = listOf(
        EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0)),
        EqualizerPreset("Rock", listOf(5, 3, -1, 3, 6)),
        EqualizerPreset("Pop", listOf(-1, 2, 5, 2, -2)),
        EqualizerPreset("Jazz", listOf(4, 2, -2, 2, 5)),
        EqualizerPreset("Classical", listOf(5, 3, -2, 4, 4)),
        EqualizerPreset("Heavy Metal", listOf(4, 1, 9, 3, 0)),
        EqualizerPreset("Club", listOf(2, 4, 3, 2, 0)),
        EqualizerPreset("Bass Boost", listOf(8, 5, 1, 0, 0)),
        EqualizerPreset("Vocal", listOf(-2, 0, 6, 3, -1)),
        EqualizerPreset("Techno", listOf(5, 3, 0, 3, 5))
    )

    suspend fun getVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val deviceVideos = queryDeviceVideos()
        if (deviceVideos.isNotEmpty()) {
            deviceVideos + sampleVideos
        } else {
            sampleVideos
        }
    }

    suspend fun getAudioTracks(): List<MediaItem> = withContext(Dispatchers.IO) {
        val deviceAudio = queryDeviceAudio()
        if (deviceAudio.isNotEmpty()) {
            deviceAudio + sampleAudioTracks
        } else {
            sampleAudioTracks
        }
    }

    private fun queryDeviceVideos(): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.RESOLUTION
            )
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistCol = it.getColumnIndex(MediaStore.Video.Media.ARTIST)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val resCol = it.getColumnIndex(MediaStore.Video.Media.RESOLUTION)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Video $id"
                    val artist = if (artistCol != -1) it.getString(artistCol) ?: "Unknown" else "Unknown"
                    val duration = it.getLong(durationCol)
                    val size = it.getLong(sizeCol)
                    val res = if (resCol != -1) it.getString(resCol) ?: "HD" else "HD"
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    list.add(
                        MediaItem(
                            id = "device_vid_$id",
                            title = title,
                            artist = artist,
                            durationMs = duration,
                            uri = contentUri.toString(),
                            mediaType = MediaType.VIDEO,
                            sizeBytes = size,
                            resolution = res
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Permission or MediaStore access fallback
        }
        return list
    }

    private fun queryDeviceAudio(): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
            )
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Track $id"
                    val artist = if (artistCol != -1) it.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                    val album = if (albumCol != -1) it.getString(albumCol) ?: "Unknown Album" else "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val size = it.getLong(sizeCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    list.add(
                        MediaItem(
                            id = "device_aud_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            uri = contentUri.toString(),
                            mediaType = MediaType.AUDIO,
                            sizeBytes = size,
                            resolution = "Local Audio"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        return list
    }

    suspend fun browseDirectory(path: String?): List<FileItem> = withContext(Dispatchers.IO) {
        val targetDir = if (path.isNullOrEmpty()) {
            context.getExternalFilesDir(null)?.parentFile ?: File("/sdcard")
        } else {
            File(path)
        }

        val items = mutableListOf<FileItem>()
        try {
            val files = targetDir.listFiles()
            if (files != null) {
                for (file in files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))) {
                    if (!file.isHidden) {
                        items.add(
                            FileItem(
                                name = file.name,
                                path = file.absolutePath,
                                isDirectory = file.isDirectory,
                                sizeBytes = if (file.isDirectory) 0 else file.length(),
                                isMedia = file.name.matches(Regex(".*\\.(mp4|mkv|webm|avi|mov|mp3|flac|wav|ogg|m4a|aac)$", RegexOption.IGNORE_CASE))
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        items
    }
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val isMedia: Boolean
)
