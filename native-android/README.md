# Gesture XR Native Android

This is the phone-buildable native Android prototype. It does not require Unity.

## Build on Android with Termux

Install Termux from F-Droid/GitHub, then install a JDK and Gradle. From the repository root:

```sh
cd native-android
gradle :app:assembleDebug
```

The APK is created at:

`app/build/outputs/apk/debug/app-debug.apk`

Install it with:

```sh
termux-open app/build/outputs/apk/debug/app-debug.apk
```

## Current prototype

- Android MediaProjection screen capture
- Foreground screen-capture service
- Captured screen displayed in an interactive window
- Touch dragging of the window
- ARCore and MediaPipe dependencies are included for the next native XR stage

The native prototype intentionally does not pretend to have finished 6DoF or hand tracking yet. Those require wiring ARCore camera/world tracking and MediaPipe hand landmarks into the renderer and interaction layer.
