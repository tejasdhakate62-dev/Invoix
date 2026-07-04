package com.example.invoice;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

public class activity_view_invoice extends AppCompatActivity {

    private TextView tvViewBizName, tvViewBizAddress, tvViewBizPhone, tvViewBizTaxId;
    private TextView tvViewInvoiceTitle, tvViewInvoiceNum, tvViewClientName, tvViewClientEmail, tvViewDueDate;
    private TextView tvViewTaxRate, tvViewTotalAmount;
    private View vHeaderDivider;
    private LinearLayout llViewItemsContainer, llInvoiceSheet;
    private Button btnShare, btnPrint;
    private ImageView ivViewShopLogo;

    private Invoice invoice;
    private String businessName = "";
    private String businessAddress = "";
    private String businessPhone = "";
    private String businessTaxId = "";
    private String logoBase64String = "";
    private WebView printWebView;
    private boolean printLaunched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_invoice);

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
            Toast.makeText(this, "Failed to load invoice details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind Views
        tvViewBizName = findViewById(R.id.tvViewBizName);
        tvViewBizAddress = findViewById(R.id.tvViewBizAddress);
        tvViewBizPhone = findViewById(R.id.tvViewBizPhone);
        tvViewBizTaxId = findViewById(R.id.tvViewBizTaxId);
        tvViewInvoiceTitle = findViewById(R.id.tvViewInvoiceTitle);
        tvViewInvoiceNum = findViewById(R.id.tvViewInvoiceNum);
        tvViewClientName = findViewById(R.id.tvViewClientName);
        tvViewClientEmail = findViewById(R.id.tvViewClientEmail);
        tvViewDueDate = findViewById(R.id.tvViewDueDate);
        tvViewTaxRate = findViewById(R.id.tvViewTaxRate);
        tvViewTotalAmount = findViewById(R.id.tvViewTotalAmount);
        vHeaderDivider = findViewById(R.id.vHeaderDivider);
        llViewItemsContainer = findViewById(R.id.llViewItemsContainer);
        llInvoiceSheet = findViewById(R.id.llInvoiceSheet);
        btnShare = findViewById(R.id.btnShare);
        btnPrint = findViewById(R.id.btnPrint);
        ivViewShopLogo = findViewById(R.id.ivViewShopLogo);

        // Load Business Details from Firebase
        loadBusinessProfile();

        // Populate Invoice Details
        populateInvoiceDetails();

        // Setup Buttons
        btnPrint.setOnClickListener(v -> printInvoice());
        btnShare.setOnClickListener(v -> shareInvoiceText());
    }

    private void loadBusinessProfile() {
        // 1. Try to load profile details directly from the invoice's saved snapshot first (historical view)
        if (invoice != null && invoice.getBusinessName() != null && !invoice.getBusinessName().isEmpty()) {
            businessName = invoice.getBusinessName();
            businessAddress = invoice.getBusinessAddress();
            businessPhone = invoice.getBusinessPhone();
            businessTaxId = invoice.getBusinessTaxId();
            logoBase64String = invoice.getBusinessLogoBase64();

            tvViewBizName.setText(businessName);
            tvViewBizAddress.setText(businessAddress != null ? businessAddress : "");
            tvViewBizPhone.setText(businessPhone != null && !businessPhone.isEmpty() ? "Phone: " + businessPhone : "");
            tvViewBizTaxId.setText(businessTaxId != null && !businessTaxId.isEmpty() ? "Tax ID: " + businessTaxId : "");

            if (logoBase64String != null && !logoBase64String.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(logoBase64String, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (ivViewShopLogo != null) {
                        ivViewShopLogo.setImageBitmap(bitmap);
                        ivViewShopLogo.setVisibility(View.VISIBLE);
                    }
                } catch (Exception ignored) {}
            }
            return; // Snapshot found, do not fall back
        }

        // 2. Otherwise, fall back to current SharedPreferences / Firestore profile (for legacy invoices)
        // Load locally first for instant populate/offline fallback
        android.content.SharedPreferences prefs = getSharedPreferences("InvoixPrefs", MODE_PRIVATE);
        businessName = prefs.getString("businessName", "Your Business");
        businessAddress = prefs.getString("businessAddress", "");
        businessPhone = prefs.getString("businessPhone", "");
        businessTaxId = prefs.getString("businessTaxId", "");
        logoBase64String = prefs.getString("businessLogoBase64", "");

        tvViewBizName.setText(businessName);
        tvViewBizAddress.setText(businessAddress);
        tvViewBizPhone.setText(businessPhone.isEmpty() ? "" : "Phone: " + businessPhone);
        tvViewBizTaxId.setText(businessTaxId.isEmpty() ? "" : "Tax ID: " + businessTaxId);

        if (logoBase64String != null && !logoBase64String.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(logoBase64String, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (ivViewShopLogo != null) {
                    ivViewShopLogo.setImageBitmap(bitmap);
                    ivViewShopLogo.setVisibility(View.VISIBLE);
                }
            } catch (Exception ignored) {}
        }

        FirebaseHelper.getInstance().getUserProfile(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                businessName = doc.getString("businessName");
                businessAddress = doc.getString("businessAddress");
                businessPhone = doc.getString("businessPhone");
                businessTaxId = doc.getString("taxId");
                String logo = doc.getString("logoBase64");

                tvViewBizName.setText(businessName != null && !businessName.isEmpty() ? businessName : "Your Business");
                tvViewBizAddress.setText(businessAddress != null && !businessAddress.isEmpty() ? businessAddress : "");
                tvViewBizPhone.setText(businessPhone != null && !businessPhone.isEmpty() ? "Phone: " + businessPhone : "");
                tvViewBizTaxId.setText(businessTaxId != null && !businessTaxId.isEmpty() ? "Tax ID: " + businessTaxId : "");

                if (logo != null && !logo.isEmpty()) {
                    logoBase64String = logo;
                    try {
                        byte[] decoded = Base64.decode(logo, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (ivViewShopLogo != null) {
                            ivViewShopLogo.setImageBitmap(bitmap);
                            ivViewShopLogo.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception ignored) {}
                }

                // sync cache
                LocalDatabaseHelper.saveLocalProfile(activity_view_invoice.this, 
                        doc.getString("name"), doc.getString("email"), 
                        businessName, businessTaxId, businessPhone, businessAddress, logoBase64String);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (printLaunched) {
            printLaunched = false;
            Intent intent = new Intent(this, activity_dashboard.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }

    private void populateInvoiceDetails() {
        String currencySymbol = LocalDatabaseHelper.getCurrencySymbol(this);
        tvViewInvoiceNum.setText(invoice.getInvoiceNumber());
        tvViewClientName.setText(invoice.getClientName());
        tvViewClientEmail.setText(invoice.getClientEmail());
        tvViewDueDate.setText(invoice.getDueDate());
        tvViewTaxRate.setText(String.format(Locale.getDefault(), "%.1f%%", invoice.getTaxRate()));
        tvViewTotalAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, invoice.getTotalAmount()));

        // Populate items list
        llViewItemsContainer.removeAllViews();
        for (InvoiceItem item : invoice.getItems()) {
            View itemView = getLayoutInflater().inflate(R.layout.item_invoice, llViewItemsContainer, false);
            
            TextView tvItemName = itemView.findViewById(R.id.tvInvoiceNum); // Reusing standard view IDs
            TextView tvItemPrice = itemView.findViewById(R.id.tvTotalAmount);
            TextView tvClient = itemView.findViewById(R.id.tvClientName);
            TextView tvDue = itemView.findViewById(R.id.tvDueDate);
            TextView tvStatus = itemView.findViewById(R.id.tvStatus);
            TextView tvAction = itemView.findViewById(R.id.tvToggleStatusAction);

            // Re-style item card for item layout list view inside invoice sheet
            tvClient.setVisibility(View.GONE);
            tvDue.setVisibility(View.GONE);
            tvStatus.setVisibility(View.GONE);
            tvAction.setVisibility(View.GONE);
            itemView.setBackgroundColor(Color.TRANSPARENT);
            itemView.setPadding(0, 8, 0, 8);

            tvItemName.setText(item.getName());
            tvItemName.setTextSize(14);
            tvItemPrice.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, item.getPrice()));
            tvItemPrice.setTextSize(14);

            llViewItemsContainer.addView(itemView);
        }

        // Apply Template Styles
        applyTemplateStyles();
    }

    private void applyTemplateStyles() {
        String template = invoice.getTemplate() != null ? invoice.getTemplate().toLowerCase() : "classic";
        switch (template) {
            case "minimal":
                // Minimal: Dark charcoal accents, no header color, clean typography
                tvViewInvoiceTitle.setTextColor(Color.parseColor("#212121"));
                vHeaderDivider.setBackgroundColor(Color.parseColor("#E0E0E0"));
                tvViewTotalAmount.setTextColor(Color.parseColor("#212121"));
                break;
            case "compact":
                // Compact: Tight padding, dense font styling
                llInvoiceSheet.setPadding(12, 12, 12, 12);
                tvViewInvoiceTitle.setTextColor(Color.parseColor("#424242"));
                vHeaderDivider.setBackgroundColor(Color.parseColor("#9E9E9E"));
                tvViewTotalAmount.setTextColor(Color.parseColor("#424242"));
                break;
            case "classic":
            default:
                // Classic: Royal blue accents
                tvViewInvoiceTitle.setTextColor(Color.parseColor("#1976D2"));
                vHeaderDivider.setBackgroundColor(Color.parseColor("#1976D2"));
                tvViewTotalAmount.setTextColor(Color.parseColor("#1976D2"));
                break;
        }
    }

    private void printInvoice() {
        printWebView = new WebView(this);
        String htmlContent = getInvoiceHtml();
        printWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null);

        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager != null) {
            String jobName = "Invoice_" + invoice.getInvoiceNumber();
            PrintDocumentAdapter printAdapter = printWebView.createPrintDocumentAdapter(jobName);
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());

            // Set flag to navigate back when user returns from the print spooler
            printLaunched = true;
        } else {
            Toast.makeText(this, "Printing is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getInvoiceHtml() {
        String template = invoice.getTemplate() != null ? invoice.getTemplate().toLowerCase() : "classic";
        String color = "#1976D2"; // Classic
        if (template.equals("minimal")) {
            color = "#212121";
        } else if (template.equals("compact")) {
            color = "#424242";
        }

        String currencySymbol = LocalDatabaseHelper.getCurrencySymbol(this);
        String logoImgTag = "";
        if (logoBase64String != null && !logoBase64String.isEmpty()) {
            String cleanLogo = logoBase64String.replaceAll("\\s", "");
            logoImgTag = "<img src=\"data:image/png;base64," + cleanLogo + "\" style=\"max-height: 75px; max-width: 150px; object-fit: contain;\"/>";
        }

        StringBuilder itemsHtml = new StringBuilder();
        for (InvoiceItem item : invoice.getItems()) {
            itemsHtml.append(String.format(Locale.getDefault(),
                "<tr><td>%s</td><td style='text-align:right;'>%s%.2f</td></tr>",
                item.getName(), currencySymbol, item.getPrice()));
        }

        return "<html><head><style>" +
                "body { font-family: sans-serif; padding: 20px; color: #212121; }" +
                ".header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }" +
                ".biz-details { font-size: 12px; color: #757575; line-height: 1.5; }" +
                ".divider { height: 2px; background: " + color + "; margin: 20px 0; }" +
                ".bill-to { display: flex; justify-content: space-between; margin-bottom: 30px; }" +
                "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
                "th { background: #f5f5f5; padding: 10px 8px; text-align: left; font-size: 12px; border-bottom: 2px solid #e0e0e0; }" +
                "td { padding: 10px 8px; border-bottom: 1px solid #e0e0e0; font-size: 12px; }" +
                ".total { text-align: right; font-weight: bold; font-size: 14px; margin-top: 20px; color: " + color + "; }" +
                "</style></head><body>" +
                "<h1 style='text-align:center; color:" + color + "; margin:0 0 20px 0; font-size: 26px; text-transform: uppercase; letter-spacing: 2px;'>INVOICE</h1>" +
                "<div class='header'>" +
                "  <div class='biz-details'>" +
                "    <h2 style='margin:0 0 6px 0; color:#212121; font-size: 18px;'>" + (businessName != null ? businessName : "Your Business") + "</h2>" +
                "    <div>" + (businessAddress != null ? businessAddress : "") + "</div>" +
                "    <div>Phone: " + (businessPhone != null ? businessPhone : "") + "</div>" +
                "    <div>Tax ID: " + (businessTaxId != null ? businessTaxId : "") + "</div>" +
                "  </div>" +
                "  <div style='text-align:right;'>" +
                "    " + logoImgTag +
                "  </div>" +
                "</div>" +
                "<div class='divider'></div>" +
                "<div class='bill-to'>" +
                "  <div>" +
                "    <div style='font-size:10px; font-weight:bold; color:" + color + "; text-transform: uppercase;'>Bill To</div>" +
                "    <div style='font-weight:bold; font-size:14px; margin-top:4px;'>" + invoice.getClientName() + "</div>" +
                "    <div style='color:#757575; font-size:12px; margin-top:2px;'>" + invoice.getClientEmail() + "</div>" +
                "  </div>" +
                "  <div style='text-align:right; font-size:13px; line-height: 1.5;'>" +
                "    <div><strong>Invoice No:</strong> " + invoice.getInvoiceNumber() + "</div>" +
                "    <div style='margin-top:4px; color:#c62828;'><strong>Due Date:</strong> " + invoice.getDueDate() + "</div>" +
                "  </div>" +
                "</div>" +
                "<table>" +
                "  <thead><tr><th>Item Description</th><th style='text-align:right;'>Price</th></tr></thead>" +
                "  <tbody>" + itemsHtml.toString() + "</tbody>" +
                "</table>" +
                "<div class='total'>" +
                "  <div style='font-size:12px; color:#757575; font-weight:normal; margin-bottom:6px;'>Status: <span style='font-weight:bold; color:" + (invoice.getStatus() != null && invoice.getStatus().startsWith("Paid") ? "#2E7D32" : "#C62828") + ";'>" + (invoice.getStatus() != null ? invoice.getStatus() : "Pending") + "</span></div>" +
                "  <div style='font-size:12px; font-weight:normal; color:#757575;'>Tax Rate: " + String.format(Locale.getDefault(), "%.1f%%", invoice.getTaxRate()) + "</div>" +
                "  <div style='margin-top:8px; font-size:18px;'>Total Amount: " + String.format(Locale.getDefault(), "%s%.2f", currencySymbol, invoice.getTotalAmount()) + "</div>" +
                "</div>" +
                "</body></html>";
    }

    private void shareInvoiceText() {
        String currencySymbol = LocalDatabaseHelper.getCurrencySymbol(this);
        StringBuilder sb = new StringBuilder();
        sb.append("--- INVOICE ---\n");
        sb.append("From: ").append(businessName != null ? businessName : "Our Business").append("\n");
        if (businessAddress != null && !businessAddress.isEmpty()) sb.append("Address: ").append(businessAddress).append("\n");
        sb.append("Invoice No: ").append(invoice.getInvoiceNumber()).append("\n");
        sb.append("Due Date: ").append(invoice.getDueDate()).append("\n\n");
        sb.append("Bill To:\n");
        sb.append("Name: ").append(invoice.getClientName()).append("\n");
        sb.append("Email: ").append(invoice.getClientEmail()).append("\n\n");
        sb.append("Items:\n");
        for (InvoiceItem item : invoice.getItems()) {
            sb.append("- ").append(item.getName()).append(": ").append(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, item.getPrice())).append("\n");
        }
        sb.append("\nTax Rate: ").append(String.format(Locale.getDefault(), "%.1f%%", invoice.getTaxRate())).append("\n");
        sb.append("Total Amount: ").append(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, invoice.getTotalAmount())).append("\n");
        sb.append("Status: ").append(invoice.getStatus() != null ? invoice.getStatus() : "Pending").append("\n");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Invoice " + invoice.getInvoiceNumber());
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        startActivity(Intent.createChooser(shareIntent, "Share Invoice via"));
    }
}
