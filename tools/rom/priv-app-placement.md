# Priv-App Placement

Use privileged placement only for ROM builds that integrate ElyraLauncher with
platform Quickstep and Recents contracts.

The universal APK is not a privileged app and must remain installable as a
normal Home launcher.

Example platform path:

```text
system_ext/priv-app/ElyraLauncher/ElyraLauncher.apk
```

The exact partition and package name are ROM decisions.
