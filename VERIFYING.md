# Verifying

Verification results must use the following meanings:

- `PASSED`: the exact check ran successfully.
- `PENDING`: the check could not be run yet because the required source, tool,
  emulator, device, signing key, or ROM tree is unavailable.
- `FAILED`: the exact check ran and failed.

## Required Gates

- Universal APK builds from source with `./gradlew assembleLawnWithQuickstepGithubDebug`.
- APK installs on a normal Android system.
- Launcher can be selected as the default Home app.
- Home press opens ElyraLauncher.
- Workspace, drawer, app launch, folders, and widgets use Launcher3-derived
  behavior.
- Universal APK does not claim privileged Recents behavior.
- ROM Quickstep build is validated only inside a real platform tree.
- No APKs, decompiled output, proprietary assets, signing keys, or local audit
  files are tracked.

## Local Checks

```sh
./gradlew --version
./gradlew assembleLawnWithQuickstepGithubDebug
./gradlew testLawnWithQuickstepGithubDebugUnitTest
./gradlew lintLawnWithQuickstepGithubDebug
```

Emulator checks should install the APK, set or open the launcher as Home, press
Home, open the app drawer, and fail on launcher package fatal exceptions.


## Lint Baseline

`lint-baseline.xml` records exact lint debt inherited with the Launcher3-derived
Stage 1 import. Lint still runs with `abortOnError` enabled, and new issues not
present in the baseline must fail verification. New API usage must be guarded or
reviewed before being added to the baseline.

The baseline includes `MissingTranslation` entries for inherited upstream strings
whose default (English) value was rebranded during Stage 1 (for example
`smartspace_mode_lawnchair`, `restore_nova_subgrid_warning`, `dt2s_a11y_hint`,
`recents_a11y_hint`, `hotseat_mode_lawnchair`). These strings already carry the
same untranslated-locale debt as the rest of the imported base; only inherited
upstream debt is baselined.

Elyra-specific user-facing strings are maintained in English (`res/values`) and
Indonesian (`res/values-in`). Because the imported base ships ~80 upstream locale
folders that Elyra does not yet translate, those new strings carry a targeted
`tools:ignore="MissingTranslation"` (never a global lint disable) until upstream
locales are filled. New Elyra strings are not added to the lint baseline.

## Manifest Check

Confirm the merged manifest contains a Home-capable launcher activity with:

- `android.intent.action.MAIN`
- `android.intent.category.HOME`
- `android.intent.category.DEFAULT`

## Stage 4: Universal Baseline

Stage 4 hardens the universal APK as a real default Home launcher before visible
feature work begins. The table records how each baseline check is verified and its
current status. Runtime checks are `PENDING` on hosts without an Android SDK,
emulator, or device; they are exercised by the emulator smoke test (below) or a
manual device run.

| Check | How verified | Status |
| --- | --- | --- |
| Universal debug APK builds | `assembleLawnWithQuickstepGithubDebug` in CI | PASSED on CI (per-commit); PENDING locally (no SDK on maintainer host) |
| Debug lint | `lintLawnWithQuickstepGithubDebug` in CI | PASSED on CI |
| Debug unit tests | `testLawnWithQuickstepGithubDebugUnitTest` in CI | PASSED on CI |
| Home/launcher intent filter | Static manifest review (`quickstep/AndroidManifest-launcher.xml`) | PASSED — `MAIN` + `HOME` + `DEFAULT`, `exported=true` |
| Feature flags default OFF | Static code review (`ElyraFlag`, `ElyraFeatureFlags`) | PASSED — every flag `default = false`, unique-key guard |
| Flags OFF preserve base behavior | Static review — no baseline code path reads a flag yet | PASSED — flags are inert no-ops until a feature consumes them |
| Install + select as default Home | Emulator smoke test / manual device | PENDING |
| Survives Home press + process recreation | Emulator smoke test / manual device | PENDING |
| No `FATAL EXCEPTION` for `com.elyra.launcher` | Emulator smoke test / manual device | PENDING |
| Drawer opens, app launch works | Manual device | PENDING |
| Folders open and launch apps | Manual device | PENDING |
| Widget picker path | Manual device | PENDING |
| Elyra settings + Elyra Labs open, toggles persist | Manual device | PENDING |
| About / Backup & restore screens open | Manual device | PENDING |
| Privileged Quickstep/Recents | Not applicable to universal APK | ROM-only (deferred) — the universal APK must not claim it |

### Emulator smoke test

A real, opt-in smoke test lives in `.github/workflows/smoke-test.yml` and
`scripts/smoke-test.sh`. It is triggered manually (Actions tab → **Emulator smoke
test** → **Run workflow**) rather than on push, because emulator jobs are slower
and flakier than the build/lint pipeline and must not destabilize it. The job:

1. builds the universal debug APK,
2. boots an `api-level 34` `google_apis` `x86_64` emulator,
3. installs the APK and makes Elyra the Home activity,
4. launches it, presses Home, and captures logcat,
5. **fails** if the `com.elyra.launcher` process is missing or logs a
   `FATAL EXCEPTION`, and uploads the logcat artifact either way.

Status: **PENDING** — the workflow is committed but its first green run has not yet
been recorded. It is not faked; run it from the Actions tab to produce a result.

### Manual device smoke test

With `adb` and a connected device or emulator:

```sh
# Install the universal debug APK
adb install -r -g build/outputs/apk/lawnWithQuickstepGithub/debug/*.apk

# Make Elyra the Home activity (or pick it from the system Home chooser)
adb shell cmd package set-home-activity com.elyra.launcher/app.lawnchair.LawnchairLauncher

# Launch, then press Home
adb shell am start -n com.elyra.launcher/app.lawnchair.LawnchairLauncher
adb shell input keyevent KEYCODE_HOME

# Watch Elyra baseline diagnostics (startup, settings, flag changes)
adb logcat -s Elyra

# Confirm the launcher process is alive and check for crashes
adb shell pidof com.elyra.launcher
adb logcat -d | grep -A3 "FATAL EXCEPTION" || echo "no fatal exception"
```

Then manually exercise: open the app drawer, launch an app, open a folder and
launch from it, open Settings → Elyra Labs, toggle a flag and confirm it persists
across a relaunch, and open the About and Backup & restore screens.

The `Elyra` logcat tag is emitted only by debug builds (see `ElyraLog`); it carries
no telemetry and performs no network or file upload.

### Logcat smoke criteria

Capture a clean-install run and inspect it:

```sh
adb logcat -c
adb logcat -v time > elyra-smoke-logcat.txt   # run through: Home, Settings, drawer, folder
```

A run PASSES when, for `com.elyra.launcher`:

- **No** `FATAL EXCEPTION` / `AndroidRuntime` crash for the launcher process.
- The `ThemeUtils: … is an AppCompat widget …` logcat lines for launcher-owned
  views (`CustomButton`, `DoubleShadowTextView`, `IcuDateTextView`) are **non-fatal**
  and are pending a theme/context fix (see the note below); they must not be
  "resolved" by downgrading the AppCompat base classes.
- **No** default-layout `Favorite not found` for common absent apps on a clean
  install — the universal default layouts use only generic intent `<resolve>`
  fallbacks (dialer, messaging, browser, camera, settings) with no device-specific
  hardcoded packages, so a bare ROM does not log missing favorites.
- **No** hotseat out-of-bounds (e.g. `position 4 out of bounds: 0 to 3`) — the
  universal default hotseat is capped at positions `0..3`.
- **No** `LoaderCursor: Item position overlap` from the shipped default layout on a
  clean install.
- **No** `DefaultLayoutParser` `APP_MARKET` / `market://` warnings — the fragile
  market placeholders were removed from the universal default layouts.
- The `Displayed .../LawnchairLauncher: +…` first-frame time is **recorded as
  observed** (emulator cold starts are slow; record the real number, never a faked
  target).

The following are **non-fatal and acceptable**:

- Emulator OpenGL / `FrameEvents` / `eglMakeCurrent` noise unrelated to launcher
  correctness.
- `DefaultLayoutParser: No preference or single system activity found` for a
  generic non-market `<resolve>` whose intent has zero or several handlers on the
  device — this is expected `<resolve>` behavior, not a layout defect.

### AppCompat theme warnings (pending theme/context fix)

The `ThemeUtils: View class app.lawnchair.views.CustomButton /
…smartspace.DoubleShadowTextView / …smartspace.IcuDateTextView is an AppCompat
widget that can only be used with a Theme.AppCompat theme` lines are **non-fatal**
logcat warnings — the views render correctly.

Root cause: the launcher activity theme chain
`AppTheme → LauncherTheme → BaseLauncherTheme` parents the platform themes
`Theme.DeviceDefault.Light` / `Theme.Material.Light` (as launchers do, for wallpaper
and system integration), while these custom views extend AppCompat widgets, so
AppCompat's theme check fires on every inflation.

Constraint: these views **must** keep their AppCompat base classes — `CustomButton`
extends `androidx.appcompat.widget.AppCompatButton` and `CustomTextView` (parent of
`DoubleShadowTextView` → `IcuDateTextView`) extends
`androidx.appcompat.widget.AppCompatTextView`. Downgrading them to
`android.widget.Button` / `TextView` silences the warning but fails the
`AppCompatCustomView` lint check (an error), so that approach is rejected.

The correct fix, deferred to a focused follow-up, is to make the inflation
context/theme AppCompat-compatible for the affected view subtree (e.g. a
`ContextThemeWrapper` / theme overlay), **without** converting the whole launcher/Home
theme to AppCompat. Until then the warning is accepted as non-fatal and must not be
silenced by changing the base classes.

## Stage 5: Bottom Search App Drawer

Stage 5 moves the real All Apps search surface to the bottom of the drawer behind
the `elyra_bottom_search` flag (default OFF). It reuses the existing
Launcher3/Lawnchair All Apps model and search primitives; only the search bar's
anchor and the sibling layout rules change. The gate lives in one place
(`ElyraBottomSearch.isEnabled`), read by `ActivityAllAppsContainerView` and
`AllAppsSearchInput`.

| Check | How verified | Status |
| --- | --- | --- |
| Universal debug APK builds | `assembleLawnWithQuickstepGithubDebug` in CI | PENDING locally (no SDK); CI is source of truth |
| Debug lint | `lintLawnWithQuickstepGithubDebug` in CI | PENDING locally; CI is source of truth |
| Flag OFF preserves upstream drawer | Static review — every bottom-search branch is `false` when the flag is off, so the top-search layout is unchanged | PASSED (static) |
| Flag ON anchors search to bottom | Static review — `alignParentTop` for lists/header + `ALIGN_PARENT_BOTTOM` for the bar + RV bottom padding | PASSED (static) |
| Search stays real (filter + launch) | Static review — search controller/algorithm/adapter untouched; only positioning changed | PASSED (static) |
| Keyboard shows above the bottom bar | Manual device — window uses `adjustPan`, focused input is panned above the IME | PENDING |
| Insets: bar rests above gesture/nav bar | Manual device — `setInsets` bottom margin = system bottom inset + `elyra_spacing_medium` | PENDING |
| Rotation (portrait/landscape) | Manual device | PENDING |
| No crash on focus/type/clear/launch | Manual device / emulator smoke | PENDING |

### Manual Stage 5 smoke steps

1. Install the universal debug APK and set Elyra as the default Home app.
2. Open the app drawer with `elyra_bottom_search` **OFF** — the search bar is at the
   top exactly as upstream (baseline confirmation).
3. Open **Settings → Elyra Labs** and enable **Bottom search**. Return Home.
4. Open the drawer again — the search bar is now anchored at the bottom, above the
   gesture/navigation bar, visually attached to the drawer.
5. Tap the search bar — it focuses and the keyboard opens without covering the input.
6. Type an installed app name — the app list filters to real results.
7. Launch an app from the results — it starts the real app.
8. Clear the query (the clear button) — results reset to the full drawer.
9. Rotate the device (if supported) and repeat 5–7.
10. Press Back — the keyboard/search dismisses before the drawer closes.

Capture logcat (`adb logcat`) during the run and confirm, for
`com.elyra.launcher`: no `FATAL EXCEPTION` / `AndroidRuntime` crash, and no repeated
All Apps / search inflation or keyboard/insets exceptions.

### Stage 5 tests

No JVM unit-test source set is wired for the app module (`testLawnWithQuickstep…`
runs zero app tests today), and the drawer/search path is a UI flow that requires an
instrumentation host. Automated Stage 5 coverage is therefore **PENDING** an
instrumentation/emulator harness; it is not faked. The flag's default-OFF contract is
guarded at construction by the `ElyraFeatureFlags` key/default invariant, and the OFF
path is verified statically (every bottom-search branch is inert when the flag is off).

## Contamination Checks

Before release:

```sh
git ls-files | grep -Ei 'apk|jadx|apktool|decompiled'
# Review tracked files for tool-specific or generated-task notes before release.
```

Any match must be reviewed before shipping.
