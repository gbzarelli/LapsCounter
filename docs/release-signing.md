# Android release signing and publishing

## Signing credentials

Release keystores are maintained outside this checkout in the sibling `../android-keys/` repository. The release build reads credentials from the ignored root `keystore.properties`; never commit this file or a keystore. Copy `keystore.properties.example` and set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. `storeFile` may be absolute or relative to the repository root. Release tasks stop if any value is missing or the keystore file does not exist.

Before creating a bundle, verify the selected alias and compare its SHA-256 certificate fingerprint with the **upload certificate** registered for that app in Play Console. Play App Signing may use a different certificate to sign delivered APKs. If the upload certificate differs, stop; do not reset a key or upload an artifact until the owner approves the Play Console reset flow.

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
