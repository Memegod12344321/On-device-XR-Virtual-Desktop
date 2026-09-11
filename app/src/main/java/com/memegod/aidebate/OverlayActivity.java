package com.memegod.aidebate;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class OverlayActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(1024, 1024);
        getWindow().getDecorView().setSystemUiVisibility(5894);

        TextView chat = new TextView(this);
        chat.setText(getIntent().getStringExtra("chat"));
        chat.setTextColor(Color.WHITE);
        chat.setTextSize(18);
        chat.setGravity(Gravity.BOTTOM | Gravity.START);
        chat.setPadding(24, 24, 24, 24);
        chat.setBackgroundColor(Color.rgb(24, 24, 30));
        setContentView(chat);
    }
}
