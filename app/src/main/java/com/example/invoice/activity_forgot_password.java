package com.example.invoice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class activity_forgot_password extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnResetPassword;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        etResetEmail = findViewById(R.id.etResetEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Action: Go back to login page
        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(activity_forgot_password.this, activity_login.class);
            startActivity(intent);
            finish();
        });

        // Action: Submit recovery link (Firebase Auth)
        btnResetPassword.setOnClickListener(v -> {
            String email = etResetEmail != null ? etResetEmail.getText().toString().trim() : "";
            if (email.isEmpty()) {
                Toast.makeText(activity_forgot_password.this, "Please enter your recovery email address", Toast.LENGTH_SHORT).show();
                return;
            }

            btnResetPassword.setEnabled(false);
            btnResetPassword.setText("Sending link...");

            FirebaseHelper.getInstance().sendPasswordResetEmail(email, task -> {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Send Recovery Link");
                if (task.isSuccessful()) {
                    Toast.makeText(activity_forgot_password.this, "Recovery link sent to your email!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String errMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset link.";
                    Toast.makeText(activity_forgot_password.this, "Error: " + errMsg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}