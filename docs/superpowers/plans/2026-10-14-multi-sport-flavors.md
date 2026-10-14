# LapsCounter Multi-Sport Flavors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task in the current session. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver separately installable Swimming, Running, and Cycling apps from the LapsCounter module while sharing timer, lap, settings, history, and presentation code.

**Architecture:** Keep common behavior and layouts in `src/main`; provide a `SportProfileProvider` and product identity resources from each sport source set. Calculate metrics from elapsed time and distance in metres, then format pace or average speed for the active profile. Keep the existing Swimming package, persisted data, branding, and production ads intact.

**Tech Stack:** Android Gradle Plugin 8.13.2, Gradle 8.13, Kotlin 2.3.20, Android SDK 36, Java 17, Realm 10.19.0, AndroidX, local JUnit tests, Android vector and adaptive launcher resources.

**Spec:** `docs/superpowers/specs/2026-10-14-multi-sport-flavors-design.md`

## Global Constraints

- Release packages: `br.com.helpdev.lapscounter.swimming`, `br.com.helpdev.lapscounter.running`, `br.com.helpdev.lapscounter.cycling`.
- Preserve Swimming's package, app name, icon, local records, production AdMob IDs, 50 m default, and min/100 m pace.
- Running and Cycling use manual laps; GPS, routes, sensors, and automatic splits remain out of scope.
- Persist all split and travelled distances in metres.
- Sport defaults: Swimming 50 m; Running 1 km; Cycling 5 km.
- Sport metrics: Swimming min/100 m; Running min/km; Cycling average speed km/h.
- Keep layouts, timer, settings flow, activities, sharing, headset handling, and final-lap policy common.
- Debug variants use Google test AdMob IDs. Release variants require flavor-specific production IDs and release signing.
- Use MDI Apache 2.0 vectors for selected in-app symbols with attribution; create original launcher artwork in the existing visual family.
- Target API 36, minimum API 24, and Java/Kotlin 17 as configured in the repository.

## Review Focus

- Zero elapsed time or distance must produce an unavailable metric without division by zero. Pin this in `SportMetricCalculatorTest`.
- Fractional and custom distances must retain unit accuracy when converted between metres and display kilometres. Pin this in `SportDistanceFormatterTest` and `LapDistancePreferenceTest`.
- The final lap must follow the existing counted/uncounted rule in live and saved activity metrics. Pin this in `LapDistancePolicyTest` and share-format tests.
- Existing Swimming preference values and Realm records must keep their meaning after the package and schema stay unchanged. Pin preference conversion in `LapDistancePreferenceTest`; verify no Realm schema change in the variant smoke pass.
- Every debug variant must resolve test AdMob IDs, and a new sport release must reject empty or Google test IDs. Pin this with flavor-aware release checks and merged-resource inspection in Task 7.

---

## File Structure

**Common domain (`src/main/java/br/com/helpdev/lapscounter/sport/`):**

- `SportProfile.kt`: immutable sport ID, default distance, display unit, and metric definition.
- `SportMetricCalculator.kt`: pure calculation of pace, average speed, or unavailable.
- `SportDistanceFormatter.kt`: pure conversion/formatting of metre distances for a display unit.
- `SportMetricFormatter.kt`: localized presentation using passed labels and locale-aware number formatting.
- `SportMetricsPresenter.kt`: pure assembly of display metrics for live and saved activity consumers.
- `LapDistancePolicy.kt`: pure counted-lap decision shared by live, saved, and shared metrics.

**Common consumers:**

- `ChronometerActivity.kt`, `ActivityViewModel.kt`, `ChronometerShareUtils.kt`, `LapsCounterUtils.kt`.
- `PreferenceUtils.kt`, `FloatPickerPreference.kt`, `FloatPickerDialog.kt`, `SettingsFragment.kt`, `settings_preference.xml`.
- `src/main/res/values/strings.xml`, `values-pt/strings.xml`, and shared layouts.

**Flavor sources:**

- `src/{swimming,running,cycling}/java/br/com/helpdev/lapscounter/ChronometerActivityImpl.kt`.
- `src/{swimming,running,cycling}/java/br/com/helpdev/lapscounter/sport/SportProfileProvider.kt`.
- Per-flavor `values/` and `values-pt/` overlays, sport illustration vectors, and launcher resources.
- `src/debug/res/values/admob.xml` remains the common test-ID overlay.
- Running/Cycling release ad resources start empty and are rejected by release validation until real IDs are supplied.

**Tests:**

- `src/test/java/br/com/helpdev/lapscounter/sport/` for profiles, metric calculations, distance formatting, and final-lap policy.
- `src/test/java/br/com/helpdev/lapscounter/utils/` for preference distance conversion and shared metric consumers where they can be tested as pure Kotlin.

## Task 1: Add the Three Flavor Contracts

**Files:**

- Modify: `lapsCounter/build.gradle`
- Remove: `lapsCounter/src/running/java/br/com/helpdev/lapscounter/MainActivity.kt`
- Remove: `lapsCounter/src/cycling/java/br/com/helpdev/lapscounter/MainActivity.kt`
- Keep: `lapsCounter/src/swimming/java/br/com/helpdev/lapscounter/ChronometerActivityImpl.kt`
- Create: `lapsCounter/src/running/java/br/com/helpdev/lapscounter/ChronometerActivityImpl.kt`
- Create: `lapsCounter/src/cycling/java/br/com/helpdev/lapscounter/ChronometerActivityImpl.kt`
- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/SportProfile.kt`
- Create: one `SportProfileProvider.kt` in each of `swimming`, `running`, and `cycling` source sets.
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/SportProfileProviderTest.kt`

**Interfaces:**

- `SportProfile` exposes `sportId`, `defaultLapDistanceMeters`, `distanceDisplayUnit`, and `metricDefinition` as immutable values.
- `DistanceDisplayUnit` is `METRES` or `KILOMETRES` and exposes its `metersPerUnit` conversion factor.
- Each flavor defines `object SportProfileProvider` with `fun get(): SportProfile`; the common source set contains no competing provider type.
- `MetricDefinition` is a sealed type with `Pace(referenceDistanceMeters: Float)` and `AverageSpeed` cases.
- Swimming profile: `50f`, metres, pace reference `100f`.
- Running profile: `1000f`, kilometres, pace reference `1000f`.
- Cycling profile: `5000f`, kilometres, average speed.

- [ ] **Step 1: Write the profile contract test**

Add tests asserting the active provider returns the exact profile values for the current build flavor, including metric type and reference distance.

- [ ] **Step 2: Run the Swimming test to verify the missing provider fails**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.SportProfileProviderTest`

Expected: compile/test failure because the profile contract does not exist yet.

- [ ] **Step 3: Add `SportProfile`, `MetricDefinition`, and `SportProfileProvider`**

Use immutable Kotlin data types in the `sport` package. Keep Android `Context`, resources, mutable preference values, and Realm entities out of the profile.

- [ ] **Step 4: Add one profile provider and launcher wrapper per sport**

Each `ChronometerActivityImpl` extends the shared `ChronometerActivity`. Replace the orphan `MainActivity : AbsMainActivity` files because `AbsMainActivity` is absent and the common manifest declares `ChronometerActivityImpl`.

- [ ] **Step 5: Declare all three Gradle flavors and package suffixes**

Add `running` and `cycling` beside `swimming` in the existing `sport` dimension. Preserve `.swimming`; assign `.running` and `.cycling` to the new flavors. Retain the `.debug` build-type suffix.

- [ ] **Step 6: Run profile tests and assemble every debug variant**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:testRunningDebugUnitTest :lapsCounter:testCyclingDebugUnitTest :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug`

Expected: all six tasks succeed; merged manifests report the expected distinct package IDs and `ChronometerActivityImpl` launcher.

- [ ] **Step 7: Commit the flavor contract**

Commit: `feat(flavors): add running and cycling variants`

## Task 2: Implement and Test Sport Metrics

**Files:**

- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/SportMetricCalculator.kt`
- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/LapDistancePolicy.kt`
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/SportMetricCalculatorTest.kt`
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/LapDistancePolicyTest.kt`

**Interfaces:**

- `SportMetricCalculator.calculate(runningTimeMillis: Long, distanceMeters: Float, definition: MetricDefinition): SportMetricResult`.
- `SportMetricResult` has `Pace(totalSeconds: Long, referenceDistanceMeters: Float)`, `AverageSpeed(kilometresPerHour: Double)`, and `Unavailable` cases.
- `LapDistancePolicy.countedLapCount(totalLaps: Int, isFinished: Boolean, countLastLap: Boolean): Int` returns the common number of counted laps under the existing stop/final-lap semantics.
- `LapDistancePolicy.countedDistanceMeters(totalLaps: Int, lapDistanceMeters: Float, isFinished: Boolean, countLastLap: Boolean): Float` uses the counted-lap result.

- [ ] **Step 1: Add failing metric tests**

Cover 2:00 per 100 m for Swimming, 5:00/km for a 5 km run taking 25 minutes, 30.0 km/h for a 20 km ride taking 40 minutes, zero time, zero distance, negative input, and rounding at a half-second boundary.

- [ ] **Step 2: Run the metric tests and confirm they fail**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.SportMetricCalculatorTest`

Expected: compile/test failure because the calculator and result type do not exist.

- [ ] **Step 3: Implement `SportMetricCalculator.calculate`**

Use `Double` intermediates to avoid overflow from multiplying elapsed milliseconds by distance. Convert pace to total seconds per reference distance and round once to the nearest second. Convert speed from metres/millisecond to kilometres/hour. Return `Unavailable` for non-positive elapsed time, distance, or pace reference.

- [ ] **Step 4: Add failing final-lap policy tests**

Assert that an unfinished timer excludes the current incomplete lap, a finished timer includes the final lap when `countLastLap` is enabled, and a finished timer excludes it when disabled; also cover zero laps.

- [ ] **Step 5: Implement `LapDistancePolicy` and run both test classes**

Replace the private final-lap branching in `LapsCounterUtils` with the pure policy in a later consumer task. At this task, test the policy alone.

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.SportMetricCalculatorTest --tests br.com.helpdev.lapscounter.sport.LapDistancePolicyTest`

Expected: both classes pass, including unavailable results and the five specified sport samples.

- [ ] **Step 6: Commit the metric domain**

Commit: `feat(metrics): calculate sport-specific pace and speed`

## Task 3: Add Pure Metric and Distance Formatting

**Files:**

- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/SportDistanceFormatter.kt`
- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/SportMetricFormatter.kt`
- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/SportMetricsPresenter.kt`
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/SportDistanceFormatterTest.kt`
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/SportMetricFormatterTest.kt`

**Interfaces:**

- `SportDistanceFormatter.format(distanceMeters: Float, unit: DistanceDisplayUnit, locale: Locale): String` formats in metres or kilometres with a localized decimal separator.
- `SportMetricFormatter.format(metric: SportMetricResult, paceUnitLabel: String, speedUnitLabel: String, unavailableLabel: String, locale: Locale): String` formats only the supplied metric and labels.
- `SportMetricsPresenter.present(runningTimeMillis: Long, splitDistanceMeters: Float, travelledDistanceMeters: Float, profile: SportProfile, labels: SportMetricLabels, locale: Locale): SportMetricsPresentation` returns pure strings for split distance, travelled distance, and the primary metric.
- `SportMetricLabels` contains `paceReferenceUnit`, `speedUnit`, and `unavailable` strings. `SportMetricsPresentation` contains `splitDistance`, `travelledDistance`, and `metric` strings.
- Pace output is `m:ss` followed by the localized reference unit; Cycling speed has one decimal place.

- [ ] **Step 1: Add failing formatter tests**

Cover `50 m`, `1 km`, a fractional `1.25 km`, Portuguese decimal comma, `2:00 / 100 m`, `5:00 / km`, `30.0 km/h`, and unavailable output.

- [ ] **Step 2: Run formatter tests and confirm they fail**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.SportDistanceFormatterTest --tests br.com.helpdev.lapscounter.sport.SportMetricFormatterTest`

Expected: compile/test failure because the formatters do not exist.

- [ ] **Step 3: Implement locale-aware distance formatting**

Use `NumberFormat` with the passed `Locale`; retain up to two fractional digits and remove insignificant trailing zeroes. Append unit labels outside the numeric domain formatter.

- [ ] **Step 4: Implement metric formatting**

Format `Pace` from total seconds and its profile reference, format Cycling speed to one decimal place, and map `Unavailable` to the passed localized label. Do not read Android resources from the pure formatter.

- [ ] **Step 5: Run formatter tests and commit**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.SportDistanceFormatterTest --tests br.com.helpdev.lapscounter.sport.SportMetricFormatterTest`

Expected: all metric and distance formatting assertions pass in English and Portuguese locales.

Commit: `feat(metrics): format distances and sport metrics`

## Task 4: Migrate Shared UI, Saved Activities, and Sharing

**Files:**

- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/ChronometerActivity.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/viewmodel/ActivityViewModel.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/viewmodel/objects/HeaderDistances.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/utils/LapsCounterUtils.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/utils/ChronometerShareUtils.kt`
- Modify: `lapsCounter/src/main/res/values/strings.xml`
- Modify: `lapsCounter/src/main/res/values-pt/strings.xml`
- Create: shared metric consumer tests under `lapsCounter/src/test/java/br/com/helpdev/lapscounter/utils/`.
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/SportMetricsPresenterTest.kt`.

**Interfaces:**

- Common consumers call `SportProfileProvider.get()` for the active build profile.
- Live and saved views use one `SportMetricCalculator` and one `SportMetricFormatter` path.
- `LapsCounterUtils` delegates counted-lap decisions to `LapDistancePolicy` and keeps distances in metres.
- `HeaderDistances.pace` may be renamed to `metric`; update both portrait and landscape data-binding declarations in the same change.

- [ ] **Step 1: Add failing consumer tests for live, saved, and shared metrics**

Assert sport-specific distance and metric labels/values for all three profiles, including a stopped activity with the final lap excluded. Share formatting tests assert that per-lap and whole-activity output use the same formatter.

- [ ] **Step 2: Run consumer tests and confirm the old hard-coded pace output fails**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.SportMetricsPresenterTest`

Expected: failure showing the presenter does not yet return the sport-specific distance and metric values.

- [ ] **Step 3: Route `ChronometerActivity` through the profile and metric services**

Use `SportMetricsPresenter` with the current profile for live split distance, total distance, and metric. Preserve timer states, buttons, layout IDs, and headset behavior.

- [ ] **Step 4: Route saved activity details through the same services**

Update `ActivityViewModel` and `HeaderDistances` to display the selected unit and metric label for each flavor by calling the same `SportMetricsPresenter`, using `ActivityEntity` values stored in metres.

- [ ] **Step 5: Route both share paths through the shared services**

Update new-session and saved-activity branches of `ChronometerShareUtils`, including per-lap metrics, total distance, metric unit labels, and unavailable values. Remove hard-coded `"m"` and `getPace` call sites from this file.

- [ ] **Step 6: Remove the 100 m assumption from `LapsCounterUtils`**

Delegate pace/speed to `SportMetricCalculator` with the active profile definition. Keep `getDistanceTravelled` returning metres and apply `LapDistancePolicy` in one place.

- [ ] **Step 7: Add localized common labels and run Swimming regression tests**

Keep existing Swimming wording and `min/100m` rendering. Add resource keys for unavailable metrics, pace reference units, and speed units in English and Portuguese.

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug`

Expected: tests pass; all variants compile; Swimming metric strings preserve existing output.

- [ ] **Step 8: Commit shared metric consumers**

Commit: `refactor(metrics): share sport-aware activity presentation`

## Task 5: Make Split Distance Settings Sport-Aware

**Files:**

- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/utils/PreferenceUtils.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/widget/FloatPickerPreference.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/widget/FloatPickerDialog.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/fragment/SettingsFragment.kt`
- Modify: `lapsCounter/src/main/res/xml/settings_preference.xml`
- Modify: English and Portuguese preference resource overlays for all flavors.
- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/LapDistancePreferenceAdapter.kt`.
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/LapDistancePreferenceTest.kt`.

**Interfaces:**

- `LapDistancePreferenceAdapter.toDisplayValue(distanceMeters: Float, unit: DistanceDisplayUnit): Float`.
- `LapDistancePreferenceAdapter.toStoredMeters(displayValue: Float, unit: DistanceDisplayUnit): Float`.
- The persisted preference value and `ActivityEntity.lapDistance` remain metres. The dialog displays metres for Swimming and kilometres for Running/Cycling.

- [ ] **Step 1: Add failing preference conversion tests**

Cover existing Swimming `50f -> 50f`, Running `1000f -> 1f -> 1000f`, Cycling `5000f -> 5f -> 5000f`, fractional kilometres, and preservation of an existing Swimming preference value.

- [ ] **Step 2: Run the preference test and confirm it fails**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.LapDistancePreferenceTest`

Expected: compile/test failure because the adapter does not exist.

- [ ] **Step 3: Implement the pure preference conversion adapter**

Use each profile's distance unit scale. Reject non-positive values before persistence and keep all stored values in metres.

- [ ] **Step 4: Update `FloatPickerPreference` and `FloatPickerDialog`**

Load the persisted metre value or the active profile's default, convert it for display, and convert the chosen value back to metres before `persistFloat`. Keep the stable Swimming preference key.

- [ ] **Step 5: Update `SettingsFragment` and preference resources**

Format the summary through `SportDistanceFormatter`. Remove the shared `50` XML integer default so it cannot override a flavor profile; provide flavor wording and localized input hint.

- [ ] **Step 6: Run preference tests and assemble all debug variants**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:testRunningDebugUnitTest :lapsCounter:testCyclingDebugUnitTest :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug`

Expected: all tests pass; each Settings screen displays its profile default and unit; Swimming's existing persisted values still represent metres.

- [ ] **Step 7: Commit sport-aware distance settings**

Commit: `feat(settings): use sport-specific lap distance defaults`

## Task 6: Add Product Copy, MDI Attribution, and Original Launcher Icons

**Files:**

- Create: Running and Cycling `values/strings.xml` and `values-pt/strings.xml` overlays.
- Create: flavor-specific sport illustration vectors such as `ic_sport_24.xml` and `ic_sport_36.xml`.
- Create: flavor-specific launcher foreground vectors, adaptive launcher XML, and `mipmap-anydpi/ic_launcher.xml` fallbacks.
- Keep: existing Swimming app name and launcher assets unchanged.
- Create: `THIRD_PARTY_NOTICES.md` with MDI attribution and license details.
- Modify: `README.md` to list the three product variants and current store availability.

**Interfaces:**

- Running display name: `Running Stopwatch`; Portuguese: `Cronômetro de corrida`.
- Cycling display name: `Cycling Stopwatch`; Portuguese: `Cronômetro de ciclismo`.
- Running launcher background: `#F47721`; Cycling launcher background: `#4657B8`.
- Both launchers use a white original sport pictogram and small stopwatch in the upper right, matching the Swimming icon's visual arrangement.
- MDI vectors are limited to ordinary in-app symbols; launcher sport art is original and contains no third-party brand/logos.

- [ ] **Step 1: Implement Running and Cycling localized copy**

Supply the English and Portuguese strings for app name, lap/split terminology, setting summary, input hint, distances, pace/speed, and share output. Leave unrelated common app copy in `main`.

- [ ] **Step 2: Create the sport and launcher vector assets**

Create a runner-plus-stopwatch composition and bicycle-rider-plus-stopwatch composition as Android vector XML. Add adaptive icons for API 26+ and any-density vector fallback resources for API 24–25. Do not modify or regenerate the Swimming launcher files.

- [ ] **Step 3: Add selected MDI in-app vectors and attribution**

Record each imported MDI icon's name, upstream version, and Apache 2.0 attribution in `THIRD_PARTY_NOTICES.md`. Exclude the Brand/Logos category.

- [ ] **Step 4: Update README and run resource/variant checks**

Run: `sh gradlew :lapsCounter:processSwimmingDebugResources :lapsCounter:processRunningDebugResources :lapsCounter:processCyclingDebugResources :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug`

Expected: resource processing succeeds and each merged variant resolves its unique product strings and icon resources.

- [ ] **Step 5: Commit product identity and assets**

Commit: `feat(branding): add running and cycling identities`

## Task 7: Guard Release Ads and Document Per-App Publishing

**Files:**

- Modify: `lapsCounter/build.gradle`
- Keep: `lapsCounter/src/debug/res/values/admob.xml` with Google test IDs.
- Keep: `lapsCounter/src/swimming/res/values/admob.xml` with existing Swimming production IDs.
- Create: `lapsCounter/src/running/res/values/admob.xml` with empty production-ID placeholders.
- Create: `lapsCounter/src/cycling/res/values/admob.xml` with empty production-ID placeholders.
- Modify: `docs/release-signing.md`.
- Modify: `README.md` if release state needs clarification.

**Interfaces:**

- Add a Gradle validation task or configuration check that runs only for `*Release` variants and reads the selected variant's AdMob app, banner, and interstitial IDs.
- Accept IDs only when all required values are non-empty, are not Google's documented test IDs, and have the expected AdMob ID shape.
- Preserve `bundleSwimmingRelease` behavior with current production values and the existing signing check. Reject Running/Cycling releases until their own IDs and release signing values exist.

- [ ] **Step 1: Implement flavor-aware release validation**

Replace the `swimmingRelease` task-name special case with variant-aware validation for every sport. Keep credentials in ignored `keystore.properties`; do not add real ad IDs or signing secrets to this plan's implementation.

- [ ] **Step 2: Add empty Running and Cycling production ad resources**

Use empty string values so all resources resolve during configuration; the release guard must stop bundling with a clear message until owners supply real IDs. The shared debug overlay must continue to win for every debug flavor.

- [ ] **Step 3: Update release documentation**

Document package IDs, commands for each release bundle, upload-certificate verification, independent Play listing/version tracks, the need for sport-specific AdMob IDs, and which values block a release.

- [ ] **Step 4: Verify debug and release guards**

Run: `sh gradlew :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug :lapsCounter:bundleSwimmingRelease :lapsCounter:bundleRunningRelease :lapsCounter:bundleCyclingRelease`

Expected: all debug builds succeed; Swimming release succeeds only when local signing configuration exists; Running/Cycling release fail with the explicit missing-AdMob configuration message until owners provide IDs. If the local upload key is unavailable, record that release signing was not verified.

- [ ] **Step 5: Commit release safeguards and docs**

Commit: `build(release): validate sport-specific ads and signing`

## Task 8: Full Regression and Artifact Review

**Files:**

- Modify only defects found during this task; do not change the approved sport profile values.
- Verify generated artifacts under `lapsCounter/build/outputs/` without committing them.

- [ ] **Step 1: Run all flavor unit tests**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:testRunningDebugUnitTest :lapsCounter:testCyclingDebugUnitTest`

Expected: all three flavor test tasks pass, including profile, metric, distance, preference, and final-lap tests.

- [ ] **Step 2: Assemble all debug variants**

Run: `sh gradlew :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug`

Expected: all variants build without missing classes/resources.

- [ ] **Step 3: Inspect each merged manifest and resource set**

Verify package ID, launcher class, app label, launcher icon, and Google test ad IDs for all three debug variants. Confirm Swimming's release resource set retains the existing production ad IDs.

- [ ] **Step 4: Run one device or emulator smoke pass per app**

Check start, lap, pause/resume, stop, save, saved activity list/detail, settings, headset if available, and sharing. Confirm Swimming 50 m / min/100 m, Running 1 km / min/km, and Cycling 5 km / km/h across live and saved views.

- [ ] **Step 5: Review the final diff and repository state**

Run: `git diff --check && git status --short && git diff --stat`

Expected: no whitespace errors, no generated APK/AABs, local properties, keystores, or Google services credentials tracked.

- [ ] **Step 6: Report build, test, smoke, and release readiness separately**

Record any missing device, keystore, AdMob account, Play Console package, or test-track setup without describing those checks as passed.
