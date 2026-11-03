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
    primary = PlayMuseIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = PlayMuseIndigoDark,
    onPrimaryContainer = Color.White,
    secondary = PlayMuseVioletAccent,
    onSecondary = Color.White,
    secondaryContainer = PlayMuseSurfaceVariantDark,
    onSecondaryContainer = PlayMuseTextPrimaryDark,
    background = PlayMuseBackgroundDark,
    onBackground = PlayMuseTextPrimaryDark,
    surface = PlayMuseSurfaceDark,
    onSurface = PlayMuseTextPrimaryDark,
    surfaceVariant = PlayMuseSurfaceVariantDark,
    onSurfaceVariant = PlayMuseTextSecondaryDark,
    outline = PlayMuseDivider,
    error = PlayMuseError,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PlayMuseIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = PlayMuseVioletAccent,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
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
