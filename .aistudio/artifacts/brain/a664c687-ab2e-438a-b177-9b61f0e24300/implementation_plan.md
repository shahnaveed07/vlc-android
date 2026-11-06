# Task 04 — Complete Audio/Video Playback Controls, Sleep Timer and A–B Segment Loop

## 1. Objective & Scope

Incrementally enhance the audio and video playback controls in **PLayMuse** (`:app`) while preserving the Black & Gold theme, the AndroidX Media3 / ExoPlayer 1.5.1 engine, the removed Streams state, and all existing application functionality:

1. **Unified Playback Options Panel:** Provide an accessible Material 3 Playback Options bottom sheet/panel inside both `VideoPlayerScreen` and `NowPlayingScreen`.
2. **Shared Audio & Video Sleep Timer:** Expose Sleep Timer in both video and audio players, add custom duration (`hours` + `minutes`, 1 min to 24 hours) with input validation, implement a drift-free monotonic countdown updating every second (`MM:SS` / `HH:MM:SS`), and pause playback on expiry.
3. **A–B Segment Repeat:** Implement A–B segment looping in `VlcPlayerController` using ExoPlayer `PlayerMessage` scheduling at point B (`setDeleteAfterDelivery(false)`), with full segment validation, seek-bar A/B visual markers, manual-seek handling, and automatic reset on media change.
4. **Playback Duration Formatting Fix:** Introduce a shared hour-aware duration formatter (`00:45`, `59:59`, `1:00:00`, `1:30:00`, `12:05:40`) used across `VideoPlayerScreen`, `NowPlayingScreen`, and `MediaModels`.
5. **Regression Tests:** Add unit and Robolectric tests covering Sleep Timer presets/custom durations/expiry, A–B segment loop validation/cycling/media transitions, duration formatting, and UI control accessibility.

---

## 2. Planned Changes by File

### 2.1 `app/src/main/java/org/videolan/vlc/model/MediaModels.kt`
- **Duration & Timer Formatting Helpers:**
  - `formatPlaybackDuration(durationMs: Long, allowZero: Boolean = true): String`:
    - Formats `< 60` minutes as `MM:SS` (`00:45`, `09:59`, `59:59`).
    - Formats `>= 60` minutes as `H:MM:SS` / `HH:MM:SS` (`1:00:00`, `1:30:00`, `12:05:40`).
    - Safely returns `"--:--"` for negative or unknown durations (`durationMs < 0L` or `!allowZero && durationMs == 0L`).
  - `formatSleepTimerCountdown(totalSeconds: Int): String`:
    - Formats `< 1` hour as `MM:SS` (`15:42`, `00:09`) so remaining seconds never display as `0m`.
    - Formats `>= 1` hour as `HH:MM:SS` (`01:05:30`).
- **Sleep Timer Custom Validation Helper:**
  - `validateCustomSleepTimerMinutes(hoursText: String, minutesText: String): Int?` (and detailed validation result) enforcing `1..1440` minutes (`1 minute` to `24 hours`) and rejecting zero, negative, malformed, or out-of-range inputs.

### 2.2 `app/src/main/java/org/videolan/vlc/player/VlcPlayerController.kt`
- **Lifecycle-Safe Coroutine Scope:**
  - Use `CoroutineScope(SupervisorJob() + mainDispatcher)` and cancel the scope inside `release()`.
- **Monotonic Sleep Timer:**
  - Validate `minutes in 1..1440` in `setSleepTimer(minutes: Int): Boolean`.
  - Cancel any existing `sleepTimerJob` before starting a new timer.
  - Track a monotonic deadline (`elapsedRealtime() + minutes * 60_000L`) and update `_sleepTimerSeconds` every second without drift.
  - On expiry, set `_sleepTimerSeconds.value = null` and call `exoPlayer.pause()` while preserving the current media item and queue.
- **A–B Segment Repeat State & `PlayerMessage` Scheduling:**
  - Expose reactive `StateFlow` properties:
    - `abPointAMs: StateFlow<Long?>`
    - `abPointBMs: StateFlow<Long?>`
    - `isAbPointASelected: StateFlow<Boolean>`
    - `isAbPointBSelected: StateFlow<Boolean>`
    - `isAbLoopActive: StateFlow<Boolean>`
    - `abLoopStatusMessage: StateFlow<String>`
    - `abLoopErrorMessage: StateFlow<String?>`
    - `isCurrentMediaSeekable: StateFlow<Boolean>`
  - Provide controller methods:
    - `setPointA(positionMs: Long = currentPositionMs()): Boolean`
    - `setPointB(positionMs: Long = currentPositionMs()): Boolean`
    - `activateAbLoop(): Boolean`
    - `clearAbLoop()`
  - **Validation:**
    - Require seekable media with a known finite duration (`duration > 0`). Disable and explain when media is live, unseekable, or has unknown duration.
    - Require `0 <= A < duration` and `A < B <= duration`. Reject `B <= A` with a clear error message without activating an invalid loop.
  - **Loop Execution:**
    - Schedule an ExoPlayer `PlayerMessage` at position `B` on the current media item index with `setDeleteAfterDelivery(false)`.
    - When `B` is reached (or if `B` is at the end of media and `STATE_ENDED` triggers), seek immediately to `A`, continue playback if playing, or preserve pause state if paused.
    - Handle manual seeking (`seekTo` / `seekRelative`) predictably so the loop stays active and synchronized.
    - Automatically clear A–B markers when switching to a different media item (`playMedia`), while preserving A–B markers across `retry()` of the same media item.

### 2.3 `app/src/main/java/org/videolan/vlc/ui/modals/VlcModals.kt`
- **Enhanced `SleepTimerDialog`:**
  - Keep existing presets (`15 minutes`, `30 minutes`, `45 minutes`, `1 hour`, `1.5 hours`).
  - Add custom `Hours` and `Minutes` inputs (`1 min` – `24 hours`) with inline error feedback for invalid values.
  - Show live formatted countdown (`15:42` / `01:05:30`), state clearly that playback will pause when the timer expires, and provide `Cancel Active Timer`.
- **Unified `PlaybackOptionsSheet` (and `AbRepeatControlsCard`):**
  - Provide a compact Material 3 bottom sheet styled in the Black & Gold theme:
    - **Video Mode:** Playback speed, Sleep Timer (with live countdown), A–B segment repeat (`Set A`, `Set B`, `Activate Loop`, `Clear Loop`, status & timestamps), Aspect ratio, Equalizer, and Lock controls.
    - **Audio Mode:** Playback speed, Sleep Timer (with live countdown), A–B segment repeat (`Set A`, `Set B`, `Activate Loop`, `Clear Loop`, status & timestamps), Shuffle, Repeat (`Off` / `All` / `One`), Equalizer, and Queue transport controls.

### 2.4 `app/src/main/java/org/videolan/vlc/ui/video/VideoPlayerScreen.kt` & `MainActivity.kt`
- Pass `onOpenSleepTimer = { viewModel.showModal(ActiveModal.SLEEP_TIMER) }` from `MainActivity.kt` into `VideoPlayerScreen`.
- Add Sleep Timer, A–B Repeat quick controls/status pill, and **Playback Options** button to `VideoPlayerScreen`.
- Render `A` and `B` visual markers along the video seek bar when set.
- Replace inline `%02d:%02d` modulo formatting with `formatPlaybackDuration(...)`.

### 2.5 `app/src/main/java/org/videolan/vlc/ui/audio/NowPlayingScreen.kt`
- Add A–B Repeat status/controls and **Playback Options** button to `NowPlayingScreen`.
- Render `A` and `B` visual markers along the audio seek bar when set.
- Display accurate Sleep Timer countdown (`formatSleepTimerCountdown(...)`) instead of `${sleepSeconds / 60}m`.
- Replace inline `%02d:%02d` modulo formatting with `formatPlaybackDuration(...)`.

### 2.6 Regression Tests (`app/src/test/java/org/videolan/vlc/PlaybackControlsAndLoopTest.kt`)
- **Duration & Timer Formatting Tests:** Short (`00:45`), `09:59`, `59:59`, `1:00:00`, `1:30:00`, `12:05:40`, and sleep timer countdown (`15:42`, `01:05:30`, `< 60s` never showing `0m`).
- **Sleep Timer Tests:** All presets (`15`, `30`, `45`, `60`, `90`), custom duration validation (`1m`..`24h`, rejecting `0`, negative, malformed, `> 24h`), replacing active timer, cancelling timer, expiry pausing playback and resetting timer state, single active timer job, and shared state across audio and video.
- **A–B Segment Loop Tests:** Set A, Set B, reject `B < A`, reject `B == A`, activate valid loop, loop jump from `B` to `A` across repeated cycles, clear loop, clear markers on media change, preserve markers on retry, independence from track `RepeatMode`, rejection on unseekable/unknown-duration media, manual seek behaviour, and pause/resume preservation.
- **UI Verification Tests:** Verify Playback Options, Sleep Timer, and A–B Loop controls and status indicators are accessible in both `VideoPlayerScreen` and `NowPlayingScreen`.

---

## 3. Verification Plan

1. Run unit and Robolectric tests: `gradle :app:testDebugUnitTest --rerun-tasks`.
2. Run Android Lint: `lint_applet` (`:app:lintDebug`).
3. Build debug APK: `compile_applet` (`:app:assembleDebug`).
4. Verify Black & Gold theme integrity, Streams removal preservation, and Media3 engine preservation.
