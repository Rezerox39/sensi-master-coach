# Sensi Master Coach Agent Notes

Sensi Master Coach is an independent Free Fire companion app. It must never modify, inject into, automate, or interfere with Free Fire or Free Fire MAX.

## Workflow

For substantial work:

1. Inspect relevant code and docs.
2. Make the smallest coherent change.
3. Keep the deterministic recommendation engine separate from coach copy.
4. Validate with Gradle build, tests, and lint where relevant.
5. Update docs when product, architecture, release, or policy decisions change.

## Product Rules

- Do not claim guaranteed headshots, wins, rank increases, FPS boosts, or perfect settings.
- Do not fabricate creator/pro settings. Creator presets require a source and collection date.
- Do not use creator photos, logos, channel artwork, or Garena-owned assets unless rights are documented.
- Crosshair tools are in-app preview/export only in MVP. Do not add overlay permission without a fresh Play policy review.
- Keep the app offline-first. Online community, sync, and remote AI are post-MVP.
- Never store secrets, signing keys, tokens, or credentials in source or docs.

## Engineering Rules

- Kotlin, Jetpack Compose, Material 3, Coroutines/Flow.
- Prefer Android platform APIs and simple domain logic before new dependencies.
- Add dependencies only when they remove real complexity.
- Keep UI AMOLED-friendly with strong contrast and restrained glow.
- All user-facing Free Fire references must include independent-app framing where appropriate.
