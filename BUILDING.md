# Building

## Prerequisites

- JDK 21 for the current imported Gradle/Android Gradle Plugin combination.
- Android SDK installed by Android Studio or CI.
- Network access for Gradle dependency resolution on first build.

## Universal APK

The default Gradle build imports a Launcher3-derived standalone configuration.
Build the universal debug APK with:

```sh
./gradlew assembleDebug
```

Expected debug artifact location:

```text
build/outputs/apk/**/debug/*.apk
```

The universal target uses application ID `com.elyra.launcher` and app label
`Elyra Launcher`.

## Tests

Run available debug unit tests and lint with:

```sh
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Instrumentation and emulator checks require `adb` and an Android emulator or
connected device.

## GitHub Actions

`.github/workflows/ci.yml` builds the debug APK, runs debug unit tests, runs
debug lint, and uploads APK/report artifacts.

## ROM Integration

ROM integration notes live under `tools/rom/`. Platform Quickstep and Recents
validation is PENDING until a real ROM tree build is executed.
