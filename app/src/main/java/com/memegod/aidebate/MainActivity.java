package com.memegod.aidebate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView status, signInStatus, chatPreview;
    private EditText aiApiKey, youtubeApiKey, videoId;
    private Spinner providerSpinner, modelSpinner;
    private Button streamButton;
    private TextToSpeech tts;
    private boolean googleSignedIn = false;
    private String activeLiveChatId = "";
    private String nextPageToken = "";
    private boolean chatPolling = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, r -> {
            if (r == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
        buildUi();
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.LTGRAY);
        v.setTextSize(14);
        v.setPadding(0, 14, 0, 5);
        return v;
    }

    private EditText field(String hint, boolean secret) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.GRAY);
        e.setSingleLine(true);
        e.setPadding(16, 10, 16, 10);
        if (secret) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            e.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        return e;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(Color.rgb(12, 12, 16));

        TextView title = new TextView(this);
        title.setText("AI Debate Stream Bot");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("Ready");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        Button google = new Button(this);
        google.setText("SIGN IN WITH GOOGLE");
        root.addView(google);
        signInStatus = new TextView(this);
        signInStatus.setText("Not signed in");
        signInStatus.setTextColor(Color.GRAY);
        signInStatus.setGravity(Gravity.CENTER);
        root.addView(signInStatus);
        google.setOnClickListener(v -> signInGoogle());

        root.addView(label("AI provider"));
        providerSpinner = new Spinner(this);
        providerSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Auto-detect", "OpenAI", "Groq"}));
        root.addView(providerSpinner);

        root.addView(label("AI API key — every character is masked as •"));
        aiApiKey = field("Paste your OpenAI or Groq API key", true);
        root.addView(aiApiKey);

        Button detect = new Button(this);
        detect.setText("DETECT PROVIDER + LOAD ALL MODELS");
        root.addView(detect);
        detect.setOnClickListener(v -> loadModels());

        root.addView(label("Model — loaded from the provider, not hardcoded"));
        modelSpinner = new Spinner(this);
        modelSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Enter an API key first"}));
        root.addView(modelSpinner);

        root.addView(label("YouTube live video ID or URL"));
        videoId = field("https://youtube.com/watch?v=...", false);
        root.addView(videoId);

        root.addView(label("YouTube Data API key — separate from AI key"));
        youtubeApiKey = field("Paste YouTube Data API key", true);
        root.addView(youtubeApiKey);

        Button connect = new Button(this);
        connect.setText("CONNECT TO YOUTUBE LIVE CHAT");
        root.addView(connect);
        connect.setOnClickListener(v -> connectToYouTube());

        streamButton = new Button(this);
        streamButton.setText("START STREAM OVERLAY");
        streamButton.setEnabled(false);
        root.addView(streamButton);
        streamButton.setOnClickListener(v -> startOverlay());

        chatPreview = new TextView(this);
        chatPreview.setText("CHAT PREVIEW\n\nSign in, connect a live chat, then start the overlay.");
        chatPreview.setTextColor(Color.WHITE);
        chatPreview.setTextSize(16);
        chatPreview.setBackgroundColor(Color.rgb(24, 24, 30));
        chatPreview.setPadding(18, 18, 18, 18);
        root.addView(chatPreview, new LinearLayout.LayoutParams(-1, 260));

        TextView info = new TextView(this);
        info.setText("OpenAI and Groq models are fetched from each provider's live /models endpoint. TTS uses Android's built-in speech engine. The stream overlay Activity contains only the chat box.");
        info.setTextColor(Color.GRAY);
        info.setTextSize(12);
        root.addView(info);

        setContentView(root);
    }

    private void signInGoogle() {
        try {
            // Opens Google's official sign-in page. A production YouTube OAuth flow requires a
            // Google Cloud OAuth client ID and redirect configuration; no secret is embedded here.
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://accounts.google.com/ServiceLogin?continue=https://www.youtube.com/")));
            googleSignedIn = true;
            signInStatus.setText("Google sign-in opened — stream button unlocked");
            streamButton.setEnabled(true);
            status.setText("Google account step opened");
        } catch (Exception e) {
            status.setText("Could not open Google sign-in");
        }
    }

    private String key() { return aiApiKey.getText().toString().trim(); }

    private String provider() {
        int selected = providerSpinner.getSelectedItemPosition();
        if (selected == 1) return "openai";
        if (selected == 2) return "groq";
        String k = key().toLowerCase(Locale.US);
        if (k.startsWith("gsk_")) return "groq";
        if (k.startsWith("sk-")) return "openai";
        return "";
    }

    private void loadModels() {
        String k = key();
        String p = provider();
        if (k.isEmpty()) {
            Toast.makeText(this, "Enter an API key first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (p.isEmpty()) {
            Toast.makeText(this, "Unknown key format — choose OpenAI or Groq", Toast.LENGTH_SHORT).show();
            return;
        }
        status.setText("Loading every model available to this key…");
        executor.execute(() -> {
            try {
                String endpoint = p.equals("groq")
                        ? "https://api.groq.com/openai/v1/models"
                        : "https://api.openai.com/v1/models";
                JSONObject response = getJson(endpoint, k);
                JSONArray data = response.optJSONArray("data");
                if (data == null) throw new Exception("Provider returned no model list");

                ArrayList<String> models = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (int i = 0; i < data.length(); i++) {
                    String id = data.getJSONObject(i).optString("id");
                    if (!id.isEmpty() && seen.add(id)) models.add(id);
                }
                models.sort(Comparator.naturalOrder());

                mainHandler.post(() -> {
                    modelSpinner.setAdapter(new ArrayAdapter<String>(this,
                            android.R.layout.simple_spinner_dropdown_item, models));
                    status.setText("Loaded " + models.size() + " available " + p.toUpperCase(Locale.US) + " models");
                });
            } catch (Exception e) {
                mainHandler.post(() -> status.setText("Model loading failed: " + e.getMessage()));
            }
        });
    }

    private JSONObject getJson(String endpoint, String bearer) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + bearer);
        c.setRequestProperty("Accept", "application/json");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        int code = c.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(code < 400 ? c.getInputStream() : c.getErrorStream()));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) out.append(line);
        br.close();
        if (code >= 400) throw new Exception("HTTP " + code);
        return new JSONObject(out.toString());
    }

    private void connectToYouTube() {
        String id = extractVideoId(videoId.getText().toString().trim());
        String ytKey = youtubeApiKey.getText().toString().trim();
        if (id.isEmpty()) {
            Toast.makeText(this, "Enter a YouTube live video ID or URL", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ytKey.isEmpty()) {
            Toast.makeText(this, "Enter a YouTube Data API key", Toast.LENGTH_SHORT).show();
            return;
        }
        status.setText("Connecting to live chat…");
        executor.execute(() -> {
            try {
                String url = "https://www.googleapis.com/youtube/v3/videos?part=liveStreamingDetails&id="
                        + URLEncoder.encode(id, "UTF-8") + "&key=" + URLEncoder.encode(ytKey, "UTF-8");
                JSONObject root = getPlainJson(url);
                JSONArray items = root.optJSONArray("items");
                if (items == null || items.length() == 0) throw new Exception("Live video not found");
                JSONObject details = items.getJSONObject(0).optJSONObject("liveStreamingDetails");
                String chatId = details == null ? "" : details.optString("activeLiveChatId", "");
                if (chatId.isEmpty()) throw new Exception("No active live chat on this stream");
                activeLiveChatId = chatId;
                nextPageToken = "";
                chatPolling = true;
                mainHandler.post(() -> {
                    status.setText("YouTube live chat connected");
                    chatPreview.setText("CHAT CONNECTED\n\nWaiting for messages…");
                });
                pollChat();
            } catch (Exception e) {
                mainHandler.post(() -> status.setText("YouTube: " + e.getMessage()));
            }
        });
    }

    private JSONObject getPlainJson(String endpoint) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        int code = c.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(code < 400 ? c.getInputStream() : c.getErrorStream()));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) out.append(line);
        br.close();
        if (code >= 400) throw new Exception("HTTP " + code);
        return new JSONObject(out.toString());
    }

    private String extractVideoId(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.contains("v=")) s = s.substring(s.indexOf("v=") + 2);
        else if (s.contains("youtu.be/")) s = s.substring(s.lastIndexOf("/") + 1);
        if (s.contains("&")) s = s.substring(0, s.indexOf('&'));
        if (s.contains("?")) s = s.substring(0, s.indexOf('?'));
        return s.trim();
    }

    private void pollChat() {
        if (!chatPolling || activeLiveChatId.isEmpty()) return;
        final String chatId = activeLiveChatId;
        final String page = nextPageToken;
        final String ytKey = youtubeApiKey.getText().toString().trim();
        executor.execute(() -> {
            try {
                String url = "https://www.googleapis.com/youtube/v3/liveChat/messages?part=snippet,authorDetails&liveChatId="
                        + URLEncoder.encode(chatId, "UTF-8") + "&maxResults=50&key="
                        + URLEncoder.encode(ytKey, "UTF-8");
                if (!page.isEmpty()) url += "&pageToken=" + URLEncoder.encode(page, "UTF-8");
                JSONObject root = getPlainJson(url);
                nextPageToken = root.optString("nextPageToken", "");
                JSONArray items = root.optJSONArray("items");
                if (items != null && items.length() > 0) {
                    ArrayList<String> lines = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        JSONObject author = item.optJSONObject("authorDetails");
                        JSONObject snippet = item.optJSONObject("snippet");
                        String name = author == null ? "Viewer" : author.optString("displayName", "Viewer");
                        String text = snippet == null ? "" : snippet.optString("displayMessage", "");
                        if (!text.isEmpty()) lines.add(name + ": " + text);
                    }
                    if (!lines.isEmpty()) {
                        mainHandler.post(() -> {
                            chatPreview.setText(joinLines(lines));
                            speak(lines.get(lines.size() - 1));
                        });
                    }
                }
            } catch (Exception ignored) {
                // Keep polling; transient YouTube API errors should not kill the chat overlay.
            }
            mainHandler.postDelayed(this::pollChat, 2500);
        });
    }

    private String joinLines(ArrayList<String> lines) {
        StringBuilder b = new StringBuilder("LIVE CHAT\n\n");
        int start = Math.max(0, lines.size() - 12);
        for (int i = start; i < lines.size(); i++) b.append(lines.get(i)).append('\n');
        return b.toString();
    }

    private void startOverlay() {
        if (!googleSignedIn) {
            Toast.makeText(this, "Sign in with Google first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, OverlayActivity.class);
        i.putExtra("chat", chatPreview.getText().toString());
        startActivity(i);
        status.setText("Overlay opened — only the chat box is shown");
    }

    private void speak(String text) {
        if (tts != null && !text.isEmpty()) tts.speak(text, TextToSpeech.QUEUE_ADD, null, "debate-" + System.currentTimeMillis());
    }

    @Override protected void onDestroy() {
        chatPolling = false;
        if (tts != null) { tts.stop(); tts.shutdown(); }
        executor.shutdownNow();
        super.onDestroy();
    }
}
