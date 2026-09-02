# Vite-like without Android Studio

You asked for Vite HMR-like workflow for StreetComplete without AS. Done.

## What you have now

- **Emulator**: `Small_Phone` `android-34` `arm64` `7.7G /data` `emulator-5554 device` `sys.boot_completed=1` at `/Users/slavendjervida/Library/Android/sdk/emulator/emulator`
- **JDK**: `openjdk@21` `21.0.12.1` at `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` (linked to `~/Library/Java/JavaVirtualMachines/openjdk-21.jdk`) — required for `buildSrc:compileKotlin` `languageVersion=21` `Gradle 9.7.1` `Kotlin 2.4.0` `Compose 1.12.0`.
- **Gradle**: `9.7.1` daemon `Xmx4096m` `org.gradle.parallel=true` `caching=true` `configureondemand=true` in `gradle.properties:7-12`. First build `23m46s` `68 tasks`, incremental `2m20-24s` `3-7 tasks` (10x faster).
- **Package**: release `de.westnordost.streetcomplete` `v63.4` `95967409` bytes `/tmp/StreetComplete-v63.4.apk` vs debug `de.westnordost.streetcomplete.debug` `116M` `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Vite-like script (no AS)

`./vite-like.sh` at project root — `chmod +x` done `vite-like.sh:1-65`:

```bash
./vite-like.sh
```

What it does:
1. `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/...`
2. Checks `adb devices` `emulator-5554`, starts `emulator -avd Small_Phone` if needed, waits `sys.boot_completed`.
3. `gradlew --stop` (kill wrong JDK daemon) + `./gradlew :androidApp:installDebug` (warm, preserves `/data` — **no** `uninstall`, **no** `wipe-data` unlike earlier `5.8G 100%` failure).
4. `am start -n de.westnordost.streetcomplete.debug/...MainActivity`.
5. Watches `app/src` `androidApp/src` via `fswatch` (installed `1.22.0` `/opt/homebrew/bin/fswatch`) — on `.kt/.xml` change: debounce 1s → `./gradlew :androidApp:installDebug` → `am start`. Logs tail. `Ctrl+C` stops.

Preserves data: `adb install -r` not `uninstall` + `wipe-data`. State like `hasShownTutorial` `MainScreen.kt:175` and `isLoggedIn` kept.

### Alternative without fswatch

If `fswatch` missing:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew :androidApp:installDebug --continuous
```

Gradle watches task inputs (like `vite --watch`). Edit `CompassButton.kt:19-44`, save — auto rebuild + reinstall.

## Live Edit vs this

| Feature | Vite (web) | AS Live Edit | This (no AS) |
|---------|------------|--------------|--------------|
| Push `@Composable` body | 0.1s websocket | 0.8s JVMTI | 2m20s `assembleDebug` incremental `7 tasks` / `3 tasks` |
| Resources `strings.xml` | HMR | needs restart | rebuild |
| Native `MapLibre` | — | restart | restart |
| Preserves state | yes | yes | yes (`-r`) |
| Needs AS | no | yes 4GB+ RAM | no |

Compose Hot Reload CLI (`composeHotReload` gradle task) exists for `Compose 1.12.0` but not wired for `KMP/android` `9.1.1` `android 9.1.1` library — would need `hotReload { }` block. Not stable, so script approach is practical.

## Why app reset like freshly installed

Earlier we did:
- `adb uninstall` before `install` → deletes `/data/data/de.westnordost.streetcomplete.debug/`
- `emulator -wipe-data` to fix `5.8G 100%` `/data` full → `7.7G 523M free`
- Release vs debug `package` distinct `debug` suffix `app/build.gradle.kts:85` `namespace streetcomplete` — each has separate `/data`.

Now with `installDebug -r` and no wipe, edits preserve quest answers, login, map position like Vite.

## Usage

```bash
# one-shot fast rebuild (2m vs 23m)
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew :androidApp:installDebug

# vite-like watch
./vite-like.sh

# manual install preserving data
adb -s emulator-5554 install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s emulator-5554 shell am start -n de.westnordost.streetcomplete.debug/de.westnordost.streetcomplete.screens.main.MainActivity
```

Edit `CompassButton.kt` to test: change `AnimatedVisibility` to `alpha 0.35` variant `vite-like.sh:26-32` and watch auto-reinstall.

## Next

- Keep emulator running `PID 55747` `qemu-system-aarch64` to avoid `2m` boot.
- Don't change `compileSdk 37` `build.gradle.kts:86` without clean (forces full 23m).
- For true 0.8s hot reload, later wire `org.jetbrains.compose.hotReload` plugin (needs `AGP 9.1.1` + `kotlin 2.4.0` compat check).

