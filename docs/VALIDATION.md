# Version 1.3.7 validation

Checked on September 12, 2026 with Java 17, Android Platform 36, Build Tools 36.0.0, and the included Gradle wrapper.

- `./scripts/test-logic.sh`: passed all three standalone test programs. Daily-count input tests cover zero, 40, whitespace, the maximum, empty and nonnumeric values, negatives, and overflow. Chart and reminder invariants also pass.
- `./gradlew assembleDebug lintDebug` and `./gradlew assembleRelease lintRelease`: passed. Lint reports no errors; localization, SDK update suggestions, and tooling findings remain review items.
- `./scripts/verify-apk.sh`: passed signature, alignment, package inspection, no Internet permission, and no private seed/key checks. The package remains `com.thuc.pushlog`; version code is 17 and the signing certificate matches earlier builds.
- The original dashboard athlete is retained with transparent whitespace cropped and a compact 112 dp presentation. This is not a startup or frame-time measurement.
- Source review against the Android Native UI checklist confirms fixed app bars, a focused initial home viewport, 48 dp app-bar and segmented controls, native overflow/date/time surfaces, platform Back/Up behavior, and scroll fallback for large text or short/landscape windows.
- Android resource XML parses. The approved high-resolution upper-body launcher foreground was inspected at full size and 48 px under rounded-square and circular masks; the head and hair remain clear of the mask edges.
- Staged source excludes the original personal history, signing key/password, and generated build artifacts.

No emulator or physical-device testing was performed. Verify launcher masks, themed icons, data-preserving upgrades, and the main workflows on a device before distributing a release. GitHub Actions is configured but has not run until the repository is published.
