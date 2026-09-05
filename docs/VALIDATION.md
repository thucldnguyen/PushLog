# Initial preparation checks

Checked on September 5, 2026 with Java 17, Android Platform 36, Build Tools 36.0.0, and the included Gradle wrapper.

- `./scripts/test-logic.sh`: passed both existing standalone test programs. Chart scale invariants cover maxima from 0 through 999,999; reminder tests include threshold and scheduling checks.
- `./gradlew assembleDebug lintDebug`: passed after fixing the API 27-only navigation-bar style. Lint still reports 16 non-fatal warnings; localization, SDK update suggestions, and tooling findings remain review items.
- `./scripts/verify-apk.sh`: passed signature, alignment, package inspection, no Internet permission, and no private seed/key checks.
- Debug APK: approximately 1.4 MB. This is not a startup or frame-time measurement.
- Android resource XML parses; launcher and documentation SVG paths match. The documentation icon was rendered and visually inspected.
- Staged source excludes the original personal history, signing key/password, and generated build artifacts.

No emulator or physical-device testing was performed. Verify launcher masks, themed icons, data-preserving upgrades, and the main workflows on a device before distributing a release. GitHub Actions is configured but has not run until the repository is published.
