# Sport App Icons and Play Store Artwork Implementation Plan

**Goal:** Reproduce the user-selected direction A visual mockup, prepare signed version 2.2.4 / code 12 bundles, merge the change, then publish the three sport apps only after the user reviews the final artwork preview.

**Architecture:** Keep editable SVG and PNG store artwork under `imgs/play-store/<sport>/`. Keep adaptive and legacy launcher vectors in the corresponding Running and Cycling flavor resources. Use the existing signed Production tracks and Play listings.

**Spec:** `docs/superpowers/specs/2026-10-30-sport-play-store-artwork-design.md`.

## Global constraints

- Preserve package IDs `br.com.helpdev.lapscounter.swimming`, `br.com.helpdev.lapscounter.running`, and `br.com.helpdev.lapscounter.cycling`.
- App icon exports: 512 × 512 RGBA PNG, at most 1 MB.
- Feature graphic exports: 1024 × 500 RGB PNG without alpha, at most 15 MB.
- Match the HTML direction A wording, composition, and gradients: Running `#F47B20` → `#FFAE51`, Cycling `#3447A4` → `#6377E4`, Swimming `#007C9D` → `#21B3CA`.
- Release version: 2.2.4, version code 12; production is currently code 11 / 2.2.3.
- Keep `keystore.properties`, signing passwords, and keystores ignored and out of commits, chat, and command output.
- Leave app behavior, store descriptions, screenshots, privacy disclosures, ad configuration, countries, and rollout settings unchanged.
- Do not change anything in Play Console until the user has reviewed the final preview.

## Review focus

- Running icon is the white stopwatch mark; Cycling icon is the white bicycle mark, matching HTML direction A.
- Three feature graphics follow the HTML layout at full resolution, including English labels and movement strokes.
- Adaptive and legacy launcher vectors match their store icon marks and compile for each flavor.
- Verify PNG dimensions, modes, alpha, and size; inspect full size and 48 px.
- Verify each bundle's package ID, version code 12, and signing certificate.
- Preview all three listing graphics and two changed icons to the user before any Play Console upload or release submission.

---

### Task 1: Create source artwork and Play Store exports

- [x] Draw editable SVGs following direction A: Running stopwatch icon, Cycling bicycle icon, and sport-specific feature graphics.
- [x] Export 512 × 512 RGBA icons and 1024 × 500 RGB banners.
- [x] Replace `imgs/baner.png` with the Swimming export and document assets.
- [x] Inspect all exported art at full size and 48 px; verify metadata and size.

### Task 2: Update launcher resources and release version

- [x] Mirror the Running stopwatch and Cycling bicycle marks in adaptive and legacy launcher resources.
- [x] Use the same sport-specific gradient family in adaptive icon backgrounds; update legacy fallback artwork.
- [x] Set version 2.2.4 / code 12.
- [x] Copy the existing ignored `keystore.properties` into this worktree without printing or committing its contents.
- [x] Verify key alias/fingerprint locally and against each Play app's Upload key certificate.
- [x] Run the Swimming, Running, and Cycling flavor unit-test tasks.
- [x] Build all three signed release bundles and inspect package IDs, version code, and signing certificates.

### Task 3: Open, review, and merge the pull request

- [ ] Commit the artwork, resources, version bump, and docs with a Conventional Commit subject.
- [ ] Push `gz/play-store-assets` and open a PR to `master`, including existing base commit `d67ae45`.
- [ ] Review the complete diff and required CI checks; resolve build/resource issues.
- [ ] Merge as authorized and verify the merged revision on `origin/master`.

### Task 4: Show and publish the three Production releases

- [ ] Show the finished feature graphics and both updated icons to the user. Wait for review before any Play Console write or release submission.
- [ ] After user approval, rebuild signed bundles from the merged revision and reverify package IDs, version/code, and upload key.
- [ ] Upload each default listing's feature graphic, plus Running and Cycling app icons; leave listing text, screenshots, and locales untouched.
- [ ] Create three Production releases with localized release notes, preserve existing countries, and request 100% rollout.
- [ ] Review each change summary before submitting and verify Play Console's resulting state for every app.
