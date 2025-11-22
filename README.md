# Shaarli Poster (Android)

Early scaffolding for an Android app that shares links to a self-hosted Shaarli instance. The app is built with Kotlin, Jetpack Compose, and a minimal MVVM setup.

## Prerequisites
- JDK 17
- Android SDK with Platform 34 (via Android Studio or `sdkmanager`)
- Optional: local Gradle installation to generate the wrapper (`gradle wrapper --gradle-version 8.3.2`)

## Project structure
- `app/` – Android application module
  - `MainActivity` handles launcher/share intents.
  - `ui/` – Compose UI (theme, screens, view model).
  - `data/metadata/OkHttpTitleFetcher` – fetches page titles for prefill (timeouts applied).
  - `util/UrlNormalizer` – base URL normalization helper.
  - `src/test` & `src/androidTest` – unit and instrumentation test scaffolding.

## Setup
1) Ensure Android SDK and an emulator or device are available.
2) (Optional, if no Gradle wrapper is present) Generate the wrapper with a local Gradle install (8.4 recommended for AGP 8.3.x):
   ```bash
   gradle wrapper --gradle-version 8.4
   ```
3) Open the project in Android Studio **or** build from the CLI.

## Build on Linux
From the project root:
```bash
./gradlew assembleDebug        # or: ./gradlew assembleRelease
./gradlew test                 # unit tests
./gradlew connectedAndroidTest # instrumentation tests (emulator/device required)
```
Or use the helper script:
```bash
bash build.sh            # installs deps + builds
bash build.sh --build-only  # assumes deps/SDK/Gradle already installed
```

## Run on a device
1) Enable USB debugging on the phone and connect it (or start an emulator).
2) Verify ADB sees your device:
   ```bash
   adb devices
   ```
   You should see your device listed as `device` (not `unauthorized`). If unauthorized, check the device screen for an RSA prompt and accept it.
3) Build & install directly from Gradle:
   ```bash
   ./gradlew installDebug
   ```
   This will push `app-debug.apk` to the connected device or running emulator.
4) (Alternative) Build then sideload manually:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Use `-d` to target a specific device by serial if multiple are connected: `adb -s <serial> install -r ...`
5) Launch and test:
   - Open “Shaarli Poster” from the launcher to set your Shaarli URL and credentials.
   - From a browser or any app, use Android’s Share action on a link and pick “Shaarli Poster”; the URL/title should prefill.
   - If offline, save as draft and retry when back online.
6) Optional debugging:
   - View logs: `adb logcat | grep -i shaarli`
   - Clear app data: `adb shell pm clear com.shaarli.poster`

## Features
- Encrypted settings (URL + Shaarli API secret) stored with Android Keystore.
- Share flow with URL/title/description/tags/private toggle, title prefetch, offline draft saving, and retry for pending drafts.
- Simple status indicators for connection tests and posting attempts.
- Shaarli API calls use JWT HS512 tokens generated from the configured API secret, sent as `Authorization: Bearer <token>` (and `jwt: <token>` for compatibility), per Shaarli API docs.

## Google Play submission (high level)
1) Create or reuse a signing key:
   ```bash
   keytool -genkeypair -v -keystore shaarli-poster.keystore -alias shaarli -keyalg RSA -keysize 4096 -validity 10000
   ```
2) Configure signing in `app/build.gradle.kts` (or via Android Studio) and enable Play App Signing.
3) Build a release bundle:
   ```bash
   ./gradlew bundleRelease
   ```
4) Upload `app/build/outputs/bundle/release/app-release.aab` in the Play Console, create store listing, privacy policy, and roll out an internal test track before production.

## Current state
- Launcher + share intent plumbing with Compose UI for settings and share form.
- Title prefill scaffolding using OkHttp with basic HTML parsing.
- Tests for URL normalization, title prefill logic, and a basic UI existence check.
- Posting/auth flows, persistence, offline queueing, and robust error handling still need implementation (see `todo.md`).
