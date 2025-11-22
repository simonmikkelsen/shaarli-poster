# Shaarli Poster – Requirements

## Purpose
Create an Android application that lets users save links to their self-hosted Shaarli instance directly from the system share sheet, with minimal setup and reliable posting.

## Scope & Users
- Audience: Shaarli users who want to post bookmarks from mobile browsers and apps.
- In scope: Android client app, authentication to a single Shaarli instance, link submission flow, metadata prefill, basic settings, automated tests, build/release documentation.
- Out of scope: Hosting Shaarli itself, account creation on Shaarli, browsing existing bookmarks, editing/deleting bookmarks, multi-account support, tablet-specific layouts.

## Platforms & Constraints
- Android target: API 24+ (Android 7.0+) for modern share intent support; strive for compatibility up to latest stable Android release.
- Form factor: Phone-first, responsive layouts.
- Offline: Draft input persists locally, but posting requires network connectivity.
- Data is stored locally only for settings and unsent drafts.

## Assumptions
- Shaarli instance exposes the official API for creating links (token-based), or standard session-based login with CSRF support; approach is configurable per instance if needed.
- User provides the base URL to their Shaarli instance and credentials/API secret.
- Network access to the Shaarli instance is available from the device.

## Functional Requirements
### First-run & Settings
- Prompt user to enter and save the Shaarli base URL.
- Support authentication: username/password with session, or API token/secret; validate and store securely (Android Keystore-backed preferences).
- Show connectivity/authentication status and last successful sync time.
- Allow editing/replacing stored instance settings and logging out (clearing credentials and cookies/tokens).

### Link Sharing & Posting
- Register as a share target for URLs (ACTION_SEND with text/plain or url data).
- When invoked via share: open a compose dialog/screen with fields:
  - URL (prefilled from shared content).
  - Title (attempted prefill by fetching the URL and parsing `<title>`; allow manual edit).
  - Description/notes (optional text area).
  - Tags (comma-separated input with chip visualization).
  - Private/Public toggle if supported by Shaarli API.
- Allow invoking the compose screen from inside the app with manual URL entry.
- Permit posting immediately or saving as local draft if offline; queued drafts auto-post when back online and authenticated.
- Provide progress indicator during post; show success toast + clear form; show non-blocking, actionable error messages on failure (with retry).

### Networking & Metadata Fetch
- Fetch page title over HTTPS if available; respect redirects and common encodings; apply timeout and size limits to avoid long waits.
- Do not block UI while fetching metadata; show loading indicator and allow user override if fetch fails.
- Use modern HTTP client with connection and read timeouts; follow system proxy settings.

### Security & Privacy
- Store credentials/tokens in encrypted storage; never log secrets.
- Validate and normalize Shaarli base URL to prevent malformed requests.
- Use TLS for remote calls when available; warn on plain HTTP.
- Respect Android scoped storage; do not request unrelated permissions.

### Error Handling & Observability
- User-visible errors for network failures, auth errors, or invalid inputs with guidance to fix.
- Log non-sensitive diagnostics for debugging; provide optional in-app log export with PII redaction.

### Accessibility & UX
- Support TalkBack labels, focus order, and sufficient contrast.
- Support both light and dark themes following Material baseline components.
- Keep interactions reachable with one hand; ensure touch targets ≥48dp.

## Non-Functional Requirements
- Performance: Prefill fetch returns or times out within 3 seconds by default; posting attempts return within 5 seconds under normal network conditions.
- Reliability: No data loss for drafts on process death; crash-free rate goal ≥99.5% for release builds.
- Internationalization: English-first; layout ready for future localization.
- Maintainability: Modular architecture (e.g., MVVM), clear boundaries between UI, networking, and storage.
- Testing: Automated tests required for every feature change; write failing test before fixing a bug. Include unit tests for parsing, networking logic (with mocks), and instrumentation/UI tests for share flow.

## Build, Release, and Distribution
- Provide Gradle-based project with reproducible builds.
- README must include Linux build steps, device testing instructions (ADB), and Play Store submission guidance (signing, bundles, Play Console).
- Release build must support Play App Signing and generate a signed Android App Bundle (AAB).

## Compliance
- Follow Google Play policies for permissions and user data.
- Include open-source licenses for third-party libraries in the app and repository.
