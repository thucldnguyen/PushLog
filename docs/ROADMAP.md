# Pushup Log: next improvements

These are proposals based on source review, not claims of measured performance problems. The existing framework UI, background database executors, and lack of runtime dependencies are a good foundation.

## 1. Protect records and make core actions complete

- **Backup and restore first.** Add a versioned export format through Android's document picker, with a round-trip test and clear restore preview. Current imports only read the old app's format; Android backup is disabled.
- **Flexible logging.** Keep +10 as the fast path and add +1 and direct numeric entry for today. Past-date correction now replaces the selected daily total explicitly.
- **Date boundaries.** Capture the edited date when opening the log dialog; `saveTodayTotal` currently resolves today's date again on save. Test a dialog left open over midnight.
- **Database errors.** Check `insertWithOnConflict` results so import transactions cannot report success after a failed insertion. Add rollback tests with synthetic data.
- **Lifecycle coverage.** Test rotation and leaving the screen during load/save/import. Some asynchronous callbacks are guarded, but dialog callbacks need a consistent lifecycle approach.
- **Import hardening.** Bound total archive work as well as the selected database size; the current importer can scan arbitrary preceding ZIP entries.

## 2. Make the UI feel finished

- Retain the charcoal-and-gold identity and use consistent spacing, typography, and button hierarchy.
- Add an intentional empty state, clearer import feedback, and a subtle save confirmation.
- Extract hardcoded UI strings into resources; verify TalkBack, large font sizes, landscape, contrast, and 48 dp touch targets.
- Add light/system appearance only after the dark UI is consistent.
- Preview the new icon on circle, squircle, rounded-square, and themed launchers. Artwork follows [Android adaptive-icon guidance](https://developer.android.com/codelabs/basic-android-kotlin-compose-training-change-app-icon).

## 3. Keep performance measurable

- Measure cold start and history navigation on a modest phone before introducing architectural changes.
- Cache chart labels and daily values outside `MonthlyBarChartView.onDraw`; it currently creates number formatters, date objects, and label strings during each draw. This is a small optimization candidate, not a demonstrated bottleneck.
- Size the large hero image to its actual display needs and compare WebP quality and APK savings before replacing it.
- Retain background database work; consider a small repository layer to centralize errors and lifecycle handling as features grow.
- Avoid caching all-time statistics until profiling justifies the invalidation complexity. Existing date-keyed month queries are appropriate for this data size.
- Evaluate release shrinking with release-build smoke checks and measured APK/startup comparisons.

## Before Google Play

Implement backup/export, validate upgrade preservation with the intended signing strategy, run device tests, add screenshots and release notes, and review current Play Console requirements at release time. Keep signing credentials private. The original local signing key is preserved locally and must never be committed.

## Changes in this initial public preparation

- Added past-date correction from both calendar and bar-chart history views, with direct numeric entry and future-date protection.
- Refined the original launcher silhouette at high resolution, focusing the crop on the muscular upper body and moving the head safely inward while preserving its black-on-gold identity; added Android 13+ monochrome support.
- Removed automatic personal-history seeding; existing database contents are untouched.
- Preserved the original archive, key, and personal backup locally; excluded them from Git and public APK contents.
- Standardized the documented build on the Gradle wrapper and added an explicit runner for the existing Java logic tests.
- Added project documentation, privacy information, and contributor guidance; retained the original MIT license.
- Fixed an existing API 27-only theme attribute by selecting it only on Android 8.1+, retaining Android 8.0 support.
- Added GitHub Actions build, lint, logic, and APK verification checks.
