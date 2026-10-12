# TILLU HACKER RGB Panel — Android Studio project

Floating RGB-style performance dashboard for Android, designed for your Motorola Edge 60 Fusion.

## Features
- Floating `TH` bubble; tap to open or hide the panel, drag to move it.
- CPU usage, RAM usage, battery percentage, and battery temperature where Android exposes them.
- Three locally saved profile labels: Balanced, Rush, Precision.
- No game memory access, enemy ESP, silent aim, or gameplay automation.

## Build an APK
1. Install Android Studio on a computer.
2. Open this folder as an existing Android Studio project.
3. Let Gradle sync. Use JDK 17 (Android Studio's bundled JDK is fine).
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Install the generated debug APK on your phone.
6. Open the app, grant “Display over other apps” permission, then tap Start floating panel.

## Notes
- This ZIP contains Android source code, not a prebuilt APK.
- `SYSTEM_ALERT_WINDOW` permission is required for a floating overlay.
- Android may restrict overlays over some secure screens or game UI.
- CPU/RAM metrics are best-effort and may vary by Android version/vendor.
- Root is not required for these metrics; the app does not run root commands.
- This starter project has not been compiled on your device. Build it in Android Studio and fix any SDK/Gradle sync prompts if shown.
