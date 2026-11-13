# Wear lap feedback Implementation Plan

> **For agentic workers:** Native inline execution requested by the user. The plan uses checkbox syntax for tracking.

**Goal:** Briefly show each saved lap's split on the Wear timer and list recorded laps newest first.

**Architecture:** The ViewModel will expose transient feedback only after lap persistence succeeds and clear it after one second, restarting the interval for a newer lap. The Compose screen will crossfade the main display between that feedback and the live stopwatch, and render the saved splits in reverse sequence order.

**Tech Stack:** Kotlin, Jetpack Compose for Wear OS, existing `WatchTimerController` and Room repository.

**Spec:** `docs/superpowers/specs/2026-11-13-wear-lap-feedback-design.md`

## Global Constraints

- Use each recorded split's `durationMillis` for lap feedback and history.
- Do not change timer persistence, synchronization, or session timing semantics.
- Preserve English, Portuguese, and Spanish localization.
- Keep the history on the existing scrollable timer screen.

## Review Focus

- Failed persistence must not produce a lap-success animation.
- A newer lap during the one-second display must replace the older feedback and restart the timeout.
- Live elapsed time must keep advancing during the feedback display.
- Lap list ordering must be newest first while displayed times remain split durations.
- Restored active sessions must show saved laps without replaying a new-lap animation.

---

### Task 1: Lap feedback state and timer screen

**Files:**
- Modify: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/WearViewModel.kt`
- Modify: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/TimerScreen.kt`
- Modify: `wear/build.gradle`
- Modify: `gradle/libs.versions.toml`
- Modify: `wear/src/main/res/values/strings.xml`
- Modify: `wear/src/main/res/values-pt/strings.xml`
- Modify: `wear/src/main/res/values-es/strings.xml`

- [x] Add transient `WatchLapSplit` feedback to `WearUiState`; only set it after the repository saves the recorded snapshot and clear/restart its one-second timeout when another lap arrives.
- [x] Add the Compose animation dependency and crossfade the main timer display between live elapsed time and localized lap number plus split duration.
- [x] Render saved splits from newest to oldest below the controls, using the existing scroll list and split durations.
- [x] Build Swimming debug with `sh gradlew :wear:assembleSwimmingDebug`.
- [x] Install and launch the app on the Wear emulator, leaving the timer at its idle screen for the user's manual interaction check.
