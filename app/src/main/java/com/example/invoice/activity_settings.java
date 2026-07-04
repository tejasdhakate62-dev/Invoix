package com.example.invoice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class activity_settings extends AppCompatActivity {

    private LinearLayout llLogout, llEditProfile;
    private Button btnSaveSettings;
    private RadioGroup rgTheme;
    private RadioButton rbThemeSystem, rbThemeLight, rbThemeDark;
    private android.widget.AutoCompleteTextView actvCurrency;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        llLogout = findViewById(R.id.llLogout);
        llEditProfile = findViewById(R.id.llEditProfile);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        rgTheme = findViewById(R.id.rgTheme);
        rbThemeSystem = findViewById(R.id.rbThemeSystem);
        rbThemeLight = findViewById(R.id.rbThemeLight);
        rbThemeDark = findViewById(R.id.rbThemeDark);
        actvCurrency = findViewById(R.id.actvCurrency);
        String[] currencyOptions = getResources().getStringArray(R.array.currency_options);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                currencyOptions
        );
        if (actvCurrency != null) {
            actvCurrency.setAdapter(adapter);
        }

        prefs = getSharedPreferences("InvoixPrefs", MODE_PRIVATE);

        // Load saved preferences
        loadSavedSettings();

        // Action: Theme selection changes - apply instantly with loop guard
        if (rgTheme != null) {
            rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
                int selectedThemeMode = 0;
                if (checkedId == R.id.rbThemeLight) {
                    selectedThemeMode = 1;
                } else if (checkedId == R.id.rbThemeDark) {
                    selectedThemeMode = 2;
                } else {
                    selectedThemeMode = 0;
                }

                int currentSavedMode = prefs.getInt("themeMode", 0);
                if (selectedThemeMode != currentSavedMode) {
                    prefs.edit().putInt("themeMode", selectedThemeMode).apply();
                    if (selectedThemeMode == 1) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (selectedThemeMode == 2) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                }
            });
        }

        // Action: Click Edit Business Profile -> Load registration screen in editing context
        if (llEditProfile != null) {
            llEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(activity_settings.this, activity_business_registration.class);
                intent.putExtra("IS_EDIT_MODE", true);
                startActivity(intent);
            });
        }

        // Action: Save settings changes and return back
        if (btnSaveSettings != null) {
            btnSaveSettings.setOnClickListener(v -> {
                saveSettings();
                Toast.makeText(activity_settings.this, "Settings Saved!", Toast.LENGTH_SHORT).show();
                finish(); // Returns user back to the dashboard layout screen
            });
        }

        // Action: Sign Out link sequence -> Drops back down to the login wall
        if (llLogout != null) {
            llLogout.setOnClickListener(v -> {
                FirebaseHelper.getInstance().logout();
                Intent intent = new Intent(activity_settings.this, activity_login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadSavedSettings() {
        int themeMode = prefs.getInt("themeMode", 0); // 0 = System, 1 = Light, 2 = Dark
        if (themeMode == 1) {
            if (rbThemeLight != null) rbThemeLight.setChecked(true);
        } else if (themeMode == 2) {
            if (rbThemeDark != null) rbThemeDark.setChecked(true);
        } else {
            if (rbThemeSystem != null) rbThemeSystem.setChecked(true);
        }

        int currencyPos = prefs.getInt("currencyPosition", 0); // 0 = USD ($), 1 = INR (₹), 2 = EUR (€), 3 = GBP (£)
        String[] currencyOptions = getResources().getStringArray(R.array.currency_options);
        if (actvCurrency != null && currencyPos < currencyOptions.length) {
            actvCurrency.setText(currencyOptions[currencyPos], false);
        }
    }

    private void saveSettings() {
        int selectedCurrencyPos = 0;
        if (actvCurrency != null) {
            String selectedText = actvCurrency.getText().toString();
            String[] currencyOptions = getResources().getStringArray(R.array.currency_options);
            for (int i = 0; i < currencyOptions.length; i++) {
                if (currencyOptions[i].equals(selectedText)) {
                    selectedCurrencyPos = i;
                    break;
                }
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("currencyPosition", selectedCurrencyPos);
        editor.apply();
    }
}