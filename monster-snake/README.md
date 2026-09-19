# Monster Snake

A friendly Snake game for children aged 6+. Works completely offline, has no
ads, no tracking, no accounts and no internet permission at all.

## Install the APK on an Android phone

1. Copy `MonsterSnake.apk` to the phone (USB cable, Google Drive, email to
   yourself, or download it straight from the phone's browser).
2. Open the file with the phone's **Files** app and tap it.
3. Android will ask permission to install apps from this source. Tap
   **Settings**, switch on **Allow from this source**, then tap **Back** and
   **Install**.
4. Tap **Open**. The game needs no setup, no login and no network.

Requires Android 7.0 (API 24) or newer. The APK is signed with a self-signed
key that lives in `keystore/`, so updates install over the top of each other.
If a previous version was installed with a *different* key, uninstall it first.

## How to play

Swipe anywhere on the board, or tap the big arrow buttons underneath it, to
steer the snake. Catch monsters to grow and score, dodge the walls, blocks,
roaming spiky blobs and your own tail. Each level shows its goal at the top -
catch a number of monsters, reach a score, or collect stars or coins.

* **Shield** - one free crash; the snake bounces around instead of dying.
* **Speed** - move faster and score double for a few seconds.
* **Magnet** - nearby goodies come to you.
* **Slow** - everything calms down for a moment.
* **Bonus** - instant extra points.

Coins earned in play buy new snake skins in the *Monsters* screen. Skins are
purely cosmetic - nothing in the game can be bought with real money.

## Project layout

```
monster-snake/
  app/src/main/java/com/bambiproj/monstersnake/
    MainActivity.kt   fullscreen activity, immersive mode, music lifecycle
    GameView.kt       Choreographer render loop, screen stack, touch routing
    Engine.kt         all game rules: stepping, collisions, items, power-ups
    Levels.kt         the 20 level definitions, obstacle layouts, endless mode
    PlayScreen.kt     gameplay HUD, controls, pause / win / game-over flows
    Screens.kt        main menu, level select, skin shop, settings
    Art.kt            every sprite, drawn procedurally onto the Canvas
    Ui.kt             buttons, panels, icons, chips
    Fx.kt             particles, confetti and pop-up words
    Sfx.kt            audio synthesised at first launch
    Prefs.kt          SharedPreferences save file
  keystore/           self-signed release key
```

### Design notes

* **No dependencies.** The app uses only the Android framework - no AndroidX,
  no game engine, no ad or analytics SDK. That keeps the APK small and the
  build reproducible.
* **No asset files.** Characters, scenery and icons are vector drawing code in
  `Art.kt`. Sound effects and the music loop are synthesised into WAV files in
  the app cache the first time the game runs (`Sfx.kt`).
* **No permissions.** `AndroidManifest.xml` declares none, so the game cannot
  reach the network even in principle.
* **Rendering** is a single hardware-accelerated `View` driven by
  `Choreographer`, redrawing the whole scene each frame. Snake movement is
  interpolated between grid steps so it looks smooth rather than steppy.
* **Saving** is a single `SharedPreferences` file holding coins, per-level
  stars, unlocked levels and skins, and the settings switches.

## Building another APK

Needs JDK 17 and the Android SDK (platform 35 and build-tools 35.0.0).

```bash
cd monster-snake
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

`./gradlew assembleDebug` also produces an installable APK signed with the same
key. CI builds the release APK on every push - see
`.github/workflows/monster-snake-apk.yml`, which uploads the APK as a workflow
artifact and attaches it to the `monster-snake-latest` release.

The signing key in `keystore/monster-snake.jks` (password `monstersnake`) is a
throwaway key for sideloading. Generate your own before publishing anywhere.
