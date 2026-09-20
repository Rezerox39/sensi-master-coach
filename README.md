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

- SENSIS-inspired matte black/red/gold Compose theme.
- Splash screen at startup.
- Multi-step setup wizard (device, DPI, play style, experience, control).
- Deterministic sensitivity generator and calibration loop.
- Real crosshair editor: live preview, presets, colors, export, saved list (in-app only).
- Profile export preview and improvement history.
- Original blob mascot with no mouth and gyro-reactive blinking eyes.
- Offline-first; community, sync, and remote AI are post-MVP.
## CI Build

`.github/workflows/build-apk.yml` builds on an amd64 GitHub runner: unit tests, lint, and `assembleDebug`, then uploads `app-debug.apk` as an artifact (`dist/app-debug-apk/` after `gh run download`).
