# Android DnD Roguelike

Minimal native Android starter. No game rules or engine have been chosen yet.

## Installed app
- Version display, automatic update check on launch, and manual check.
- Stable GitHub Releases from main only; drafts and prereleases are ignored.
- In-app download, progress, package/version/signing-certificate checks, and Android installation prompt.
- Android requires permission to install updates and confirmation for each installation.
- Offline startup works; failed checks have retryable status messages.

## Continue development from your phone
Edit the repository through your preferred GitHub-connected development tool. Keep applicationId and signing key unchanged. Increment BOTH values in version.properties for each release (next: versionCode=2, versionName=0.1.1). Every push to main builds and checks the app; with the signing secret configured, it publishes a signed APK and update.json. Pull requests only build and lint.

## One-time signing setup
Add the repository Actions secret SIGNING_KEYSTORE_BASE64 using the locally generated signing file supplied during setup. The keystore is PKCS12; password android-dnd-roguelike. Never commit or publish the keystore or secret text. Keep a private backup. This key must be preserved for updates.

The build intentionally skips release publication until the secret exists; it never substitutes a temporary signing key. After adding the secret, run the Android APK workflow manually. It builds and publishes v0.1.0.

## Build
JDK 17, Android SDK 35, Gradle 8.11.1, Android Gradle Plugin 8.9.1.
Run gradle :app:assembleDebug :app:lintDebug.
CI installs the pinned Gradle version using gradle/actions/setup-gradle.
For release set SIGNING_KEYSTORE_PATH to the absolute PKCS12 path and run gradle :app:assembleRelease.
