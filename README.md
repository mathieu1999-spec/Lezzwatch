# Lezzwatch

A free, no-login IPTV player for Android with a bundled channel playlist, favorites, search/filter/sort, gesture-controlled brightness/volume, Picture-in-Picture, and Google Cast — built with Kotlin, Jetpack Compose, Material 3, and Media3/ExoPlayer.

## Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17 (bundled with recent Android Studio)
- Android SDK Platform 34, min supported device: Android 8.0 (API 26)

## Getting started

1. Open this project's root folder (`Lezzwatch/`) directly in Android Studio — **File → Open**, not "Import Project."
2. Let Gradle sync. Android Studio will download the Gradle distribution and dependencies automatically on first sync (this project doesn't commit the Gradle wrapper jar; Android Studio regenerates it, or run `gradle wrapper` once from a machine with Gradle installed if you're building from the command line).
3. Run the `app` configuration on an emulator or device (API 26+).

There is no backend, no API keys, and no login required for the app to run and play channels.

## Project structure

```
app/src/main/java/com/lezzwatch/app/
├── data/
│   ├── model/        Channel, ChannelFilter, SortOption
│   ├── parser/        M3UParser, GenreClassifier, CountryCodes
│   ├── local/db/       Room (favorites)
│   ├── local/prefs/     DataStore (theme, autoplay, default sort, last channel)
│   └── repository/      ChannelRepository (single source of truth), PlaylistSource
├── di/             Tiny hand-rolled DI container + ViewModel factory helper
├── player/           PlayerActivity, PlayerViewModel, gestures, Cast, channel drawer
├── ui/
│   ├── home/ channels/ coffee/ settings/ about/   Screens + ViewModels
│   ├── components/     Shared Compose components (channel card, search field, filter chips)
│   ├── navigation/      Bottom-nav NavHost
│   └── theme/         Material 3 theme (dark-first)
└── util/            Constants, small platform helpers
```

`MainActivity` hosts Home / Channels / Support behind a bottom nav bar via Compose Navigation. Playing a channel launches a separate `PlayerActivity` — a dedicated full-screen player Activity, so Picture-in-Picture and orientation changes have a clean lifecycle instead of fighting with the bottom-nav host.

## Configuration — the two things you'll actually want to change

### 1. The playlist

The bundled playlist lives at **`app/src/main/assets/playlist.m3u`**. To ship your own channels, replace that file with your own `.m3u`/`.m3u8` file (keep the filename `playlist.m3u`, or change the filename passed to `AssetPlaylistSource` in `di/AppContainer.kt`). The parser (`data/parser/M3UParser.kt`) reads standard `#EXTINF` attributes — `tvg-id`, `tvg-name`, `tvg-logo`, `group-title` — and is tolerant of missing/malformed entries, so most real-world playlists will drop in without changes.

The app only ever bundles the playlist you give it — Lezzwatch itself contains no channel URLs of its own and doesn't attempt to work around geo-blocking, DRM, or authentication on any stream.

**A note on Country/Genre with the included sample playlist:** the sample file's `group-title` is `"English"` for every single channel (a language tag, not a category), so Genre is instead derived by matching keywords in the channel name (News/Sports/Movies/Kids/Music/Documentary/etc., falling back to "General"). Country is derived from the two-letter code embedded in `tvg-id` (e.g. `tvg-id="BBCNews.uk@SD"` → United Kingdom). If your replacement playlist has real `group-title` categories (anything other than a language name), the parser will prefer that for Genre automatically — see `GenreClassifier.NON_GENRE_GROUP_TITLES` if you need to extend that exclusion list.

### 2. Buy Me a Coffee URL

Set in **`util/Constants.kt`**:

```kotlin
const val SUPPORT_URL = "https://www.buymeacoffee.com/lezzwatch"
```

Swap it for your own buymeacoffee.com/Ko-fi/Patreon/PayPal.me page. No payment credentials of any kind are stored in the app — it's just an outbound link.

## Google Cast setup

Also in `util/Constants.kt`:

```kotlin
const val CAST_RECEIVER_APP_ID = "CC1AD845" // Google's public Default Media Receiver
```

This default lets you test casting immediately — no Cast SDK Developer Console registration required — because it points at Google's shared "Default Media Receiver," which can play back generic HLS streams on a Chromecast/Google TV/Android TV device on the same network. For a store-published app, register your own receiver at the [Google Cast SDK Developer Console](https://cast.google.com/publish) and put your app ID here instead.

The Cast integration uses Media3's `CastPlayer` (`androidx.media3:media3-cast`), which implements the same `Player` interface as `ExoPlayer` — the player screen just swaps which `Player` implementation the `PlayerView` is bound to when a cast session starts/stops. `player/cast/CastPlayerController.kt` fails soft: on a device without a working Google Play Services Cast stack, the Cast button simply doesn't render, rather than crashing.

## Known platform limitations (things that need a real device or account to verify)

- **Picture-in-Picture** and **Casting** both require testing on a real device — the emulator's PiP support is limited and there's no Chromecast to discover from an emulator. Both are implemented per Android/Media3's documented APIs and fail gracefully where the platform doesn't support them.
- **Cast content-type compatibility**: streams are declared to Cast as HLS (`application/x-mpegurl`). Not every IPTV stream is guaranteed playable on every Cast receiver (auth-walled or DRM-protected streams, non-standard containers, etc.) — the app doesn't crash on this; casting simply won't visibly succeed for that stream, matching the "don't assume every stream is cast-compatible" requirement.
- **Live-TV streams themselves**: playability of any individual channel in the playlist depends entirely on that stream still being online, geographically available, and reachable from the user's network — Lezzwatch is a player, not a stream host, and can't fix a dead upstream URL.
- The app icon (`res/drawable/ic_launcher_*.xml`) is a simple placeholder vector mark. Swap in real brand artwork before shipping — Image Asset Studio in Android Studio (right-click `res` → New → Image Asset) is the easiest way to regenerate a full icon set from your own artwork.
- `minSdk` is 26 (Android 8.0), chosen so `PictureInPictureParams.Builder` and adaptive icons are available without extra version-gating code, covering the large majority of active devices.

## Architecture notes for extending the app

- **Swapping in a remote playlist later**: `data/repository/PlaylistSource.kt` is the seam — implement the interface with a network-backed version (download → parse → fall back to the bundled asset on failure) and wire it into `AppContainer` instead of `AssetPlaylistSource`. Nothing else in the app needs to change, since `ChannelRepository` only depends on the `PlaylistSource` interface.
- **EPG, multiple playlists, recently-watched, parental controls**: none of these are implemented, but the layering (repository → ViewModel → Compose screen, with Room for structured local data and DataStore for settings) is meant to make each of these additive rather than requiring a rewrite.
- **Favorites** live in Room (`data/local/db`); **settings** (theme, autoplay-last-channel, default sort, last-watched channel id) live in DataStore Preferences (`data/local/prefs`). Both persist across app restarts, device reboots, and app updates.
- Dependency injection is a small hand-rolled container (`di/AppContainer.kt`) rather than Hilt/Koin — the app is small enough that a DI framework would be pure overhead. ViewModels get their dependencies via `viewModelFactory { initializer { ... } }`, the officially supported non-framework pattern.

## Legal

Lezzwatch is only a media player. It does not host, provide, or endorse any of the streams in a bundled playlist; the playlist is developer-supplied configuration, not app functionality, and is expected to be content you have the right to distribute/consume. The app does not implement or facilitate bypassing geo-blocking, DRM, authentication, or any other access control on any stream.
