# ElyraLauncher

ElyraLauncher is a ROM-first Android launcher project built around the real
Launcher3 and Quickstep architecture. The project targets a universal APK for
custom ROM users first, while keeping a clean path for privileged ROM
integration later.

[![CI](https://github.com/ElyraLauncher/android_packages_apps_ElyraLauncher/actions/workflows/ci.yml/badge.svg)](https://github.com/ElyraLauncher/android_packages_apps_ElyraLauncher/actions/workflows/ci.yml)
[![Emulator](https://github.com/ElyraLauncher/android_packages_apps_ElyraLauncher/actions/workflows/emulator.yml/badge.svg)](https://github.com/ElyraLauncher/android_packages_apps_ElyraLauncher/actions/workflows/emulator.yml)

## Goals

- Provide a normal installable Home launcher APK for broad custom ROM support.
- Preserve Launcher3 workspace, app drawer, folders, widgets, model loading, and
  database behavior.
- Keep Quickstep and Recents work on the ROM-integrated path where privileged
  system integration is available.
- Build modern launcher features behind explicit feature flags.
- Avoid telemetry, hidden network behavior, proprietary dependencies, and
  reference-derived proprietary assets or code.

## Status

Stage 0 repository bootstrap is complete. A real Launcher3/Quickstep source base
is still pending and will be recorded in UPSTREAM.md before application code is
added.

## Targets

- `universal`: installable APK with Home intent support and no privileged
  Recents claims.
- `rom`: platform-ready Launcher3/Quickstep target for ROM trees, with privileged
  features validated only inside a real platform build.

See BUILDING.md and VERIFYING.md for build and validation rules.

