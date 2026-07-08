# Roadmap

## Stage 0: Repository Foundation

Complete. Public project documentation, license files, ignore rules, and
verification language are in place.

## Stage 1: Launcher3 Base

Complete when the imported Launcher3-derived Gradle source builds a universal
Home-capable APK and the Stage 1 commit is clean.

## Stage 2: Feature Matrix

Audit the local reference launcher without copying proprietary source or assets,
then document neutral feature areas and acceptance criteria. See
[docs/FEATURE_MATRIX.md](docs/FEATURE_MATRIX.md).

## Stage 3: Flags and Settings

Add Elyra feature flags, persistent settings, theme tokens, and English and
Indonesian strings.

## Stage 4: Universal Home Baseline

Harden the universal APK as a real default Home launcher before visible feature
work: confirm the Home/launcher intent, verify app-model, workspace, folder, and
settings baseline paths, confirm feature flags default OFF and stay inert, add
debug-only Elyra logging (`ElyraLog`, no telemetry/network), and add an opt-in
emulator smoke test. See [VERIFYING.md](VERIFYING.md) for the baseline checklist
and current PASSED/PENDING status. Privileged Quickstep/Recents stays ROM-only.

## Stage 5: Bottom Search App Drawer

Move the real All Apps search surface to the bottom of the drawer behind the
`elyra_bottom_search` flag (default OFF). Reuses the existing Launcher3/Lawnchair
All Apps model and search primitives — the search bar is re-anchored to the bottom
of the drawer (above the gesture/nav inset), the lists fill from the top and clear
the bar with extra bottom padding, and the search itself (focus, keyboard, live
filtering, clear, app launch) is unchanged. When the flag is OFF the drawer is
byte-for-byte the upstream top-search layout. See [VERIFYING.md](VERIFYING.md) for
the Stage 5 drawer-search check. Categories, suggestions, and the A-Z rail remain
deferred to Stage 6.

## Stage 6: Drawer Categories, Suggestions, A-Z Rail

Add local drawer organization on top of the real AllApps model, each behind its
Stage 3 flag (default OFF):

- `elyra_drawer_categories` — groups the drawer app list into local category buckets
  (Communication, Social, Media, Games, Tools, Productivity, Finance, Shopping,
  Travel, System, Other) using a deterministic on-device classifier
  (`ElyraAppCategorizer`, from `ApplicationInfo.category` + package hints). No cloud,
  network, or proprietary classifier.
- `elyra_drawer_suggestions` — prepends a small local suggestions section
  (`ElyraDrawerSuggestions`, deterministic package-recency ranking with a label
  tiebreak; a usage-score hook is reserved for later). Local only — no web/provider
  rows, no network, no telemetry.
- `elyra_az_rail` — reveals the existing Launcher3 fast-scroll rail (A-Z with the
  section popup) on the drawer.

All three reuse existing adapter/scroller primitives and add no fake screen. When a
flag is OFF the drawer is unchanged, and Stage 5 bottom search stays local-first
(search uses the search-results path, so categories/suggestions do not affect it).
See [VERIFYING.md](VERIFYING.md) for the Stage 6 checks.

## Stage 7-13: User Features

Implement wallpaper-adaptive icons, icon and grid controls, large folders, edit
tools, layout lock, layout history, motion, blur, glow, widgets, hidden apps,
backup, and settings polish.

## Stage 14: ROM Quickstep Path

Validate platform integration files and run a real ROM build when a ROM tree is
available.

## Stage 15: Release Quality

Clean lint and resources, confirm artifacts, verify contamination checks, and
prepare the first universal preview release.
