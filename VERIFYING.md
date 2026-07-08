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

## Contamination Checks

Before release:

```sh
git ls-files | grep -Ei 'apk|jadx|apktool|decompiled'
# Review tracked files for tool-specific or generated-task notes before release.
```

Any match must be reviewed before shipping.
