# Sensi Master Coach

**Your Personal FF Sensi Coach**

Sensi Master Coach is an independent Free Fire companion/reference Android app. It helps players generate, test, adjust, and save manual sensitivity setups.

The app does not modify, inject into, automate, or interfere with Free Fire or Free Fire MAX.

## Build

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew bundleRelease
```

## MVP Status

- Compose AMOLED UI.
- Onboarding and first profile.
- Deterministic sensitivity generator.
- Calibration adjustment loop.
- In-app crosshair preview.
- Tool center, profile export preview, history, and safety copy.
- Offline-first; community and remote AI are post-MVP.
## CI Build

`.github/workflows/build-apk.yml` builds on an amd64 GitHub runner: unit tests, lint, and `assembleDebug`, then uploads `app-debug.apk` as an artifact (`dist/app-debug-apk/` after `gh run download`).
