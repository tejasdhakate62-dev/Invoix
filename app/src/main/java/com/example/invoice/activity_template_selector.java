package com.example.invoice;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class activity_template_selector extends AppCompatActivity {

    private LinearLayout llTemplateClassic, llTemplateMinimal, llTemplateCompact;
    private Button btnConfirmTemplate;
    private Invoice invoice;
    private String selectedTemplate = "classic";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_template_selector);

        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Retrieve Invoice from Intent
        invoice = (Invoice) getIntent().getSerializableExtra("INVOICE");
        if (invoice == null) {
            Toast.makeText(this, "Failed to load invoice data.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        llTemplateClassic = findViewById(R.id.llTemplateClassic);
        llTemplateMinimal = findViewById(R.id.llTemplateMinimal);
        llTemplateCompact = findViewById(R.id.llTemplateCompact);
        btnConfirmTemplate = findViewById(R.id.btnConfirmTemplate);

        // Highlight default template
        selectTemplate("classic");

        // Action: Click Template Card -> Select and highlight
        if (llTemplateClassic != null) {
            llTemplateClassic.setOnClickListener(v -> selectTemplate("classic"));
        }
        if (llTemplateMinimal != null) {
            llTemplateMinimal.setOnClickListener(v -> selectTemplate("minimal"));
        }
        if (llTemplateCompact != null) {
            llTemplateCompact.setOnClickListener(v -> selectTemplate("compact"));
        }

        // Action: Complete flow, save to Firestore and open View Invoice page
        btnConfirmTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnConfirmTemplate.setEnabled(false);
                btnConfirmTemplate.setText("Generating...");

                invoice.setTemplate(selectedTemplate);

                // Save to Firestore
                FirebaseHelper.getInstance().saveInvoice(invoice, task -> {
                    btnConfirmTemplate.setEnabled(true);
                    btnConfirmTemplate.setText("Confirm Template");

                    if (task.isSuccessful() && task.getResult() != null) {
                        // Store the generated document ID
                        invoice.setId(task.getResult().getId());

                        // Save locally to keep local cache in sync!
                        LocalDatabaseHelper.saveInvoiceLocally(activity_template_selector.this, invoice);

                        Toast.makeText(activity_template_selector.this, "Invoice Created!", Toast.LENGTH_SHORT).show();

                        // Route directly to View Invoice screen
                        Intent intent = new Intent(activity_template_selector.this, activity_view_invoice.class);
                        intent.putExtra("INVOICE", invoice);
                        // Clean task stack to prevent going back to template selector
                        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                        startActivity(intent);
                        finish();
                    } else {
                        // Firebase sync failed (e.g. Permission Denied) - Fallback to local storage
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Database error.";
                        Toast.makeText(activity_template_selector.this, "Sync failed: " + errMsg + ". Saving locally...", Toast.LENGTH_LONG).show();
                        
                        // Save locally
                        LocalDatabaseHelper.saveInvoiceLocally(activity_template_selector.this, invoice);

                        // Route directly to View Invoice screen
                        Intent intent = new Intent(activity_template_selector.this, activity_view_invoice.class);
                        intent.putExtra("INVOICE", invoice);
                        // Clean task stack to prevent going back to template selector
                        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }

    private void selectTemplate(String name) {
        selectedTemplate = name;

        // Reset backgrounds
        if (llTemplateClassic != null) llTemplateClassic.setBackgroundColor(Color.TRANSPARENT);
        if (llTemplateMinimal != null) llTemplateMinimal.setBackgroundColor(Color.TRANSPARENT);
        if (llTemplateCompact != null) llTemplateCompact.setBackgroundColor(Color.TRANSPARENT);

        // Highlight selected template
        int highlightColor = Color.parseColor("#E3F2FD"); // Light blue accent background
        if (name.equals("classic") && llTemplateClassic != null) {
            llTemplateClassic.setBackgroundColor(highlightColor);
        } else if (name.equals("minimal") && llTemplateMinimal != null) {
            llTemplateMinimal.setBackgroundColor(highlightColor);
        } else if (name.equals("compact") && llTemplateCompact != null) {
            llTemplateCompact.setBackgroundColor(highlightColor);
        }
    }
}