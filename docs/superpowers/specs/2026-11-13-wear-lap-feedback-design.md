# Wear lap feedback design

## Goal

When the user records a lap from the Wear timer, briefly show that lap's split duration, then return the main display to the running stopwatch. The timer continues running throughout. The timer screen also lists recorded laps newest first.

## Interaction

- After a successful lap save, crossfade the main time display to the lap number and its split duration for about one second.
- Crossfade back to the live elapsed time after that interval.
- If another lap is recorded during the feedback interval, show the newest lap and restart the interval.
- Failed lap persistence must not show a successful-lap animation.
- List the saved laps beneath the timer controls and other session information, ordered by descending sequence (for example, Lap 3, Lap 2, Lap 1).
- Use each lap's `durationMillis`, not total elapsed time.
- Keep all visible lap labels localized for English, Portuguese, and Spanish.

## Implementation boundary

Keep timing and persistence in `WearViewModel`; keep rendering and the crossfade in `TimerScreen`. Reuse the existing scrollable `WearList` and `elapsedLabel` formatting. No timer behavior, sync direction, or persistence schema changes are required.

## Validation

Build and launch the Swimming debug Wear app in the existing Wear emulator. Manual check: start, record multiple laps, observe the brief split display and return to the advancing total, then scroll to confirm newest-first ordering.
