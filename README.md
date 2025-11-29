# Shaarli Poster (Android)

Android app that shares links to a self-hosted Shaarli instance.

## Testers needed - minimal work

In order to get the app in Google Play store, I must recruit 12 people to test the app for 14 days.

if you have an Shaarli instance it is very easy:

 - Open the app and enter your URL and REST API key - the app tells you where to find it.
 - Share a link to the app and see it will try to fetch the title.
 - Post the link to your Shaarli.
 - Share the same link to the app and see that it will fetch the info you entered..

If you would like to help, please send me an e-mail to app at zip.dk

Thank you

## License

- GNU GPL v3, see [LICENSE](LICENSE).

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
   - If offline, posting will fail with an error; retry when back online.
6) Optional debugging:
   - View logs: `adb logcat | grep -i shaarli`
   - Clear app data: `adb shell pm clear com.shaarli.poster`

## Features
- Encrypted settings (URL + Shaarli API secret) stored with Android Keystore.
- Share flow with URL/title/description/tags/private toggle, title prefetch, and clear status/error feedback.
- Simple status indicators for connection tests and posting attempts.
