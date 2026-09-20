# APK Study Notes

Reviewed four uploaded third-party apps for feature and UX inspiration. All implementation here is original: no assets, branding, or copy were copied.

## Apps reviewed

- **FF Sensi Pro 3.0.0** (`com.gaurav.ffsensi`) - small sensi generator tool, branded splash, web-ish pages.
- **SENSIS そ 3.2** (`gg.manuelita.freesensis`) - minimal "sensi" coach: onboarding, device search, generator, saved setups, nick review, motivation/help, VIP, update log.
- **Sensi Pro & Game Tools 14.2** (`com.sensi.pro.booster.ffgame`) - largest: starting form (RAM/storage/DPI/fire-button), personalize, results, saved sensi, settings, crosshair editor, theme pickers, floating tools.
- **Sensi Master 4.2.7** (`com.allakore.sensimasterff`) - preference-driven saved sets, ads, notifications.

## Theme adopted (SENSIS-style)

- Matte black backgrounds: `#151515` (surface), `#141414`/`#1b1b1b` gray panels, `#2e2e2e` borders.
- Red accents: `#ff0000` primary, `#c60000` buttons; gold `#ffd700` secondary highlights (from Pro & Tools).
- White headline text with bold weights, minimal UI, dark status/nav bars.

Mapped in `Theme.kt`: `SensisBg #0D0D0D`, `SensisSurface #151515`, `SensisSurfaceHigh #1B1B1B`, `SensisSurfaceLow #141414`, `SensisBorder #2E2E2E`, `SensisAccent #FF1F1F`, `SensisAccentDeep #C60000`, `SensisGold #FFD700`, `SensisMuted #9AA0AA`.

## Cherry-picked features

- Splash screen at startup (SENSIS/Pro & Tools pattern) - original Compose splash.
- Multi-step setup wizard: welcome, device + DPI, play style, experience, control, summary.
- Real crosshair editor with live preview, presets, colors, export (Pro & Tools pattern), in-app only.
- Saved setups area shared by profiles and crosshairs.

## Explicitly not adopted

- Overlay/floating crosshair, floating buttons, fake device/performance claims, paywalls, ads, game-file access.
- Those stay out of the MVP per `AGENTS.md` and `docs/decisions.md`.
