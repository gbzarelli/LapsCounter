# LapsCounter Multi-Sport Flavors Design

## Goal

Operate three independently published Android applications from one codebase:

| Flavor | Release package | Product identity |
|---|---|---|
| `swimming` | `br.com.helpdev.lapscounter.swimming` | Existing Swimming Stopwatch app |
| `running` | `br.com.helpdev.lapscounter.running` | Running Stopwatch app |
| `cycling` | `br.com.helpdev.lapscounter.cycling` | Cycling Stopwatch app |

The apps share manual lap timing, pause handling, settings, saved activities,
sharing, headset control, ads infrastructure, and visual layout. Each flavor
owns its app name, launcher assets, sport image, wording, default split
distance, metric presentation, AdMob production IDs, and Play release.

## Confirmed Product Decisions

- Natação keeps its existing package, local data, published identity, and
  production AdMob configuration.
- Corrida and ciclismo are separate Play Store applications with separate
  package names and local data stores.
- The first release of running and cycling uses the existing manual-lap model.
  GPS, route recording, automatic split detection, sensors, and importing
  external activity data are out of scope.
- Distances remain persisted in metres. The chosen display unit is a
  presentation concern, not a database concern.
- A sport profile supplies the defaults and metric rule. Shared code calculates
  and formats the result.
- Internal UI icons can use Material Design Icons (MDI) vectors under Apache
  2.0. The launcher icons are original artwork and must not be derived from
  MDI or third-party marks.

## Sport Rules

| Concern | Swimming | Running | Cycling |
|---|---|---|---|
| Default manual split | 50 m | 1 km | 5 km |
| Main distance display | metres | kilometres | kilometres |
| Main metric | Pace | Pace | Average speed |
| Metric unit | min/100 m | min/km | km/h |
| Incomplete final split | Existing preference and behavior | Same shared behavior | Same shared behavior |

The cycling default represents a manual segment rather than a GPS-derived
distance. A user can set any split distance in settings; the default only
pre-populates a useful value for a rider timing longer segments.

## Architecture

### Gradle variants and release identity

Keep the `sport` flavor dimension and declare all three product flavors in
`lapsCounter/build.gradle`. Each flavor receives its own `applicationIdSuffix`:
`.swimming`, `.running`, and `.cycling`. The existing debug suffix continues to
produce installable debug packages such as
`br.com.helpdev.lapscounter.running.debug`.

The current `swimming` release package must stay unchanged. Running and cycling
begin as new Play applications, so their first published version codes are
independent of the swimming version-code history. Build version values may be
shared when convenient, but release checklists must validate each app against
its own Play Console track.

Release signing validation currently recognizes only `swimmingRelease`. It must
be generalized to reject any sport release without the required signing
configuration. A single upload key may sign all three new applications if that
is the desired Play setup; the design does not require it.

### Flavor boundary

`src/main` stays the only location for reusable behavior and layout. Each
flavor contains a minimal implementation of two types with the same fully
qualified names:

```text
br.com.helpdev.lapscounter.ChronometerActivityImpl
br.com.helpdev.lapscounter.sport.SportProfileProvider
```

`ChronometerActivityImpl` remains the launcher activity declared by the common
manifest and extends the shared `ChronometerActivity`. Keeping that class name
preserves the existing swimming manifest contract and avoids a launcher-activity
migration for installed users. The obsolete `running` and `cycling`
`MainActivity : AbsMainActivity` files are replaced because `AbsMainActivity`
does not exist in the current codebase.

`SportProfileProvider` supplies an immutable `SportProfile` for the active
variant. This lets common Kotlin code depend on a stable API rather than on
flavor names, `BuildConfig` conditionals, or string values that encode behavior.

```text
SportProfile
  id: SWIMMING | RUNNING | CYCLING
  defaultLapDistanceMeters: Float
  distanceDisplayUnit: METERS | KILOMETRES
  metricDefinition: Pace(referenceMeters) | AverageSpeed
```

The profile describes rules. It does not contain Android views, activity
references, persistence, or mutable user settings.

### Metric and distance presentation

Replace the 100-metre-specific `LapsCounterUtils.getPace` API with common,
domain-level metric calculation and formatting components:

```text
Lap/Activity time + counted distance (metres)
        |
        v
SportMetricCalculator + SportProfile
        |
        +-- Pace(minutes, seconds, reference distance)
        \-- AverageSpeed(kilometres per hour)
        |
        v
SportMetricFormatter + flavor strings
        |
        v
toolbar, activity details, list, sharing
```

The calculator returns an explicit no-value result when the counted distance or
elapsed running time is zero. UI and sharing format that result as a localized
placeholder instead of producing an invalid divide-by-zero value.

The distance formatter receives metres and the active profile. It renders
swimming distances in metres and running/cycling distances in kilometres,
using localized decimal formatting. Labels and sentence structure remain
resource-driven. For example, the same shared layout can show `Volta 50 m`,
`Split 1 km`, or `Volta 5 km` without calculating units in XML resources.

The following consumers must use these shared formatters rather than build
unit strings directly:

- the live header in `ChronometerActivity`;
- saved-activity headers in `ActivityViewModel`;
- both branches of `ChronometerShareUtils`;
- per-lap sharing text;
- settings summaries and initial values.

The existing rule that may omit the final partial lap stays in the common
distance/time calculation. No sport gets a special implementation of this
rule, so the behavior remains consistent in the timer, activity history, and
shared report.

### Preferences and persistence

The shared preference key for split distance remains stable because each app
has a different package and therefore its own default shared-preferences file.
`FloatPickerPreference` and `PreferenceUtils` obtain the fallback from the
active `SportProfile`; the XML preference no longer owns a competing numerical
default.

Realm entities remain sport-neutral. `ActivityEntity.lapDistance` and
`travelledDistance` retain metres, and no `sport` field is necessary: each
installed app can only create its own sport's activities. This avoids a Realm
schema migration and preserves existing swimming records. Historical swimming
records are re-rendered through the swimming profile and therefore retain their
current meaning.

### Resources, wording, and visual identity

Keep layout XML in `src/main/res`. Move or override sport-facing values into
each flavor, with matching Portuguese resources in `values-pt`:

- app title and activity titles;
- button and lap terminology where a sport needs different language;
- split-distance setting title, summary, and input hint;
- live and saved distance labels;
- metric labels and share-report phrases;
- sport illustration vectors and launcher resources.

Swimming keeps its present product name and app icon. Running and cycling get
their own names and icon sets. The three launcher icons form one original visual
family: a rounded-square field, white sport pictogram, and small stopwatch at
the upper right. Swimming retains its teal artwork; running uses a warm orange
field with a runner; cycling uses a cool blue or violet field with a rider and
bicycle. Each flavor supplies adaptive-icon XML and all required launcher
density assets, preventing a running or cycling release from inheriting the
swimming launcher image.

Use MDI for ordinary in-app pictograms when it improves clarity, converting the
needed SVG path to Android vector XML. Include the upstream Apache 2.0 notice
and the icon name/version in a third-party-notices document. Do not use the MDI
brand/logos category.

### Ads and privacy

`AdsManager` and consent flow remain shared. `src/debug/res/values/admob.xml`
continues to override every debug variant with Google's test identifiers.
Production AdMob application, banner, and interstitial IDs belong in each
flavor's release resource configuration and must be created for the matching
package before distribution.

The release Gradle checks must fail for running or cycling if their production
ad configuration is absent, malformed, or still points to Google test
inventory. They must not block debug builds. Each new Play listing needs its
own privacy-policy/declaration review and consent behavior validation before
its release track receives an AAB.

## Incremental Implementation Plan

### Phase 1: Establish the flavor contract without changing swimming behavior

1. Declare `running` and `cycling` flavors with their release package suffixes.
2. Generalize signing/release task checks to recognize every sport flavor.
3. Add the common sport-profile API and the three source-set providers.
4. Replace the stale running/cycling entry activities with
   `ChronometerActivityImpl` wrappers compatible with the shared manifest.
5. Add flavor resources sufficient for every debug variant to compile.
6. Build and run the existing swimming tests before and after this phase to
   demonstrate that the published variant has not regressed.

### Phase 2: Generalize metric and distance behavior

1. Introduce metric calculation types for pace and average speed.
2. Introduce localized distance formatting by profile.
3. Migrate `ChronometerActivity`, `ActivityViewModel`, and
   `ChronometerShareUtils` to the new APIs.
4. Remove or narrow old methods whose names imply pace but hard-code 100 m.
5. Keep the counted/uncounted final-lap rule in one tested calculation path.
6. Add JVM tests for zero values, partial-lap handling, and every sport's
   standard metric example.

### Phase 3: Complete running identity

1. Add English and Portuguese running resource overlays.
2. Add runner sport vectors and original adaptive launcher assets.
3. Supply running production ad IDs through the release-only configuration.
4. Exercise start, lap, pause, stop, save, history, settings, and share flows
   in `runningDebug`.
5. Register the running package in Play Console and validate its signed AAB
   through internal testing before a public release.

### Phase 4: Complete cycling identity

1. Add English and Portuguese cycling resource overlays.
2. Add cycling sport vectors and original adaptive launcher assets.
3. Supply cycling production ad IDs through the release-only configuration.
4. Exercise the same functional flows in `cyclingDebug`, with special attention
   to km/h formatting in live, saved, and shared output.
5. Register the cycling package in Play Console and validate its signed AAB
   through internal testing before a public release.

### Phase 5: Release safety and documentation

1. Update the README to describe the three apps and their package IDs.
2. Expand release-signing documentation with flavor-neutral commands and
   app-specific verification points.
3. Document MDI attribution and launcher-asset ownership.
4. Build all debug variants and each configured release AAB.
5. Verify package ID, icon, app name, metric unit, test-vs-production ad IDs,
   consent flow, and signed certificate for every distributable artifact.

## Test Matrix

| Area | Swimming | Running | Cycling |
|---|---|---|---|
| Assemble debug | `assembleSwimmingDebug` | `assembleRunningDebug` | `assembleCyclingDebug` |
| JVM tests | `testSwimmingDebugUnitTest` | `testRunningDebugUnitTest` | `testCyclingDebugUnitTest` |
| Default split | 50 m | 1 km | 5 km |
| Metric sample | 2:00 / 100 m | 5:00 / km | 30.0 km/h |
| UI/share smoke test | Required | Required | Required |
| Debug ads | Google test IDs | Google test IDs | Google test IDs |
| Release ads | Existing production IDs | Own production IDs | Own production IDs |

The metric samples use values chosen to make unit mistakes obvious: a 2-minute
100-metre swimming interval must not become 2:00/km; a 5-kilometre run in 25
minutes must show 5:00/km; a 20-kilometre cycling activity in 40 minutes must
show 30.0 km/h.

## Acceptance Criteria

1. `swimmingDebug`, `runningDebug`, and `cyclingDebug` each compile and launch
   the same common timer flow.
2. Swimming retains package `br.com.helpdev.lapscounter.swimming`, existing
   local records, existing app name/icon, 50-metre default, and min/100-metre
   pace presentation.
3. Running has package `br.com.helpdev.lapscounter.running`, running identity,
   a 1-kilometre default, and min/km in every metric surface.
4. Cycling has package `br.com.helpdev.lapscounter.cycling`, cycling identity,
   a 5-kilometre default, and km/h in every metric surface.
5. Shared settings, activities, headset control, final-lap handling, and
   sharing remain one implementation path with sport-aware presentation.
6. Debug builds cannot request production ad inventory. A release build cannot
   be produced for a new flavor until valid flavor-specific ad and signing
   configuration exists.
7. Metric/distance tests cover all three profiles and protect the swimming
   behavior from regression.
