# Contributing

## Development Rules

- Preserve Launcher3/Quickstep architecture.
- Keep universal APK behavior separate from ROM-only privileged behavior.
- Gate major Elyra features behind flags.
- Add English and Indonesian strings for every new user-facing string.
- Add tests for new model, settings, and UI behavior where practical.

## License and Source Hygiene

- Apache-2.0 compatible source is allowed with attribution.
- Do not add proprietary APKs, decompiled output, copied assets, copied code,
  signing keys, or local audit files.
- Do not copy reference launcher strings, resources, shaders, fonts, native
  libraries, classes, package identifiers, or certificates.

## Build Truth

Only mark checks as passed when the exact command or workflow ran successfully.
Skipped, unavailable, or simulated checks must be marked pending.

