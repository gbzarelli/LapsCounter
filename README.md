# LapsCounter
Android phone and standalone Wear OS stopwatch apps for swimming, running, and cycling. The apps share sport rules, units, and metrics. The phone provides activity editing and sharing; the watch provides independent timing and local history.

## Product variants

| Sport | Phone and Wear app name | Shared release application ID | Existing Play listing |
|---|---|---|---|
| Swimming | Swimming Stopwatch | `br.com.helpdev.lapscounter.swimming` | [Google Play](https://play.google.com/store/apps/details?id=br.com.helpdev.lapscounter.swimming) |
| Running | Running Stopwatch | `br.com.helpdev.lapscounter.running` | [Google Play](https://play.google.com/store/apps/details?id=br.com.helpdev.lapscounter.running) |
| Cycling | Cycling Stopwatch | `br.com.helpdev.lapscounter.cycling` | [Google Play](https://play.google.com/store/apps/details?id=br.com.helpdev.lapscounter.cycling) |

All three sports support manual timing on the phone and watch. Wear OS supports independent timing, configurable lap distance/final-lap counting, generated activity names, local Room history, and sport metrics (swimming pace per 100 m, running pace per km, cycling speed in km/h). The watch has no ads. GPS, automatic splits, and health-sensor tracking are not included.

Saved watch activities sync one way into the matching Android phone app's Realm history. The phone advertises `lapscounter_phone`; UUID deduplication makes retries safe, and the phone returns a UUID/schemaVersion acknowledgement only after the import commits. The watch retains its local copy after acknowledgement. Offline saves remain pending and retry when the app resumes or the phone capability becomes reachable; sync is eventual and requires compatible Google Play services on both devices. Phone edits/deletes and settings do not sync back to the watch.

[Data Layer pairing](https://developer.android.com/training/wearables/data/overview) supports Android phones, requires matching package IDs and signing certificates, and does not support iOS. Debug IDs append `.debug` on both devices; install the same sport and use the same debug keystore.

## Build and test

```bash
sh gradlew :sportCore:testSwimmingDebugUnitTest :sportCore:testRunningDebugUnitTest :sportCore:testCyclingDebugUnitTest
sh gradlew :wear:testSwimmingDebugUnitTest :wear:testRunningDebugUnitTest :wear:testCyclingDebugUnitTest
sh gradlew :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:testRunningDebugUnitTest :lapsCounter:testCyclingDebugUnitTest
sh gradlew :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug
sh gradlew :wear:assembleSwimmingDebug :wear:assembleRunningDebug :wear:assembleCyclingDebug
ANDROID_SERIAL=<watch-serial> sh gradlew :wear:connectedSwimmingDebugAndroidTest :wear:connectedRunningDebugAndroidTest :wear:connectedCyclingDebugAndroidTest
ANDROID_SERIAL=<phone-serial> sh gradlew :lapsCounter:connectedSwimmingDebugAndroidTest :lapsCounter:connectedRunningDebugAndroidTest :lapsCounter:connectedCyclingDebugAndroidTest
```

Use Java 17+ and the Android SDK configured in `gradle/libs.versions.toml`; Wear requires API 30+, phone API 24+. `sportCore` holds shared sport rules and the versioned sync contract. Instrumentation covers real local Room/Realm persistence and import idempotency; it is not proof of live paired delivery. Before release, pair corresponding sport apps and test an offline save → reconnection → phone UUID/laps → ACK → retained watch history. Check timer, summary, settings, history/detail, metrics and sync status on small/large round watches in English, Portuguese and Spanish.

Phone version codes use `appVersionCode`; Wear uses `1_000_000 + appVersionCode`, with a shared `appVersionName`. Current values are phone `15`, Wear `1000015`, name `2.2.6`. Recheck every listing's version history before release. Wear belongs to the existing same-name sport Play listing and its dedicated Wear OS form-factor closed testing track; see the release guide for signing, screenshots and local bundle commands.

See [third-party notices](THIRD_PARTY_NOTICES.md) for in-app icon attribution.

## Release signing

Release keystores are maintained outside this checkout in the sibling `../android-keys/` repository. Create the ignored root `keystore.properties` from `keystore.properties.example`, then use only a key whose SHA-256 fingerprint matches the upload certificate registered for the target Play listing. Verify each flavor separately; see [the release signing guide](docs/release-signing.md).

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[Apache-2.0](https://choosealicense.com/licenses/apache-2.0/)
