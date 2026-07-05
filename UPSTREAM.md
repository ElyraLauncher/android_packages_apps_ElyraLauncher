# Upstream

## Selected Source

Launcher3-derived standalone base from Lawnchair Launcher.

- Repository: https://github.com/LawnchairLauncher/lawnchair
- Branch: `15-dev`
- Commit: `505dbc40e6154c05158b5d0271c45f6a885a411b`
- Import method: shallow source import into this repository, with nested Git
  metadata removed and the required SystemUI library submodule imported as
  normal source.

## SystemUI Library Source

- Repository: https://github.com/LawnchairLauncher/platform_frameworks_libs_systemui
- Commit: `6a11ef767998885838a599331b5485f768b3d725`
- Import method: submodule content copied as normal source under
  `platform_frameworks_libs_systemui/`.

## License

Apache License 2.0.

The imported source includes Android Open Source Project Launcher3/Quickstep
code and Lawnchair modifications under Apache-2.0. The root project LICENSE is
Apache-2.0 and NOTICE records attribution.

## Import Date

2026-07-05

## Package and Application ID Decisions

- Universal target application ID: `com.elyra.launcher`.
- App label: `Elyra Launcher`.
- Source package names are preserved where needed to avoid destabilizing the
  Launcher3-derived architecture during import.
- ROM target package decisions remain pending platform-tree validation.

## Known Deviations

- Build output names are changed to ElyraLauncher artifacts.
- Universal builds use the imported Gradle standalone configuration.
- Universal APK must not claim privileged Recents behavior.
- ROM Quickstep validation is PENDING until a real platform build is run.
- Optional imported app-category assets containing device-specific package names
  were not retained as part of Elyra source hygiene.
