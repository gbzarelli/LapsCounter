# LapsCounter Recovery and Modernization Design

## Goal

Recover and republish the existing Play application `br.com.helpdev.lapscounter.swimming` while modernizing the app and its chronometer-library dependency, restoring compliant AdMob serving, and preserving the existing Play listing, package name, and AdMob unit IDs.

## Confirmed State

- Play Console lists the app as **Removed by Google** (last updated January 7, 2020); its production track is inactive, but its dashboard still exposes release setup.
- The application is currently `targetSdk 28`, `minSdk 19`, Gradle 5.4.1, Android Gradle Plugin 3.5.2, and Kotlin 1.3.61. The library is older still (AGP 3.4.2 and Kotlin 1.3.41).
- Play requires app updates to target API 36. See [the official requirement](https://developer.android.com/google/play/requirements/target-sdk).
- The AdMob app is Ready, its two existing units match the IDs in the source, and the AdMob Policy Center reports no current serving restriction. The legacy interstitial API in source must be replaced. See [the official migration guide](https://developers.google.com/admob/android/migration).

## Technical Architecture

Modernize `chronometer-library` first, publish a tested semantic-versioned release, then update LapsCounter to consume it. Both repositories use JDK 17, current stable Gradle/AGP/Kotlin versions mutually compatible at implementation time, Google and Maven Central repositories, API 36 compile/target SDK, and `minSdk 24`.

Replace Android synthetics with View Binding, legacy lifecycle/navigation APIs with their AndroidX equivalents, and obsolete preference APIs. Preserve Realm-backed data and chronometer behavior; add regression coverage before changing converters, timing, saved activities, or preference logic.

Use an isolated ads component. It initializes Mobile Ads once, loads banners per visible screen, preloads an interstitial, and reports load/show/failure events to callers. Display an interstitial only at an existing natural transition and preserve the 15-minute cap. Debug builds use Google test IDs and a registered test device; release builds use the existing production units. Obtain applicable UMP consent before production ad requests and align Play Data safety and privacy disclosures with the implemented behavior.

## Signing and Release Design

Remove the hard-coded placeholder credentials and absolute keystore path from Gradle. Read release signing values from an ignored `keystore.properties` file locally or CI secrets. Extract the local keystore's certificate fingerprint without exposing credentials and compare it with Play's **upload certificate**. If it differs, reset only the upload key through Play Console; never replace the Play app-signing key without explicit approval.

The release flow retains the existing package and increments `versionCode` above 7. Resolve the exact removal reason, developer-verification status, policy declarations, Data safety form, privacy-policy URL, content rating, countries, and other release blockers before uploading. Deliver a signed AAB to internal/closed testing first, resolve pre-launch and policy findings, then use a staged production rollout and monitor crashes, ANRs, policy status, and ad fill.

## Scope and Non-Goals

This work restores the current swimming application and preserves its public identity. Creating a new Play app, changing the application ID, replacing AdMob IDs, or changing billing/monetization strategy is out of scope unless Play formally prevents recovery. The old Android 6-and-earlier audience is intentionally no longer supported because the approved current Mobile Ads SDK path requires `minSdk 24`.

## Acceptance Criteria

1. Both repositories build and test with the selected current Android toolchain.
2. LapsCounter produces a signed `swimmingRelease` AAB for the existing package with an incremented version code.
3. Core stopwatch, lap persistence, saved-activity, settings, headset, sharing, and rotation flows pass automated and device-level smoke checks.
4. Debug ads use only test inventory; release ads load the existing app and ad-unit IDs, respect consent and frequency limits, and expose actionable load-failure diagnostics.
5. The existing Play listing reaches a compliant production rollout after a successful closed test and completed console declarations.
