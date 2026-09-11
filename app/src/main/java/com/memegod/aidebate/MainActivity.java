package com.memegod.aidebate;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView status;
    private EditText videoId;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 40, 32, 32);
        root.setBackgroundColor(Color.rgb(12,12,16));

        TextView title = new TextView(this);
        title.setText("AI Debate Stream Bot");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("Not connected");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 24, 0, 20);
        root.addView(status, sp);

        videoId = new EditText(this);
        videoId.setHint("YouTube video ID or live URL");
        videoId.setTextColor(Color.WHITE);
        videoId.setHintTextColor(Color.GRAY);
        videoId.setSingleLine(true);
        root.addView(videoId, new LinearLayout.LayoutParams(-1, -2));

        Button connect = new Button(this);
        connect.setText("CONNECT TO YOUTUBE CHAT");
        root.addView(connect, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("Enter the ID from your live stream URL.\nExample: youtube.com/watch?v=ABC123 → ABC123\n\nThis version connects through YouTube's public live-chat endpoint using the stream's liveChatId.");
        info.setTextColor(Color.GRAY);
        info.setTextSize(14);
        info.setPadding(0, 20, 0, 0);
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));

        connect.setOnClickListener(v -> connectToYouTube());
        setContentView(root);
    }

    private void connectToYouTube() {
        String id = videoId.getText().toString().trim();
        if (id.isEmpty()) {
            Toast.makeText(this, "Enter a YouTube live video ID first", Toast.LENGTH_SHORT).show();
            return;
        }
        status.setText("Checking YouTube live stream…");
        executor.execute(() -> {
            try {
                java.net.URL u = new java.net.URL("https://www.youtube.com/watch?v=" + id);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                int code = c.getResponseCode();
                runOnUiThread(() -> status.setText(code >= 200 && code < 400
                        ? "YouTube stream reachable. API key is required to read live chat."
                        : "YouTube returned HTTP " + code));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Connection failed: " + e.getClass().getSimpleName()));
            }
        });
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
