# Chronometer Library Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a regression-tested, API-36-compatible chronometer-library release that LapsCounter can consume.

**Architecture:** Preserve the public `Chronometer`, `ChronometerManager`, and `ChronometerUtils` API to avoid an application data migration. Modernize only build infrastructure and Android presentation code, while making time-dependent unit tests deterministic through injected clocks.

**Tech Stack:** Kotlin, Android library, JUnit 4, Gradle, Android Gradle Plugin, GitHub/JitPack release artifact.

**Spec:** `docs/superpowers/specs/2026-10-14-lapscounter-recovery-modernization-design.md`

## Global Constraints

- Work in `/Volumes/gbz-ext/repositories/helpdev/chronometer-library`; do not edit LapsCounter until its dependent plan begins.
- Select the then-current stable Gradle/AGP/Kotlin versions as a compatible set from Android's official compatibility table; record all selected versions in version catalog or build constants.
- Use JDK 17, `compileSdk`/`targetSdk` 36, and `minSdk` 24.
- Keep the package `br.com.helpdev.chronometerlib` and public method signatures source-compatible.
- Publish only after the final tag commit has passing unit and instrumented tests.

## Review Focus

- A pause that spans a process or configuration change must not add running time.
- Removing a finished versus active lap must retain the existing accepted/invalid boundary.
- Negative or non-monotonic timestamps must continue to fail with the established exception contract.
- Consumers must receive an immutable lap list after the Kotlin and collection upgrades.
- The published artifact must resolve outside the local checkout, not only through `mavenLocal()`.

---

### Task 1: Establish a reproducible modern Android library build

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/settings.gradle`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/build.gradle`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/build.gradle`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/gradle/wrapper/gradle-wrapper.properties`
- Create: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/gradle/libs.versions.toml`

**Interfaces:**
- Produces: a JDK-17 Gradle build exposing `:lib:testDebugUnitTest`, `:lib:connectedDebugAndroidTest`, and `:lib:assembleRelease`.

- [ ] **Step 1: Record the currently selected AGP, Gradle, Kotlin, AndroidX, compile SDK, target SDK, and min SDK versions in `gradle/libs.versions.toml`.**
- [ ] **Step 2: Replace deprecated `buildscript`, `jcenter()`, `kotlin-android-extensions`, and legacy `maven` upload configuration with the selected plugin/dependency-management model.**
- [ ] **Step 3: Add `namespace = "br.com.helpdev.chronometerlib"`, API 36 SDK settings, Java/Kotlin 17 toolchains, and explicit `buildFeatures { viewBinding = true }` only if the widget needs generated bindings.**
- [ ] **Step 4: Run `sh gradlew :lib:assembleDebug :lib:testDebugUnitTest` with JDK 17.**

Expected: both tasks succeed without dependency resolution from JCenter or Maven Local.

- [ ] **Step 5: Commit the build migration.**

```bash
git add build.gradle settings.gradle gradle lib/build.gradle
git commit -m "build: modernize chronometer library toolchain"
```

### Task 2: Preserve and test stopwatch semantics before UI migration

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/src/test/java/br/com/helpdev/chronometerlib/ChronometerTest.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/src/test/java/br/com/helpdev/chronometerlib/ChronometerManagerTest.kt`
- Modify if required by failing test: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/src/main/java/br/com/helpdev/chronometerlib/Chronometer.kt`
- Modify if required by failing test: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/src/main/java/br/com/helpdev/chronometerlib/ChronometerManager.kt`

**Interfaces:**
- Consumes: current public `Chronometer` and `ChronometerManager` API.
- Produces: deterministic tests that pass an injected `currentSystemTime` lambda; no test sleeps.

- [ ] **Step 1: Add tests for start, pause/resume, stop, lap removal, immutable lap access, and invalid timestamp/lap inputs using a controllable clock.**
- [ ] **Step 2: Run `sh gradlew :lib:testDebugUnitTest` and confirm each new test fails before changing production code.**
- [ ] **Step 3: Make the smallest source-compatible correction needed for any exposed timing defect; do not change the public API.**
- [ ] **Step 4: Replace the existing `Thread.sleep` tests with the controllable-clock cases and rerun `sh gradlew :lib:testDebugUnitTest`.**

Expected: deterministic green test suite with no wall-clock waits.

- [ ] **Step 5: Commit the regression coverage and any minimal fix.**

```bash
git add lib/src/main lib/src/test
git commit -m "test: make chronometer timing coverage deterministic"
```

### Task 3: Modernize the optional chronometer widget and device validation

**Files:**
- Modify as required: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/src/main/java/br/com/helpdev/chronometerlib/widget/ChronometerWidget.kt`
- Modify as required: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/src/main/AndroidManifest.xml`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/src/androidTest/java/br/com/helpdev/chronometerlib/ExampleInstrumentedTest.kt`

**Interfaces:**
- Produces: a library that inflates its widget under API 24+ and emits no obsolete synthetic-view access.

- [ ] **Step 1: Add an instrumented test that inflates the widget and verifies its initial displayed state.**
- [ ] **Step 2: Run `sh gradlew :lib:connectedDebugAndroidTest` on an API-36 emulator; confirm the test fails before replacing obsolete view access.**
- [ ] **Step 3: Migrate widget view lookup to View Binding or direct safe lookup, retaining its XML attributes and public behavior.**
- [ ] **Step 4: Rerun `sh gradlew :lib:connectedDebugAndroidTest :lib:lint`.**

Expected: debug library installs and widget test passes on API 36.

### Task 4: Publish and independently consume the release

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/README.md`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/lib/build.gradle`
- Create if needed: `/Volumes/gbz-ext/repositories/helpdev/chronometer-library/.github/workflows/release.yml`

**Interfaces:**
- Produces: a tagged immutable artifact `com.github.gbzarelli:chronometer-library:<new-version>` resolvable by LapsCounter.

- [ ] **Step 1: Choose the next semantic version, document JDK/API support and the dependency coordinate in `README.md`, and update the library version once.**
- [ ] **Step 2: Build `sh gradlew :lib:assembleRelease :lib:testDebugUnitTest` and publish the tag only after the commands succeed.**
- [ ] **Step 3: From a clean temporary consumer or the LapsCounter dependency-resolution task, resolve the tagged coordinate without `mavenLocal()`.**
- [ ] **Step 4: Commit version/docs, tag the verified commit, and push the tag only after explicit user approval for the external publication.**

Expected: the released artifact can be downloaded by a clean consumer and retains the tested public API.
