# Doc Scanner (Android)

A simple, CamScanner-style document scanner. Tap **Scan Document**, capture one
or more pages (the camera automatically detects document edges, lets you crop
and apply filters), and the app saves the result as a **PDF** in your
**Downloads** folder. You can then open or share it.

It is built on **Google's ML Kit Document Scanner**, which provides the full
capture-crop-enhance flow and produces the PDF directly.

## Requirements

- Android 10 (API 29) or newer.
- **Google Play Services** must be present and up to date on the device (this is
  the case for virtually all standard Android phones). The scanner UI is
  downloaded on demand the first time you scan.

## Install the APK

1. Download `DocScanner-debug.apk` (from the GitHub **Release** named
   *"Doc Scanner (latest debug build)"*, or from `dist/DocScanner-debug.apk` in
   this branch).
2. On your phone, allow installing from your browser / file manager
   (**Settings → Apps → Special access → Install unknown apps**).
3. Open the APK and tap **Install**.

The APK is **debug-signed**, so it installs directly for personal use without
Play Store distribution.

## Build it yourself

The APK is built automatically by GitHub Actions
(`.github/workflows/android-build.yml`) on every push that touches `android/`.

To build locally you need the Android SDK (platform 35, build-tools 35.0.0):

```bash
cd android
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

- `app/src/main/java/com/bambiproj/docscanner/MainActivity.kt` — the whole app:
  launches the scanner and saves the resulting PDF to Downloads.
- `app/src/main/res/` — layout, strings, theme, launcher icons.
- `app/build.gradle.kts` — dependencies (ML Kit Document Scanner) and SDK config.
