# PLayMuse (VLC for Android Fork) — Technical Baseline Audit & Remediation Plan

A comprehensive, strictly read-only technical baseline audit of the **PLayMuse** (`com.dsc.playmuse`) repository (`shahnaveed07/vlc-android`), evaluating both the active Jetpack Compose + Media3 application module (`:app`) and the unlinked upstream VideoLAN modules (`/application/*`) against LibVLC parity and Media3 production readiness.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> This baseline audit was conducted in **strict read-only mode** with zero source file modifications. All findings below are categorized into **Runtime-Verified Findings** (confirmed via Gradle build, Android Lint, HTTP stream verification, and Robolectric JVM execution) and **Static Code Analysis Findings** (confirmed via full source inspection).

- **Confirmed Decision 1 (Module Scope)**: Audit covers both the active `:app` Jetpack Compose + Media3 module and the legacy `/application/*` upstream VideoLAN modules.
- **Confirmed Decision 2 (Evaluation Standard)**: Evaluated against both **LibVLC functional parity** and **AndroidX Media3 production readiness**.
- **Confirmed Decision 3 (Verification Execution)**: Read-only Gradle compilation (`compile_applet`), Android Lint analysis (`lint_applet`), and Robolectric unit test execution (`testDebugUnitTest`) were run to establish runtime-verified ground truth.

---

## 1. Overview & Core Concept

- **What It Does**: **PLayMuse** (`com.dsc.playmuse`, version `3.6.0-playmuse`) is a custom-branded Android multimedia player developed by **Naveed Mushtaq** under **DarkSkullCorporation**, forked from VideoLAN's VLC for Android (GPLv2+). It provides local video and audio playback, directory browsing, network stream playback (HLS/MP4/Icecast), a 5-band hardware audio equalizer with Bass Boost and 3D Virtualizer, sleep timer, and playback speed controls.
- **Target Audience / Persona**: Android users seeking a modern, lightweight Material 3 media player capable of coexisting side-by-side with official VLC (`org.videolan.vlc`) while supporting local device media and public network streams.
- **Key Value & Current Baseline Reality**: The repository is currently a **hybrid split-architecture workspace**. Only the newly authored Jetpack Compose + AndroidX Media3 module (`:app`) is wired into Gradle (`settings.gradle.kts`). The original upstream VideoLAN modules (`/application/*`) remain partially in the working tree as uncompiled dead code, and several core upstream modules (`:application:vlc-android`, `:application:television`, `:application:tools`, `:libvlcjni:libvlc`, `:medialibrary`) are absent from the repository.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Video Library Flow**: Launches into a 5-tab `Scaffold` (`Videos`, `Audio`, `Streams`, `Browse`, `More`). Users toggle between an adaptive 2-column grid and a compact list, search by title/artist, inspect media metadata, or launch the full-screen `VideoPlayerScreen` (or play video as audio).
  2. **Full-Screen Video Player Flow**: Renders video via Media3 `PlayerView` inside Compose `AndroidView`, with custom vertical drag gestures (left half for window brightness, right half for system music volume), double-tap 10-second seek with animated central HUD feedback, aspect ratio cycling (`FIT`, `FILL`, `ZOOM`, `16:9`, `4:3`), control lock mode, and an inline error recovery card.
  3. **Audio & Now Playing Flow**: Displays local `MediaStore.Audio` tracks + built-in sample tracks with category tabs (`Tracks`, `Artists`, `Albums`, `Genres`), shuffle-all action, persistent `VlcMiniPlayer`, and an expandable full-screen `NowPlayingScreen` featuring a rotating vinyl animation, scrubber, repeat/shuffle toggles, speed dialog, and sleep timer.
  4. **Network Stream & File Browser Flow**: Allows entering custom HTTP/HTTPS/RTSP/HLS URLs, saving/removing stream bookmarks in memory, and browsing local directories with quick filter chips (`Internal Storage`, `Movies`, `Music`).
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Modern dark-first Material 3 interface rebranded from VLC's classic traffic-cone orange to **PLayMuse Electric Indigo & Violet**.
  - *Color Palette & Mood*:
    - Primary Indigo (`PlayMuseIndigoPrimary`: `#6366F1`, Dark: `#4F46E5`)
    - Secondary Violet Accent (`PlayMuseVioletAccent`: `#8B5CF6`, Light: `#A78BFA`)
    - Dark Surfaces (`PlayMuseBackgroundDark`: `#0F0F17`, `PlayMuseSurfaceDark`: `#181824`, `PlayMuseSurfaceVariantDark`: `#232336`)
    - Semantic Compatibility Aliases: Legacy token names (`VlcOrangePrimary`, `VlcOrangeDark`, `VlcOrangeAccent`) are aliased in `Color.kt` to the new Indigo/Violet palette.
  - *Typography & Hierarchy*: Uses default Material 3 system Roboto typography (`MaterialTheme.typography`) with bold uppercase section headers (`1.sp`–`1.5.sp` letter spacing) and tabular time readouts.
  - *Component Styling & Layout*: Edge-to-edge `Scaffold` with custom `VlcTopBar`, floating-style `VlcMiniPlayer` (`16.dp` corner radius, `6.dp` tonal elevation), and Material 3 `NavigationBar`.
- **Interactive Feedback & Motion**:
  - `InfiniteTransition` 14-second linear vinyl rotation when audio is playing (`NowPlayingScreen`).
  - `AnimatedVisibility` (`fadeIn + scaleIn` / `fadeOut + scaleOut`) for gesture HUD overlays, playback error cards, and full-screen player controls with a 4-second auto-hide timer.

---

## 3. Key Product Decisions & Trade-Offs (17-Point Audit Inventory)

### 1.Repository Structure & Module Overview
- **Active Build Module**: Only `:app` (`applicationId = "com.dsc.playmuse"`, `namespace = "org.videolan.vlc"`) is included in `settings.gradle.kts`.
- **Unlinked Legacy Modules (`/application/*`)**: Contains 9 subdirectories (`app`, `donations`, `live-plot-graph`, `mediadb`, `moviepedia`, `remote-access-client`, `remote-access-server`, `resources`, and `vlc-android` which only contains a single Python script `prep_media.py`).
- **Missing Upstream Modules**: The legacy `build.gradle` files reference `project(':application:vlc-android')`, `project(':application:television')`, `project(':application:tools')`, `project(':libvlcjni:libvlc')`, and `project(':medialibrary')`, none of which exist as Gradle modules in the tree. Consequently, `/application/*` cannot be compiled without restoring those upstream modules.

### 2. LibVLC & Native Integration vs. Media3 Engine
- **Actual Engine in `:app`**: Uses **AndroidX Media3 ExoPlayer (`1.5.1`)**, not `LibVLC`. Despite `BuildConfig.UPSTREAM_ATTRIBUTION` referencing `"VideoLAN / VLC Authors (LibVLC Engine)"`, there is zero JNI or `org.videolan.libvlc` dependency in `:app/build.gradle.kts`.
- **Codec & Protocol Impact**: Relies on Android `MediaCodec` + standard `ExoPlayer` extractors. Notably, `:app/build.gradle.kts` omits `androidx.media3:media3-exoplayer-hls`, `media3-exoplayer-dash`, and `media3-exoplayer-rtsp`, meaning HLS (`.m3u8`) and RTSP streams will fail at runtime with `UnrecognizedInputFormatException` unless those protocol modules are added.

### 3. MediaLibrary & Data Persistence
- **Current Implementation**: `MediaRepository` queries Android's `MediaStore.Video` and `MediaStore.Audio` via `ContentResolver` on `Dispatchers.IO` and merges results with hardcoded sample media (`sampleVideos` and `sampleAudioTracks`).
- **Persistence Gap**: Unlike upstream VLC's C++ `medialibrary` + Room `MediaDatabase` (present but unlinked in `/application/mediadb`), `:app` holds all user state (`VlcSettings`, custom `StreamBookmark` entries, equalizer band levels, `isFavorite`, `lastPositionMs`) **purely in RAM** inside `VlcViewModel` and `MediaRepository`. All user changes are lost on process death or app restart.

### 4. Playback Services, Background Playback & Notifications
- **Current Implementation**: `AndroidManifest.xml` declares `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and `app/build.gradle.kts` includes `androidx.media3:media3-session:1.5.1`.
- **Gap**: No `MediaSessionService`, `MediaLibraryService`, or `MediaSession` is declared in `AndroidManifest.xml` or instantiated in Kotlin code. `ExoPlayer` lives directly inside `VlcViewModel` (`VlcPlayerController`). Lock-screen media controls, Bluetooth/headset media buttons, Android Auto, and foreground service notifications are completely absent.

### 5. Audio, Video, Subtitles & Audio Tracks
- **Audio & Video**: Supports play/pause, seek, queue navigation, speed (`0.25x`–`2.0x`), repeat (`OFF`, `ALL`, `ONE`), shuffle, and hardware `Equalizer`/`BassBoost`/`Virtualizer` bound to `exoPlayer.audioSessionId`.
- **Subtitles & Multi-Audio Tracks**: Completely unimplemented in `:app`. While `VlcSettings` defines `val subtitleSize: String = "Normal"`, there is no UI in `SettingsScreen` or `VideoPlayerScreen` to select subtitle tracks, load external `.srt`/`.vtt` files, or switch embedded audio tracks.

### 6. Media Discovery, Storage Permissions & Android SDK Compatibility
- **SDK Configuration**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, Java 21.
- **Permissions**: Requests `READ_MEDIA_VIDEO` and `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` on API $\le$ 32.
- **Scoped Storage Failure in Browse Tab**: `BrowseScreen` and `MediaRepository.browseDirectory()` use raw `java.io.File.listFiles()` on `/sdcard` and `Environment.getExternalStoragePublicDirectory(...)`. On Android 11+ (API 30+) with `targetSdk = 36`, raw `File.listFiles()` on shared storage directories blocks non-media files or returns empty arrays without `MANAGE_EXTERNAL_STORAGE` or Storage Access Framework (`DocumentFile`).

### 7. Security, Privacy & Release Readiness
- **Cleartext Traffic**: `android:usesCleartextTraffic="true"` is enabled globally in `AndroidManifest.xml` (needed for HTTP Icecast/local streams, but should be scoped via `network_security_config.xml` if desired).
- **Release Signing & R8/ProGuard**: In `app/build.gradle.kts`, the `release` buildType has `isMinifyEnabled = false` and **no `signingConfig` assigned**, whereas `debug` uses `debugConfig` (`debug.keystore`).

### 8. Existing Project-Specific Modifications vs. Upstream VLC
- Replaced the multi-module XML/DataBinding/LibVLC architecture with a clean single-module Jetpack Compose + Media3 architecture (`:app`).
- Rebranded application identity to **PLayMuse** (`com.dsc.playmuse`), developer attribution (**Naveed Mushtaq**), and copyright (**DarkSkullCorporation**).
- Replaced the custom permission namespace with `${applicationId}.permission.READ_EXTENSION_DATA` so the APK installs side-by-side with official VLC (`org.videolan.vlc`) without `INSTALL_FAILED_DUPLICATE_PERMISSION`.
- Added unit tests (`PlayMuseIdentityTest.kt`) verifying app identity, coexistence, branding drawables, and sample repository data.

---

## 4. Technical Architecture & Complete Bug Audit *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PLayMuse Repository Workspace                        │
├──────────────────────────────────────┬──────────────────────────────────────┤
│     ACTIVE GRADLE MODULE (:app)      │   UNLINKED LEGACY TREE (/application)│
│   com.dsc.playmuse (Compose/Media3)  │   org.videolan.vlc (Dead Code Tree)  │
└──────────────────┬───────────────────┴──────────────────┬───────────────────┘
                   │                                      │
                   ▼                                      ▼
┌──────────────────────────────────────┐ ┌────────────────────────────────────┐
│ MainActivity (Single-Activity UI)    │ │ • :application:app (VLCApplication)│
│ ├─ VlcTopBar / VlcMiniPlayer         │ │ • :application:mediadb (Room DAO)  │
│ ├─ VideoListScreen / AudioScreen     │ │ • :application:moviepedia          │
│ ├─ StreamScreen / BrowseScreen       │ │ • :application:remote-access-*     │
│ ├─ SettingsScreen / EqualizerScreen  │ │ • :application:resources / donations│
│ └─ VideoPlayerScreen / NowPlaying    │ │ ✗ Missing :application:vlc-android │
└──────────────────┬───────────────────┘ │ ✗ Missing :libvlcjni / :medialibrary│
                   │                     └────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────┐
│ VlcViewModel (In-Memory StateFlows)  │
│ ├─ Navigation & Modal State          │
│ ├─ Search & Category Filter State    │
│ └─ VlcSettings (Volatile RAM State)  │
└─────────┬────────────────────┬───────┘
          │                    │
          ▼                    ▼
┌───────────────────┐ ┌───────────────────────────────────────────────────────┐
│ MediaRepository   │ │ VlcPlayerController                                   │
│ ├─ MediaStore I/O │ │ ├─ ExoPlayer (DefaultHttpDataSource + MediaSource)    │
│ ├─ Sample Catalog │ │ ├─ Hardware AudioFX (Equalizer/BassBoost/Virtualizer) │
│ └─ File.listFiles │ │ └─ CoroutineScope(Dispatchers.Main) Progress & Timer  │
└───────────────────┘ └───────────────────────────────────────────────────────┘
```

---

### Confirmed Bugs & Technical Findings (Static vs. Runtime-Verified)

#### Category A: Critical & High Severity Bugs

1. **[CRITICAL | Confirmed via Static & Dependency Inspection] Missing Media3 HLS (`media3-exoplayer-hls`) & RTSP (`media3-exoplayer-rtsp`) Modules Cause Stream Playback Failure**
   - **Location**: `app/build.gradle.kts` (`dependencies` block) & `MediaRepository` (`sample_video_1`, `curatedStreams`) & `StreamScreen`.
   - **Technical Explanation**: `MediaRepository` includes HLS `.m3u8` streams (`https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`) as the very first sample video (`sample_video_1`) and first featured stream bookmark, and `StreamScreen` prompts users to enter HLS/RTSP URLs. However, `app/build.gradle.kts` only declares `media3-exoplayer`, `media3-ui`, `media3-common`, and `media3-session`. In Media3, HLS (`HlsMediaSource`) and RTSP (`RtspMediaSource`) reside in separate artifacts (`androidx.media3:media3-exoplayer-hls:1.5.1` and `androidx.media3:media3-exoplayer-rtsp:1.5.1`). When `DefaultMediaSourceFactory` attempts to prepare an `.m3u8` URL without `media3-exoplayer-hls` on the classpath, ExoPlayer throws `UnrecognizedInputFormatException` / `IllegalStateException`.
   - **Reproduction Steps**: Launch the app $\rightarrow$ Tap the first video `"Big Buck Bunny (HLS Stream)"` or the first stream bookmark `"Big Buck Bunny HLS Live"`.
   - **Recommended Correction**: Add `implementation("androidx.media3:media3-exoplayer-hls:1.5.1")`, `implementation("androidx.media3:media3-exoplayer-dash:1.5.1")`, and `implementation("androidx.media3:media3-exoplayer-rtsp:1.5.1")` to `app/build.gradle.kts`.
   - **Verification Method**: Compile APK and play `sample_video_1` (`x36xhzz.m3u8`); verify `Player.STATE_READY` is reached without `PlaybackException`.

2. **[HIGH | Confirmed via Static Analysis] Missing `MainActivity` Intent Handling for External Media & Deep Links (`ACTION_VIEW`)**
   - **Location**: `MainActivity.onCreate()` & `AndroidManifest.xml` (lines 44–59).
   - **Technical Explanation**: `AndroidManifest.xml` registers two `<intent-filter>` blocks on `MainActivity` for `android.intent.action.VIEW` (`http`, `https`, `rtsp` with `video/*` and `audio/*` MIME types, plus custom scheme `playmuse://`). However, `MainActivity.onCreate()` and `onNewIntent()` never inspect `intent?.action` or `intent?.data`. Opening a video/audio file from the Android system Files app or clicking a `playmuse://` link simply opens the home screen and ignores the incoming URI.
   - **Reproduction Steps**: Trigger `Intent(Intent.ACTION_VIEW, Uri.parse("https://media.w3.org/2010/05/sintel/trailer.mp4"))` targeting `com.dsc.playmuse` $\rightarrow$ App opens to the default Video list without playing the URI.
   - **Recommended Correction**: Handle `intent?.data` in `MainActivity.onCreate()` and override `onNewIntent()`, routing incoming `content://`, `file://`, `http://`, `https://`, `rtsp://`, and `playmuse://` URIs to `viewModel.playStream()` or `viewModel.playVideo()`.
   - **Verification Method**: Robolectric test launching `MainActivity` with an `ACTION_VIEW` data URI and asserting `viewModel.playerController.currentMedia.value?.uri` matches the intent URI.

3. **[HIGH | Confirmed via Static Analysis] Missing `MediaSession` & `MediaSessionService` Breaks Background Playback, Lock-Screen Controls, and Audio Focus**
   - **Location**: `VlcPlayerController`, `MainActivity`, `SettingsScreen`, `AndroidManifest.xml`.
   - **Technical Explanation**:
     1. `ExoPlayer.Builder(context)` is constructed without `.setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)` and without `.setHandleAudioBecomingNoisy(true)`. Consequently, incoming phone calls, other media apps, or unplugging headphones will not pause PLayMuse automatically.
     2. Although `media3-session:1.5.1` is in `build.gradle.kts` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` is in `AndroidManifest.xml`, no `MediaSession` or `MediaSessionService` is implemented, and `SettingsScreen`'s `"Background Audio Playback"` toggle (`settings.backgroundPlayback`) is never read anywhere in the codebase.
   - **Reproduction Steps**: Start audio playback $\rightarrow$ Toggle `"Background Audio Playback"` off in `More` tab $\rightarrow$ Minimize the app. Playback continues unmanaged without a media notification, or gets killed by the OS after a few minutes in the background.
   - **Recommended Correction**: Configure `AudioAttributes` with `handleAudioFocus = true` and `setHandleAudioBecomingNoisy(true)` on `ExoPlayer.Builder`, bind a Media3 `MediaSession`, and wire `settings.backgroundPlayback` to pause playback in `Activity.onStop()` when disabled or run a `MediaSessionService` when enabled.
   - **Verification Method**: Verify audio focus handling on `ExoPlayer` and lifecycle pause behavior via unit/Robolectric tests.

4. **[HIGH | Confirmed via Static Analysis] Unwired Functional UI Controls (Dead-End State in Audio Categories, Settings Toggles, Shuffle Queue, and File Browser)**
   - **Location**:
     - `AudioScreen` & `VlcViewModel.filteredAudioTracks`: `AudioScreen` displays 4 tabs (`"Tracks"`, `"Artists"`, `"Albums"`, `"Genres"`), updating `_selectedAudioCategory`. However, `filteredAudioTracks` in `VlcViewModel` only combines `_audioTracks` and `_searchQuery`—it completely ignores `_selectedAudioCategory`. Selecting `"Artists"`, `"Albums"`, or `"Genres"` renders the exact same flat track list.
     - `SettingsScreen` & `VlcSettings`: Toggles for `hardwareAcceleration`, `backgroundPlayback`, `resumePlayback`, `audioBoost`, and `gesturesEnabled` mutate `_settings.value`, but **only `themeMode`** is ever read (`MainActivity.kt:58`). Specifically, `VideoPlayerScreen` ignores `settings.gesturesEnabled`, `VlcPlayerController` ignores `settings.resumePlayback` and `settings.audioBoost` (`LoudnessEnhancer`), and `MainActivity` lacks a UI selector to change `settings.themeMode` (`"Dark"`, `"Light"`, `"System"`)!
     - `VlcPlayerController.playNext()` / `playPrevious()` & `toggleShuffle()`: `toggleShuffle()` sets `exoPlayer.shuffleModeEnabled = newShuffle`, but `playMedia()` only passes a single `MediaItem` to `exoPlayer.setMediaItem(exoItem)` instead of passing the queue to ExoPlayer, and `playNext()` steps linearly (`currentIndex + 1`) through `_playlist.value` without checking `_isShuffle.value`!
   - **Reproduction Steps**:
     - Tap `"Artists"` or `"Albums"` in the Audio tab $\rightarrow$ UI does not group or filter by artist/album.
     - Disable `"Playback Gestures"` in the More tab $\rightarrow$ Open a video and swipe vertically $\rightarrow$ Brightness/volume gestures still fire.
     - Enable Shuffle in `NowPlayingScreen` $\rightarrow$ Tap Next $\rightarrow$ Plays the next sequential track instead of a shuffled track.
   - **Recommended Correction**: Wire `_selectedAudioCategory` into `AudioScreen` / `VlcViewModel`, connect all `VlcSettings` flags to `VideoPlayerScreen` and `VlcPlayerController`, add the Theme Mode selector in `SettingsScreen`, and respect `_isShuffle.value` in `playNext()`.
   - **Verification Method**: Robolectric unit tests verifying state transitions for each setting and audio category filter.

---

#### Category B: Medium Severity Bugs (Lifecycle, Memory, Scoped Storage & Formatting)

5. **[MEDIUM | Confirmed via Static Analysis] Unbounded Coroutine Scope Leak in `VlcPlayerController`**
   - **Location**: `VlcPlayerController.kt` (line 35 & lines 178–190).
   - **Technical Explanation**: `VlcPlayerController` creates `private val scope = CoroutineScope(Dispatchers.Main)` without a `SupervisorJob()` and never calls `scope.cancel()` inside `release()`. Furthermore, `startProgressUpdater()` runs an unconditional `while (true) { ... delay(500) }` loop on the main thread even when no media is loaded or when playback is paused.
   - **Reproduction Steps**: Instantiate `VlcPlayerController` $\rightarrow$ Observe `progressJob` waking the main looper every 500ms indefinitely even while idle.
   - **Recommended Correction**: Add `SupervisorJob()` to `scope`, cancel `scope` inside `release()`, and run the progress polling loop only while `_isPlaying.value == true` (triggered from `onIsPlayingChanged`).
   - **Verification Method**: Inspect coroutine job lifecycle in unit tests after calling `playerController.release()`.

6. **[MEDIUM | Confirmed via Static Analysis] Duration and Position Formatting Bug for Videos/Audio Over 1 Hour & `file://` URI Parsing Issue**
   - **Location**:
     - `VideoPlayerScreen.kt` (lines 444–445) & `NowPlayingScreen.kt` (lines 266–267): Computes `String.format("%02d:%02d", (posSec % 3600) / 60, posSec % 60)`. For any movie $\ge$ 60 minutes (e.g., 1h 15m), `(durSec % 3600) / 60` wraps around to `15:00`, hiding the hours digit completely!
     - `BrowseScreen.kt` (line 153): Constructs `uri = "file://${file.path}"` instead of `Uri.fromFile(File(file.path)).toString()`. If a local file or folder name contains spaces, `#`, or `%`, `Uri.parse("file://${file.path}")` produces a malformed URI that fails to play.
   - **Reproduction Steps**: Load a 90-minute movie $\rightarrow$ Player scrubber displays `30:00` instead of `1:30:00`. Or tap a file with `#` in its name in `BrowseScreen`.
   - **Recommended Correction**: Use a shared locale-safe `formatDurationMs(ms: Long)` helper (including hours when `hours > 0`) and `Uri.fromFile(File(file.path)).toString()`.
   - **Verification Method**: Unit test `formatDurationMs(5400_000L)` asserting `"1:30:00"`.

7. **[MEDIUM | Runtime-Verified via Android Lint] Scoped Storage & Hardcoded `/sdcard` Path in `MediaRepository.browseDirectory()`**
   - **Location**: `MediaRepository.kt` (line 309) & `BrowseScreen.kt` (lines 58, 66, 76) & `AndroidManifest.xml` (line 19).
   - **Technical Explanation**:
     - Flagged by Android Lint (`SdCardPath` at `MediaRepository.kt:309` and `SelectedPhotoAccess` at `AndroidManifest.xml:19`).
     - When `currentPath == null`, `MediaRepository.browseDirectory(null)` falls back to `context.getExternalFilesDir(null)?.parentFile ?: File("/sdcard")`. `getExternalFilesDir(null)?.parentFile` resolves to `/storage/emulated/0/Android/data/com.dsc.playmuse` (the app-private sandbox), which is empty! And on Android 13–16 (`targetSdk = 36`), raw `File.listFiles()` on shared storage directories fails to list general files or requires `Environment.getExternalStorageDirectory()` / `MediaStore` fallback.
   - **Reproduction Steps**: Open the `Browse` tab on a fresh install $\rightarrow$ Displays `"Directory is empty"` because `getExternalFilesDir(null)?.parentFile` points inside `Android/data/com.dsc.playmuse`.
   - **Recommended Correction**: Replace hardcoded `/sdcard` and app-sandbox default with `Environment.getExternalStorageDirectory()`, and surface discovered `MediaStore` folders when direct `File.listFiles()` is restricted by Scoped Storage.
   - **Verification Method**: Verify `lint_applet` clears `SdCardPath` and `Browse` tab lists accessible media directories.

---

#### Category C: Low Severity & Polish Issues (Runtime-Verified via Android Lint)

8. **[LOW | Runtime-Verified via Android Lint] 36 Android Lint Warnings Across Locale, Iconography, and Resources**
   - **Locations & Breakdown**:
     - `DefaultLocale` (10 warnings): `String.format(...)` called without `Locale.US` in `MediaModels.kt` (lines 52, 54, 65, 66, 67), `BrowseScreen.kt` (line 186), `NowPlayingScreen.kt` (lines 266, 267), and `VideoPlayerScreen.kt` (lines 444, 445).
     - `MonochromeLauncherIcon` (2 warnings): `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` lack a `<monochrome android:drawable="@drawable/ic_launcher_foreground" />` layer for Android 13–16 themed icons.
     - `ObsoleteSdkInt` (1 warning): `res/mipmap-anydpi-v26` qualifier is redundant since `minSdk = 26`.
     - `UnusedResources` (2 warnings): `R.string.about_playmuse_title` and `R.string.about_playmuse_description` in `strings.xml` are unused because `AboutVlcDialog` in `VlcModals.kt` hardcodes English strings instead of calling `stringResource(...)` (and also omits `R.string.upstream_vlc_attribution`).
     - `RedundantLabel` (1 warning): `android:label="@string/app_name"` on `<activity android:name=".MainActivity">` duplicates `<application android:label="@string/app_name">`.
     - `UseKtx` (1 warning): `Uri.parse(item.uri)` in `VlcPlayerController.kt:201` can use `item.uri.toUri()`.
     - `ContextCastToActivity` / Window Brightness Risk: `VideoPlayerScreen.kt:146` uses `context as? Activity` instead of `ContextWrapper` unwrapping to adjust window brightness.

---

### Build, Test & Release-Readiness Assessment

| Audit Dimension | Current Status | Details |
| :--- | :--- | :--- |
| **Debug Compilation (`compile_applet`)** | **PASS (Verified)** | `:app:assembleDebug` compiles cleanly in ~8s with Kotlin `2.2.10` and AGP `9.1.1`. |
| **Unit & Robolectric Tests (`testDebugUnitTest`)** | **PASS (Verified)** | 4/4 tests in `PlayMuseIdentityTest` pass, verifying package coexistence (`com.dsc.playmuse`), branding resources, and sample repository data. |
| **Android Lint (`lint_applet`)** | **PASS with 36 Warnings** | 0 errors, 36 warnings (`DefaultLocale`, `SdCardPath`, `SelectedPhotoAccess`, `UnusedResources`, `MonochromeLauncherIcon`, `UseKtx`). |
| **Legacy `/application/*` Modules** | **UNLINKED / NON-COMPILABLE** | Excluded from `settings.gradle.kts`; missing `:application:vlc-android`, `:application:television`, `:application:tools`, `:libvlcjni:libvlc`, and `:medialibrary`. |
| **Release Readiness (`:app`)** | **PROTOTYPE / BETA** | Ready for interactive emulator preview of MP4/MP3 streams and local MediaStore files, but requires P0/P1 remediations below before production release. |

---

### Prioritised Remediation Roadmap (Awaiting Explicit Instruction)

> [!NOTE]
> Per your strict read-only audit instructions, **no files have been modified**. If you choose to proceed with remediation in a subsequent turn, the following prioritized roadmap is recommended:

1. **Phase 1 — P0 Playback Engine & Protocol Completeness (`:app`)**
   - Add `androidx.media3:media3-exoplayer-hls:1.5.1`, `media3-exoplayer-dash:1.5.1`, and `media3-exoplayer-rtsp:1.5.1` to `app/build.gradle.kts` so HLS (`.m3u8`), DASH (`.mpd`), and RTSP streams play natively without `UnrecognizedInputFormatException`.
   - Configure `AudioAttributes` (`handleAudioFocus = true`) and `setHandleAudioBecomingNoisy(true)` on `ExoPlayer.Builder`, plus `LoudnessEnhancer` for the Audio Boost setting.
   - Fix `VlcPlayerController.playNext()` shuffle selection and scope the progress polling coroutine strictly to active playback.
2. **Phase 2 — P1 Functional Wiring, Deep Links & Persistence (`:app`)**
   - Wire `MainActivity` `ACTION_VIEW` intent handling (`onCreate` and `onNewIntent`) so external media files and `playmuse://` links play immediately.
   - Connect all unwired UI controls: Audio category filtering/grouping (`Tracks`, `Artists`, `Albums`, `Genres`), `VlcSettings` toggles (`gesturesEnabled`, `resumePlayback`, `audioBoost`, `backgroundPlayback`, and a `themeMode` selector in `SettingsScreen`), and `SharedPreferences` persistence for settings and custom stream bookmarks.
3. **Phase 3 — P2 Scoped Storage, Formatting & Lint Cleanliness (`:app`)**
   - Fix the $>1\text{ hour}$ duration truncation bug in `VideoPlayerScreen` and `NowPlayingScreen`, replace `"file://${file.path}"` with safe `Uri.fromFile(...)`, fix the default browse root path (`Environment.getExternalStorageDirectory()`), wire `strings.xml` resources into `AboutVlcDialog`, and resolve `DefaultLocale` / `MonochromeLauncherIcon` Lint warnings.
   - Expand `PlayMuseIdentityTest.kt` with Robolectric tests covering HLS media source creation, settings persistence, audio category filtering, and duration formatting.
