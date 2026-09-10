package com.gesturexr.nativebridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String ACTION_START = "com.gesturexr.nativebridge.START";
    private static final String ACTION_STOP = "com.gesturexr.nativebridge.STOP";
    private static final String CHANNEL = "gesture_xr_capture";
    private static final int NOTIFICATION_ID = 4173;
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;

    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private int width;
    private int height;
    private byte[] latestRgba;
    private final Object frameLock = new Object();

    public static void start(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(context, ScreenCaptureService.class)
                .setAction(ACTION_START)
                .putExtra("resultCode", resultCode)
                .putExtra("resultData", data);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Gesture XR screen capture", NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopCapture();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_START.equals(intent.getAction())) {
            Notification notification = new Notification.Builder(this, CHANNEL)
                    .setContentTitle("Gesture XR")
                    .setContentText("Phone screen is being shared to a spatial window")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
            startCapture(intent);
        }
        return START_NOT_STICKY;
    }

    private void startCapture(Intent intent) {
        stopCapture();
        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data = intent.getParcelableExtra("resultData");
        if (data == null) return;

        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = manager.getMediaProjection(resultCode, data);
        if (projection == null) return;

        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() {
                stopCapture();
                UnityPlayerBridge.send("ScreenCaptureWindow", "OnCaptureStopped", "");
            }
        }, null);

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        float scale = Math.min(1f, Math.min((float) MAX_WIDTH / screenW, (float) MAX_HEIGHT / screenH));
        width = Math.max(2, ((int) (screenW * scale)) & ~1);
        height = Math.max(2, ((int) (screenH * scale)) & ~1);

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(r -> {
            Image image = null;
            try {
                image = r.acquireLatestImage();
                if (image == null) return;
                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                int rowPadding = rowStride - pixelStride * width;
                byte[] packed = new byte[width * height * 4];
                byte[] row = new byte[rowStride];
                for (int y = 0; y < height; y++) {
                    int read = Math.min(rowStride, buffer.remaining());
                    buffer.get(row, 0, read);
                    int copy = Math.min(width * 4, read);
                    System.arraycopy(row, 0, packed, y * width * 4, copy);
                }
                synchronized (frameLock) {
                    latestRgba = packed;
                }
            } catch (Throwable ignored) {
            } finally {
                if (image != null) image.close();
            }
        }, null);

        display = projection.createVirtualDisplay(
                "GestureXR",
                width,
                height,
                getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(),
                null,
                null);
    }

    public byte[] getLatestFrame() {
        synchronized (frameLock) {
            return latestRgba == null ? null : latestRgba.clone();
        }
    }

    public int getWidthValue() { return width; }
    public int getHeightValue() { return height; }

    private void stopCapture() {
        if (display != null) { display.release(); display = null; }
        if (reader != null) { reader.close(); reader = null; }
        if (projection != null) { projection.stop(); projection = null; }
        synchronized (frameLock) { latestRgba = null; }
    }

    @Override public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
