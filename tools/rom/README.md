# ROM Integration

This directory documents the platform integration path for ElyraLauncher.

Universal APK builds are normal installable Home launcher builds. They must not
claim privileged Recents behavior. Quickstep and Recents validation belongs to a
real Android platform tree with privileged app placement and platform signing.

## Expected Platform Work

- Place ElyraLauncher under the ROM source tree as a Launcher3-derived package.
- Include the launcher package from product configuration.
- Install the ROM target as a privileged app only when required by platform
  Quickstep integration.
- Grant privileged permissions through product-owned permission XML only after
  review.
- Validate Recents through a real platform build and device or emulator boot.

Platform build status is PENDING until those steps run in a ROM tree.
