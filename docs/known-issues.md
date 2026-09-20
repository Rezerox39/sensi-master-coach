# Known Issues

- Persistence is not implemented yet; MVP state resets after process death.
- Creator dataset is intentionally sparse until reliable sources are collected.
- Community sharing is local/export-only until a backend and moderation workflow exist.
- Remote AI is not implemented; the coach uses local deterministic rules and copy.
- Device hardware metrics are limited to Android APIs exposed on the device.
# Known Issues

## arm64 Linux host cannot build with local SDK AAPT2

The Android SDK ships x86-64-only `aapt2` binaries, and this aarch64 host's kernel has no binfmt_misc, so native builds fail at resource processing. Use the GitHub Actions workflow (`Build APK`) for builds; it uploads `app-debug.apk` as an artifact.
