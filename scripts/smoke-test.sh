#!/usr/bin/env bash
#
# Universal debug APK emulator smoke test.
#
# Installs the universal debug APK, makes Elyra the Home activity, launches it,
# presses Home, and fails if the launcher process is absent or logs a FATAL
# EXCEPTION. Intended to run inside reactivecircus/android-emulator-runner (see
# .github/workflows/smoke-test.yml) but also works against any connected device
# or emulator with `adb` on PATH.
#
# It does NOT set up or assert privileged Quickstep/Recents behavior — that is a
# ROM-only path and is intentionally out of scope for the universal APK.

set -euo pipefail

PKG="com.elyra.launcher"
ACTIVITY="app.lawnchair.LawnchairLauncher"
COMPONENT="${PKG}/${ACTIVITY}"
LOGCAT_OUT="smoke-logcat.txt"

APK="$(find build/outputs/apk/lawnWithQuickstepGithub/debug -name '*.apk' | head -n 1)"
if [ -z "${APK}" ]; then
  echo "SMOKE FAIL: no universal debug APK found under build/outputs/apk/lawnWithQuickstepGithub/debug"
  exit 1
fi
echo "Using APK: ${APK}"

adb wait-for-device
# -g grants runtime permissions; -r reinstalls if already present.
adb install -r -g "${APK}"

# Best-effort: make Elyra the default Home activity. Ignored on images/APIs where
# the command is unavailable; the direct launch below still exercises startup.
adb shell cmd package set-home-activity "${COMPONENT}" || true

adb logcat -c
echo "Launching ${COMPONENT} ..."
adb shell am start -W -n "${COMPONENT}"
sleep 8

echo "Pressing Home ..."
adb shell input keyevent KEYCODE_HOME
sleep 5

adb logcat -d > "${LOGCAT_OUT}" || true

# Positive check: the launcher process must be alive after Home.
if ! adb shell pidof "${PKG}" >/dev/null 2>&1; then
  echo "SMOKE FAIL: ${PKG} process is not running after launch + Home"
  exit 1
fi

# Negative check: no fatal crash attributed to our package.
if grep -A3 "FATAL EXCEPTION" "${LOGCAT_OUT}" | grep -q "Process: ${PKG}"; then
  echo "SMOKE FAIL: FATAL EXCEPTION detected for ${PKG}"
  grep -B1 -A6 "FATAL EXCEPTION" "${LOGCAT_OUT}" || true
  exit 1
fi

echo "SMOKE PASS: ${PKG} launched as Home and survived a Home press with no fatal exception"
