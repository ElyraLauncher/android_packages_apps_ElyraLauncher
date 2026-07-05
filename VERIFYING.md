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

## Manifest Check

Confirm the merged manifest contains a Home-capable launcher activity with:

- `android.intent.action.MAIN`
- `android.intent.category.HOME`
- `android.intent.category.DEFAULT`

## Contamination Checks

Before release:

```sh
git ls-files | grep -Ei 'apk|jadx|apktool|decompiled'
# Review tracked files for tool-specific or generated-task notes before release.
```

Any match must be reviewed before shipping.
