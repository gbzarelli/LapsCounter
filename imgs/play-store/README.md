# Google Play listing assets

## App icons

The selected A visual direction is implemented here:

- `running/app-icon.svg` and `running/app-icon.png`: warm orange gradient with the white stopwatch mark.
- `cycling/app-icon.svg` and `cycling/app-icon.png`: blue gradient with the white bicycle mark.
- Swimming's launcher and Play listing app icon remain unchanged.

PNG exports are 512 × 512 RGBA. Android adaptive launcher resources use the corresponding VectorDrawable art and sport-gradient background under `lapsCounter/src/running/res/` and `lapsCounter/src/cycling/res/`.

## Play Store feature graphics

`running/feature-graphic`, `cycling/feature-graphic`, and `swimming/feature-graphic` are the same banner system shown in direction A: a sport-gradient field, “LAPS COUNTER” wordmark, matching “<SPORT> STOPWATCH” line, large white chronometer, and a motion stroke (two water strokes for swimming). Editable SVG masters are beside each PNG.

Each PNG is 1024 × 500 pixels in RGB mode without an alpha channel. `../baner.png` remains the legacy Swimming banner and is identical to `swimming/feature-graphic.png`.

## Localized phone screenshots

Running and Cycling listing screenshots are grouped by flavor and Play Store locale:

- `running/en-US/` and `cycling/en-US/`: English (United States)
- `running/es-419/` and `cycling/es-419/`: Spanish (Latin America)

Each locale has five PNGs in listing order: ready, running, splits, paused, and history. All images are 1080 × 1920 pixels (9:16). The Portuguese (Brazil) listing screenshots are not included in this set.
