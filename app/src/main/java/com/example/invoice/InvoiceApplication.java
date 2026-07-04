package com.example.invoice;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class InvoiceApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Restore Theme Preference globally at application start
        SharedPreferences prefs = getSharedPreferences("InvoixPrefs", MODE_PRIVATE);
        int themeMode = prefs.getInt("themeMode", 0); // 0 = System, 1 = Light, 2 = Dark
        if (themeMode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeMode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
