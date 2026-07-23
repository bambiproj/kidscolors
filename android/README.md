# Doc Scanner (Android)

A simple, friendly document scanner that works **entirely on-device**. Open the
app to a warm control panel — *"What are we scanning today, my love?"* — capture
your pages with the camera, and save them as a single **PDF** in your
**Downloads** folder.

## No network, ever

The app does **not** request the `INTERNET` permission, so it physically cannot
reach the network. Pages are captured with your device's camera app and the PDF
is assembled locally with Android's built-in `PdfDocument`. Nothing leaves the
phone.

## How it works

1. **Add a page** — opens your camera; snap a photo of the document.
2. Repeat for as many pages as you like; thumbnails appear in the panel.
3. **Enhance** (on by default) gives pages a crisp grayscale "scanned" look.
   Turn it off to keep full colour.
4. **Save as PDF** — writes one multi-page PDF to Downloads.
5. **Open** or **Share** the result.

## Requirements

- Android 10 (API 29) or newer.
- A camera app (essentially every phone has one). No special permissions are
  requested — the system camera app handles the capture.

## Install the APK

1. Download `DocScanner-debug.apk` (from the GitHub **Release** named
   *"Doc Scanner (latest debug build)"*, or from `dist/DocScanner-debug.apk` in
   this branch).
2. On your phone, allow installing from your browser / file manager
   (**Settings → Apps → Special access → Install unknown apps**).
3. Open the APK and tap **Install**.

The APK is **debug-signed**, so it installs directly for personal use.

## Build it yourself

Built automatically by GitHub Actions (`.github/workflows/android-build.yml`) on
every push that touches `android/`. Locally (needs the Android SDK, platform 35,
build-tools 35.0.0):

```bash
cd android
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

- `app/src/main/java/com/bambiproj/docscanner/MainActivity.kt` — capture pages,
  enhance, and assemble the PDF (all on-device).
- `app/src/main/res/layout/activity_main.xml` — the control panel UI.
- `app/src/main/res/values/` — strings, colours, theme, launcher icons.
