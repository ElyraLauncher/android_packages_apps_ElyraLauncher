# Building

## Prerequisites

- JDK compatible with the selected Android Gradle Plugin.
- Android SDK and platform tools.
- Gradle wrapper from the imported Launcher3 base.

The Gradle wrapper and Android project files are added during the Launcher3 base
import stage. This bootstrap stage intentionally does not create a placeholder
launcher application.

## Universal APK

After the Launcher3 base is imported, the expected local command will be:

```sh
./gradlew assembleUniversalDebug
```

If the selected upstream uses a different task name, this file will be updated
with the exact task.

## Tests

Expected local checks after the Android project exists:

```sh
./gradlew test lint
```

Instrumentation and emulator checks require an Android emulator or connected
device with `adb` available.

## GitHub Actions

CI workflows will compile APK artifacts, run tests, run lint, upload reports,
and provide emulator install and launch coverage where available.

## ROM Integration

ROM builds must be validated in a real Android platform tree. Platform Quickstep
integration is PENDING until a ROM tree build is executed and recorded.

