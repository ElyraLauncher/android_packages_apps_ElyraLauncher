# Architecture

ElyraLauncher is designed as a Launcher3/Quickstep-derived launcher, not a
standalone imitation.

## Source Base

The project will import a properly licensed Launcher3/Quickstep upstream. The
selected source, license, commit or tag, import date, package decisions, and
local deviations are tracked in UPSTREAM.md.

## Targets

### Universal APK

The universal target is a normal installable Home launcher. It must provide real
Launcher3 workspace, drawer, folders, widgets, model loading, and database
behavior. Privileged Recents behavior is not exposed here.

### ROM Target

The ROM target keeps Launcher3/Quickstep integration ready for platform builds.
Privileged permissions, system placement, and Recents integration are validated
only in a ROM tree.

## Feature Flags

Major Elyra features are gated behind flags so disabled features return as close
as possible to upstream Launcher3 behavior. Experimental features default off
until stable.

## Model and Database Safety

Workspace and folder mutations must use Launcher3 model paths or a reviewed
Elyra-safe transaction layer. Layout lock, layout snapshots, and migration
guards are required before destructive operations.

## Privacy

Search, suggestions, ranking, and customization are local by default. Network
providers, if ever added, must be opt-in and documented.

