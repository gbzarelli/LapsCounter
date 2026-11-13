# Wear OS Multi-Sport Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add standalone Wear OS versions of Swimming, Running, and Cycling that save sessions on the watch and sync each saved session to the matching Android app.

**Architecture:** Extract the existing sport profiles and metric rules into a flavor-aware `:sportCore` library. Add an independent `:wear` application module with the same three flavors, a Wear Compose UI, Room-backed sessions, and Data Layer transport. The phone keeps Realm as its local store and imports watch sessions by UUID before acknowledging receipt.

**Tech Stack:** AGP 8.13.2, Gradle 8.13, Kotlin 2.3.20, Java 17, compile/target SDK 36, Wear Compose, Room, AndroidX, Google Play services Wearable Data Layer, Realm 10.19.0 on mobile, local JUnit and Android instrumentation tests.

**Spec:** `docs/superpowers/specs/2026-11-04-wear-os-multi-sport-design.md`

## Global Constraints

- The watch timer and saved session history must work without a paired phone or network connection.
- Create three Wear OS variants: Swimming, Running, and Cycling.
- Preserve profile defaults: Swimming 50 m splits and pace per 100 m; Running 1 km splits and pace per km; Cycling 5 km splits and average speed in km/h.
- Default final-lap inclusion to `true`, matching the current phone preference.
- Sync saved activities only from watch to phone. The phone may return an activity UUID/version acknowledgement; it must not send phone history or settings to the watch.
- Keep the existing mobile app's package IDs, saved records, and release behavior intact.
- Keep the existing mobile app's package IDs: `br.com.helpdev.lapscounter.swimming`, `br.com.helpdev.lapscounter.running`, and `br.com.helpdev.lapscounter.cycling`.
- Use those same IDs for the corresponding Wear variants; debug package IDs must also match between the paired phone and watch variants.
- Keep the watch's Room database and phone's Realm database as their local sources of truth. The Data Layer is transport only.
- Do not add GPS, routes, automatic lap detection, sensor tracking, Health Services, iOS sync, Tiles, complications, watch faces, cloud accounts, or a new backend.
- Do not replace Realm in the phone app. Keep phone ads, sharing, and full settings out of the Wear app.
- Use `com.google.android.wearable.standalone=true` and declare `android.hardware.type.watch` in the Wear manifest.
- Target SDK 36 and min SDK 30 in the Wear module; keep current phone SDK settings unless required for Wear sync integration.
- Use separate Wear OS packaging and release tracks; do not use legacy embedded-Wear packaging or upload an artifact as part of this plan.
- Preserve the phone `versionCode = appVersionCode`; assign Wear `versionCode = 1_000_000 + appVersionCode` for each sport listing. Guard the Wear range so `appVersionCode` stays below 1,000,000. Keep `versionName` shared and verify both codes against Play Console before producing a release bundle.
- Keep all string resources localized for English, Portuguese, and Spanish, matching the existing app's supported locales.
- Keep the existing production AdMob configuration phone-only; the Wear module contains no ads.

## Review Focus

- **Process recreation during a running or paused timer:** elapsed time must restore from monotonic anchors without counting paused time twice. Pin this in `WatchTimerControllerTest` with `restoresRunningSessionAfterRecreation`, `restoresPausedSessionAfterRecreation`, and `resumeDoesNotCountPausedDuration` (Task 3).
- **Sport profile and final-lap semantics:** all three defaults and metric types must match the phone, and the stopped partial lap must follow the shared policy. Pin this in `SportProfileProviderTest` and `LapDistancePolicyTest` (Task 1), and in the watch summary test (Task 4).
- **Phone absent or devices disconnected:** local save must succeed, remain pending, and sync after reconnection without losing the watch copy. Pin pending/outbox transitions in `ActivitySyncOutboxTest` (Task 5) and exercise reconnect in the paired-device check (Task 7).
- **Duplicate, wrong-flavor, unsupported-version, truncated, or large sync payload:** the phone must not create corrupt/duplicate activities or acknowledge invalid content. Pin codec and importer cases in `ActivitySyncCodecTest` (Task 5) and `ActivitySyncImporterTest` (Task 6).
- **Small round display and locale-specific labels:** controls and current lap must remain usable without clipping in English, Portuguese, and Spanish. Pin visible actions in `WearTimerScreenTest` (Task 4) and verify round layouts on both emulator sizes (Task 7).

---

## File Structure

**Shared sport library (`sportCore/`):**

- `build.gradle`: Android library setup and the `sport` flavor dimension shared by phone and Wear consumers.
- `src/main/java/br/com/helpdev/lapscounter/sport/`: sport profile contracts, metric calculators/formatters, lap policy, and preference distance conversion.
- `src/{swimming,running,cycling}/java/br/com/helpdev/lapscounter/sport/SportProfileProvider.kt`: one profile provider per flavor.
- `src/test/java/br/com/helpdev/lapscounter/sport/`: existing pure sport tests moved from the app module.

**Wear application (`wear/`):**

- `build.gradle`: Wear app flavors, package IDs, SDK levels, version codes, and Wear Compose/Room/Data Layer dependencies.
- `src/main/AndroidManifest.xml`: watch hardware feature, standalone metadata, launcher activity, and listener service.
- `src/main/java/br/com/helpdev/lapscounter/wear/timer/`: elapsed-time state machine and session controller.
- `src/main/java/br/com/helpdev/lapscounter/wear/data/`: Room entities, DAO, database, and repositories for sessions, laps, settings, and sync status.
- `src/main/java/br/com/helpdev/lapscounter/wear/ui/`: Wear Compose timer, summary, history, and settings screens.
- `src/main/java/br/com/helpdev/lapscounter/wear/sync/`: Data Layer sender, pending outbox, acknowledgement listener, and payload adapter.
- `src/{swimming,running,cycling}/res/values*/`: app names and sport-specific localized terms/icons where required.
- `src/test/` and `src/androidTest/`: timer, Room, sync, and Compose interaction coverage.

**Phone integration (`lapsCounter/`):**

- `build.gradle`: depend on `:sportCore` and use the mobile half of the version-code formula.
- `src/main/AndroidManifest.xml`: declare the Wearable Data Layer listener for activity records and acknowledgements.
- `src/main/java/br/com/helpdev/lapscounter/sync/ActivitySyncListenerService.kt`: receive, validate, import, and acknowledge watch records.
- `src/main/java/br/com/helpdev/lapscounter/sync/ActivitySyncImporter.kt`: map versioned sync records to existing `ActivityEntity` objects.
- `src/main/java/br/com/helpdev/lapscounter/model/dao/ActivityDao.kt`: expose transaction completion for sync acknowledgements.
- `src/androidTest/`: verify duplicate receipt leaves one Realm activity.

**Project configuration and docs:**

- `settings.gradle`: include `:sportCore` and `:wear`.
- `gradle/libs.versions.toml`: pin Wear Compose, Room, and Play services Wearable versions compatible with Kotlin 2.3.20 and AGP 8.13.2.
- `README.md`: list Wear variants and debug build/test commands.
- `docs/release-signing.md`: document the Wear version-code mapping and same-listing upload-key check.

---

### Task 1: Extract shared sport rules into `:sportCore`

**Files:**

- Create: `sportCore/build.gradle`
- Move: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/{SportProfile,LapDistancePolicy,LapDistancePreferenceAdapter,SportDistanceFormatter,SportMetricCalculator,SportMetricFormatter,SportMetricsPresenter}.kt` to `sportCore/src/main/java/br/com/helpdev/lapscounter/sport/`
- Move: `lapsCounter/src/{swimming,running,cycling}/java/br/com/helpdev/lapscounter/sport/SportProfileProvider.kt` to matching `sportCore/src/<flavor>/...`
- Move sport-only tests from `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sport/` to `sportCore/src/test/java/br/com/helpdev/lapscounter/sport/`
- Modify: `settings.gradle`
- Modify: `gradle/libs.versions.toml` to add the Android library plugin alias.
- Modify: `lapsCounter/build.gradle`

**Interfaces:**

- `SportProfileProvider.get(): SportProfile` remains flavor-selected at compile time.
- Preserve existing public APIs, including `SportMetricCalculator.calculate(...)`, `SportMetricsPresenter.present(...)`, and `LapDistancePolicy.countedDistanceMeters(...)`.
- `:lapsCounter` consumes `project(':sportCore')`; the future `:wear` module consumes the same library and matching sport flavor.

- [ ] **Step 1: Add a library profile contract test**

Create or move `SportProfileProviderTest` into `sportCore` and assert the exact profile values for Swimming, Running, and Cycling.

- [ ] **Step 2: Run the Swimming core test to confirm the module contract is missing**

Run: `sh gradlew :sportCore:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sport.SportProfileProviderTest`

Expected: Gradle reports that `:sportCore` or its test task is not configured yet.

- [ ] **Step 3: Create the flavor-aware Android library and move pure sport code**

Create `:sportCore` with the existing `sport` flavor dimension and profile providers. Keep `Context`, Android resources, Realm entities, and UI classes in `:lapsCounter`. Add `implementation project(':sportCore')` to the phone module and update the source/test paths.

- [ ] **Step 4: Run every sport-core unit test variant and the phone unit test suite**

Run: `sh gradlew :sportCore:testSwimmingDebugUnitTest :sportCore:testRunningDebugUnitTest :sportCore:testCyclingDebugUnitTest :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:testRunningDebugUnitTest :lapsCounter:testCyclingDebugUnitTest`

Expected: all six tasks pass with unchanged profile, formatter, preference-conversion, and final-lap behavior.

- [ ] **Step 5: Commit the shared domain extraction**

Commit: `refactor(sport): share sport rules across app modules`

---

### Task 2: Add the Wear module and three installable variants

**Files:**

- Create: `wear/build.gradle`
- Create: `wear/src/main/AndroidManifest.xml`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/WearMainActivity.kt`
- Create: `wear/src/test/java/br/com/helpdev/lapscounter/wear/WearBuildConfigContractTest.kt`
- Create: one round Wear launcher icon for each flavor under `wear/src/{swimming,running,cycling}/res/`
- Create: default and `values-pt`/`values-es` resource files under `wear/src/main/res/values*` and flavor overlays under `wear/src/{swimming,running,cycling}/res/`
- Modify: `settings.gradle`
- Modify: `gradle/libs.versions.toml` for the Compose compiler plugin, Wear Compose, Room, Data Layer, and test dependencies.
- Modify: `lapsCounter/build.gradle`

**Interfaces:**

- Wear variants use the same release IDs and the same `.debug` package suffix pattern as the corresponding phone variants.
- Wear uses `targetSdk 36`, `minSdk 30`, and `versionCode = 1_000_000 + appVersionCode`; phone keeps `versionCode = appVersionCode`. Guard the Wear range so the base code remains below 1,000,000. Both retain the shared `versionName`.
- Wear manifest declares `android.hardware.type.watch` with `required=true` and `com.google.android.wearable.standalone=true`.
- The module does not use AGP's legacy `wearApp` embedding configuration.

- [ ] **Step 1: Write failing Wear variant contract tests**

Add `WearBuildConfigContractTest` assertions named `applicationIdMatchesSportFlavor` and `versionCodeUsesWearFormFactorOffset`. Check the active `BuildConfig.FLAVOR`, `BuildConfig.APPLICATION_ID`, and `BuildConfig.VERSION_CODE`; for the current base code 12, expect Wear code 1,000,012. Merged manifest assertions remain part of Step 5.

- [ ] **Step 2: Run the Swimming variant check to verify the new module fails before setup**

Run: `sh gradlew :wear:testSwimmingDebugUnitTest`

Expected: Gradle reports that `:wear` is not yet included/configured.

- [ ] **Step 3: Configure `:wear` with Compose for Wear OS, Room, Data Layer, and test dependencies**

Pin compatible stable versions in the version catalog. Use Wear-specific Compose Material/navigation, Room runtime/compiler/testing, Google Play services Wearable, Compose UI testing, and AndroidX test libraries. Apply the existing Kotlin KAPT plugin for Room code generation and the Kotlin Compose compiler plugin at Kotlin 2.3.20. Enable Compose and BuildConfig generation. Match `sport` flavor names, application IDs, debug suffixes, and SDK levels; preserve the phone version code and set Wear's code to `1_000_000 + appVersionCode`, with a guard that rejects base codes of 1,000,000 or higher. Read release signing values only from the ignored root `keystore.properties`, matching the phone module's convention; release fingerprint verification is Task 7.

- [ ] **Step 4: Add the Wear manifest and flavor identity resources**

Register the launcher activity; add the watch hardware feature and standalone metadata. Add localized Swimming, Running, and Cycling names plus round launcher icons using the existing sport identity. Do not copy phone-only AdMob metadata or activities into the Wear manifest.

- [ ] **Step 5: Run the variant contract test, assemble all Wear debug variants, and inspect manifests**

Run: `sh gradlew :wear:testSwimmingDebugUnitTest :wear:testRunningDebugUnitTest :wear:testCyclingDebugUnitTest :wear:assembleSwimmingDebug :wear:assembleRunningDebug :wear:assembleCyclingDebug`

Expected: tests and all three APK assemblies succeed; merged manifests target Wear OS and identify the correct package/flavor. Inspect the produced version codes and confirm they follow the formula.

- [ ] **Step 6: Commit the Wear module skeleton**

Commit: `feat(wear): add standalone sport app variants`

---

### Task 3: Persist active timer state and watch-local activity history

**Files:**

- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/timer/ElapsedRealtimeClock.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/timer/WatchTimerController.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/data/WatchActivityEntity.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/data/WatchLapEntity.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/data/WearSettingsEntity.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/data/WatchActivityDao.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/data/WatchDatabase.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/data/WatchActivityRepository.kt`
- Create: `wear/src/test/java/br/com/helpdev/lapscounter/wear/timer/WatchTimerControllerTest.kt`
- Create: `wear/src/androidTest/java/br/com/helpdev/lapscounter/wear/data/WatchActivityRepositoryTest.kt`

**Interfaces:**

- `ElapsedRealtimeClock.nowMillis(): Long` supplies monotonic time and is replaceable with a fake in unit tests.
- `WatchTimerController` exposes `start()`, `pause()`, `resume()`, `recordLap()`, `stop()`, `discard()`, and `snapshot()`; every state-changing method returns a persistable `WatchSessionSnapshot`.
- `WatchSessionSnapshot` records UUID, flavor ID, start wall-clock timestamp, monotonic start/pause/stop anchors, accumulated paused milliseconds, session state, lap distance, final-lap setting, and lap splits.
- `WatchActivityRepository` persists the active snapshot after each state transition and lap; it stores completed sessions and their laps locally in Room.

- [ ] **Step 1: Write failing timer state tests**

Add tests named `restoresRunningSessionAfterRecreation`, `restoresPausedSessionAfterRecreation`, `resumeDoesNotCountPausedDuration`, `recordsLapFromMonotonicElapsedTime`, and `stopFinalizesCurrentPartialLap`.

- [ ] **Step 2: Run the timer tests and confirm the missing controller fails**

Run: `sh gradlew :wear:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.wear.timer.WatchTimerControllerTest`

Expected: compilation fails because `WatchTimerController` and snapshot types do not exist.

- [ ] **Step 3: Implement the timer state machine with an injected monotonic clock**

Compute elapsed running time from `SystemClock.elapsedRealtime()` anchors and accumulated pause time. Do not update the database on every display tick; persist on start, pause, resume, lap, stop, and discard. On restore, resume the prior state without adding a second copy of elapsed or paused time.

- [ ] **Step 4: Add Room activity, lap, and settings entities with DAOs**

Store distances in metres and times in milliseconds. Give each activity a UUID and each lap a stable sequence number. Store final-lap preference and sync status with the activity/session record so process recreation retains the selected behavior.

- [ ] **Step 5: Add repository persistence tests and run timer tests**

Run: `sh gradlew :wear:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.wear.timer.WatchTimerControllerTest && sh gradlew :wear:connectedSwimmingDebugAndroidTest`

Expected: timer tests pass; the Room instrumentation test reopens the database and reads the exact active snapshot, laps, settings, and completed record.

- [ ] **Step 6: Commit the timer and local persistence layer**

Commit: `feat(wear): persist stopwatch sessions locally`

---

### Task 4: Build the Wear timer, summary, history, and settings UI

**Files:**

- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/WearApp.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/TimerScreen.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/SessionSummaryScreen.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/ActivityHistoryScreen.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/SettingsScreen.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/ui/WearViewModel.kt`
- Create: `wear/src/androidTest/java/br/com/helpdev/lapscounter/wear/ui/WearTimerScreenTest.kt`
- Create or modify: `wear/src/main/res/values*/strings.xml` and matching flavor resources.

**Interfaces:**

- `WearViewModel` exposes the active `WatchSessionSnapshot`, sport metrics, history, settings, and sync state as lifecycle-aware state consumed by Compose.
- `TimerScreen` displays elapsed time, current lap, sport metric, and state-appropriate Start/Pause/Resume/Lap/Stop actions.
- `SessionSummaryScreen` saves locally with a localized generated name based on sport and start timestamp; it does not require keyboard input.
- `SettingsScreen` edits lap distance and final-lap inclusion. Lap distance defaults come from `SportProfileProvider.get()`; final-lap inclusion defaults to `true`, matching the phone app.

- [ ] **Step 1: Add failing Wear Compose interaction tests**

Assert that the idle state shows Start, running state shows Lap and Pause, paused state shows Resume and Stop, and the summary state shows Save and Discard. Assert sport-specific metrics use the active profile.

- [ ] **Step 2: Run the UI test on a Wear emulator before implementing the screens**

Run: `sh gradlew :wear:connectedSwimmingDebugAndroidTest`

Expected: compilation fails because the Wear screens and ViewModel do not yet exist.

- [ ] **Step 3: Implement the timer and summary flows with Wear Compose components**

Use Wear-specific Material components and shallow swipe-dismiss navigation. Use round-screen-safe spacing and large touch targets. Drive all button actions through `WearViewModel` and `WatchTimerController`.

- [ ] **Step 4: Implement local history and sport settings screens**

List completed watch sessions and show per-session split details. Load and save lap-distance and final-lap settings in Room. Use the active flavor profile for labels, defaults, and pace/speed presentation.

- [ ] **Step 5: Add localized generated activity names and update UI assertions**

Use localized sport name plus the session's local start timestamp. Add English, Portuguese, and Spanish string resources. Ensure saved names are the same values later sent to the phone.

- [ ] **Step 6: Run Compose tests for all three sport flavors**

Run: `sh gradlew :wear:connectedSwimmingDebugAndroidTest :wear:connectedRunningDebugAndroidTest :wear:connectedCyclingDebugAndroidTest`

Expected: all action-state assertions pass, and no action label or summary field is clipped on the configured round emulator.

- [ ] **Step 7: Commit the Wear interaction flow**

Commit: `feat(wear): add timer and activity history screens`

---

### Task 5: Add the shared sync contract and watch outbox

**Files:**

- Create: `sportCore/src/main/java/br/com/helpdev/lapscounter/sync/ActivitySyncRecord.kt`
- Create: `sportCore/src/main/java/br/com/helpdev/lapscounter/sync/ActivitySyncCodec.kt`
- Create: `sportCore/src/test/java/br/com/helpdev/lapscounter/sync/ActivitySyncCodecTest.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/sync/ActivitySyncTransport.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/sync/DataLayerActivitySyncTransport.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/sync/ActivitySyncOutbox.kt`
- Create: `wear/src/main/java/br/com/helpdev/lapscounter/wear/sync/ActivityAckListenerService.kt`
- Modify: `wear/src/main/AndroidManifest.xml`
- Create: `wear/src/test/java/br/com/helpdev/lapscounter/wear/sync/ActivitySyncOutboxTest.kt`
- Create: `wear/src/androidTest/java/br/com/helpdev/lapscounter/wear/sync/DataLayerActivitySyncTransportTest.kt`

**Interfaces:**

- `ActivitySyncRecord` includes schema version, sport ID, activity UUID, generated name, optional description, start timestamp, lap distance, travelled distance, final-lap setting, running/paused time, and ordered lap records.
- `ActivitySyncCodec.encode(record): ByteArray` and `ActivitySyncCodec.decode(bytes): ActivitySyncRecord` use a stable versioned binary format with strict bounds and trailing-data checks.
- `ActivitySyncTransport.send(record)` sends one record through the Wear OS Data Layer. Inline payloads are capped at 90 KiB; use a Data Layer `Asset` for larger encoded records.
- `ActivitySyncOutbox` reads pending records from Room, retries send requests safely, and marks an activity synced only after receiving the matching UUID/version acknowledgement.
- The phone returns only acknowledgement metadata; activity content flows watch to phone.

- [ ] **Step 1: Add failing payload codec tests**

Cover round-trip of metadata and all lap fields, unknown version, truncated payload, excessive lap count, and an encoded record larger than 90 KiB.

- [ ] **Step 2: Run codec tests to confirm the contract is missing**

Run: `sh gradlew :sportCore:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sync.ActivitySyncCodecTest`

Expected: compilation fails because the sync record and codec are not defined.

- [ ] **Step 3: Implement the versioned shared activity record and codec**

Reject unsupported schema versions, invalid IDs, negative durations/distances, duplicate lap indexes, truncation, and trailing bytes. Preserve the exact same UUID used by the watch Room row.

- [ ] **Step 4: Add a testable watch sync transport and outbox**

Implement Data Layer `DataClient` delivery using a stable `/activities/<uuid>` path, `DataMap`, and `Asset` above the inline limit. Persist `PENDING` before requesting delivery and keep the Room copy after sync. Add a listener for `/activity-acks/<uuid>`.

- [ ] **Step 5: Test inline and Asset delivery selection**

Assert payloads up to 90 KiB use inline bytes and larger payloads use a Data Layer Asset. Inject the DataClient adapter so payload selection is testable without live Play services.

Run: `sh gradlew :wear:connectedSwimmingDebugAndroidTest`

Expected: the small/large payload tests pass and preserve the activity UUID and schema version.

- [ ] **Step 6: Test offline retry, duplicate send, and acknowledgement transitions**

Run: `sh gradlew :wear:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.wear.sync.ActivitySyncOutboxTest`

Expected: a disconnected/failed transport leaves the row pending, retry uses the same activity UUID, and only a matching acknowledgement changes status to synced.

- [ ] **Step 7: Run shared codec tests for all sport variants and commit**

Run: `sh gradlew :sportCore:testSwimmingDebugUnitTest :sportCore:testRunningDebugUnitTest :sportCore:testCyclingDebugUnitTest --tests br.com.helpdev.lapscounter.sync.ActivitySyncCodecTest`

Expected: all variants pass the same codec boundary tests.

Commit: `feat(wear): queue versioned activity sync`

---

### Task 6: Import watch activities into the matching phone app

**Files:**

- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sync/ActivitySyncImporter.kt`
- Create: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sync/ActivitySyncListenerService.kt`
- Modify: `lapsCounter/src/main/AndroidManifest.xml`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/model/dao/ActivityDao.kt`
- Modify: `lapsCounter/src/main/java/br/com/helpdev/lapscounter/model/repository/ActivityRepository.kt`
- Create: `lapsCounter/src/test/java/br/com/helpdev/lapscounter/sync/ActivitySyncImporterTest.kt`
- Create: `lapsCounter/src/androidTest/java/br/com/helpdev/lapscounter/sync/ActivitySyncRealmIntegrationTest.kt`

**Interfaces:**

- `ActivitySyncImporter.toActivityEntity(record, expectedSportId): ActivityEntity` validates schema/flavor and maps every field to the existing Realm entity, including identical UUID, dates, metre distances, and ordered lap rows.
- `ActivityRepository.importSynced(record, onCommitted, onError)` upserts through Realm and invokes `onCommitted` only after the Realm transaction succeeds.
- `ActivitySyncListenerService` acknowledges the activity UUID and schema version only after `onCommitted`; malformed or mismatched records receive no success acknowledgement.

- [ ] **Step 1: Add failing importer tests**

Assert exact field and lap mapping, unchanged UUID, rejection of wrong flavor and unsupported schema version, and safe handling of invalid payload values.

- [ ] **Step 2: Run importer tests and confirm the mapper is missing**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sync.ActivitySyncImporterTest`

Expected: compilation fails because the importer does not exist.

- [ ] **Step 3: Implement the DTO-to-Realm mapping and flavor validation**

Use `SportProfileProvider.get().sportId` as the expected flavor. Set `ActivityEntity.id` to the watch UUID and map every lap in order. Do not add a Realm schema field for watch origin or sync status.

- [ ] **Step 4: Add Realm transaction completion callbacks for sync imports**

Keep the existing `save` behavior intact. Add a sync-specific repository method that calls `Realm.insertOrUpdate` and reports success/error after the transaction callback.

- [ ] **Step 5: Register the phone Data Layer listener and acknowledgement path**

Handle incoming activity DataItems, decode inline bytes or Assets, import through the repository, and publish `/activity-acks/<uuid>` only after commit. Ignore and do not acknowledge invalid versions or cross-flavor payloads.

- [ ] **Step 6: Run importer tests and Realm idempotency instrumentation test**

Run: `sh gradlew :lapsCounter:testSwimmingDebugUnitTest --tests br.com.helpdev.lapscounter.sync.ActivitySyncImporterTest && sh gradlew :lapsCounter:connectedSwimmingDebugAndroidTest`

Expected: importing the same UUID twice leaves one activity with unchanged lap records; failed imports do not emit an acknowledgement.

- [ ] **Step 7: Commit phone-side sync ingestion**

Commit: `feat(sync): import watch activities into phone history`

---

### Task 7: Complete flavor parity, integration checks, and release documentation

**Files:**

- Modify: `README.md`
- Modify: `docs/release-signing.md`
- Modify: `gradle/libs.versions.toml` or `wear/build.gradle` only if the Wear dependency/version matrix needs a compatibility correction.
- Modify: `wear/src/androidTest/` paired-device integration coverage.
- Modify: `lapsCounter/src/androidTest/` sync importer integration coverage.

**Interfaces:**

- Phone and Wear use matching sport IDs and same package IDs for each flavor; debug builds retain matching package signatures for Data Layer testing.
- Phone version codes remain `appVersionCode`; Wear version codes use `1_000_000 + appVersionCode`; `versionName` remains shared.
- Release output is three Wear OS app bundles for Play's Wear OS form-factor track. This task does not upload or publish them.

- [ ] **Step 1: Add a three-flavor sync integration test matrix**

Exercise a saved session per sport and assert the matching phone profile accepts it, the phone Realm history contains the same UUID and laps, and a duplicate delivery leaves one activity.

- [ ] **Step 2: Verify recovery, offline use, and sync acknowledgement**

On a Wear OS emulator, save one session while the phone app is absent/disconnected. Reconnect/install the matching Android debug app, confirm delivery and acknowledgement, then reopen the watch history and confirm the local copy remains.

- [ ] **Step 3: Verify round-screen and locale behavior**

Run the timer and history flows on one small-round and one larger Wear OS emulator in English, Portuguese, and Spanish. Confirm no primary action, lap value, metric, or sync status clips.

- [ ] **Step 4: Build all phone and Wear debug flavors and run all unit suites**

Run: `sh gradlew :sportCore:testSwimmingDebugUnitTest :sportCore:testRunningDebugUnitTest :sportCore:testCyclingDebugUnitTest :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:testRunningDebugUnitTest :lapsCounter:testCyclingDebugUnitTest :wear:testSwimmingDebugUnitTest :wear:testRunningDebugUnitTest :wear:testCyclingDebugUnitTest :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug :wear:assembleSwimmingDebug :wear:assembleRunningDebug :wear:assembleCyclingDebug`

Expected: all nine flavor unit-test tasks and all six application assemblies succeed.

- [ ] **Step 5: Run the three-flavor instrumentation matrix**

With a Wear OS emulator selected, run `sh gradlew :wear:connectedSwimmingDebugAndroidTest :wear:connectedRunningDebugAndroidTest :wear:connectedCyclingDebugAndroidTest`. With a phone emulator selected, run `sh gradlew :lapsCounter:connectedSwimmingDebugAndroidTest :lapsCounter:connectedRunningDebugAndroidTest :lapsCounter:connectedCyclingDebugAndroidTest`. Pair corresponding phone and Wear emulators for the sync case.

Expected: timer/UI/Room checks pass on Wear, Realm importer checks pass on phone, and each sport session reaches only its matching phone history once.

- [ ] **Step 6: Inspect merged manifests and version codes for all variants**

Confirm the phone manifests do not gain the watch hardware feature, each Wear manifest declares it and `standalone=true`, package IDs match by sport, and phone/Wear codes are unique and increasing according to the formula.

- [ ] **Step 7: Document Wear builds, sync limits, signing, and Play track steps**

Update README build commands and product table. Document Android-only Data Layer pairing, local offline history, one-way activity sync, version-code formula, existing same-name Play listing, closed Wear OS testing track, screenshot requirements, and per-flavor Upload key certificate verification.

- [ ] **Step 8: Verify the release signing certificate for each sport listing**

For Swimming, Running, and Cycling, verify the configured alias and SHA-256 certificate against that listing's registered Upload key certificate using the release-signing guide. Do not print or expose passwords. Stop before release builds if any fingerprint is unavailable or mismatched.

- [ ] **Step 9: Generate but do not upload the three Wear release bundles**

Run: `sh gradlew :wear:bundleSwimmingRelease :wear:bundleRunningRelease :wear:bundleCyclingRelease`

Expected: three signed Wear OS AABs are produced with the expected package names and version codes. Stop before Play Console upload or rollout.

- [ ] **Step 10: Commit integration and release documentation**

Commit: `docs(wear): document multi-sport watch release flow`

---

## Self-Review

- Spec coverage: watch independence, sport defaults/metrics, local session persistence, generated watch-side names, one-way sync, UUID idempotency, Android-only pairing, matching package IDs, Play manifest metadata, version codes, localization, ads exclusion, and release/testing expectations each map to Tasks 1–7.
- Task boundaries: shared rules, app packaging, local timer, UI, watch transport, phone ingestion, and final integration can each be reviewed and validated independently.
- Review Focus tests are pinned to the tasks that own each behavior.
- No task uploads or publishes an artifact; the final release-bundle step stops before Play Console.
