package com.gesturexr.nativebridge;

import android.app.Activity;
import android.content.Intent;

public final class ScreenCaptureBridge {
    private ScreenCaptureBridge() {}

    public static void request(Activity activity) {
        if (activity instanceof GestureXRActivity) {
            ((GestureXRActivity) activity).requestScreenCapture();
        }
    }

    public static byte[] getLatestFrame() {
        return ScreenCaptureService.getLatestFrameStatic();
    }

    public static int getWidth() {
        return ScreenCaptureService.getWidthStatic();
    }

    public static int getHeight() {
        return ScreenCaptureService.getHeightStatic();
    }

    public static void stop(Activity activity) {
        Intent intent = new Intent(activity, ScreenCaptureService.class)
                .setAction("com.gesturexr.nativebridge.STOP");
        activity.startService(intent);
    }
}
