package com.example.invoice;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

@SuppressWarnings("ALL")
public class activity_signup extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Handle Edge-to-Edge window spacing layout bar offsets safely via root view ID "main"
        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize UI Elements locally
        EditText etSignUpName = findViewById(R.id.etSignUpName);
        EditText etSignUpEmail = findViewById(R.id.etSignUpEmail);
        EditText etSignUpPassword = findViewById(R.id.etSignUpPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnSignUpNext = findViewById(R.id.btnSignUpNext);
        Button btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Configure Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Configure Google Sign-In launcher result
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken(), account.getDisplayName(), account.getEmail());
                            }
                        } catch (ApiException e) {
                            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        if (btnGoogleSignUp != null) {
            btnGoogleSignUp.setOnClickListener(v -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }

        // Action: Validate input and transition to business registration
        if (btnSignUpNext != null) {
            btnSignUpNext.setOnClickListener(v -> {
                String name = etSignUpName != null ? etSignUpName.getText().toString().trim() : "";
                String email = etSignUpEmail != null ? etSignUpEmail.getText().toString().trim() : "";
                String password = etSignUpPassword != null ? etSignUpPassword.getText().toString().trim() : "";
                String confirmPassword = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    Toast.makeText(activity_signup.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(activity_signup.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(activity_signup.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(activity_signup.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Intent routing to the next step of the onboarding profile set up
                Intent intent = new Intent(activity_signup.this, activity_business_registration.class);
                intent.putExtra("SIGNUP_NAME", name);
                intent.putExtra("SIGNUP_EMAIL", email);
                intent.putExtra("SIGNUP_PASSWORD", password);
                startActivity(intent);
            });
        }

        // Action: Click Back to Login -> Return to authentication screen
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> {
                Intent intent = new Intent(activity_signup.this, activity_login.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void firebaseAuthWithGoogle(String idToken, String displayName, String email) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseHelper.getInstance().loginWithCredential(credential, task -> {
            if (task.isSuccessful()) {
                // Check if Firestore user profile document exists
                FirebaseHelper.getInstance().getUserProfile(profileTask -> {
                    if (profileTask.isSuccessful() && profileTask.getResult() != null && profileTask.getResult().exists()) {
                        // User exists with profile -> Sync to local and go to dashboard
                        com.google.firebase.firestore.DocumentSnapshot doc = profileTask.getResult();
                        LocalDatabaseHelper.saveLocalProfile(
                                activity_signup.this,
                                doc.getString("name"),
                                doc.getString("email"),
                                doc.getString("businessName"),
                                doc.getString("taxId"),
                                doc.getString("businessPhone"),
                                doc.getString("businessAddress"),
                                doc.getString("logoBase64")
                        );
                        Toast.makeText(activity_signup.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(activity_signup.this, activity_dashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // New user -> Go to business registration setup page to complete profile details
                        Toast.makeText(activity_signup.this, "Sign-In Successful! Please set up your business details.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(activity_signup.this, activity_business_registration.class);
                        intent.putExtra("SIGNUP_NAME", displayName != null ? displayName : "");
                        intent.putExtra("SIGNUP_EMAIL", email != null ? email : "");
                        intent.putExtra("IS_GOOGLE_SIGNUP", true); // Flag to skip credentials creation
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                String errMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                Toast.makeText(activity_signup.this, "Google auth error: " + errMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}