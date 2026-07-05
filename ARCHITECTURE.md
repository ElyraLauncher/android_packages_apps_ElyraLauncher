# Architecture

ElyraLauncher is a Launcher3/Quickstep-derived launcher, not a standalone
imitation.

## Source Base

Stage 1 imports an Apache-2.0 Launcher3-derived standalone base from Lawnchair
Launcher. UPSTREAM.md records the exact source repositories, commits, import
method, and local deviations.

Core Launcher3 paths are preserved, including Launcher, Workspace, CellLayout,
Hotseat, All Apps, folders, widgets, model/database code, and Quickstep source
paths.

## Targets

### Universal APK

The universal target is a normal installable Home launcher with application ID
`com.elyra.launcher`. It provides Launcher3-derived workspace, drawer, folders,
widgets, model loading, and database behavior. Privileged Recents behavior is
not claimed here.

### ROM Target

The ROM target keeps Launcher3/Quickstep integration ready for platform builds.
Privileged permissions, system placement, and Recents integration are validated
only in a ROM tree. ROM notes are under `tools/rom/`.

## Feature Flags

Major Elyra features will be gated behind flags so disabled features return as
close as possible to upstream Launcher3-derived behavior. Stage 3 adds the Elyra
flag foundation.

## Model and Database Safety

Workspace and folder mutations must use Launcher3 model paths or a reviewed
Elyra-safe transaction layer. Layout lock, layout snapshots, and migration
guards are required before destructive operations.

## Privacy

Search, suggestions, ranking, and customization are local by default. Network
providers, if ever added, must be opt-in and documented.
