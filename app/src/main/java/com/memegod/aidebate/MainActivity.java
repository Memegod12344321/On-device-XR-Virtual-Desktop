package com.memegod.aidebate;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private TextView status, signInStatus, chatPreview;
    private EditText aiApiKey, youtubeApiKey, videoId;
    private Spinner providerSpinner, modelSpinner;
    private Button streamButton;
    private TextToSpeech tts;
    private boolean googleSignedIn = false;
    private boolean overlayOpen = false;
    private boolean chatPolling = false;
    private String activeLiveChatId = "";
    private String nextPageToken = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
        buildUi();
        loadSavedSettings();
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

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 24, 28, 28);
        root.setBackgroundColor(Color.rgb(12, 12, 16));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("AI Debate Stream Bot");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("Ready");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 8, 0, 8);
        root.addView(status);

        Button google = button("SIGN IN WITH GOOGLE");
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

        root.addView(label("AI API key — masked as •••••••••"));
        aiApiKey = field("OpenAI or Groq API key", true);
        root.addView(aiApiKey);

        LinearLayout keyRow = new LinearLayout(this);
        keyRow.setOrientation(LinearLayout.HORIZONTAL);
        Button detect = button("DETECT + LOAD MODELS");
        Button save = button("SAVE");
        keyRow.addView(detect, new LinearLayout.LayoutParams(0, -2, 1));
        keyRow.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(keyRow);
        detect.setOnClickListener(v -> loadModels());
        save.setOnClickListener(v -> saveSettings());

        root.addView(label("Available model"));
        modelSpinner = new Spinner(this);
        modelSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Enter API key and press DETECT + LOAD MODELS"}));
        root.addView(modelSpinner);
        Button refreshModels = button("REFRESH MODELS");
        root.addView(refreshModels);
        refreshModels.setOnClickListener(v -> loadModels());

        LinearLayout ttsRow = new LinearLayout(this);
        ttsRow.setOrientation(LinearLayout.HORIZONTAL);
        Button testTts = button("TEST TTS");
        Button stopTts = button("STOP TTS");
        ttsRow.addView(testTts, new LinearLayout.LayoutParams(0, -2, 1));
        ttsRow.addView(stopTts, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(ttsRow);
        testTts.setOnClickListener(v -> speak("This is the AI debate stream text to speech test."));
        stopTts.setOnClickListener(v -> { if (tts != null) tts.stop(); status.setText("TTS stopped"); });

        root.addView(label("YouTube live video ID or URL"));
        videoId = field("https://youtube.com/watch?v=...", false);
        root.addView(videoId);
        root.addView(label("YouTube Data API key"));
        youtubeApiKey = field("YouTube API key", true);
        root.addView(youtubeApiKey);

        Button connect = button("CONNECT TO YOUTUBE LIVE CHAT");
        root.addView(connect);
        connect.setOnClickListener(v -> connectToYouTube());

        LinearLayout chatControls = new LinearLayout(this);
        chatControls.setOrientation(LinearLayout.HORIZONTAL);
        Button clear = button("CLEAR CHAT");
        Button stopChat = button("STOP CHAT POLLING");
        chatControls.addView(clear, new LinearLayout.LayoutParams(0, -2, 1));
        chatControls.addView(stopChat, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(chatControls);
        clear.setOnClickListener(v -> chatPreview.setText("LIVE CHAT\n\nCleared."));
        stopChat.setOnClickListener(v -> { chatPolling = false; status.setText("Chat polling stopped"); });

        streamButton = button("START LIVESTREAM OVERLAY");
        streamButton.setEnabled(false);
        root.addView(streamButton);
        streamButton.setOnClickListener(v -> startOverlay());

        Button closeOverlay = button("CLOSE OVERLAY");
        root.addView(closeOverlay);
        closeOverlay.setOnClickListener(v -> {
            overlayOpen = false;
            status.setText("Overlay can be closed with Android Back");
        });

        chatPreview = new TextView(this);
        chatPreview.setText("CHAT PREVIEW\n\nSign in, connect a live chat, then start the overlay.");
        chatPreview.setTextColor(Color.WHITE);
        chatPreview.setTextSize(16);
        chatPreview.setBackgroundColor(Color.rgb(24, 24, 30));
        chatPreview.setPadding(18, 18, 18, 18);
        root.addView(chatPreview, new LinearLayout.LayoutParams(-1, 280));

        TextView info = new TextView(this);
        info.setText("The overlay contains only the live chat box. API keys are masked and saved locally. Models are fetched from the provider's live /models endpoint. TTS uses Android's built-in engine.");
        info.setTextColor(Color.GRAY);
        info.setTextSize(12);
        info.setPadding(0, 12, 0, 0);
        root.addView(info);

        setContentView(scroll);
    }

    private void loadSavedSettings() {
        aiApiKey.setText(prefs.getString("ai_key", ""));
        youtubeApiKey.setText(prefs.getString("yt_key", ""));
        videoId.setText(prefs.getString("video", ""));
        int provider = prefs.getInt("provider", 0);
        if (providerSpinner != null) providerSpinner.setSelection(provider);
    }

    private void saveSettings() {
        prefs.edit()
                .putString("ai_key", aiApiKey.getText().toString().trim())
                .putString("yt_key", youtubeApiKey.getText().toString().trim())
                .putString("video", videoId.getText().toString().trim())
                .putInt("provider", providerSpinner.getSelectedItemPosition())
                .apply();
        status.setText("Settings saved locally");
    }

    private void signInGoogle() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.google.com/ServiceLogin?continue=https://www.youtube.com/")));
            googleSignedIn = true;
            signInStatus.setText("Google sign-in opened — livestream overlay unlocked");
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
        if (k.isEmpty()) { Toast.makeText(this, "Enter an API key first", Toast.LENGTH_SHORT).show(); return; }
        if (p.isEmpty()) { Toast.makeText(this, "Choose OpenAI or Groq", Toast.LENGTH_SHORT).show(); return; }
        status.setText("Loading all models available to this key…");
        executor.execute(() -> {
            try {
                String endpoint = p.equals("groq") ? "https://api.groq.com/openai/v1/models" : "https://api.openai.com/v1/models";
                JSONObject response = getJson(endpoint, k);
                JSONArray data = response.optJSONArray("data");
                if (data == null) throw new Exception("No model list returned");
                ArrayList<String> models = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (int i = 0; i < data.length(); i++) {
                    String id = data.getJSONObject(i).optString("id");
                    if (!id.isEmpty() && seen.add(id)) models.add(id);
                }
                models.sort(Comparator.naturalOrder());
                handler.post(() -> {
                    modelSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, models));
                    status.setText("Loaded " + models.size() + " " + p.toUpperCase(Locale.US) + " models");
                });
            } catch (Exception e) { handler.post(() -> status.setText("Model loading failed: " + e.getMessage())); }
        });
    }

    private JSONObject getJson(String endpoint, String bearer) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + bearer);
        c.setRequestProperty("Accept", "application/json");
        c.setConnectTimeout(15000); c.setReadTimeout(15000);
        return readResponse(c);
    }

    private JSONObject getPlainJson(String endpoint) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000); c.setReadTimeout(15000);
        return readResponse(c);
    }

    private JSONObject readResponse(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(code < 400 ? c.getInputStream() : c.getErrorStream()));
        StringBuilder out = new StringBuilder(); String line;
        while ((line = br.readLine()) != null) out.append(line);
        br.close();
        if (code >= 400) throw new Exception("HTTP " + code);
        return new JSONObject(out.toString());
    }

    private void connectToYouTube() {
        String id = extractVideoId(videoId.getText().toString().trim());
        String ytKey = youtubeApiKey.getText().toString().trim();
        if (id.isEmpty()) { Toast.makeText(this, "Enter a YouTube live video ID or URL", Toast.LENGTH_SHORT).show(); return; }
        if (ytKey.isEmpty()) { Toast.makeText(this, "Enter a YouTube Data API key", Toast.LENGTH_SHORT).show(); return; }
        saveSettings(); status.setText("Connecting to live chat…");
        executor.execute(() -> {
            try {
                String url = "https://www.googleapis.com/youtube/v3/videos?part=liveStreamingDetails&id=" + URLEncoder.encode(id, "UTF-8") + "&key=" + URLEncoder.encode(ytKey, "UTF-8");
                JSONObject root = getPlainJson(url);
                JSONArray items = root.optJSONArray("items");
                if (items == null || items.length() == 0) throw new Exception("Live video not found");
                JSONObject details = items.getJSONObject(0).optJSONObject("liveStreamingDetails");
                String chatId = details == null ? "" : details.optString("activeLiveChatId", "");
                if (chatId.isEmpty()) throw new Exception("No active live chat on this stream");
                activeLiveChatId = chatId; nextPageToken = ""; chatPolling = true;
                handler.post(() -> { status.setText("YouTube live chat connected"); chatPreview.setText("CHAT CONNECTED\n\nWaiting for messages…"); });
                pollChat();
            } catch (Exception e) { handler.post(() -> status.setText("YouTube: " + e.getMessage())); }
        });
    }

    private String extractVideoId(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.contains("v=")) s = s.substring(s.indexOf("v=") + 2);
        else if (s.contains("youtu.be/")) s = s.substring(s.lastIndexOf('/') + 1);
        if (s.contains("&")) s = s.substring(0, s.indexOf('&'));
        if (s.contains("?")) s = s.substring(0, s.indexOf('?'));
        return s.trim();
    }

    private void pollChat() {
        if (!chatPolling || activeLiveChatId.isEmpty()) return;
        final String chatId = activeLiveChatId, page = nextPageToken, ytKey = youtubeApiKey.getText().toString().trim();
        executor.execute(() -> {
            try {
                String url = "https://www.googleapis.com/youtube/v3/liveChat/messages?part=snippet,authorDetails&liveChatId=" + URLEncoder.encode(chatId, "UTF-8") + "&maxResults=50&key=" + URLEncoder.encode(ytKey, "UTF-8");
                if (!page.isEmpty()) url += "&pageToken=" + URLEncoder.encode(page, "UTF-8");
                JSONObject root = getPlainJson(url);
                nextPageToken = root.optString("nextPageToken", "");
                JSONArray items = root.optJSONArray("items");
                if (items != null && items.length() > 0) {
                    ArrayList<String> lines = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i), author = item.optJSONObject("authorDetails"), snippet = item.optJSONObject("snippet");
                        String name = author == null ? "Viewer" : author.optString("displayName", "Viewer");
                        String text = snippet == null ? "" : snippet.optString("displayMessage", "");
                        if (!text.isEmpty()) lines.add(name + ": " + text);
                    }
                    if (!lines.isEmpty()) handler.post(() -> { chatPreview.setText(joinLines(lines)); speak(lines.get(lines.size() - 1)); });
                }
            } catch (Exception ignored) { }
            handler.postDelayed(this::pollChat, 2500);
        });
    }

    private String joinLines(ArrayList<String> lines) {
        StringBuilder b = new StringBuilder("LIVE CHAT\n\n");
        for (String line : lines) b.append(line).append('\n');
        return b.toString();
    }

    private void startOverlay() {
        if (!googleSignedIn) { Toast.makeText(this, "Sign in with Google first", Toast.LENGTH_SHORT).show(); return; }
        Intent i = new Intent(this, OverlayActivity.class);
        i.putExtra("chat", chatPreview.getText().toString());
        startActivity(i);
        overlayOpen = true;
        status.setText("Livestream overlay opened — chat box only");
    }

    private void speak(String text) {
        if (tts != null && !text.isEmpty()) tts.speak(text, TextToSpeech.QUEUE_ADD, null, "chat-" + System.currentTimeMillis());
    }

    @Override protected void onDestroy() {
        chatPolling = false;
        if (tts != null) { tts.stop(); tts.shutdown(); }
        executor.shutdownNow();
        super.onDestroy();
    }
}
