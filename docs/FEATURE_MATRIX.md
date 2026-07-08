# Elyra Launcher — Feature Matrix

Central planning document for Elyra Launcher feature parity and staged
implementation. This document is the source of truth for what Elyra will build,
how each capability is classified, and how it will be verified.

## 1. Purpose

- **Feature parity target.** Define the set of modern launcher capabilities Elyra
  Launcher aims to reach, expressed as neutral, brand-independent features.
- **Clean-room implementation policy.** Elyra features are implemented
  independently on the real Launcher3/Lawnchair-derived base. A reference launcher
  was inspected only as a neutral feature and behavior inventory (file/resource
  naming and architecture categories). No proprietary code, layouts, drawables,
  assets, fonts, native libraries, strings, or package names are copied or
  reproduced. No proprietary brand names appear in this repository.
- **Universal APK first.** Every feature must define what is achievable in the
  universal, installable APK (no system privileges) before any ROM-integrated
  behavior is considered.
- **ROM integration later.** Capabilities that genuinely require a privileged or
  platform-integrated build (real Recents/Quickstep, taskbar hooks) are documented
  as ROM-only and are deferred to the ROM integration stage. The universal APK
  ships a graceful fallback instead of faking privileged behavior.

## 2. Support classification

Each feature below is classified with these fields:

- **Universal APK** — YES / PARTIAL / NO: works in the standard installable build.
- **ROM-integrated build** — YES / PARTIAL / NO: works in a privileged/platform build.
- **Privileged/system dependency** — YES / NO: requires system signature, priv-app
  placement, or platform APIs unavailable to a normal app.
- **Stage** — the implementation stage from the stage map in section 4.
- **Flag** — the Elyra feature-flag name that gates the feature.
- **Acceptance** — the observable condition that counts as "implemented".
- **Test** — how the feature is verified (see section 6 for the shared strategy).

Feature-flag names use the `elyra_*` prefix and are neutral. Flags default OFF
until a feature meets its acceptance criteria, then default ON for the universal
APK where applicable.

## 3. Feature buckets

### A. Home workspace

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Grid profiles | YES | YES | NO | 8 | `elyra_grid_profiles` | User can select grid density; workspace + drawer relayout and persist across restart | Unit (profile math), instrumentation (relayout), device |
| Icon resize | YES | YES | NO | 8 | `elyra_icon_resize` | Icon size setting changes rendered icon + label metrics live and persists | Unit, instrumentation, device |
| Label sizing | YES | YES | NO | 8 | `elyra_label_sizing` | Label text size/lines configurable; truncation correct; persists | Unit, instrumentation |
| Widget support | YES | YES | NO | 4/12 | `elyra_widgets` | Add, bind, resize, remove app widgets on workspace | Instrumentation, device |
| Dock/hotseat polish | YES | YES | NO | 4 | `elyra_hotseat_polish` | Hotseat spacing, icon count, background contrast configurable and stable | Instrumentation, device |
| Wallpaper contrast | YES | YES | NO | 4 | `elyra_wallpaper_contrast` | Text/scrim adapts to wallpaper luminance for legibility | Instrumentation, device (light/dark walls) |

### B. App drawer

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Bottom search | YES | YES | NO | 5 | `elyra_bottom_search` | Search field anchored at the bottom, reachable one-handed, focus/dismiss correct | Instrumentation, device |
| Local app search | YES | YES | NO | 5 | `elyra_local_search` | Typed query filters installed apps locally, no network | Unit (matcher), instrumentation |
| App suggestions | YES | YES | NO | 6 | `elyra_drawer_suggestions` | Local suggestion section from on-device signals; hideable | Unit, instrumentation |
| Drawer categories | YES | YES | NO | 6 | `elyra_drawer_categories` | Apps grouped into local categories (deterministic classifier) | Unit, instrumentation |
| A-Z rail | YES | YES | NO | 6 | `elyra_az_rail` | Fast-scroll alphabet rail jumps to sections | Instrumentation, device |
| Empty/no-result states | YES | YES | NO | 5/6 | `elyra_drawer_empty_states` | Clear empty and no-result UI shown appropriately | Instrumentation |
| Work profile compatibility | PARTIAL | YES | NO | 6 | `elyra_work_profile` | Work tab lists work apps; respects quiet mode where the platform allows | Instrumentation, device (managed profile) |

### C. Search

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Bottom search surface | YES | YES | NO | 5 | `elyra_bottom_search` | Unified bottom search surface hosts results | Instrumentation |
| Keyboard behavior | YES | YES | NO | 5 | `elyra_search_keyboard` | IME shows/hides with search focus; enter launches top result | Instrumentation, device |
| Fuzzy matching | YES | YES | NO | 5 | `elyra_fuzzy_search` | Approximate/typo-tolerant local matching ranks sensibly | Unit (ranking), instrumentation |
| Local-only result source | YES | YES | NO | 5 | `elyra_local_only_search` | Results come only from on-device sources; no network by default | Unit, instrumentation (no-network assertion) |
| Optional provider hooks (disabled by default) | PARTIAL | PARTIAL | NO | 5 | `elyra_search_providers` | Extension points exist but are OFF by default and require explicit opt-in | Unit (flag gating) |

### D. Folders

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Normal folders | YES | YES | NO | 4 | `elyra_folders` | Create, rename, add/remove, open/close folders | Instrumentation, device |
| Large folder grid-4 | YES | YES | NO | 9 | `elyra_large_folder` | Folder can render an expanded 4-cell preview mode | Unit, instrumentation |
| Large folder grid-9 | YES | YES | NO | 9 | `elyra_large_folder` | Folder can render an expanded 9-cell preview mode | Unit, instrumentation |
| Hero folder mode | YES | YES | NO | 9 | `elyra_hero_folder` | Enlarged folder tile occupying multiple cells renders + persists | Instrumentation, device |
| Direct launch of visible apps | YES | YES | NO | 9 | `elyra_folder_direct_launch` | Tapping a visible in-folder app launches it without opening the folder | Instrumentation, device |
| Resize/convert modes | YES | YES | NO | 9 | `elyra_folder_resize` | Folder can convert between normal and large/hero and resize | Instrumentation |
| Reorder and drag/drop | YES | YES | NO | 9 | `elyra_folder_dnd` | Items reorder within and across folders via drag/drop | Instrumentation, device |

### E. Icon system

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Wallpaper-adaptive themed icons | PARTIAL | YES | NO | 7 | `elyra_themed_icons` | Supported icons tint to wallpaper/system theme; unsupported fall back cleanly | Unit (mapping), instrumentation, device |
| Monochrome adaptive support | YES | YES | NO | 7 | `elyra_monochrome_icons` | Adaptive icons with monochrome layers render themed | Instrumentation, device |
| Legacy icon mask fallback | YES | YES | NO | 7 | `elyra_legacy_icon_mask` | Non-adaptive icons are masked to the chosen shape consistently | Unit, instrumentation |
| Icon shape | YES | YES | NO | 7/8 | `elyra_icon_shape` | User-selectable icon shape applied app-wide and persisted | Unit, instrumentation |
| Cache invalidation | YES | YES | NO | 7 | `elyra_icon_cache` | Icon cache refreshes on pack/theme/shape change and app install/update | Unit, instrumentation |
| Light/dark contrast | YES | YES | NO | 7 | `elyra_icon_contrast` | Themed icons keep legible contrast in light and dark | Instrumentation, device |

### F. Motion and effects

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Spring drawer motion | YES | YES | NO | 11 | `elyra_spring_motion` | Drawer open/close uses spring dynamics; interruptible | Instrumentation, device |
| Folder open/close motion | YES | YES | NO | 11 | `elyra_folder_motion` | Folder transitions animated and interruptible | Instrumentation, device |
| Icon press feedback | YES | YES | NO | 11 | `elyra_icon_press` | Press/scale/ripple feedback on icons | Instrumentation, device |
| Progressive blur | PARTIAL | YES | NO | 11 | `elyra_progressive_blur` | Blur layer behind surfaces where the platform supports it; disabled fallback otherwise | Instrumentation, device |
| Contour glow | YES | YES | NO | 11 | `elyra_contour_glow` | Optional glow/edge effect renders without proprietary assets | Instrumentation |
| Low-end fallback | YES | YES | NO | 11 | `elyra_motion_lowend` | Effects downgrade/disable on low-end or when animations are reduced | Unit (capability gate), device |
| Debug frame logging | YES | YES | NO | 11 | `elyra_debug_frames` | Developer-only frame/jank logging toggle; OFF by default | Unit |

### G. Edit mode and layout tools

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Workspace edit mode | YES | YES | NO | 10 | `elyra_edit_mode` | Dedicated edit mode enters/exits; items become editable | Instrumentation, device |
| Edit toolbar | YES | YES | NO | 10 | `elyra_edit_toolbar` | Toolbar exposes edit actions in edit mode | Instrumentation |
| Layout lock | YES | YES | NO | 10 | `elyra_layout_lock` | Locking prevents accidental moves/removals | Unit, instrumentation |
| Layout history | YES | YES | NO | 10 | `elyra_layout_history` | Recent layout changes can be undone/redone | Unit, instrumentation |
| Snapshot/restore | YES | YES | NO | 10 | `elyra_layout_snapshot` | User can snapshot a layout and restore it | Unit, instrumentation |
| Autofill empty cells | YES | YES | NO | 10 | `elyra_autofill_cells` | Command fills empty cells per rules | Unit, instrumentation |
| Smart arrange | YES | YES | NO | 10 | `elyra_smart_arrange` | Command rearranges items by a deterministic strategy | Unit, instrumentation |
| Cleanup empty screens | YES | YES | NO | 10 | `elyra_cleanup_screens` | Empty workspace screens are removed on demand | Unit, instrumentation |

### H. Settings

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Elyra settings structure | YES | YES | NO | 3 | `elyra_settings` | Coherent settings hierarchy in Elyra branding | Instrumentation |
| Feature flags | YES | YES | NO | 3 | `elyra_flags` | Central flag registry with defaults; readable at runtime | Unit |
| Icons settings | YES | YES | NO | 3/7 | `elyra_settings_icons` | Icon options grouped and persisted | Instrumentation |
| Drawer settings | YES | YES | NO | 3/5 | `elyra_settings_drawer` | Drawer options grouped and persisted | Instrumentation |
| Folders settings | YES | YES | NO | 3/9 | `elyra_settings_folders` | Folder options grouped and persisted | Instrumentation |
| Gestures settings | PARTIAL | YES | NO | 3 | `elyra_settings_gestures` | Gesture options present; system gestures gated by capability | Instrumentation |
| Layout settings | YES | YES | NO | 3/10 | `elyra_settings_layout` | Layout tool options grouped and persisted | Instrumentation |
| Backup/restore settings | YES | YES | NO | 13 | `elyra_settings_backup` | Backup/restore entry points wired | Instrumentation, device |
| About/licenses | YES | YES | NO | 3 | `elyra_about` | About shows Elyra identity + open-source attribution/licenses | Instrumentation |

### I. Privacy

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Hidden apps | YES | YES | NO | 13 | `elyra_hidden_apps` | Selected apps hidden from drawer; reveal path defined | Instrumentation, device |
| Local suggestions | YES | YES | NO | 6 | `elyra_drawer_suggestions` | Suggestions derive only from on-device signals | Unit, instrumentation |
| No telemetry | YES | YES | NO | 3 | `elyra_no_telemetry` | No analytics/telemetry collection in the app | Code review, unit (no-network) |
| No hidden network calls | YES | YES | NO | 3 | `elyra_no_hidden_network` | No background network beyond explicitly opted-in features | Instrumentation (network assertion) |
| Optional protected access | PARTIAL | YES | NO | 13 | `elyra_protected_access` | Optional lock/auth gate for protected surfaces where the platform allows | Instrumentation, device |

### J. Widgets and large screens

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Widget picker | YES | YES | NO | 12 | `elyra_widget_picker` | Browse and add widgets by app/category | Instrumentation, device |
| Widget resize | YES | YES | NO | 12 | `elyra_widget_resize` | Placed widgets resize within cell constraints | Instrumentation |
| Tablet/foldable profiles | YES | YES | NO | 12 | `elyra_large_screen_profiles` | Distinct profiles for tablet and foldable form factors | Unit, instrumentation, device/emulator |
| Taskbar-like universal fallback | PARTIAL | YES | NO | 12 | `elyra_taskbar_fallback` | Universal build offers an in-app dock/taskbar-like surface without system hooks | Instrumentation, device |
| ROM-only taskbar/Quickstep path | NO | YES | YES | 14 | `elyra_rom_taskbar` | Real system taskbar integration only in a privileged ROM build | ROM tree test |

### K. ROM integration

| Feature | Universal APK | ROM build | Priv dep | Stage | Flag | Acceptance | Test |
|---|---|---|---|---|---|---|---|
| Privileged launcher path | NO | YES | YES | 14 | `elyra_rom_privileged` | Builds/installs as a privileged launcher in a ROM tree | ROM tree test |
| Quickstep/Recents real integration | NO | YES | YES | 14 | `elyra_rom_quickstep` | Real Recents/Quickstep gestures and transitions function on-device | ROM device test |
| Product makefile snippet | NO | YES | YES | 14 | `elyra_rom_makefile` | Documented product `.mk` snippet integrates Elyra into a build | ROM tree test |
| priv-app placement | NO | YES | YES | 14 | `elyra_rom_privapp` | Placed under priv-app with a permissions allowlist | ROM tree test |
| Platform build gates | NO | YES | YES | 14 | `elyra_rom_build_gates` | Build flags/gates select the ROM path cleanly | ROM tree test |
| Universal APK limitation note | YES | n/a | NO | 14 | `elyra_rom_docs` | Docs clearly state which features are ROM-only and why | Doc review |

## 4. Stage mapping

- **Stage 3** — feature flags, settings scaffolding, theme tokens, EN/ID strings.
- **Stage 4** — universal home baseline polish (install, Home role, workspace,
  drawer open, app launch, hotseat, normal folders, widgets).
- **Stage 5** — bottom search drawer (surface, keyboard, local + fuzzy matching).
- **Stage 6** — categories, suggestions, A-Z rail, work profile handling.
- **Stage 7** — wallpaper-adaptive themed icons and icon pipeline.
- **Stage 8** — icon resize, label sizing, icon shape, grid profiles.
- **Stage 9** — large folders (grid-4/grid-9/hero) and direct launch.
- **Stage 10** — edit mode, layout lock, history, snapshot, layout tools.
- **Stage 11** — motion system, progressive blur, contour glow, low-end fallback.
- **Stage 12** — widgets, taskbar-like fallback, tablet/foldable large-screen support.
- **Stage 13** — hidden apps, backup/restore, privacy and final polish.
- **Stage 14** — ROM Quickstep/Recents and privileged integration.
- **Stage 15** — release quality (lint, resources, artifacts, contamination checks).

## 5. Acceptance definition

- **Implemented (YES).** The feature meets its acceptance row on the universal APK
  (or ROM build for ROM-only rows), is gated by its flag, persists across restart
  where relevant, and has passing tests per its test column.
- **Partial (PARTIAL).** Core behavior works but is capability-gated (e.g. blur or
  themed icons where the platform limits it), or an extension point exists but is
  intentionally OFF/opt-in. The graceful path must be defined and tested.
- **ROM-only (NO on universal).** The feature genuinely requires a privileged or
  platform-integrated build. The universal APK must ship a documented fallback and
  must not pretend the privileged behavior is active.
- **Must not be faked.** No mock/stub is allowed to stand in for a claimed feature:
  no faked Recents/Quickstep, no faked privileged access, no hardcoded results
  substituting for real search/suggestions, no placeholder that reports success
  without doing the work. If a capability is unavailable, the UI states the
  limitation rather than simulating success.

## 6. Test plan summary

- **Unit tests.** Pure logic: matchers/ranking, grid/profile math, flag gating,
  layout history/snapshot operations, icon mapping, capability gates.
- **Instrumentation tests.** On-device/emulator UI behavior: drawer, search,
  folders, edit mode, settings persistence, drag/drop, network-absence assertions.
- **Emulator tests.** Form-factor coverage (phone, tablet, foldable) for profiles,
  large screens, and relayout.
- **Real device tests.** Motion/blur/glow feel, themed icon contrast, gestures,
  and performance on representative and low-end hardware.
- **ROM tree tests.** Privileged build, priv-app placement, Quickstep/Recents
  integration, and product makefile validation in an actual platform build.
