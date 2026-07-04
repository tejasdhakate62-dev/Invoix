package com.example.invoice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class activity_splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Restore Theme Preference
        SharedPreferences prefs = getSharedPreferences("InvoixPrefs", MODE_PRIVATE);
        int themeMode = prefs.getInt("themeMode", 0); // 0 = System, 1 = Light, 2 = Dark
        if (themeMode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeMode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Delay timer loop to hold splash UI view before transitioning
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (FirebaseHelper.getInstance().isUserLoggedIn()) {
                intent = new Intent(activity_splash.this, activity_dashboard.class);
            } else {
                intent = new Intent(activity_splash.this, activity_login.class);
            }
            startActivity(intent);
            finish();
        }, 2500); // 2.5 seconds
    }
}