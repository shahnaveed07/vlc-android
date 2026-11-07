# Task 05 — Simplify About Screen and Implement Real Media Deletion

## 1. Objective & Scope

Execute two targeted changes in **PLayMuse** (`:app`) while preserving the Black & Gold theme, AndroidX Media3 / ExoPlayer 1.5.1 playback engine, and all existing playback features:

1. **Part A — Simplify the About Screen:** Simplify `AboutVlcDialog` to display only the **PLayMuse application logo**, **PLayMuse application name**, and **current application version**, removing visible developer/copyright attribution cards and the misleading `LibVLC Engine` description from the UI while preserving all repository legal files (`COPYING`) and source copyright headers.
2. **Parts B–G — Real Device Media Deletion:** Add a **Delete** option to Video (grid cards & list rows) and Audio track overflow menus for eligible device-local `MediaStore` items, guarded by a confirmation dialog, API-appropriate Android storage deletion (`MediaStore.createDeleteRequest` on API 30+, `RecoverableSecurityException` handling on API 29, and on-demand `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"` on API 26–28), playback/queue cleanup if the deleted item is currently playing, targeted library refresh, and Snackbar feedback.

---

## 2. Planned Changes

### 2.1 Part A — Simplify About Screen (`VlcModals.kt`, `SettingsScreen.kt`, `strings.xml`)
- **`AboutVlcDialog` (`app/src/main/java/org/videolan/vlc/ui/modals/VlcModals.kt`):**
  - Display only:
    1. PLayMuse application logo (`R.drawable.ic_playmuse_logo`).
    2. PLayMuse application name (`"PLayMuse"`).
    3. Current application version (`"Version ${BuildConfig.VERSION_NAME}"` / `"v3.6.0-playmuse"`).
  - Remove the developer attribution card, copyright card, descriptive paragraph, and Open Source Licenses / LibVLC Engine credit section from the dialog UI.
- **`SettingsScreen.kt`:**
  - Simplify the `"About PLayMuse"` row subtitle to `"Application version information"`.
- **Legal Preservation:**
  - Preserve `/COPYING`, all file copyright headers, `BuildConfig.UPSTREAM_ATTRIBUTION`, and `R.string.upstream_vlc_attribution` (updating the string value from `"VideoLAN / VLC Authors (LibVLC Engine)"` to `"VideoLAN / VLC Authors"` so it never claims the active Media3 engine is LibVLC while keeping upstream VideoLAN attribution intact).

### 2.2 Part B & C — Video & Audio Overflow Menu Delete Action (`VideoListScreen.kt`, `AudioScreen.kt`)
- **Local Media Eligibility (`isDeletableLocalMedia(item: MediaItem)`):**
  - Validate that `item.uri` parses as a `content://` URI with authority `MediaStore.AUTHORITY` (`"media"`), a valid `video/media` or `audio/media` path matching `item.mediaType`, and a positive numeric item ID (`ContentUris.parseId(uri) > 0`).
  - Remote sample items (`https://...`) and non-MediaStore URIs return `false` and do **not** show the Delete option.
- **`VideoListScreen.kt` (`VideoGridCard` & `VideoListRow`):**
  - Add `onDeleteVideo: (MediaItem) -> Unit` callback.
  - In the 3-dot `DropdownMenu`, show `Play as Audio`, `Information`, and (when `isDeletableLocalMedia(video)` is `true`) `Delete` with `Icons.Default.Delete` tinted with `MaterialTheme.colorScheme.error`.
- **`AudioScreen.kt` (`AudioTrackItem`):**
  - Add `onDeleteTrack: (MediaItem) -> Unit` callback.
  - In the 3-dot `DropdownMenu`, show `Track Details` and (when `isDeletableLocalMedia(track)` is `true`) `Delete` with `Icons.Default.Delete` tinted with `MaterialTheme.colorScheme.error`.

### 2.3 Part D — Confirmation Dialog & Android API 26–36 Deletion (`VlcModals.kt`, `MediaRepository.kt`, `AndroidManifest.xml`)
- **`DeleteMediaConfirmationDialog` (`VlcModals.kt`):**
  - **Video:** Title `"Delete Video?"`, Message `"Are you sure you want to permanently delete \"[Video Title]\" from your device?"`
  - **Audio:** Title `"Delete Audio?"`, Message `"Are you sure you want to permanently delete \"[Track Title]\" from your device?"`
  - Buttons: `Cancel` and `Delete` (using `MaterialTheme.colorScheme.error`).
- **`AndroidManifest.xml`:**
  - Add `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` for Android 8.0–9 (API 26–28) legacy deletion support. Never request `MANAGE_EXTERNAL_STORAGE` and never request write permission at startup.
- **`MediaRepository.kt` (`MediaDeleteResult` & Deletion Execution):**
  - Define typed `MediaDeleteResult`:
    - `Deleted(mediaItem)`
    - `RequiresSystemConsent(mediaItem, intentSender, requiresFollowUpDelete)`
    - `RequiresLegacyWritePermission(mediaItem)`
    - `Cancelled(mediaItem)`
    - `PermissionDenied(mediaItem, message)`
    - `NotFoundOrIneligible(mediaItem, message)`
    - `Error(mediaItem, message)`
  - **API 30+ (Android 11+):** Use `MediaStore.createDeleteRequest(contentResolver, listOf(uri))` and return `RequiresSystemConsent(..., requiresFollowUpDelete = false)`. When approved (`RESULT_OK`), treat deletion as complete without calling `ContentResolver.delete()` a second time.
  - **API 29 (Android 10):** Call `contentResolver.delete(uri, null, null)`. Catch `RecoverableSecurityException` and return `RequiresSystemConsent(mediaItem, e.userAction.actionIntent.intentSender, requiresFollowUpDelete = true)` so upon user approval, `contentResolver.delete(uri, null, null)` completes the deletion.
  - **API 26–28 (Android 8–9):** Check `WRITE_EXTERNAL_STORAGE`; if not granted, return `RequiresLegacyWritePermission(mediaItem)` so `MainActivity` requests it on demand; once granted, execute `contentResolver.delete(uri, null, null)`.

### 2.4 Part E, F & G — ViewModel Coordination, Playback/Queue Safety & Library Refresh (`VlcViewModel.kt`, `VlcPlayerController.kt`, `MainActivity.kt`)
- **Playback & Queue Safety (`VlcPlayerController.onMediaItemDeleted(item)`):**
  - If the deleted item matches `currentMedia` (by `id` or `uri`), safely stop `exoPlayer`, clear `currentMedia`, reset position/duration/A–B loop state, and close full-screen/expanded player overlays so no stale mini-player or active player points to a deleted file.
  - Remove only the deleted item from `_playlist`, leaving all other queue items untouched.
- **Targeted Library Refresh (`VlcViewModel`):**
  - Refresh only `_videos` when a video is deleted (`loadVideos()`), and only `_audioTracks` when an audio track is deleted (`loadAudioTracks()`).
  - Guard against duplicate concurrent delete requests (`isDeleteInProgress`).
  - Emit Snackbar messages for confirmed deletion or failure, and perform no removal or message on user cancellation.

### 2.5 Part H — Regression Tests (`app/src/test/java/org/videolan/vlc/AboutAndMediaDeleteTest.kt`)
- **About Screen Tests:** Verify `AboutVlcDialog` renders the PLayMuse logo, `"PLayMuse"` name, and version, while confirming developer/copyright cards and `"LibVLC Engine"` text are absent from the UI and `/COPYING` + source headers remain intact.
- **Media Delete Eligibility & Workflow Tests:**
  - Verify local `content://media/external/video/media/{id}` and `content://media/external/audio/media/{id}` URIs are eligible.
  - Verify `https://` sample videos/audio and third-party authorities (`content://com.example.provider/...`) are rejected.
  - Verify confirmation cancellation performs no deletion.
  - Verify successful deletion removes the MediaStore entry, refreshes only the relevant library (video vs. audio), and leaves the other library untouched.
  - Verify permission denial and system consent cancellation keep the item in the library and do not report success.
  - Verify deleting the currently playing item clears active playback and removes only that item from the queue without disturbing other queue items.

---

## 3. Verification Plan

1. Run all unit & Robolectric tests: `gradle :app:testDebugUnitTest --rerun-tasks`.
2. Run Android Lint: `lint_applet` (`:app:lintDebug`).
3. Build debug APK: `compile_applet` (`:app:assembleDebug`).
