# Plaquote

Android dental clinic notes/reminders app.

## Home screen
The home screen does not display the Plaquote logo or the Plaquote wordmark. The logo is used only as the Android launcher icon.

## Build
Generate the Gradle wrapper in Termux with `gradle wrapper --gradle-version 8.9`, then commit `gradlew` and `gradle/wrapper/` to GitHub. GitHub Actions builds the APK.

## Updates
The app checks the latest GitHub Release for an asset named `Plaquote.apk`. Release tags must use the `v<version>` format, e.g. `v1.2`.
