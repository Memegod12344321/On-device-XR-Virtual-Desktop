using UnityEngine;
using UnityEngine.UI;
using System;

/// <summary>
/// Real phone-screen spatial window for Gesture XR.
/// Android MediaProjection frames are copied into a Unity Texture2D and
/// displayed on world-space quads. Existing MediaPipe HandManager gesture 2
/// (grab) can move the windows in front of the AR camera.
/// </summary>
public class GestureXRScreenWindow : MonoBehaviour
{
    [Header("Capture")]
    public bool autoRequestOnStart = true;
    public int targetFps = 15;
    public int maxWidth = 1280;
    public int maxHeight = 720;

    [Header("Spatial windows")]
    public bool createTwoWindows = true;
    public float distanceFromCamera = 1.15f;
    public Vector2 primarySize = new Vector2(0.75f, 0.43f);
    public Vector2 secondarySize = new Vector2(0.58f, 0.33f);

    [Header("Existing hand tracker")]
    public HandManager handManager;
    public float grabDistance = 900f;
    public float moveScale = 0.0016f;

    private AndroidJavaClass bridge;
    private Texture2D screenTexture;
    private int frameWidth;
    private int frameHeight;
    private float nextFrameTime;
    private readonly GameObject[] windows = new GameObject[2];
    private readonly Renderer[] renderers = new Renderer[2];
    private int grabbedWindow = -1;
    private Vector3 lastHand;
    private Camera xrCamera;

    void Start()
    {
        gameObject.name = "ScreenCaptureWindow";
        xrCamera = Camera.main;
        CreateWindows();

#if UNITY_ANDROID && !UNITY_EDITOR
        bridge = new AndroidJavaClass("com.gesturexr.nativebridge.ScreenCaptureBridge");
        if (autoRequestOnStart)
            Invoke(nameof(RequestScreenCapture), 0.5f);
#else
        Debug.Log("Gesture XR screen capture requires an Android build.");
#endif
    }

    public void RequestScreenCapture()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (bridge == null) bridge = new AndroidJavaClass("com.gesturexr.nativebridge.ScreenCaptureBridge");
        using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        using (var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
        {
            bridge.CallStatic("request", activity);
        }
#endif
    }

    public void StopScreenCapture()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (bridge == null) return;
        using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        using (var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
        {
            bridge.CallStatic("stop", activity);
        }
#endif
    }

    void Update()
    {
        UpdateFrame();
        UpdateHandGrab();
    }

    private void UpdateFrame()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (bridge == null || Time.unscaledTime < nextFrameTime) return;
        nextFrameTime = Time.unscaledTime + (1f / Mathf.Max(1, targetFps));

        try
        {
            int w = bridge.CallStatic<int>("getWidth");
            int h = bridge.CallStatic<int>("getHeight");
            if (w <= 0 || h <= 0) return;

            byte[] bytes = bridge.CallStatic<byte[]>("getLatestFrame");
            if (bytes == null || bytes.Length != w * h * 4) return;

            if (screenTexture == null || frameWidth != w || frameHeight != h)
            {
                frameWidth = w;
                frameHeight = h;
                if (screenTexture != null) Destroy(screenTexture);
                screenTexture = new Texture2D(w, h, TextureFormat.RGBA32, false, false);
                screenTexture.wrapMode = TextureWrapMode.Clamp;
                screenTexture.filterMode = FilterMode.Bilinear;
                ApplyTexture();
            }

            screenTexture.LoadRawTextureData(bytes);
            screenTexture.Apply(false, false);
            ApplyTexture();
        }
        catch (Exception e)
        {
            Debug.LogWarning("Gesture XR capture frame error: " + e.Message);
        }
#endif
    }

    private void ApplyTexture()
    {
        if (screenTexture == null) return;
        for (int i = 0; i < renderers.Length; i++)
        {
            if (renderers[i] == null) continue;
            renderers[i].material.mainTexture = screenTexture;
        }
    }

    private void CreateWindows()
    {
        if (xrCamera == null) xrCamera = Camera.main;
        if (xrCamera == null) return;

        CreateWindow(0, "Gesture XR Screen", primarySize,
            new Vector3(-0.43f, 0.08f, distanceFromCamera));

        if (createTwoWindows)
            CreateWindow(1, "Gesture XR Screen 2", secondarySize,
                new Vector3(0.38f, -0.02f, distanceFromCamera));
    }

    private void CreateWindow(int index, string title, Vector2 size, Vector3 cameraLocalPosition)
    {
        var go = GameObject.CreatePrimitive(PrimitiveType.Quad);
        go.name = title;
        go.transform.SetParent(xrCamera.transform, false);
        go.transform.localPosition = cameraLocalPosition;
        go.transform.localScale = new Vector3(size.x, size.y, 1f);
        go.transform.localRotation = Quaternion.Euler(0f, 180f, 0f);

        var renderer = go.GetComponent<Renderer>();
        renderer.material = new Material(Shader.Find("Unlit/Texture"));
        renderer.material.color = Color.white;

        windows[index] = go;
        renderers[index] = renderer;
    }

    private void UpdateHandGrab()
    {
        if (handManager == null || xrCamera == null) return;

        Vector3 hp = handManager.handpos;
        int gesture = handManager.gesture_class;
        Vector3 screen = new Vector3((1f - hp.x) * Screen.width, (1f - hp.y) * Screen.height, 1f);

        int nearest = NearestWindow(screen);
        if (gesture == 2 && nearest >= 0)
        {
            if (grabbedWindow < 0)
            {
                grabbedWindow = nearest;
                lastHand = screen;
            }
            else
            {
                Vector3 delta = screen - lastHand;
                Transform t = windows[grabbedWindow].transform;
                Vector3 p = t.position + xrCamera.transform.right * (delta.x * moveScale)
                                        + xrCamera.transform.up * (delta.y * moveScale);
                t.position = Vector3.Lerp(t.position, p, 0.8f);
                lastHand = screen;
            }
        }
        else
        {
            grabbedWindow = -1;
        }
    }

    private int NearestWindow(Vector3 screen)
    {
        int result = -1;
        float best = float.MaxValue;
        for (int i = 0; i < windows.Length; i++)
        {
            if (windows[i] == null) continue;
            Vector3 s = xrCamera.WorldToScreenPoint(windows[i].transform.position);
            float d = Vector2.Distance(new Vector2(screen.x, screen.y), new Vector2(s.x, s.y));
            if (d < best && d < grabDistance)
            {
                best = d;
                result = i;
            }
        }
        return result;
    }

    public void OnCaptureStarted(string ignored)
    {
        Debug.Log("Gesture XR: MediaProjection screen capture started.");
    }

    public void OnCaptureDenied(string ignored)
    {
        Debug.LogWarning("Gesture XR: screen capture permission denied.");
    }

    public void OnCaptureStopped(string ignored)
    {
        Debug.Log("Gesture XR: MediaProjection stopped.");
    }

    void OnDestroy()
    {
        StopScreenCapture();
        if (screenTexture != null) Destroy(screenTexture);
        for (int i = 0; i < windows.Length; i++)
            if (windows[i] != null) Destroy(windows[i]);
    }
}
