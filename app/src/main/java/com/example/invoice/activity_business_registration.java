package com.example.invoice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;

import java.io.ByteArrayOutputStream;

public class activity_business_registration extends AppCompatActivity {

    private ImageView ivShopLogo;
    private Button btnUploadLogo;
    private String logoBase64String = "";
    private ActivityResultLauncher<Intent> pickLogoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_business_registration);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Get mode configuration
        boolean isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        ivShopLogo = findViewById(R.id.ivShopLogo);
        btnUploadLogo = findViewById(R.id.btnUploadLogo);
        EditText etBusinessName = findViewById(R.id.etBusinessName);
        EditText etTaxId = findViewById(R.id.etTaxId);
        EditText etBusinessPhone = findViewById(R.id.etBusinessPhone);
        EditText etBusinessAddress = findViewById(R.id.etBusinessAddress);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button btnSaveProfile = findViewById(R.id.btnSaveProfile);

        final String[] currentName = {""};
        final String[] currentEmail = {""};

        // Register visual media/gallery picker launcher
        pickLogoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                                if (ivShopLogo != null) {
                                    ivShopLogo.setImageBitmap(bitmap);
                                }
                                
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 80, baos);
                                byte[] bytes = baos.toByteArray();
                                logoBase64String = Base64.encodeToString(bytes, Base64.DEFAULT);
                            } catch (Exception e) {
                                Toast.makeText(this, "Failed to load logo image.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        // Click handlers for logo picker
        if (btnUploadLogo != null) {
            btnUploadLogo.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                pickLogoLauncher.launch(intent);
            });
        }
        if (ivShopLogo != null) {
            ivShopLogo.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                pickLogoLauncher.launch(intent);
            });
        }

        if (isEditMode) {
            // Load local cached profile first for instant pre-population
            android.content.SharedPreferences prefs = getSharedPreferences("InvoixPrefs", MODE_PRIVATE);
            currentName[0] = prefs.getString("businessOwnerName", "");
            currentEmail[0] = prefs.getString("businessOwnerEmail", "");
            etBusinessName.setText(prefs.getString("businessName", ""));
            etTaxId.setText(prefs.getString("businessTaxId", ""));
            etBusinessPhone.setText(prefs.getString("businessPhone", ""));
            etBusinessAddress.setText(prefs.getString("businessAddress", ""));

            String cachedLogo = prefs.getString("businessLogoBase64", "");
            if (!cachedLogo.isEmpty()) {
                logoBase64String = cachedLogo;
                try {
                    byte[] decoded = Base64.decode(cachedLogo, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (ivShopLogo != null) ivShopLogo.setImageBitmap(bitmap);
                } catch (Exception ignored) {}
            }

            // Load existing profile details from server
            FirebaseHelper.getInstance().getUserProfile(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    currentName[0] = doc.getString("name");
                    currentEmail[0] = doc.getString("email");
                    String bizName = doc.getString("businessName");
                    String tax = doc.getString("taxId");
                    String phone = doc.getString("businessPhone");
                    String addr = doc.getString("businessAddress");
                    String logo = doc.getString("logoBase64");

                    etBusinessName.setText(bizName != null ? bizName : "");
                    etTaxId.setText(tax != null ? tax : "");
                    etBusinessPhone.setText(phone != null ? phone : "");
                    etBusinessAddress.setText(addr != null ? addr : "");

                    if (logo != null && !logo.isEmpty()) {
                        logoBase64String = logo;
                        try {
                            byte[] decoded = Base64.decode(logo, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            if (ivShopLogo != null) ivShopLogo.setImageBitmap(bitmap);
                        } catch (Exception ignored) {}
                    }

                    // Sync local cache
                    LocalDatabaseHelper.saveLocalProfile(activity_business_registration.this, currentName[0], currentEmail[0], bizName, tax, phone, addr, logoBase64String);
                }
            });
            btnSaveProfile.setText("Update Profile");
        }

        // Get credentials passed from signup screen
        String name = getIntent().getStringExtra("SIGNUP_NAME");
        String email = getIntent().getStringExtra("SIGNUP_EMAIL");
        String password = getIntent().getStringExtra("SIGNUP_PASSWORD");
        boolean isGoogleSignup = getIntent().getBooleanExtra("IS_GOOGLE_SIGNUP", false);

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> {
                String businessName = etBusinessName != null ? etBusinessName.getText().toString().trim() : "";
                String taxId = etTaxId != null ? etTaxId.getText().toString().trim() : "";
                String businessPhone = etBusinessPhone != null ? etBusinessPhone.getText().toString().trim() : "";
                String businessAddress = etBusinessAddress != null ? etBusinessAddress.getText().toString().trim() : "";

                if (businessName.isEmpty()) {
                    Toast.makeText(activity_business_registration.this, "Please enter your business name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (businessPhone.isEmpty()) {
                    Toast.makeText(activity_business_registration.this, "Please enter your business phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (businessAddress.isEmpty()) {
                    Toast.makeText(activity_business_registration.this, "Please enter your business address", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSaveProfile.setEnabled(false);
                btnSaveProfile.setText(isEditMode ? "Updating profile..." : "Saving profile...");

                // Save profile details locally first
                LocalDatabaseHelper.saveLocalProfile(activity_business_registration.this, 
                        isEditMode ? currentName[0] : name, 
                        isEditMode ? currentEmail[0] : email, 
                        businessName, taxId, businessPhone, businessAddress, logoBase64String);

                if (isEditMode) {
                    // Update Profile directly
                    FirebaseHelper.getInstance().saveUserProfile(currentName[0], currentEmail[0], businessName, taxId, businessPhone, businessAddress, logoBase64String, profileTask -> {
                        btnSaveProfile.setEnabled(true);
                        btnSaveProfile.setText("Update Profile");
                        if (profileTask.isSuccessful()) {
                            Toast.makeText(activity_business_registration.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            String errMsg = profileTask.getException() != null ? profileTask.getException().getMessage() : "Sync failed.";
                            Toast.makeText(activity_business_registration.this, "Saved locally (Cloud sync failed: " + errMsg + ")", Toast.LENGTH_LONG).show();
                        }
                        finish(); // Return to settings screen
                    });
                } else if (isGoogleSignup) {
                    // Save Profile directly (already authenticated with Google)
                    FirebaseHelper.getInstance().saveUserProfile(name, email, businessName, taxId, businessPhone, businessAddress, logoBase64String, profileTask -> {
                        btnSaveProfile.setEnabled(true);
                        btnSaveProfile.setText("Complete Setup");
                        if (profileTask.isSuccessful()) {
                            Toast.makeText(activity_business_registration.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        } else {
                            String errMsg = profileTask.getException() != null ? profileTask.getException().getMessage() : "Sync failed.";
                            Toast.makeText(activity_business_registration.this, "Profile created locally (Cloud sync failed: " + errMsg + ")", Toast.LENGTH_LONG).show();
                        }
                        Intent intent = new Intent(activity_business_registration.this, activity_dashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    // Sign up flow with email/password
                    FirebaseHelper.getInstance().signup(email, password, authTask -> {
                        if (authTask.isSuccessful()) {
                            // 2. Save Business Profile in Firestore
                            FirebaseHelper.getInstance().saveUserProfile(name, email, businessName, taxId, businessPhone, businessAddress, logoBase64String, profileTask -> {
                                btnSaveProfile.setEnabled(true);
                                btnSaveProfile.setText("Complete Setup");
                                if (profileTask.isSuccessful()) {
                                    Toast.makeText(activity_business_registration.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                } else {
                                    String errMsg = profileTask.getException() != null ? profileTask.getException().getMessage() : "Sync failed.";
                                    Toast.makeText(activity_business_registration.this, "Profile created locally (Cloud sync failed: " + errMsg + ")", Toast.LENGTH_LONG).show();
                                }
                                Intent intent = new Intent(activity_business_registration.this, activity_dashboard.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            btnSaveProfile.setEnabled(true);
                            btnSaveProfile.setText("Complete Setup");
                            String errMsg = authTask.getException() != null ? authTask.getException().getMessage() : "Registration failed.";
                            Toast.makeText(activity_business_registration.this, "Registration Error: " + errMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }

        // Link tvSkipSetup click handler
        TextView tvSkipSetup = findViewById(R.id.tvSkipSetup);
        if (tvSkipSetup != null) {
            tvSkipSetup.setOnClickListener(v -> {
                Intent intent = new Intent(activity_business_registration.this, activity_dashboard.class);
                startActivity(intent);
                finish();
            });
        }
    }
}