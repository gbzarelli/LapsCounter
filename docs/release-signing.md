# Android release signing

The release build reads credentials from the ignored root-level `keystore.properties`; never commit this file or a keystore. Copy `keystore.properties.example` and set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. `storeFile` may be absolute or relative to the repository root. The release task intentionally fails if any value is missing.

Before building, verify the selected keystore alias and compare its SHA-256 certificate fingerprint with the **upload certificate** shown for `br.com.helpdev.lapscounter.swimming` in Play Console. Do not compare it with the Play app-signing certificate: Play App Signing may use a separate key to sign delivered APKs. If the upload certificate differs, stop; do not reset a key or upload an artifact until the owner approves the Play Console reset flow.

```bash
keytool -list -v -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS"
sh gradlew :lapsCounter:bundleSwimmingRelease
bundletool dump manifest --bundle lapsCounter/build/outputs/bundle/swimmingRelease/lapsCounter-swimming-release.aab
jarsigner -verify -verbose -certs lapsCounter/build/outputs/bundle/swimmingRelease/lapsCounter-swimming-release.aab
```

Confirm the bundle reports package `br.com.helpdev.lapscounter.swimming`, version code greater than `7`, min SDK `24`, target SDK `36`, and the verified upload certificate. Keep fingerprints and any release checklist in a secure local location, not Git. Test the signed bundle through Play internal testing before requesting a broader rollout.
