# LapsCounter Recovery and Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernize LapsCounter, restore compliant ads, verify upload signing, and republish the existing `br.com.helpdev.lapscounter.swimming` Play listing.

**Architecture:** Upgrade build and AndroidX foundations before behavior changes. Isolate ad loading behind a lifecycle-aware manager, preserve production AdMob IDs in the swimming flavor, and gate every external Play action behind verified signing, test-track evidence, and completed declarations.

**Tech Stack:** Kotlin, Android API 36, View Binding, AndroidX Lifecycle/Navigation, Realm migration assessment, Google Mobile Ads SDK, UMP consent SDK, JUnit/Espresso, Play App Signing.

**Spec:** `docs/superpowers/specs/2026-10-14-lapscounter-recovery-modernization-design.md`

## Global Constraints

- Execute only after `2026-10-14-chronometer-library-modernization.md` has produced a resolvable release tag.
- Preserve application ID `br.com.helpdev.lapscounter.swimming`, app name, existing production AdMob app ID, and the two existing production unit IDs.
- Set `minSdk 24`, `compileSdk 36`, and `targetSdk 36`; increment `versionCode` above 7.
- Use JDK 17 and mutually compatible current stable Gradle/AGP/Kotlin versions verified against Android's official compatibility table before editing version pins.
- Never commit keystores, passwords, consent test-device IDs, `local.properties`, or `google-services.json`.
- Do not submit, roll out, reset an upload key, or alter any Play/AdMob console state without user confirmation immediately before the external action.

## Review Focus

- First launch, consent denial, consent error, and consent reset must not cause live ad requests before consent resolution.
- An interstitial load failure, expiry, rotation, or destroyed activity must never loop, leak, or block navigation.
- Existing Realm data must survive update-in-place and schema migration; no timing history may be deleted.
- Android 12+ headset/Bluetooth behavior and launcher export requirements must work with newly required permissions.
- The generated AAB must carry the existing application ID, a higher version code, and the Play upload-certificate signature.

---

### Task 1: Baseline the existing app and introduce a safe modern build foundation

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/settings.gradle`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/build.gradle`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/gradle/wrapper/gradle-wrapper.properties`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/build.gradle`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/AndroidManifest.xml`
- Create: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/gradle/libs.versions.toml`
- Create: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/keystore.properties.example`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/.gitignore`

**Interfaces:**
- Produces: `swimmingDebug` and `swimmingRelease` variants that compile with JDK 17/API 36 and read signing configuration only from an ignored local/CI source.

- [ ] **Step 1: Capture baseline behavior on an API-28-compatible emulator: start/pause/resume/stop, lap/remove lap, save/view/share activity, settings, rotation, and headset input. Record failures before migration.**
- [ ] **Step 2: Replace legacy Gradle configuration, JCenter, Android Extensions, legacy Safe Args, and obsolete dependency declarations with a version catalog and current compatible plugins.**
- [ ] **Step 3: Add `namespace "br.com.helpdev.lapscounter"`, API 36 SDK settings, Java/Kotlin 17 targets, View Binding, explicit launcher `android:exported="true"`, and Android 12+ Bluetooth permission handling while retaining the swimming application-ID suffix.**
- [ ] **Step 4: Remove the unconditional Google Services plugin unless a valid, intentionally configured Firebase integration is retained; do not invent a `google-services.json`.**
- [ ] **Step 5: Replace the hard-coded `signingConfigs.releaseKey` values with optional `keystore.properties` loading, fail release builds clearly when required fields are absent, and add only the redacted example file.**
- [ ] **Step 6: Point the chronometer dependency at the verified new library release and run `sh gradlew :lapsCounter:assembleSwimmingDebug`.**

Expected: a clean checkout builds debug without JCenter, absolute signing paths, or secrets.

- [ ] **Step 7: Commit the build foundation.**

```bash
git add settings.gradle build.gradle gradle lapsCounter/build.gradle lapsCounter/src/main/AndroidManifest.xml .gitignore keystore.properties.example
git commit -m "build: modernize Android toolchain"
```

### Task 2: Replace deprecated Android UI and lifecycle APIs without changing flows

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ChronometerActivity.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/ActivitiesActivity.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/SettingsActivity.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/fragment/ActivityFragment.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/fragment/ListActivityFragment.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/utils/PreferenceUtils.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/widget/FloatPickerPreference.kt`

**Interfaces:**
- Produces: View Binding-backed screens, `ViewModelProvider` ownership, AndroidX preferences, and Activity Result APIs where a result is consumed.

- [ ] **Step 1: Add focused Robolectric or instrumentation smoke tests for chronometer controls, save/list navigation, settings persistence, and configuration recreation.**
- [ ] **Step 2: Run the new tests against the pre-migration implementation and confirm failures describe the missing/legacy lifecycle behavior being pinned.**
- [ ] **Step 3: Replace every `kotlinx.android.synthetic` import with lifecycle-safe View Binding references, clearing fragment bindings in `onDestroyView`.**
- [ ] **Step 4: Replace `ViewModelProviders`, platform `PreferenceManager`, and consumed `onActivityResult` calls with current AndroidX APIs; update Navigation dependency and graph usage together.**
- [ ] **Step 5: Run `sh gradlew :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:assembleSwimmingDebug` and repeat the device smoke script from Task 1.**

Expected: no synthetic or deprecated lifecycle/provider API remains; all existing user flows still work.

### Task 3: Upgrade persistence with an update-in-place data safety check

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/build.gradle`
- Modify as required: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/AppApplication.kt`
- Modify as required: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/model/entity/ActivityEntity.kt`
- Modify as required: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/model/entity/ChronometerEntity.kt`
- Modify as required: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/model/entity/LapEntity.kt`
- Modify as required: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/model/dao/ActivityDao.kt`
- Create: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/androidTest/java/br/com/helpdev/lapscounter/model/RealmUpgradeTest.kt`

**Interfaces:**
- Produces: a supported Realm configuration with explicit schema-version/migration policy and verified preservation of saved activity/lap data.

- [ ] **Step 1: Decide and document whether the current Realm Java dependency supports the selected AGP/Kotlin versions; migrate to the supported Realm alternative only if compatibility testing requires it.**
- [ ] **Step 2: Add an instrumentation fixture that creates the version-7 schema data, upgrades the application, and asserts activity fields, chronometer totals, and ordered laps are retained.**
- [ ] **Step 3: Run the test to establish the pre-migration result, then add explicit schema version and migration behavior before changing dependencies.**
- [ ] **Step 4: Update persistence dependencies/configuration and run `sh gradlew :lapsCounter:connectedSwimmingDebugAndroidTest`.**

Expected: upgrading an installed app preserves user timing history; no destructive Realm migration is permitted.

### Task 4: Implement a consent-aware, modern AdMob integration

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/AppApplication.kt`
- Replace: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/ads/InterstitialAd.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/ads/InterstitialActivity.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/fragment/ActivityFragment.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/SettingsActivity.kt`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/swimming/res/values/admob.xml`
- Create: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/main/java/br/com/helpdev/lapscounter/ui/ads/AdsManager.kt`
- Create: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/src/test/java/br/com/helpdev/lapscounter/ui/ads/AdsManagerTest.kt`

**Interfaces:**
- Produces: `AdsManager.requestConsent(activity)`, `AdsManager.loadInterstitial(context)`, and `AdsManager.showInterstitial(activity, onDismissed)` with release/debug ID separation.

- [ ] **Step 1: Add failing unit tests for the 15-minute cap, consent gate, one-shot dismissal callback, load failure, and expired/unavailable interstitial behavior.**
- [ ] **Step 2: Replace the legacy `MobileAds.initialize(context, appId)` and `InterstitialAd(context)` APIs with current SDK initialization, UMP consent sequencing, `InterstitialAd.load`, `InterstitialAdLoadCallback`, and `FullScreenContentCallback`.**
- [ ] **Step 3: Supply Google test banner/interstitial IDs through `src/debug` resources and retain the inspected production IDs only in `src/swimming`; configure a test device outside committed source.**
- [ ] **Step 4: Make banner loads wait for consent, route failures to structured logs, and show interstitials only after a successfully loaded, non-expired instance exists.**
- [ ] **Step 5: Run unit tests, install a debug build on a physical test device, and verify test ads, consent choices, cap behavior, rotation, navigation, and no live request.**

Expected: debug shows only test ads; release retains production IDs, has no policy-center blocker, and produces actionable load diagnostics.

### Task 5: Verify signing and generate the recovery AAB

**Files:**
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/build.gradle`
- Modify: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/lapsCounter/proguard-rules.pro`
- Create: `/Volumes/gbz-ext/repositories/helpdev/LapsCounter/docs/release-signing.md`

**Interfaces:**
- Produces: `lapsCounter-swimming-release.aab` signed by the verified Play upload key.

- [ ] **Step 1: Read the keystore password only from the local ignored secret source; use `keytool` to record alias and SHA-1/SHA-256 fingerprints in a secure local release checklist, not Git.**
- [ ] **Step 2: In Play Console, compare that SHA-256 against the app's upload certificate. If different, stop and request user confirmation immediately before using Play's upload-key-reset flow.**
- [ ] **Step 3: Set `versionCode` to the first unused integer above 7 and update `versionName` according to the release notes.**
- [ ] **Step 4: Run `sh gradlew :lapsCounter:bundleSwimmingRelease`, then use `bundletool`/`apksigner` to confirm package ID, version code, min/target SDK, and upload certificate.**
- [ ] **Step 5: Install generated device APKs on API 24 and API 36 emulators and execute the full smoke matrix.**

Expected: release artifact has the original package, a greater version code, and verified upload signing.

### Task 6: Recover the Play listing and roll out safely

**External console gates:** Play Console and AdMob settings can create public/external effects. Inspect state freely; request confirmation immediately before each submission, key reset, release rollout, or publication.

- [ ] **Step 1: In the existing Play app, retrieve the full removal notice/case and determine whether a reinstatement action, remediation, or appeal is required. Do not assume that uploading a new AAB clears removal.**
- [ ] **Step 2: Complete the current app setup requirements: developer verification, policy declarations, Data safety, privacy-policy URL, app access, ads declaration, content rating, target countries, and store-listing assets. Reconcile each answer against the final code and consent behavior.**
- [ ] **Step 3: In AdMob, retain the existing Ready app and units; confirm the production package/store listing linkage after Play recovery. Do not create new units unless the existing mapping is rejected.**
- [ ] **Step 4: Request user confirmation, upload the signed AAB to internal testing, and add controlled testers. Verify Play pre-launch results, Android vitals, consent path, production ad loading, and policy status.**
- [ ] **Step 5: Request user confirmation, promote to closed testing. Resolve every blocking pre-launch or policy issue and collect acceptance evidence before production.**
- [ ] **Step 6: Request user confirmation, submit the recovery release and start a staged production rollout. Monitor crash/ANR, review, policy, and ad-fill metrics; pause rollout if a regression or policy restriction appears.**

Expected: the existing Play listing returns through a compliant closed-test and staged-production process, without changing its package identity or replacing valid AdMob units.
