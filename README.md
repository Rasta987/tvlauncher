# Vibe Home (Android TV launcher)
Apple TV style launcher. Build: push to GitHub -> Actions -> download `app-debug` artifact.
Install: `adb install app-debug.apk`
Set default: `adb shell cmd package set-home-activity com.vibe.launcher/.MainActivity`
Hide apps: edit `HIDDEN` in MainActivity.kt
