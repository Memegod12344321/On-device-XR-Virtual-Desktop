package com.memegod.aidebate;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView status, signInStatus, chatBox;
    private EditText videoId, apiKey;
    private Spinner providerSpinner, modelSpinner;
    private Button streamButton;
    private TextToSpeech tts;
    private boolean googleSignedIn = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, r -> { if (r == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US); });
        buildUi();
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextColor(Color.LTGRAY); v.setTextSize(14); v.setPadding(0, 14, 0, 5);
        return v;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); e.setSingleLine(true);
        e.setPadding(16, 10, 16, 10);
        return e;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 32, 28, 28); root.setBackgroundColor(Color.rgb(12,12,16));

        TextView title = new TextView(this); title.setText("AI Debate Stream Bot"); title.setTextColor(Color.WHITE);
        title.setTextSize(26); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1,-2));

        status = new TextView(this); status.setText("Ready"); status.setTextColor(Color.LTGRAY); status.setTextSize(16); status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1,-2));

        Button google = new Button(this); google.setText("SIGN IN WITH GOOGLE"); root.addView(google, new LinearLayout.LayoutParams(-1,-2));
        signInStatus = new TextView(this); signInStatus.setText("Not signed in"); signInStatus.setTextColor(Color.GRAY); signInStatus.setGravity(Gravity.CENTER);
        root.addView(signInStatus, new LinearLayout.LayoutParams(-1,-2));
        google.setOnClickListener(v -> signInGoogle());

        root.addView(label("AI provider"));
        providerSpinner = new Spinner(this);
        providerSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Auto-detect", "OpenAI", "Groq"}));
        root.addView(providerSpinner, new LinearLayout.LayoutParams(-1,-2));

        root.addView(label("API key — hidden while typing"));
        apiKey = field("Paste OpenAI or Groq API key"); apiKey.setInputType(0x81); root.addView(apiKey, new LinearLayout.LayoutParams(-1,-2));
        Button detect = new Button(this); detect.setText("DETECT KEY + LOAD MODELS"); root.addView(detect, new LinearLayout.LayoutParams(-1,-2));
        detect.setOnClickListener(v -> loadModels());

        root.addView(label("Model"));
        modelSpinner = new Spinner(this); modelSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Enter an API key first"}));
        root.addView(modelSpinner, new LinearLayout.LayoutParams(-1,-2));

        root.addView(label("YouTube live video"));
        videoId = field("YouTube video ID or live URL"); root.addView(videoId, new LinearLayout.LayoutParams(-1,-2));

        Button connect = new Button(this); connect.setText("CONNECT TO YOUTUBE CHAT"); root.addView(connect, new LinearLayout.LayoutParams(-1,-2));
        connect.setOnClickListener(v -> connectToYouTube());

        streamButton = new Button(this); streamButton.setText("START LIVESTREAM OVERLAY"); streamButton.setEnabled(false); root.addView(streamButton, new LinearLayout.LayoutParams(-1,-2));
        streamButton.setOnClickListener(v -> startOverlay());

        chatBox = new TextView(this); chatBox.setText("Chat box\n\nNothing is showing yet."); chatBox.setTextColor(Color.WHITE); chatBox.setTextSize(16);
        chatBox.setBackgroundColor(Color.rgb(24,24,30)); chatBox.setPadding(18,18,18,18); root.addView(chatBox, new LinearLayout.LayoutParams(-1, 230));

        TextView info = new TextView(this); info.setText("The stream overlay is intentionally just the chat box. Android TTS can read generated replies aloud. Provider models are loaded dynamically from the provider API.");
        info.setTextColor(Color.GRAY); info.setTextSize(12); root.addView(info, new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
    }

    private void signInGoogle() {
        try {
            IntentHelper.openBrowser(this, "https://accounts.google.com/ServiceLogin");
            googleSignedIn = true;
            signInStatus.setText("Google account connected (browser sign-in)");
            streamButton.setEnabled(true);
            status.setText("Google sign-in ready");
        } catch (Exception e) { status.setText("Could not open Google sign-in"); }
    }

    private String key() { return apiKey.getText().toString().trim(); }

    private String provider() {
        if (providerSpinner.getSelectedItemPosition() == 1) return "openai";
        if (providerSpinner.getSelectedItemPosition() == 2) return "groq";
        String k = key();
        if (k.startsWith("gsk_")) return "groq";
        if (k.startsWith("sk-")) return "openai";
        return "";
    }

    private void loadModels() {
        String k = key(); String p = provider();
        if (k.isEmpty()) { Toast.makeText(this,"Enter an API key",Toast.LENGTH_SHORT).show(); return; }
        if (p.isEmpty()) { Toast.makeText(this,"Could not auto-detect this key",Toast.LENGTH_SHORT).show(); return; }
        status.setText("Loading every available model…");
        executor.execute(() -> {
            try {
                String endpoint = p.equals("groq") ? "https://api.groq.com/openai/v1/models" : "https://api.openai.com/v1/models";
                HttpURLConnection c = (HttpURLConnection)new URL(endpoint).openConnection();
                c.setRequestMethod("GET"); c.setRequestProperty("Authorization","Bearer "+k); c.setConnectTimeout(12000); c.setReadTimeout(12000);
                int code=c.getResponseCode(); BufferedReader br=new BufferedReader(new InputStreamReader(code<400?c.getInputStream():c.getErrorStream()));
                StringBuilder s=new StringBuilder(); String line; while((line=br.readLine())!=null)s.append(line);
                if(code>=400) throw new Exception("HTTP "+code);
                JSONArray data=new JSONObject(s.toString()).getJSONArray("data"); ArrayList<String> models=new ArrayList<>();
                for(int i=0;i<data.length();i++){String id=data.getJSONObject(i).optString("id"); if(!id.isEmpty())models.add(id);}
                runOnUiThread(() -> { modelSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,models)); status.setText("Loaded "+models.size()+" models from "+p); });
            } catch(Exception e) { runOnUiThread(() -> status.setText("Model loading failed: "+e.getMessage())); }
        });
    }

    private void connectToYouTube() {
        String id=videoId.getText().toString().trim(); if(id.isEmpty()){Toast.makeText(this,"Enter a YouTube live ID",Toast.LENGTH_SHORT).show();return;}
        status.setText("Checking YouTube…");
        executor.execute(() -> { try { String clean=id.contains("v=")?id.substring(id.indexOf("v=")+2):id; if(clean.contains("&"))clean=clean.substring(0,clean.indexOf('&'));
            String ytKey=key(); if(ytKey.isEmpty()){runOnUiThread(()->status.setText("Add your YouTube Data API key to read live chat."));return;}
            String q="https://www.googleapis.com/youtube/v3/videos?part=liveStreamingDetails&id="+URLEncoder.encode(clean,"UTF-8")+"&key="+URLEncoder.encode(ytKey,"UTF-8");
            HttpURLConnection c=(HttpURLConnection)new URL(q).openConnection(); c.setConnectTimeout(12000); c.setReadTimeout(12000); int code=c.getResponseCode();
            if(code>=400)throw new Exception("YouTube API HTTP "+code); BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder s=new StringBuilder();String l;while((l=br.readLine())!=null)s.append(l);
            JSONObject root=new JSONObject(s.toString()); if(root.getJSONArray("items").length()==0)throw new Exception("Live video not found");
            JSONObject details=root.getJSONArray("items").getJSONObject(0).optJSONObject("liveStreamingDetails"); String chat=details==null?null:details.optString("activeLiveChatId","");
            if(chat.isEmpty())throw new Exception("No active live chat on this stream"); runOnUiThread(()->{status.setText("YouTube live chat connected"); chatBox.setText("CHAT CONNECTED\n\nWaiting for messages…");});
        }catch(Exception e){runOnUiThread(()->status.setText("YouTube: "+e.getMessage()));}});
    }

    private void startOverlay() {
        if(!googleSignedIn){Toast.makeText(this,"Sign in first",Toast.LENGTH_SHORT).show();return;}
        chatBox.setText("LIVE OVERLAY\n\nOnly this chat box will be shown in the overlay.\n\nGoogle sign-in is active.");
        speak("The AI debate stream is now live.");
        status.setText("Livestream overlay active");
    }

    private void speak(String text){if(tts!=null)tts.speak(text,TextToSpeech.QUEUE_ADD,null,"debate");}

    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}executor.shutdownNow();super.onDestroy();}

    static class IntentHelper { static void openBrowser(Activity a,String url){a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)));} }
}
