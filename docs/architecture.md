# Architecture

## App Shape

Single Android application module using Kotlin, Jetpack Compose, Material 3, ViewModel, and simple repository-style state.

## Layers

- UI: Compose screens and mascot.
- Presentation: `SensiViewModel` owns UI state and actions.
- Domain: deterministic recommendation and calibration engines.
- Data: local seed data in MVP; Room/DataStore are planned when persistence breadth justifies them.

## Persistence Plan

MVP code keeps state in memory to keep the first build small and verifiable. Next persistence milestone:

- DataStore for onboarding completion and theme.
- Room for profiles, calibration history, creator sources, guides, and presets.

## Offline Mode

All MVP functionality works without a network. Remote AI/community features are disabled with explicit labels.
