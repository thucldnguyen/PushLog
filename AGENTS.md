# Working on PushLog

PushLog is a free, offline, ad-free native Android tracker. Preserve that product promise.

- Keep `com.thuc.pushlog` and the SQLite database compatible with existing installations. Never use destructive migrations.
- Keep signing keys, workout backups, credentials, and generated APKs out of Git. `.private/` and `keystore/` are local only.
- Prefer small framework-based changes; justify additional runtime dependencies.
- Keep disk work off the UI thread. Handle activity destruction and stale asynchronous results.
- Make UI changes accessible: readable contrast, scalable text, descriptive labels, and generous touch targets.
- Keep launcher artwork vector-based and inside Android adaptive icon safe bounds. Update `docs/icon.svg` when changing the launcher mark.
- Run `./scripts/test-logic.sh`, `./gradlew assembleDebug lintDebug`, and `./scripts/verify-apk.sh` as appropriate. Report unavailable checks honestly.
- Before release, test on a device: logging, history, imports, notification denial, reminders, rotation, midnight, large text, and launcher masks.
- Commit and publish only within the user's authorized scope.
