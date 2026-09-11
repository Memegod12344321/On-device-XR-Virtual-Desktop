package com.memegod.aidebate;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.TextView;
public class MainActivity extends Activity {
  public void onCreate(Bundle b) { super.onCreate(b); TextView v=new TextView(this); v.setText("AI Debate Stream Bot\n\nDevice disconnected\n\nWaiting for YouTube chat..."); v.setTextColor(Color.WHITE); v.setTextSize(20); v.setPadding(32,32,32,32); v.setBackgroundColor(Color.rgb(12,12,16)); setContentView(v); }
}
