package com.example.invoice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
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

public class activity_profile extends AppCompatActivity {

    private EditText etPersonalName, etPersonalEmail, etPersonalPhone;
    private EditText etBusinessName, etBusinessTaxId, etBusinessPhone, etBusinessAddress;
    private ImageView ivShopLogo;
    private Button btnUploadLogo, btnSaveProfile;
    private TextView btnBack;

    private String logoBase64String = "";
    private ActivityResultLauncher<Intent> pickLogoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize UI Elements
        btnBack = findViewById(R.id.btnBack);
        etPersonalName = findViewById(R.id.etPersonalName);
        etPersonalEmail = findViewById(R.id.etPersonalEmail);
        etPersonalPhone = findViewById(R.id.etPersonalPhone);
        
        ivShopLogo = findViewById(R.id.ivShopLogo);
        btnUploadLogo = findViewById(R.id.btnUploadLogo);
        etBusinessName = findViewById(R.id.etBusinessName);
        etBusinessTaxId = findViewById(R.id.etBusinessTaxId);
        etBusinessPhone = findViewById(R.id.etBusinessPhone);
        etBusinessAddress = findViewById(R.id.etBusinessAddress);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        // Setup gallery image selection launcher
        pickLogoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
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
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                pickLogoLauncher.launch(intent);
            });
        }
        if (ivShopLogo != null) {
            ivShopLogo.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                pickLogoLauncher.launch(intent);
            });
        }

        // Back button navigation
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Load Cache and Cloud Profile
        loadProfileData();

        // Save Profile Action
        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        }
    }

    private void loadProfileData() {
        // 1. Pre-populate from local preferences cache first (instant UI load)
        SharedPreferences prefs = getSharedPreferences("InvoixPrefs", MODE_PRIVATE);
        etPersonalName.setText(prefs.getString("businessOwnerName", ""));
        etPersonalEmail.setText(prefs.getString("businessOwnerEmail", ""));
        etPersonalPhone.setText(prefs.getString("personalPhone", ""));
        
        etBusinessName.setText(prefs.getString("businessName", ""));
        etBusinessTaxId.setText(prefs.getString("businessTaxId", ""));
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

        // 2. Fetch fresh profile details from Firestore to sync
        FirebaseHelper.getInstance().getUserProfile(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                
                String pName = doc.getString("name");
                String pEmail = doc.getString("email");
                String pPhone = doc.getString("personalPhone");
                String bName = doc.getString("businessName");
                String bTax = doc.getString("taxId");
                String bPhone = doc.getString("businessPhone");
                String bAddr = doc.getString("businessAddress");
                String logo = doc.getString("logoBase64");

                if (pName != null) etPersonalName.setText(pName);
                if (pEmail != null) etPersonalEmail.setText(pEmail);
                if (pPhone != null) etPersonalPhone.setText(pPhone);
                if (bName != null) etBusinessName.setText(bName);
                if (bTax != null) etBusinessTaxId.setText(bTax);
                if (bPhone != null) etBusinessPhone.setText(bPhone);
                if (bAddr != null) etBusinessAddress.setText(bAddr);

                if (logo != null && !logo.isEmpty()) {
                    logoBase64String = logo;
                    try {
                        byte[] decoded = Base64.decode(logo, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (ivShopLogo != null) ivShopLogo.setImageBitmap(bitmap);
                    } catch (Exception ignored) {}
                }

                // Sync local cache
                LocalDatabaseHelper.saveLocalProfile(activity_profile.this, pName, pEmail, pPhone, bName, bTax, bPhone, bAddr, logoBase64String);
            }
        });
    }

    private void saveProfileChanges() {
        String pName = etPersonalName.getText().toString().trim();
        String pEmail = etPersonalEmail.getText().toString().trim();
        String pPhone = etPersonalPhone.getText().toString().trim();
        String bName = etBusinessName.getText().toString().trim();
        String bTax = etBusinessTaxId.getText().toString().trim();
        String bPhone = etBusinessPhone.getText().toString().trim();
        String bAddr = etBusinessAddress.getText().toString().trim();

        if (pName.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bName.isEmpty()) {
            Toast.makeText(this, "Please enter your business name", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving changes...");

        // 1. Cache details locally
        LocalDatabaseHelper.saveLocalProfile(this, pName, pEmail, pPhone, bName, bTax, bPhone, bAddr, logoBase64String);

        // 2. Upload and sync with Firestore cloud database
        FirebaseHelper.getInstance().saveUserProfile(pName, pEmail, pPhone, bName, bTax, bPhone, bAddr, logoBase64String, task -> {
            btnSaveProfile.setEnabled(true);
            btnSaveProfile.setText("Save Changes");
            if (task.isSuccessful()) {
                Toast.makeText(activity_profile.this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String errMsg = task.getException() != null ? task.getException().getMessage() : "Offline storage synced.";
                Toast.makeText(activity_profile.this, "Saved offline (Cloud sync failed: " + errMsg + ")", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
