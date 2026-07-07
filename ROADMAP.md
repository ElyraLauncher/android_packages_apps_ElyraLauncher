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

## Stage 5-13: User Features

Implement bottom search, categories, suggestions, A-Z rail, wallpaper-adaptive
icons, icon and grid controls, large folders, edit tools, layout lock, layout
history, motion, blur, glow, widgets, hidden apps, backup, and settings polish.

## Stage 14: ROM Quickstep Path

Validate platform integration files and run a real ROM build when a ROM tree is
available.

## Stage 15: Release Quality

Clean lint and resources, confirm artifacts, verify contamination checks, and
prepare the first universal preview release.
