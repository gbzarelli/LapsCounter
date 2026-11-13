# LapsCounter Wear OS Multi-Sport Design

## Intent

Add a Wear OS version of each existing sport app: Swimming, Running, and Cycling. The watch is the primary timing device during an activity, so starting, pausing, resuming, recording laps, stopping, and saving a session must work without a paired phone or network connection.

When the watch is paired to an Android phone that has the matching sport app installed, completed watch sessions sync into that phone app's activity history. This first version syncs only from watch to phone; phone history and settings are not copied back to the watch.

## Goals

- Provide three separately identifiable Wear OS variants that use the existing Swimming, Running, and Cycling product identities.
- Keep the watch stopwatch and saved session history usable offline.
- Preserve each sport's existing distance defaults, units, and metric: Swimming uses 50 m splits and pace per 100 m; Running uses 1 km splits and pace per km; Cycling uses 5 km splits and average speed in km/h.
- Present a glanceable watch UI for the active timer and lap action, plus a compact summary and local history.
- Transfer each saved watch session to the matching Android phone app, including its ID, sport flavor, name, description if present, start time, lap distance, final-lap policy, elapsed and paused time, and lap records.
- Make phone ingestion idempotent so a retried transfer does not create a second activity.
- Keep the existing mobile app's package IDs, saved records, and release behavior intact.

## Non-Goals

- GPS, routes, automatic lap detection, sensor-based tracking, or Health Services workout metrics.
- Syncing phone-created activities or phone settings back to the watch.
- iOS companion-app support. The existing phone apps are Android apps, and the Wear OS Data Layer only connects Wear OS watches with Android phones.
- Tiles, complications, watch faces, cloud accounts, or a new backend.
- Replacing Realm in the existing phone app.

## Current State and Constraints

- The repository has one Android application module, `:lapsCounter`, with the `swimming`, `running`, and `cycling` product flavors. The flavor-specific `SportProfileProvider` supplies the current distances and metrics.
- The phone UI is AppCompat/XML. Its `ChronometerViewModel` owns the running timer in memory; saved activities are written to Realm when the user saves them.
- `ActivityEntity` already has a UUID primary key, lap records, metre-based distances, a start date, and a final-lap setting. The phone DAO uses `insertOrUpdate`, which can support idempotent ingestion after converting a watch record to the existing entity.
- Shared sport profile and metric classes under `lapsCounter/src/main/java/br/com/helpdev/lapscounter/sport/` are pure Kotlin and are suitable for sharing with the watch after moving them into a small common library. Keep Android resources, Compose UI, and Realm entities out of that library.
- Current build versions are AGP 8.13.2, Kotlin 2.3.20, compile/target SDK 36, Java 17, and min SDK 24. The Wear module should target SDK 36 to align with the repository. Proposed Wear min SDK is API 30 (Wear OS 3); confirm the supported device range during implementation if older watches are a product requirement.
- The Wear UI should use Compose for Wear OS and wearable-specific components. Design for round screens, glanceability, large touch targets, and low power.
- Google Play expects the watch manifest to declare `android.hardware.type.watch` and `com.google.android.wearable.standalone=true`. For an existing mobile app, use the same package name and preferably the same Play listing. Wear OS distribution is managed separately from mobile, and each form-factor artifact needs a coordinated, unique version code. Do not use legacy embedded-Wear packaging.
- The Data Layer can queue data changes while devices are disconnected, but it is a transport, not local storage. Keep the watch's Room database and the phone's Realm database as their respective local sources of truth. Data Layer synchronization is available only for Wear OS and paired Android devices.

## Approaches

### 1. Separate `:wear` application module with three sport flavors — selected

Add a standalone Wear OS application module with the same sport flavors and package IDs as the three phone apps. Use Compose for Wear OS for watch screens, Room for watch-local sessions and preferences, and a small shared Kotlin library for sport rules and metric calculations. Add a versioned watch-to-phone sync adapter that sends saved sessions to the corresponding phone flavor through the Wear OS Data Layer.

This keeps wearable UI and persistence separate from the existing phone app while preserving the sport behavior and current Play listings. It also keeps offline watch use independent from the phone. It introduces a new module and a sync DTO that both sides must maintain.

### 2. Add a `device` build dimension to `:lapsCounter`

Build phone and watch variants from one application module with sport and device dimensions, separating layouts and dependencies by source set.

This keeps one module but combines the existing AppCompat/XML application with a Compose for Wear UI, watch-specific manifest configuration, and local database. It multiplies the variant matrix and increases the chance that phone-only dependencies or resources leak into watch artifacts.

### Selected direction

Use approach 1, selected by the user. It fits the existing phone architecture and allows the Wear experience to use the toolkit and storage suited to watches. The watch remains independently usable while the phone serves as the destination for completed-session history.

## Product Behavior

### Timer and session lifecycle

- The main screen presents elapsed time, current lap, the sport-specific metric where useful, and the primary available action. State transitions mirror the phone timer: start, pause, resume, record lap, stop, save, and discard/reset.
- Persist the active timer state and laps locally at each state transition and lap so the session can be restored after screen changes or process recreation. Elapsed time must be derived from monotonic timing data, not by keeping the screen awake or updating a counter in memory.
- Saving a session must not require a phone connection. To avoid requiring text input on the small watch screen, use a localized generated name from the sport and start timestamp; the phone history receives that name unchanged. Keep optional descriptions out of watch capture in this version.
- Store time and distance in the same units and meanings as the phone data model: elapsed values in milliseconds and distances in metres. Keep the final-lap behavior consistent with the existing `LapDistancePolicy`.
- Provide a minimal watch settings surface for lap distance and whether the final lap counts. Default lap distance comes from the active sport profile; final-lap inclusion defaults to `true`, matching the existing phone preference. Do not bring the phone's ads, sharing flow, or full settings screen to the watch.

### Watch-to-phone synchronization

- The watch writes the finished activity to Room first and marks it pending sync. The sync payload is a versioned, sport-tagged DTO with the same UUID used by the local watch record.
- Use the Wear OS Data Layer for phone-watch delivery. The Data Layer can buffer changes while devices are offline; the watch's Room record remains the source of truth until the phone has accepted the activity.
- The phone listener validates payload version and expected sport flavor, maps the DTO to the existing `ActivityEntity`, and upserts by UUID into Realm. Repeated delivery must be safe.
- After Realm commits the activity, the phone sends an acknowledgement containing only the activity UUID and accepted payload version. This control acknowledgement updates the watch's sync status; it does not copy phone activities or settings to the watch.
- Show a compact sync status on the watch history/session detail (pending, synced, or unavailable) without blocking timer or save interactions. Do not delete the watch's local copy after syncing.
- If no matching Android phone app is available, leave the activity local and pending. The core timer and history remain fully usable.

## Product and Platform Constraints

- Keep existing phone package IDs and flavor identities: `br.com.helpdev.lapscounter.swimming`, `br.com.helpdev.lapscounter.running`, and `br.com.helpdev.lapscounter.cycling`.
- Use those same IDs for corresponding Wear variants so they can be attached to the existing Play listings. Verify each app's Wear OS form-factor configuration in Play Console before release.
- Keep phone and Wear version codes unique and increasing for each Play listing. Establish one explicit version-code scheme before producing release bundles; do not publish an artifact from this plan.
- Target SDK 36, which meets the current Google Play Wear OS target requirement. The watch manifest declares the watch hardware feature and standalone metadata.
- Use local debug signing for validation. For any later release upload, verify the app's registered Upload key certificate and signing configuration for each flavor as required by `AGENTS.md`.
- Keep the existing swimming, running, and cycling production AdMob configuration phone-only; the Wear module contains no ads.

## Validation Expectations

- Unit tests cover timer transitions, pause/resume accounting, lap capture, final-lap inclusion, each flavor's default profile and metric, generated session identity, and watch-to-phone DTO conversion.
- Sync tests cover offline pending state, replay after reconnect, duplicate delivery by UUID, invalid/mismatched flavor data, and incompatible payload versions.
- Build all three Wear debug flavors and all three phone debug flavors. Inspect merged manifests/package IDs and ensure the Wear hardware feature and standalone metadata are present only in watch artifacts.
- Run on small-round and larger Wear OS emulators. Exercise start/lap/pause/resume/stop/save, round-screen clipping, app reopening, process recreation, and offline use. Pair with Android phone debug apps and confirm synced sessions appear in the correct flavor history exactly once.
- For the first release, emulator QA is the device-validation target; physical Wear OS hardware is unavailable and is not a release blocker. Meet current Wear OS app quality requirements, prepare watch screenshots, and complete closed testing in each app's Wear OS Play track. Keep the paired phone-to-watch live sync check as a separate release gate.

## External References

- [Compose for Wear OS](https://developer.android.com/training/wearables/compose)
- [Wear OS Data Layer sync](https://developer.android.com/training/wearables/data/sync)
- [Wear OS Data Layer overview](https://developer.android.com/training/wearables/data/overview)
- [Package and distribute Wear OS apps](https://developer.android.com/training/wearables/packaging)
- [Wear OS app quality](https://developer.android.com/docs/quality-guidelines/wear-app-quality)
- [Play Console form-factor release tracks](https://support.google.com/googleplay/android-developer/answer/13295490)
- [Android Gradle Plugin 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
