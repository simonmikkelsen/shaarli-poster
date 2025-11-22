# Todo

- [x] Project setup: initialize Android project (Gradle, Kotlin), set minSdk 24, configure package name, versioning, and signing configs placeholders.
- [x] Dependencies: choose HTTP client (OkHttp), coroutines, and Compose stack; document licenses.
- Architecture: define modules (network, auth/session, metadata fetcher, repository, UI with MVVM), set up ViewModel/UseCase scaffolding, and navigation to settings/compose screens.
- Settings & auth: implement URL validation, credential/token storage via EncryptedSharedPreferences/Keystore, login validation call, logout/clear data flow.
- Share flow UI: register share intent filters, build compose screen/dialog with URL/title/description/tags/private toggle, tag chip UI, input validation, loading/error states.
- Metadata prefill: implement background fetch for page title with timeouts and redirects, ensure user override and graceful failure.
- Posting logic: implement Shaarli link creation API calls (token or session-based), handle success/failure, offline draft queue with retry and persistence.
- Draft storage: local DB or file persistence for pending posts; auto-retry on connectivity/auth restoration.
- Error handling & telemetry: standardized error mapper, non-sensitive logging, optional log export with redaction.
- Theming & accessibility: light/dark themes, contrast, TalkBack labels, touch target sizing.
- Testing: unit tests for URL normalization, metadata parsing, API client; UI/instrumentation tests for share intent and posting flow; ensure tests precede bug fixes.
- Tooling: set up CI (lint, unit tests, instrumentation if feasible), static analysis (ktlint/Detekt), and Gradle tasks for debug/release builds.
- Documentation: write README with Linux build steps, device testing (ADB), signing/AAB instructions, and Play Store submission guide; include licenses file.
