# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android application. The `lapsCounter/` module contains the app:

- `src/main/java/br/com/helpdev/lapscounter/` holds Kotlin application code, organized by `ui`, `model`, `utils`, `headset`, and `injector`.
- `src/main/res/` contains layouts, strings, navigation, icons, and audio assets. Keep translated strings in `values-pt/` alongside their default `values/` entries.
- `src/swimming/`, `src/running/`, and `src/cycling/` are product-flavor source sets; the active flavor is `swimming`.
- `src/test/` contains local JVM unit tests. `sampledata/` provides JSON fixtures. Root-level `imgs/` contains README or store artwork.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper; it pins Gradle 8.13 and the Android Gradle Plugin 8.13.2. Java 17 or newer is required by the current AndroidX Navigation Safe Args plugin.

```bash
sh gradlew :lapsCounter:assembleSwimmingDebug  # build a debug APK
sh gradlew :lapsCounter:testSwimmingDebugUnitTest  # run local JUnit tests
sh gradlew :lapsCounter:bundleSwimmingRelease :lapsCounter:bundleRunningRelease :lapsCounter:bundleCyclingRelease  # build signed Play bundles
sh gradlew clean  # remove generated build output
```

Open the repository in Android Studio for emulator/device runs. The SDK versions are defined in `gradle/libs.versions.toml`; install the configured Android SDK platform.

## Release Signing

- The canonical Google Play upload keystore is `~/repositories/helpdev/android-keys/laps_counter.jks` (also available at `/Volumes/gbz-ext/repositories/helpdev/android-keys/laps_counter.jks`) with alias `laps_counter`.
- Its SHA-256 upload-certificate fingerprint is `47:5D:06:51:FC:35:51:84:C9:D0:C2:BE:25:E1:C9:6E:5C:4C:DA:5C:65:42:E6:DD:97:87:2E:9B:6F:2F:9B:36`; compare this to the target app's **Upload key certificate** in Play Console before building a release. Do not compare against the Play app-signing certificate.
- A known-good local, ignored `keystore.properties` was found at `~/.codex/worktrees/multi-sport-flavors/LapsCounter/keystore.properties`. For another checkout, create its ignored root `keystore.properties` with this keystore path and the local credentials; never copy credentials into tracked files, chat, or command output.
- Do not use `../android-keys/lapscounter-upload.jks` for Play releases. It has a different certificate and is not the registered upload key.
- Keep all keystores and password files in the sibling `android-keys` repository. Before uploading each flavor, verify the keystore alias and SHA-256 fingerprint against that app's registered upload certificate.

## Coding Style & Naming Conventions

Write Kotlin using the project’s configured `kotlin.code.style=official`: four-space indentation, one declaration per line, and no trailing whitespace. Follow existing package organization and use PascalCase for classes (`ChronometerViewModel`), camelCase for functions/properties (`toActivityEntity`), and lower_snake_case for Android resources (`fragment_activity.xml`). Keep flavor-specific code and resources in the matching source set rather than branching at runtime.

## Testing Guidelines

Add local unit tests under `lapsCounter/src/test/java/` in the same package as the code under test. Name test classes after the subject (for example, `ChronometerConverterTest`) and test methods for the expected behavior. Run the flavor-specific Gradle test task before opening a pull request. This repository has minimal existing test coverage, so add regression coverage whenever you change business logic or converters.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commit-style subjects such as `feat(audio): Audio notification`, `fix(serializable): Fix bug minimize app`, and `refactor(versions): Update library`. Use concise imperative subjects with a lowercase type and an optional scope. Keep commits focused.

Pull requests should explain the behavior change, identify the affected flavor(s), link the relevant issue when available, and state the validation run. Include screenshots or a short recording for visible UI/layout changes. Do not commit generated APKs, `local.properties`, keystores, or `google-services.json`.
