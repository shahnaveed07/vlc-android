# PLayMuse — Android Identity & Rebranding Plan

Rebrands the imported VLC for Android application into **PLayMuse** (`com.dsc.playmuse`) under **DarkSkullCorporation** and developer **Naveed Mushtaq**, establishing an independent package identity, custom adaptive launcher iconography, and dual-attribution GPL compliance so PLayMuse installs and runs alongside official VLC without conflict.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following branding, visual identity, and attribution decisions were confirmed and govern this implementation:

- **Confirmed Decision 1 — Brand Motif & Iconography**: A custom **stylized play triangle merged with a musical note** replaces the VLC traffic cone across the adaptive launcher icon, legacy density raster icons, top app bar emblem, and placeholder media artwork.
- **Confirmed Decision 2 — Brand Color Palette**: An **Electric Indigo and Violet music theme** (`#6366F1` primary indigo, `#8B5CF6` electric violet accent, `#4F46E5` deep indigo container on `#0F0F17` obsidian studio surfaces) replaces the legacy orange accent while preserving high-contrast dark-mode readability.
- **Confirmed Decision 3 — Dual Attribution Layout**: The **About PLayMuse** view features a prominent **PLayMuse project header** crediting **Naveed Mushtaq** (Developer / Project Author) and **Copyright © 2026 DarkSkullCorporation**, followed by a dedicated **Upstream VideoLAN & LibVLC Attribution** section preserving original VideoLAN authors and GPLv2+ license terms.
- **Confirmed Decision 4 — Coexistence & Application Identity**: The application ID is set to `com.dsc.playmuse` with distinct authorities, deep-link intent handling, and app labels (`PLayMuse`), ensuring zero package or provider collision with official VLC (`org.videolan.vlc`).

---

## 1. Overview & Core Concept

- **What It Does**: Delivers a complete, rebranded Android multimedia player (**PLayMuse**) capable of local video playback with touch gestures, categorized audio playback with animated vinyl visualization, 5-band graphic equalization with bass boost and 3D surround, network stream playback (HLS/HTTP/RTSP), and local storage browsing.
- **Target Audience / Persona**: Android users who want a dedicated, independently installable media and music player built on the VLC/Media3 heritage with a modern studio-grade visual identity.
- **Key Value**: Full side-by-side installation compatibility alongside official VLC, distinct visual and launcher recognition, transparent open-source fork attribution, and zero loss of playback, streaming, equalizer, or browsing capabilities.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Launcher & Cold Start**: User sees the **PLayMuse** label and the electric indigo/violet play-note adaptive icon in the Android launcher alongside any existing VLC installation.
  2. **Main Media Hub & Top Bar**: The top bar displays the new PLayMuse emblem, current section title, instant search filtering, grid/list layout toggle, and quick access to the Equalizer and About PLayMuse modal.
  3. **Video & Audio Playback**: Tapping a video opens the full-screen gesture-enabled player (left-side vertical swipe for brightness, right-side vertical swipe for volume, double-tap for 10s seek, aspect ratio cycling, and speed control). Tapping an audio track launches the persistent mini-player and expandable spinning-vinyl Now Playing sheet.
  4. **About & Attribution Flow**: Opening **About PLayMuse** from the top menu or the More/Settings screen presents the PLayMuse brand emblem, version metadata, Developer (`Naveed Mushtaq`), Copyright (`Copyright © 2026 DarkSkullCorporation`), and a clearly separated upstream attribution block honoring VideoLAN, LibVLC, and the GNU General Public License v2+.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Modern dark audio-visual studio interface combining deep obsidian surfaces with electric indigo and violet highlights.
  - *Color Palette & Mood*:
    - `--background`: `#0F0F17` (Deep Studio Obsidian)
    - `--surface`: `#181824` (Elevated Charcoal Indigo)
    - `--surface-variant`: `#232336` (Card & Control Surface)
    - `--primary`: `#6366F1` (Electric Indigo)
    - `--accent`: `#8B5CF6` (Vibrant Violet)
    - `--text-primary`: `#F8FAFC` (Crisp Alabaster)
    - `--text-secondary`: `#94A3B8` (Muted Slate)
  - *Typography & Hierarchy*: Bold geometric titles for screen headers and media cards, paired with high-legibility tabular numerals for timestamps, durations, and frequency band readouts.
  - *Component Styling & Layout*: Material 3 adaptive cards (`12.dp`–`16.dp` corner radii), pill badges for video resolution and duration, and full edge-to-edge window inset handling across status and navigation bars.

- **Interactive Feedback & Motion**:
  - Smooth rotational animation on the Now Playing vinyl artwork synchronized with playback state.
  - Animated HUD overlays for volume, brightness, and 10-second seek gestures during fullscreen video playback.
  - Real-time progress bar synchronization between the full player, mini-player, and audio equalizer controls.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Application ID Change Without Destructive Package Directory Refactor**
  - *Chosen Approach*: Configure `applicationId = "com.dsc.playmuse"` in the module build configuration while keeping the internal Kotlin source namespace stable where appropriate, updating all user-facing strings, metadata, and fork headers.
  - *Why*: Guarantees Android OS treats PLayMuse as a completely separate application for side-by-side installation with official VLC while avoiding brittle, unnecessary directory moves.
  - *Alternatives Considered*: Renaming every internal source package folder, which adds high churn without affecting the compiled APK's `applicationId` or coexistence behavior.

- **Decision 2: Dual Copyright & GPL Compliance Strategy**
  - *Chosen Approach*: Add fork attribution headers (`Copyright © 2026 DarkSkullCorporation`, `Developer: Naveed Mushtaq`) to project files while explicitly retaining original VideoLAN copyright notices and the `COPYING` GPLv2+ license file.
  - *Why*: Respects open-source licensing rules and accurately distinguishes fork authorship from upstream VideoLAN contributions.
  - *Alternatives Considered*: Replacing all VLC strings globally, which would improperly alter technical engine references and upstream legal notices.

- **Decision 3: Multi-Density Adaptive & Legacy Icon Pipeline**
  - *Chosen Approach*: Generate a dedicated PLayMuse play-note emblem on a solid deep-indigo canvas, wire it into `ic_launcher_foreground` and `ic_launcher_background` adaptive drawables, and generate both square and circular-masked PNGs across all five `mipmap-*` density buckets.
  - *Why*: Ensures crisp launcher presentation on Android 8.0+ adaptive launchers as well as older or custom OEM launchers that read raster `ic_launcher.png` and `ic_launcher_round.png`.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     Android OS & Package Manager                         │
│   Application ID: com.dsc.playmuse   │   Launcher Label: PLayMuse        │
│   Adaptive & Raster Mipmap Icons     │   Coexists with org.videolan.vlc  │
└──────────────────────────────────┬───────────────────────────────────────┘
                                   │
                                   ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                        MainActivity & PLayMuseTheme                      │
│   Edge-to-Edge Scaffold │ Electric Indigo/Violet Palette │ Permissions   │
└───────┬──────────────────────────┬───────────────────────────┬───────────┘
        │                          │                           │
        ▼                          ▼                           ▼
┌───────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│  Top Bar & Modals │    │   5-Tab Navigation  │    │  Player Overlays     │
│  • PLayMuse Logo  │    │  • Video Grid/List  │    │  • VideoPlayerScreen │
│  • Search Filter  │    │  • Audio Categories │    │  • NowPlayingScreen  │
│  • Equalizer      │    │  • Network Streams  │    │  • MiniPlayer Bar    │
│  • About PLayMuse │    │  • Storage Browser  │    │  • Gesture HUD       │
│  • Media Info     │    │  • More / Settings  │    │  • Speed & Sleep     │
└─────────┬─────────┘    └──────────┬──────────┘    └──────────┬───────────┘
          │                         │                          │
          └─────────────────────────┼──────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     VlcViewModel (PLayMuse State Layer)                  │
│   Reactive StateFlows: Media Lists, Search, Active Modal, Browse Path    │
└───────────────────┬──────────────────────────────────┬───────────────────┘
                    │                                  │
                    ▼                                  ▼
┌──────────────────────────────────────┐   ┌───────────────────────────────┐
│           MediaRepository            │   │      VlcPlayerController      │
│  • Android MediaStore Video/Audio    │   │  • Playback & Queue Control   │
│  • Curated & Custom Stream Bookmarks │   │  • 5-Band Hardware Equalizer  │
│  • Local Directory File Explorer     │   │  • BassBoost & Virtualizer    │
└──────────────────────────────────────┘   └───────────────────────────────┘
```

- **Data Model & State**:
  - **Identity & Attribution Metadata**: Application name (`PLayMuse`), package identifier (`com.dsc.playmuse`), copyright holder (`DarkSkullCorporation`), developer (`Naveed Mushtaq`), and upstream attribution (`VideoLAN / LibVLC Authors`).
  - **Media & Playback State**: `MediaItem` entities (id, title, artist, album, duration, URI, resolution), `StreamBookmark` entries, `EqualizerPreset` band configurations, and `VlcSettings` preferences managed reactively via `StateFlow`.

- **Interactive Component & State Mapping**:
  - **Search & View Mode Toggle (`VlcTopBar`)**: Updates `searchQuery` and `isGridMode` in `VlcViewModel`, dynamically filtering displayed video/audio collections and switching between `LazyVerticalGrid` and `LazyColumn`.
  - **About & Attribution Dialog (`AboutVlcDialog` & `SettingsScreen`)**: Displays the PLayMuse header, `Naveed Mushtaq` developer badge, `Copyright © 2026 DarkSkullCorporation`, and upstream VideoLAN/LibVLC credits with GPLv2+ license details.
  - **Playback & AudioFX Controls (`VideoPlayerScreen`, `NowPlayingScreen`, `EqualizerScreen`)**: Mutates playback state, speed, aspect ratio, sleep timer countdown, and hardware audio session effects (`Equalizer`, `BassBoost`, `Virtualizer`).
  - **Git Branch & Commit Workflow**: All rebranding changes are committed cleanly onto the dedicated `branding/playmuse-identity` branch without modifying upstream remote history.
