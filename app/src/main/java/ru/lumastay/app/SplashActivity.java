package ru.lumastay.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

public final class SplashActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_splash);
        View content = findViewById(R.id.splash_content);
        content.setAlpha(0f);
        content.setScaleX(.86f);
        content.setScaleY(.86f);
        boolean animations = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
        content.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(animations ? 650 : 0).start();
        content.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, animations ? 900 : 100);
    }
}
