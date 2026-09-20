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

## Arise 1.4.8 (`llc.sololeveling.Arise`)

108 MB React Native/Expo companion app. The UI lives in the JS bundle (`assets/index.android.bundle`); only the native shell and theme resources are in the APK.

### Theme

- Near-black backgrounds: `#0d0f12` (dark), `#181a1e` (light/card), `#000000` activity background.
- Navy-blue primary: `#005488` / `#023c69`; electric blue used for interactive accents; orange `#ff9500` for secondary highlights.
- White headline text, muted gray body, minimal cards.

### Screens / information architecture (from bundle strings + resources)

- Splash -> onboarding with motivation/option choices -> bottom-tab hub.
- Home (dashboard), Workout generator, Stats/Progress, Quest, AI Coach chat, Settings (sign out, restore purchases, delete account), Profile editing, paywall.

### What we adopted

- Same navigation shape: splash, goal-style onboarding steps, dashboard hub with coach card, tab bar, profile/settings page.
- Navy/blue + orange palette mapped in `Theme.kt` (`AppBg #0D0F12`, `AppSurface #181A1E`, `AppPrimary #005488`, `AppAccent #3E9BFF`, `AppOrange #FF9500`).
- Mascot changed to the user's requested simple white circle with black eyes (Grok-style), keeping gyro-reactive eyes and blinking.

### What we did not adopt

- Fitness/workout content, paywalls, subscriptions, analytics, tracking, or third-party SDKs. Domain stays manual Free Fire sensitivity coaching.
