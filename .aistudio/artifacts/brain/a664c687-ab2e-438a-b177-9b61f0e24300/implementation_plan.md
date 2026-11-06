# Task 03 — Apply Premium Black & Gold Colour Theme

## 1. Objective & Scope

Transform the complete visual colour palette of **PLayMuse** (`:app`) to a refined **Black & Gold** aesthetic without altering any application logic, playback pipelines, navigation, layouts, or existing functionality.

---

## 2. Central Theme & Colour Token Architecture

### 2.1 `app/src/main/java/org/videolan/vlc/theme/Color.kt`
Introduce the canonical Black & Gold design tokens for both Dark and Light themes:

- **Dark Theme Tokens:**
  - `PlayMuseBackgroundDark = Color(0xFF090909)` (`#090909`)
  - `PlayMuseSurfaceDark = Color(0xFF121212)` (`#121212`)
  - `PlayMuseSurfaceVariantDark = Color(0xFF1B1B1B)` (`#1B1B1B`)
  - `PlayMuseElevatedSurfaceDark = Color(0xFF222222)` (`#222222`)
  - `PlayMuseGoldPrimary = Color(0xFFD4AF37)` (`#D4AF37`)
  - `PlayMuseGoldHighlight = Color(0xFFF3D36B)` (`#F3D36B`)
  - `PlayMuseGoldDeep = Color(0xFF9C7414)` (`#9C7414`)
  - `PlayMuseTextPrimaryDark = Color(0xFFF5F3EA)` (`#F5F3EA`)
  - `PlayMuseTextSecondaryDark = Color(0xFFB8B2A1)` (`#B8B2A1`)
  - `PlayMuseTextTertiaryDark = Color(0xFF858176)` (`#858176`)
  - `PlayMuseOutlineDark = Color(0xFF393326)` (`#393326`)
  - `PlayMuseDividerDark = Color(0xFF2B271D)` (`#2B271D`)
- **Light Theme Tokens:**
  - `PlayMuseBackgroundLight = Color(0xFFF4F1E8)` (`#F4F1E8`)
  - `PlayMuseSurfaceLight = Color(0xFFFBF9F2)` (`#FBF9F2`)
  - `PlayMuseSurfaceVariantLight = Color(0xFFE9E3D3)` (`#E9E3D3`)
  - `PlayMuseGoldPrimaryLight = Color(0xFF9C7414)` (`#9C7414`)
  - `PlayMuseGoldHighlightLight = Color(0xFFC39A2E)` (`#C39A2E`)
  - `PlayMuseTextPrimaryLight = Color(0xFF191814)` (`#191814`)
  - `PlayMuseTextSecondaryLight = Color(0xFF625C4E)` (`#625C4E`)
  - `PlayMuseOutlineLight = Color(0xFFB6AA8C)` (`#B6AA8C`)
  - `PlayMuseDividerLight = Color(0xFFD9D1BE)` (`#D9D1BE`)
- **Semantic Status Tokens:**
  - `PlayMuseError = Color(0xFFEF4444)` (Dark) / `PlayMuseErrorLight = Color(0xFFDC2626)` (Light)
  - `PlayMuseSuccess = Color(0xFF10B981)`
- **Compatibility Aliases:**
  - Point existing aliases (`PlayMuseIndigoPrimary`, `PlayMuseVioletAccent`, `VlcOrangePrimary`, `VlcOrangeDark`, `VlcOrangeAccent`, etc.) to the corresponding Black & Gold tokens while updating UI screens to use central Black & Gold tokens and `MaterialTheme.colorScheme`.

### 2.2 `app/src/main/java/org/videolan/vlc/theme/Theme.kt`
Update `DarkColorScheme` and `LightColorScheme` in `VLCTheme` (preserving `darkTheme: Boolean = true` default):

- **`DarkColorScheme`:**
  - `primary = PlayMuseGoldPrimary` (`#D4AF37`), `onPrimary = PlayMuseBackgroundDark` (`#090909` — high-contrast dark text/icons on gold buttons)
  - `primaryContainer = PlayMuseGoldDeep` (`#9C7414`), `onPrimaryContainer = PlayMuseTextPrimaryDark` (`#F5F3EA`)
  - `secondary = PlayMuseGoldHighlight` (`#F3D36B`), `onSecondary = PlayMuseBackgroundDark` (`#090909`)
  - `secondaryContainer = PlayMuseElevatedSurfaceDark` (`#222222`), `onSecondaryContainer = PlayMuseGoldHighlight` (`#F3D36B`)
  - `background = PlayMuseBackgroundDark` (`#090909`), `onBackground = PlayMuseTextPrimaryDark` (`#F5F3EA`)
  - `surface = PlayMuseSurfaceDark` (`#121212`), `onSurface = PlayMuseTextPrimaryDark` (`#F5F3EA`)
  - `surfaceVariant = PlayMuseSurfaceVariantDark` (`#1B1B1B`), `onSurfaceVariant = PlayMuseTextSecondaryDark` (`#B8B2A1`)
  - `outline = PlayMuseOutlineDark` (`#393326`), `outlineVariant = PlayMuseDividerDark` (`#2B271D`)
  - `error = PlayMuseError`, `onError = Color.Black`
- **`LightColorScheme`:**
  - `primary = PlayMuseGoldPrimaryLight` (`#9C7414`), `onPrimary = PlayMuseSurfaceLight` (`#FBF9F2`)
  - `primaryContainer = PlayMuseSurfaceVariantLight` (`#E9E3D3`), `onPrimaryContainer = PlayMuseTextPrimaryLight` (`#191814`)
  - `secondary = PlayMuseGoldHighlightLight` (`#C39A2E`), `onSecondary = PlayMuseTextPrimaryLight` (`#191814`)
  - `secondaryContainer = PlayMuseSurfaceVariantLight` (`#E9E3D3`), `onSecondaryContainer = PlayMuseTextPrimaryLight` (`#191814`)
  - `background = PlayMuseBackgroundLight` (`#F4F1E8`), `onBackground = PlayMuseTextPrimaryLight` (`#191814`)
  - `surface = PlayMuseSurfaceLight` (`#FBF9F2`), `onSurface = PlayMuseTextPrimaryLight` (`#191814`)
  - `surfaceVariant = PlayMuseSurfaceVariantLight` (`#E9E3D3`), `onSurfaceVariant = PlayMuseTextSecondaryLight` (`#625C4E`)
  - `outline = PlayMuseOutlineLight` (`#B6AA8C`), `outlineVariant = PlayMuseDividerLight` (`#D9D1BE`)
  - `error = PlayMuseErrorLight`, `onError = Color.White`

---

## 3. Screen & Component Updates

1. **`MainActivity.kt` (Bottom Navigation & Scaffold):**
   - Replace `VlcOrangePrimary` with `MaterialTheme.colorScheme` tokens.
   - Style `NavigationBarItemDefaults.colors` with gold selected icon and subtle gold indicator (`selectedIconColor = MaterialTheme.colorScheme.primary`, `selectedTextColor = MaterialTheme.colorScheme.primary`, `indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)`) and muted grey unselected state (`unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant`, `unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant`).
2. **`VlcTopBar.kt` (Top Bar, Overflow Menu & Search Field):**
   - Replace `PlayMuseIndigoPrimary` and `PlayMuseVioletAccent` with `MaterialTheme.colorScheme.primary` and `MaterialTheme.colorScheme.secondary`.
   - Style the search `OutlinedTextField` with a dark inset surface (`focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant`, `unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant`, `focusedBorderColor = MaterialTheme.colorScheme.primary`, `unfocusedBorderColor = MaterialTheme.colorScheme.outline`).
3. **`VideoListScreen.kt` (Video Grid/List Cards & Badges):**
   - Replace `VlcOrangeDark` and `VlcOrangePrimary` with `PlayMuseGoldDeep` and `MaterialTheme.colorScheme.primary`.
4. **`AudioScreen.kt` (Category Tabs, Shuffle Button & Track List):**
   - Replace `VlcOrangePrimary` with `MaterialTheme.colorScheme.primary`.
   - Update the "Shuffle" primary button to use `containerColor = MaterialTheme.colorScheme.primary` and `contentColor = MaterialTheme.colorScheme.onPrimary` (dark readable text on gold).
5. **`NowPlayingScreen.kt` (Full-Screen Audio Player & Vinyl Artwork):**
   - Replace `VlcOrangePrimary` and `VlcOrangeAccent` with `MaterialTheme.colorScheme.primary` and `MaterialTheme.colorScheme.secondary`.
   - Replace hardcoded navy/indigo vinyl record colors (`#141422`, `#282842`, `#33334F`, `#4F46E5`) with Black & Gold tokens (`PlayMuseSurfaceDark`, `PlayMuseElevatedSurfaceDark`, `PlayMuseOutlineDark`, `PlayMuseGoldPrimary`).
   - Update the primary Play/Pause circular button icon tint from `Color.White` to `MaterialTheme.colorScheme.onPrimary` for crisp contrast against gold.
6. **`VlcMiniPlayer.kt` (Mini-Player Bar):**
   - Replace `VlcOrangePrimary` with `MaterialTheme.colorScheme.primary`.
   - Use gold primary action styling for Play/Pause and muted secondary styling (`MaterialTheme.colorScheme.onSurfaceVariant`) for Next.
7. **`VideoPlayerScreen.kt` (Video Player Controls, Gesture HUD & Error Overlay):**
   - Replace `VlcOrangePrimary` with `PlayMuseGoldPrimary` / `MaterialTheme.colorScheme.primary`.
   - Use dark readable text/icon (`PlayMuseBackgroundDark`) on gold primary buttons (Play/Pause, Lock FAB, Retry button) and gold outline on secondary `OutlinedButton`.
8. **`EqualizerScreen.kt` (Equalizer Sliders, Presets & Sound Effects):**
   - Replace `VlcOrangePrimary` and `VlcOrangeAccent` with `MaterialTheme.colorScheme.primary`.
   - Update Switch (`checkedThumbColor = MaterialTheme.colorScheme.onPrimary`, `checkedTrackColor = MaterialTheme.colorScheme.primary`), "Select Preset" primary button (`contentColor = MaterialTheme.colorScheme.onPrimary`), sliders (`inactiveTrackColor = MaterialTheme.colorScheme.outline`), and "Reset to Flat" secondary `OutlinedButton` (charcoal surface with gold outline).
9. **`SettingsScreen.kt` (Settings Cards, Switches & Dividers):**
   - Replace `VlcOrangePrimary` with `MaterialTheme.colorScheme.primary`.
   - Update `SwitchDefaults.colors` (`checkedThumbColor = MaterialTheme.colorScheme.onPrimary`, `checkedTrackColor = MaterialTheme.colorScheme.primary`) and `HorizontalDivider` (`color = MaterialTheme.colorScheme.outlineVariant`).
10. **`BrowseScreen.kt` (Storage Filter Chips & Directory Browser):**
    - Replace `VlcOrangePrimary` and `VlcOrangeAccent` with `MaterialTheme.colorScheme.primary`.
11. **`VlcModals.kt` (Speed, Sleep Timer, Media Info & About Dialogs):**
    - Replace `PlayMuseIndigoPrimary`, `PlayMuseVioletAccent`, and `VlcOrangePrimary` with `MaterialTheme.colorScheme.primary` and `MaterialTheme.colorScheme.secondary`.
    - Update primary dialog buttons to use gold background (`MaterialTheme.colorScheme.primary`) with dark readable text (`MaterialTheme.colorScheme.onPrimary`).
12. **In-App & Launcher Vector Drawables (`ic_playmuse_logo.xml`, `ic_launcher_background.xml`, `ic_launcher_foreground.xml`):**
    - Update the Indigo/Violet vector colours (`#1E1B4B`, `#4F46E5`, `#7C3AED`, `#4338CA`, `#6D28D9`, `#6366F1`, `#C4B5FD`) to the Black & Gold palette (`#090909`, `#121212`, `#1B1B1B`, `#9C7414`, `#D4AF37`, `#F3D36B`, `#F5F3EA`) so the logo rendered in the Top Bar, Mini-Player, Now Playing vinyl, Empty States, and About dialog matches the Black & Gold theme seamlessly.

---

## 4. Verification Plan

1. **Accent Search (`grep`):** Verify no visible Indigo, Violet, Purple, or Orange hex values or active references remain in `app/src/main/`.
2. **Git Diff Inspection (`git status` / `git diff`):** Confirm only colour definitions, theme mappings, and visual styling references changed.
3. **Debug APK Build (`compile_applet`):** Confirm clean compilation.
4. **Unit & Robolectric Tests (`gradle :app:testDebugUnitTest --rerun-tasks`):** Confirm all tests in `PlayMuseIdentityTest` pass.
5. **Android Lint (`lint_applet`):** Confirm zero lint regressions.
