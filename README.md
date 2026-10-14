# LapsCounter
One Android codebase for separately published stopwatch apps for swimming, running, and cycling. Each app uses the same manual timing, saved activities, settings, and sharing flow, with sport-specific units, metrics, names, and icons.

## Product variants

| Sport | App name | Application ID | Store status |
|---|---|---|---|
| Swimming | Swimming Stopwatch | `br.com.helpdev.lapscounter.swimming` | [Available on Google Play](https://play.google.com/store/apps/details?id=br.com.helpdev.lapscounter.swimming) |
| Running | Running Stopwatch | `br.com.helpdev.lapscounter.running` | Not published yet |
| Cycling | Cycling Stopwatch | `br.com.helpdev.lapscounter.cycling` | Not published yet |

Running and Cycling currently use manually recorded splits; GPS and automatic split tracking are not included.

## Build and test

```bash
sh gradlew :lapsCounter:testSwimmingDebugUnitTest :lapsCounter:testRunningDebugUnitTest :lapsCounter:testCyclingDebugUnitTest
sh gradlew :lapsCounter:assembleSwimmingDebug :lapsCounter:assembleRunningDebug :lapsCounter:assembleCyclingDebug
```

See [third-party notices](THIRD_PARTY_NOTICES.md) for in-app icon attribution.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[Apache-2.0](https://choosealicense.com/licenses/apache-2.0/)
