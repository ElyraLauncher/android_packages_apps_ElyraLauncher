<!--
Thank you for contributing to Elyra Launcher. Please fill in every section.
Delete a section only if it is genuinely not applicable, and say why.
Every commit in this PR must be signed off (git commit -s / DCO).
-->

## Summary

<!-- What does this PR do, in one or two sentences? -->

## Motivation / root cause

<!-- Why is this change needed? What problem, bug, or risk does it address? -->

## Architectural approach

<!-- How is it implemented? Key design decisions and why they are safe. -->

## Affected modules

<!-- Tick all that apply. -->

- [ ] Launcher3 core (`src/`, `res/`)
- [ ] Quickstep / Recents (`quickstep/`)
- [ ] Elyra additions (`lawnchair/src/com/elyra/`)
- [ ] Lawnchair layer (`lawnchair/`)
- [ ] Build / Gradle
- [ ] GitHub automation (`.github/`)
- [ ] Documentation
- [ ] Tests

## Impact assessment

- **Universal APK impact:** <!-- behavior in the universal com.elyra.launcher APK -->
- **ROM / Quickstep impact:** <!-- privileged/ROM path; state "none" or "not validated on a real platform build" honestly -->
- **Database / LauncherModel impact:** <!-- schema, migration, or model behavior changes; "none" if none -->
- **Feature flag status:** <!-- which ElyraFlag gates this; default value; or "not flag-gated" with justification -->
- **Privacy / security impact:** <!-- new permissions, exported components, logging of sensitive data, network, etc. -->
- **Localization impact:** <!-- new user-facing strings; English + Indonesian provided? -->

## Validation

<!-- List the EXACT commands/workflows you ran and their real result.
     Mark each PASS / FAIL / SKIPPED / PENDING. Do not claim a pass without evidence. -->

```
# e.g.
# ./gradlew assembleLawnWithQuickstepGithubDebug --stacktrace --no-daemon --warning-mode all   -> PASS
# ./gradlew lintLawnWithQuickstepGithubDebug --stacktrace --no-daemon --warning-mode all        -> PASS
# ./gradlew testLawnWithQuickstepGithubDebugUnitTest --stacktrace --no-daemon --warning-mode all -> PASS
```

## Screenshots / screen recordings

<!-- Required for any user-visible UI change. Light and dark mode where relevant. -->

## Risk and rollback

<!-- What could regress? How would we roll this back? -->

## Upstream / rebase impact

<!-- Does this touch upstream-derived files in a way that complicates future
     Launcher3/Quickstep rebases? How was that minimized? -->

## Documentation

<!-- Which docs were updated (README, ARCHITECTURE, BUILDING, VERIFYING,
     CHANGELOG, UPSTREAM, PRIVACY), or why none were needed. -->

---

## Checklist

- [ ] Universal APK compiles (`assembleLawnWithQuickstepGithubDebug`)
- [ ] Lint run (`lintLawnWithQuickstepGithubDebug`)
- [ ] Unit tests run (`testLawnWithQuickstepGithubDebugUnitTest`)
- [ ] Code formatting checked
- [ ] Emulator smoke test run (if the change affects launch/drawer/home behavior)
- [ ] ROM / Quickstep path is **not** falsely marked as validated
- [ ] No proprietary code, assets, strings, fonts, or decompiled output introduced
- [ ] No telemetry, analytics, or hidden network behavior added
- [ ] User-facing strings provided in **English and Indonesian**
- [ ] Behavior gated behind an appropriate **Elyra feature flag** where relevant
- [ ] Every commit has a `Signed-off-by:` trailer (DCO, `git commit -s`)
- [ ] Test results reported truthfully; skipped/unavailable checks marked as such
