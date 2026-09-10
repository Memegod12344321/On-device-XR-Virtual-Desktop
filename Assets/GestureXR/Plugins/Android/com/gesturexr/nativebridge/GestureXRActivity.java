package com.gesturexr.nativebridge;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import com.unity3d.player.UnityPlayerActivity;

public class GestureXRActivity extends UnityPlayerActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 4172;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void requestScreenCapture() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                ScreenCaptureService.start(this, resultCode, data);
                UnityPlayerBridge.send("ScreenCaptureWindow", "OnCaptureStarted", "");
            } else {
                UnityPlayerBridge.send("ScreenCaptureWindow", "OnCaptureDenied", "");
            }
        }
    }
}
