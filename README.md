# On-device-XR-Workspace-System
On-device XR Workspace System Using Real-time Hand Gesture Recognition
> The `Library/` folder has been excluded from this repository due to GitHub's file size limitations.

### Project Overview
___

This project implements a mobile on-device extended reality (XR) workspace system.
Users can intuitively interact with virtual objects through hand gestures, without relying on external servers. The existing project uses Unity + AR Foundation + MediaPipe hand tracking and targets Android.

## Gesture XR phone-screen integration

A new `Assets/GestureXR/` integration has been added. It uses Android's official **MediaProjection** API to request permission to capture the phone screen, captures frames through an `ImageReader`, and feeds the RGBA frames into Unity textures displayed on world-space quads.

The Unity component is:

`Assets/GestureXR/GestureXRScreenWindow.cs`

It creates two floating 3D screen windows in front of the AR camera and uses the existing `HandManager` grab gesture (`gesture_class == 2`) to move them. The Android bridge lives under:

`Assets/GestureXR/Plugins/Android/`

### Setup

1. Open the project with its existing Unity version (the original project uses Unity 2021.3.3f1).
2. Open `Assets/MyScene/MyScene.unity`.
3. Create an empty GameObject named `ScreenCaptureWindow`.
4. Add `GestureXRScreenWindow` to it.
5. Assign the existing `HandManager` component to the `handManager` field.
6. Build for Android.
7. On first launch, Android will show its official screen-capture permission dialog.
8. Approve it. The captured phone display should appear on the two spatial Unity quads.

The capture is deliberately limited to 1280x720 and about 15 FPS to keep the CPU/GPU and memory cost reasonable for a phone prototype. The next performance step is replacing the CPU `ImageReader` byte-copy path with an Android OES/SurfaceTexture GPU path.

### Existing features

- Unity 2021.3.3f1
- AR Foundation 4.2.8
- ARCore XR plugin 4.2.8
- MediaPipe hand tracking and gesture recognition
- Existing VNC remote desktop support
- Spatial hand-driven object interaction
