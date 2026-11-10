# Resonance

A local-files music player for Android, designed with Material 3 / Material You.
No accounts, no ads, no internet permission: it plays the music that is already on your phone.

## Features

- **Library**: songs, albums and artists from the device (MediaStore), updated automatically
- **Search**: across titles, artists and albums, ignoring case and accents
- **Playlists and queue**: create and edit playlists, play next, add to queue, reorder, save the queue as a playlist
- **Equalizer**: system equalizer with presets, bass boost and virtualizer (where the device supports them)
- **Statistics**: plays, listening time, top songs and artists, recently played (stored only on the device)
- **Album colors**: the full-screen player and mini player take their colors from the current cover
- **Visualizer**: live spectrum ring around the cover, measured from the player's own audio (no microphone permission)
- **Gestures on the cover**: swipe for next/previous, swipe down to close, double-tap the sides for ±10 s, tap to toggle the ring
- **Favorites**, **folders**, **sleep timer**, playback speed, skip silence
- **Settings**: light/dark/AMOLED theme, wallpaper or accent colors
- **Lyrics** from .lrc files in a folder you choose, **vinyl record mode**, drag-and-drop queue, smart lists, daily chart and streaks in statistics
- **1.2.2**: multi-select songs, speed slider, wavy seek bar, fade between songs
- **1.2.1**: song sorting, background photo, go to artist/album, pitch, custom covers, own lyrics, edit title/artist/album (stored in the app only)
- **Home screen widget** with cover and playback buttons
- Background playback with notification and lock-screen controls (Media3)
- Material You wallpaper colors, light and dark theme, English and Russian

## Build

Requires JDK 17 and Android SDK 35.

```
./gradlew testDebugUnitTest assembleDebug
```

The APK appears in `app/build/outputs/apk/debug/`.

### GitHub Actions

- `ci.yml` runs unit tests, lint and builds a debug APK (downloadable from the run's artifacts).
- `release.yml` runs on tags like `v1.0.0` and publishes an APK to GitHub Releases.
  To sign it, add the secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`;
  without them an unsigned APK is attached.

```
git tag v1.0.0 && git push origin v1.0.0
```

## Privacy

Resonance declares no `INTERNET` permission. Playlists and statistics live in a local database and are never uploaded.

## How the cover colors work

`core/AlbumScheme.kt` converts the dominant cover color to CIELAB and builds a Material 3 tonal palette
(primary, secondary, tertiary, neutral) at the standard light/dark tones, clipping chroma to what sRGB can show.
It has no Android dependencies and is covered by unit tests.

## License

MIT
