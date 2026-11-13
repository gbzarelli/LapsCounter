# Android release signing and publishing

## Signing credentials

The canonical upload keystore is `~/repositories/helpdev/android-keys/laps_counter.jks` (also available at `/Volumes/gbz-ext/repositories/helpdev/android-keys/laps_counter.jks`), alias `laps_counter`. Keep all keystores and password files in the external `android-keys` repository. Do not use `../android-keys/lapscounter-upload.jks`, which has a different certificate. The release build reads credentials from the ignored root `keystore.properties`; never commit this file or a keystore. Copy `keystore.properties.example` and set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. `storeFile` may be absolute or relative to the repository root. Release tasks stop if any value is missing or the keystore file does not exist.

For both phone and Wear, before creating a bundle, verify the selected alias and compare its SHA-256 certificate fingerprint with the **upload certificate** registered for that app in Play Console. Play App Signing may use a different certificate to sign delivered APKs. If the upload certificate differs, stop; do not reset a key or upload an artifact until the owner approves the Play Console reset flow.

```bash
keytool -list -v -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS"
```

Check each Play listing independently. A shared upload key is valid only when its SHA-256 fingerprint matches the upload certificate registered for every target app. Do not use the app-signing certificate fingerprint for this comparison. If a matching key is unavailable, stop; do not reset an upload key or upload a differently signed bundle without approval from the app owner.

## Variants and bundle commands

| Flavor | Package ID | Bundle task | Output bundle |
|---|---|---|---|
| Swimming | `br.com.helpdev.lapscounter.swimming` | `:lapsCounter:bundleSwimmingRelease` | `lapsCounter/build/outputs/bundle/swimmingRelease/lapsCounter-swimming-release.aab` |
| Running | `br.com.helpdev.lapscounter.running` | `:lapsCounter:bundleRunningRelease` | `lapsCounter/build/outputs/bundle/runningRelease/lapsCounter-running-release.aab` |
| Cycling | `br.com.helpdev.lapscounter.cycling` | `:lapsCounter:bundleCyclingRelease` | `lapsCounter/build/outputs/bundle/cyclingRelease/lapsCounter-cycling-release.aab` |

Build the intended app only after completing its checks:

```bash
sh gradlew :lapsCounter:bundleSwimmingRelease
sh gradlew :lapsCounter:bundleRunningRelease
sh gradlew :lapsCounter:bundleCyclingRelease
bundletool dump manifest --bundle lapsCounter/build/outputs/bundle/swimmingRelease/lapsCounter-swimming-release.aab
jarsigner -verify -verbose -certs lapsCounter/build/outputs/bundle/swimmingRelease/lapsCounter-swimming-release.aab
```

Run `bundletool` and `jarsigner` with the corresponding output path for Running or Cycling as needed. Confirm package ID, version code, min SDK `24`, target SDK `36`, and upload-certificate fingerprint for the selected listing. Keep fingerprints and release checklists in secure local storage, not Git.

Each flavor has its own Play listing and version-code history. Verify the package ID, registered upload certificate, and next version code for each listing before upload. Shared Gradle version values do not replace these per-app checks.

## Production AdMob IDs

Release builds resolve production IDs from the selected flavor's `src/<flavor>/res/values/admob.xml`. Every flavor needs its own production app, banner, and interstitial IDs. The release guard validates the ID shape and rejects missing values and Google's documented test publisher IDs. Keep production IDs in their flavor resources; `src/debug/res/values/admob.xml` supplies test IDs only to debug builds.

`src/debug/res/values/admob.xml` supplies Google's test IDs to all debug flavors. Do not move production IDs into `main` or `debug`, and do not add test IDs to a release flavor. Debug variants remain buildable without production ad setup.

Before release, validate consent, ad loading, privacy policy, Play declarations, and the store listing through the app's internal test track.

## Wear OS release gate and local bundles

Phone and Wear share each sport's release package ID and signing configuration. Debug packages append `.debug` on both modules and must share the debug signing certificate for paired testing. Phone codes are `appVersionCode`; Wear codes are `1_000_000 + appVersionCode`; names are shared. Keep `appVersionCode` in 1–999999 and increase it for each release. The current values (`13` / `1000013`, `2.2.4`) are build configuration, not evidence that the next upload is accepted by Play. Inspect each app's entire version history before a release.

For **each** Swimming, Running, and Cycling listing:

1. Verify the package ID in the product table. In Play Console, open **Protected with Play → Play Store protection → Manage Play app signing** (older UI: **Test and release → App integrity → App signing**).
2. Read/copy the SHA-256 fingerprint specifically under **Upload key** or download that section's certificate and inspect it with `keytool -printcert -file <downloaded-certificate>`. The **App signing key** and Digital Asset Links fingerprints are not upload-key evidence.
3. Compare to the locally configured alias using the `keytool -list` command above; use an interactive password prompt, never a password in command arguments or logs. Record the listing, date, package, alias, and match result in private release evidence.
4. If any registered fingerprint is unavailable or differs, **stop before all Wear release bundle tasks**. Do not reset keys, upload, publish, or substitute an app-signing certificate. A known local key or a certificate filename alone does not verify the current registration.

Only after all three listing checks pass, generate these local artifacts:

```bash
sh gradlew :wear:bundleSwimmingRelease :wear:bundleRunningRelease :wear:bundleCyclingRelease
```

| Flavor | Wear bundle |
|---|---|
| Swimming | `wear/build/outputs/bundle/swimmingRelease/wear-swimming-release.aab` |
| Running | `wear/build/outputs/bundle/runningRelease/wear-running-release.aab` |
| Cycling | `wear/build/outputs/bundle/cyclingRelease/wear-cycling-release.aab` |

For each AAB, use `bundletool dump manifest --bundle <path>`, `jarsigner -verify -verbose -certs <path>`, and `keytool -printcert -jarfile <path>`. Verify the sport package, shared version name, Wear code formula, min SDK 30, target SDK 36, `android.hardware.type.watch` required, `com.google.android.wearable.standalone=true`, and the matching upload certificate. Phone manifests must not declare the watch hardware feature. Wear has no AdMob dependency or ad configuration; the production AdMob section concerns only the phone module.

## Wear OS Play preparation (separate from local builds)

Use each existing same-name sport listing; do not create a second listing or upload the Wear bundle to the phone track. Add/select **Wear OS** under **Advanced settings → Form factors**, then prepare a dedicated **Wear OS closed testing** track with approved testers. Keep the standalone declaration consistent with independent offline timing/history. [Android packaging guidance](https://developer.android.com/training/wearables/packaging) and [dedicated form-factor tracks](https://support.google.com/googleplay/android-developer/answer/13295490) describe the current Console flow.

Supply at least one real Wear OS screenshot per listing as required by Play, showing the current watch app. Prepare localized English, Portuguese and Spanish timer and history/detail captures; verify small and large round layouts, readable splits/metrics/status, and reachable primary actions. Phone screenshots do not replace Wear screenshots. Recheck the Console's current screenshot requirements before submission.

Before any separately authorized upload, complete paired Android/watch testing for each sport: save offline without the phone, reconnect/install the matching signed phone package, confirm identical UUID and lap values in phone history, retry to confirm a single activity, observe the committed-import ACK, and reopen the retained watch copy. Local transport/Room/Realm tests do not replace this live test. Review the Wear quality checklist and store declarations. Local bundle generation does not authorize upload, submission, rollout, or publication.

### Reproducible live sync fixture

`PairedActivitySyncTest` (Wear) and `PairedActivityHistoryTest` (phone) are opt-in tests and skip in the ordinary local instrumentation matrix. Use a fresh canonical UUID for `pairedActivityId` on every run and repeat for each sport. Install the corresponding debug and androidTest APKs on the watch, and install the matching signed phone debug/androidTest APKs when reconnecting. These tests intentionally retain their fixture in debug history for inspection; use disposable test devices.

Build the Swimming fixture APKs (repeat with Running/Cycling):

```bash
sh gradlew :wear:assembleSwimmingDebug :wear:assembleSwimmingDebugAndroidTest :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleSwimmingDebugAndroidTest
```

Install the app APKs from `<module>/build/outputs/apk/swimming/debug/` and test APKs from `<module>/build/outputs/apk/androidTest/swimming/debug/`, using `adb -s <serial> install -r <apk>` for the appropriate device. With the matching phone app absent/disconnected, run on the watch (replace Swimming/package/UUID for each sport):

```bash
adb -s <watch-serial> shell am instrument -w \
  -e class br.com.helpdev.lapscounter.wear.sync.PairedActivitySyncTest \
  -e pairedPhase save -e pairedActivityId <fresh-uuid> \
  br.com.helpdev.lapscounter.swimming.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Pair/reconnect the Android phone and install/open its matching sport app. Run the same watch command with `pairedPhase verify` and the same UUID; it waits up to 60 seconds for the real committed-import ACK and reopens the persisted local copy. Then assert actual phone Realm history:

```bash
adb -s <phone-serial> shell am instrument -w \
  -e class br.com.helpdev.lapscounter.sync.PairedActivityHistoryTest \
  -e pairedActivityId <same-uuid> \
  br.com.helpdev.lapscounter.swimming.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Do not run Gradle connected tests or uninstall the watch app between phases: their installation lifecycle may clear the fixture. The ordinary phone integration matrix separately submits each sport twice and verifies that only the matching flavor accepts it and keeps one UUID with ordered laps. The opt-in pair test verifies real delivery and acknowledgement, not a second distinct Data Layer delivery event; exercise duplicate delivery separately when validating a paired release.
