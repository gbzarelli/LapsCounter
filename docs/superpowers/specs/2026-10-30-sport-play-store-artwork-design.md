# Sport App Icons and Play Store Artwork Design

## Intent

Refresh the Running and Cycling launcher/store icons and make one coherent family of Play Store feature graphics for Running, Cycling, and Swimming.

## Selected direction

Follow the user-selected direction A from the visual mockup:

- Running icon: stopwatch outline in white on a warm orange gradient.
- Cycling icon: bicycle outline in white on a blue gradient.
- Each feature graphic: two-line “LAPS COUNTER” heading, sport-specific “<SPORT> STOPWATCH” line, a large white stopwatch on the right, and a white motion stroke. Swimming adds two wave strokes.
- Use the palette from the HTML: Running `#F47B20` → `#FFAE51`, Cycling `#3447A4` → `#6377E4`, Swimming `#007C9D` → `#21B3CA`.

## Goals

- Reproduce direction A's layout, symbols, text, gradients, and motion details faithfully at full Play Store dimensions.
- Keep editable SVG masters and exact-size PNG exports beside one another.
- Update the legacy `imgs/baner.png` with the Swimming feature graphic.
- Preserve the Swimming launcher and Play listing app icon and all existing screenshots.
- Publish all three apps with version code 12 / version 2.2.4.

## Non-goals

- No changes to app behavior, screens, privacy disclosures, ad configuration, or screenshots.
- No redesign of the Swimming app icon.
- No other custom campaign or visual direction.

## Constraints

- Preserve package IDs `br.com.helpdev.lapscounter.swimming`, `br.com.helpdev.lapscounter.running`, and `br.com.helpdev.lapscounter.cycling`.
- App icon exports are 512 × 512 PNG in RGBA mode, at most 1 MB.
- Feature graphics are 1024 × 500 PNG in RGB mode without alpha, at most 15 MB.
- Current production releases are code 11 / version 2.2.3; target release is code 12 / version 2.2.4.
- The canonical upload key, alias, and fingerprint are recorded in the project `AGENTS.md` and were checked against all three Play Console upload certificates.
- The user requested a visual review before Play Store publication. Do not make Play Console listing or production changes until the user has reviewed the final assets.

## Implementation decisions

- Running and Cycling app icons mirror the separate stopwatch and bicycle marks in the selected HTML mini-icons.
- Feature graphics reproduce the HTML reference's English copy, sport gradient, stopwatch position and shape, and motion strokes.
- Keep all screenshots, store descriptions, locale settings, and countries unchanged.
- Build signed bundles with the existing production upload key after verifying the alias and registered upload certificate for each app.
