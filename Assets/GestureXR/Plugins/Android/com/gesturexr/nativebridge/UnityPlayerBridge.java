package com.gesturexr.nativebridge;

import com.unity3d.player.UnityPlayer;

final class UnityPlayerBridge {
    static void send(String objectName, String method, String message) {
        try {
            UnityPlayer.UnitySendMessage(objectName, method, message);
        } catch (Throwable ignored) {
        }
    }
}
