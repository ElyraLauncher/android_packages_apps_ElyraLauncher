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

## Product Identity and Namespaces

User-facing product identity is **Elyra Launcher**: app label, launcher label,
settings and About titles, and normal in-app wording present Elyra as the app.
Legal and license attribution to the Launcher3/Lawnchair open-source base is kept
in `NOTICE`, `UPSTREAM.md`, and the in-app licenses/attribution section only.

Internal upstream structure is intentionally preserved to keep future rebases
onto upstream Launcher3/Lawnchair practical:

- `com.android.launcher3` remains the Launcher3 core namespace.
- `app.lawnchair` remains the imported Lawnchair namespace.
- Existing upstream folders, packages, class names, resource names, and the
  Gradle variant name (`lawnWithQuickstepGithub`) are left in place; a blanket
  rename would create a huge, risky diff against upstream.

New Elyra-specific code should live under the `com.elyra.launcher` namespace
(see `ElyraBranding`), and new Elyra resources should use an `elyra_` prefix.
This keeps Elyra additions separable from upstream so rebases stay mechanical.

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

## Feature Flags and Design Tokens

Major Elyra features are gated behind flags so disabled features stay as close as
possible to upstream Launcher3-derived behavior. The Stage 3 foundation lives
under `com.elyra.launcher`:

- `flags/ElyraFlag` — the enum of feature flags (stable key, default, `romOnly`).
- `flags/ElyraFeatureFlags` — the pure-Kotlin registry (single source of truth,
  unique-key guard).
- `flags/ElyraFlagStore` — persistence via the existing opto `PreferenceManager`
  over a dedicated `elyra_preferences` Jetpack DataStore file (no new storage
  system, no DB mutation, no network, no telemetry).
- `flags/ElyraFlagsRepository` — `isEnabled` / `observe` / `setEnabled` access for
  feature code.

Every flag defaults **OFF**, so adding a flag or a `flags.isEnabled(...)` gate
never changes current launcher behavior; stable behavior is preserved when flags
are off. New Elyra features must be gated behind their flag and must not fake
unfinished behavior. Flags are exposed for development in the **Elyra Labs**
settings screen (`com.elyra.launcher.ui.ElyraLabsPreferences`), reachable from the
preferences overflow menu.

Design tokens: color, spacing, radius, icon-size and layout-surface tokens are
resources (`lawnchair/res/values/elyra_tokens.xml` plus a `-night` variant);
motion and effect tokens are code constants (`com.elyra.launcher.theme`). These
are additive — nothing consumes them in Stage 3 — and carry no values copied from
any reference launcher.

## Model and Database Safety

Workspace and folder mutations must use Launcher3 model paths or a reviewed
Elyra-safe transaction layer. Layout lock, layout snapshots, and migration
guards are required before destructive operations.

## Privacy

Search, suggestions, ranking, and customization are local by default. Network
providers, if ever added, must be opt-in and documented.
