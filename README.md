<p align="center"><img src="docs/icon-preview.png" width="128" alt="Pushup Log: muscular push-up silhouette on warm gold"></p>

# Pushup Log

**Your reps. Your progress. No interruptions.**

A small, native Android push-up tracker by Thuc Nguyen. Free, open source, offline, and ad-free. No subscriptions, accounts, analytics, or Internet permission.

I built Pushup Log because logging a few push-ups should never mean sitting through an ad or paying for a subscription. This is a personal project being improved in the open, with help from AI coding tools.

## What it does

- Log today's total with quick −10 / +10 controls.
- See lifetime totals, your best day, and your daily average.
- Browse monthly history as a calendar or bar chart.
- Correct a missed or mistaken total on any past date from monthly history.
- Use the 10 PM local-time reminder, or choose your own time. Pushup Log only nudges you when you've logged fewer than 10 reps and skips stale overnight alerts.
- Import compatible `.puud` backups from the old Push Ups app.
- Use a high-resolution launcher icon based on the original Pushup Log silhouette, including Android 13+ themed icons.

Android 8.0+ · Java · Android framework UI · SQLite · No third-party runtime dependencies

## Project status

Early personal app, preparing for a polished public release. Not yet published on Google Play. Version 1.3.10 gives calendar and chart history an equal-height visualization region, spaces calendar days across six stable rows, and includes subtle author attribution on the balanced home dashboard.

Fresh installs start empty. Existing on-device records are preserved when updating with the same application ID and signing certificate. Personal workout archives and signing keys are not part of this repository.

**Data care:** records stay on your phone. Export from Pushup Log is not implemented yet, and Android backup is disabled. Uninstalling or clearing app storage deletes your records. Import replaces the backup's entire date range, including zero or missing days, while preserving dates outside that range. Review the confirmation before importing.

## Build and check

Install a Java 17 JDK and the Android SDK with Platform 36 and Build Tools 36.0.0, or open this folder in a compatible Android Studio version.

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
./scripts/test-logic.sh
./gradlew assembleDebug lintDebug
./scripts/verify-apk.sh
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. Debug signing is for development. A debug APK cannot update a differently signed personal installation; do not uninstall your existing app just to try it without a backup plan.

`./scripts/build-apk.sh` is a convenience wrapper around the Gradle debug build. The standalone logic tests exercise chart scaling and reminder scheduling; they use Java `main` methods and must be run explicitly rather than relying on Gradle test discovery.

## Contributing

Small, focused improvements are welcome. Keep the app fast, offline, free, and ad-free. Include relevant checks and screenshots for UI changes. See [the roadmap and code review](docs/ROADMAP.md) and [contributor guidance](AGENTS.md).

## Privacy

See [PRIVACY.md](PRIVACY.md). No data is transmitted by the app.

## License

[MIT](LICENSE), copyright 2026 Thuc Nguyen.
