/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PlayMuseGoldPrimary,
    onPrimary = PlayMuseBackgroundDark,
    primaryContainer = PlayMuseGoldDeep,
    onPrimaryContainer = PlayMuseTextPrimaryDark,
    secondary = PlayMuseGoldHighlight,
    onSecondary = PlayMuseBackgroundDark,
    secondaryContainer = PlayMuseElevatedSurfaceDark,
    onSecondaryContainer = PlayMuseGoldHighlight,
    background = PlayMuseBackgroundDark,
    onBackground = PlayMuseTextPrimaryDark,
    surface = PlayMuseSurfaceDark,
    onSurface = PlayMuseTextPrimaryDark,
    surfaceVariant = PlayMuseSurfaceVariantDark,
    onSurfaceVariant = PlayMuseTextSecondaryDark,
    outline = PlayMuseOutlineDark,
    outlineVariant = PlayMuseDividerDark,
    error = PlayMuseError,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PlayMuseGoldPrimaryLight,
    onPrimary = PlayMuseSurfaceLight,
    primaryContainer = PlayMuseSurfaceVariantLight,
    onPrimaryContainer = PlayMuseTextPrimaryLight,
    secondary = PlayMuseGoldHighlightLight,
    onSecondary = PlayMuseTextPrimaryLight,
    secondaryContainer = PlayMuseSurfaceVariantLight,
    onSecondaryContainer = PlayMuseTextPrimaryLight,
    background = PlayMuseBackgroundLight,
    onBackground = PlayMuseTextPrimaryLight,
    surface = PlayMuseSurfaceLight,
    onSurface = PlayMuseTextPrimaryLight,
    surfaceVariant = PlayMuseSurfaceVariantLight,
    onSurfaceVariant = PlayMuseTextSecondaryLight,
    outline = PlayMuseOutlineLight,
    outlineVariant = PlayMuseDividerLight,
    error = PlayMuseErrorLight,
    onError = Color.White
)

@Composable
fun VLCTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
