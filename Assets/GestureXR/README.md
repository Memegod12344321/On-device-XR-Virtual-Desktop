# Gesture XR integration

This folder contains the native Android screen-capture bridge used by the Gesture XR workspace.

The bridge requests Android MediaProjection permission and exposes the capture lifecycle to Unity. The Unity side can use the returned Surface/texture path to render a phone-screen window in an AR Foundation scene while the existing MediaPipe hand-tracking system handles gesture interaction.
