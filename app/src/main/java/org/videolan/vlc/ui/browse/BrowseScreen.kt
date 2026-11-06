/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.ui.browse

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.data.FileItem
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType

@Composable
fun BrowseScreen(
    currentPath: String?,
    files: List<FileItem>,
    onFolderClick: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = currentPath != null) {
        onNavigateUp()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Quick storage shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentPath == null,
                onClick = { onFolderClick(Environment.getExternalStorageDirectory().absolutePath) },
                label = { Text("Internal Storage", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            FilterChip(
                selected = false,
                onClick = {
                    val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    onFolderClick(moviesDir.absolutePath)
                },
                label = { Text("Movies", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            FilterChip(
                selected = false,
                onClick = {
                    val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    onFolderClick(musicDir.absolutePath)
                },
                label = { Text("Music", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Current Directory Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentPath != null) {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Up",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = currentPath ?: "Root Directory",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Directory is empty",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(files, key = { it.path }) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (file.isDirectory) {
                                    onFolderClick(file.path)
                                } else if (file.isMedia) {
                                    val isVideo = file.name.matches(Regex(".*\\.(mp4|mkv|webm|avi|mov)$", RegexOption.IGNORE_CASE))
                                    onMediaClick(
                                        MediaItem(
                                            id = file.path,
                                            title = file.name,
                                            uri = "file://${file.path}",
                                            mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                                            sizeBytes = file.sizeBytes,
                                            resolution = if (isVideo) "Local Video" else "Local Audio"
                                        )
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                file.isDirectory -> Icons.Default.Folder
                                file.name.matches(Regex(".*\\.(mp4|mkv|webm|avi|mov)$", RegexOption.IGNORE_CASE)) -> Icons.Default.Movie
                                else -> Icons.Default.AudioFile
                            },
                            contentDescription = null,
                            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!file.isDirectory && file.sizeBytes > 0) {
                                val sizeMb = file.sizeBytes / (1024.0 * 1024.0)
                                Text(
                                    text = String.format("%.1f MB", sizeMb),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (file.isDirectory) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
