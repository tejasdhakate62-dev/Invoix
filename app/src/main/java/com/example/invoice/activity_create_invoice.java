package com.example.invoice;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class activity_create_invoice extends AppCompatActivity {

    private EditText etInvoiceNumber, etClientName, etClientEmail, etInvoiceDate, etDueDate, etTaxRate;
    private TextView tvInvoiceTotal, tvAddItem;
    private LinearLayout llItemsContainer;
    private Button btnGenerateInvoice;
    private RadioGroup rgDueDateOption;
    private RadioButton rbDueImmediately, rbDueCustom;

    private final ArrayList<InvoiceItem> invoiceItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_invoice);

        // Handle Edge-to-Edge safe layout bar offsets via root view ID "main"
        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        etInvoiceNumber = findViewById(R.id.etInvoiceNumber);
        etClientName = findViewById(R.id.etClientName);
        etClientEmail = findViewById(R.id.etClientEmail);
        etInvoiceDate = findViewById(R.id.etInvoiceDate);
        etDueDate = findViewById(R.id.etDueDate);
        etTaxRate = findViewById(R.id.etTaxRate);
        tvInvoiceTotal = findViewById(R.id.tvInvoiceTotal);
        tvAddItem = findViewById(R.id.tvAddItem);
        llItemsContainer = findViewById(R.id.llItemsContainer);
        btnGenerateInvoice = findViewById(R.id.btnGenerateInvoice);
        rgDueDateOption = findViewById(R.id.rgDueDateOption);
        rbDueImmediately = findViewById(R.id.rbDueImmediately);
        rbDueCustom = findViewById(R.id.rbDueCustom);

        // Clear default static sample items in container
        if (llItemsContainer != null) {
            llItemsContainer.removeAllViews();
        }

        // Auto-populate Invoice Date with today's date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String today = sdf.format(new Date());
        if (etInvoiceDate != null) {
            etInvoiceDate.setText(today);
        }

        // Due date option toggling
        if (rgDueDateOption != null) {
            rgDueDateOption.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbDueCustom) {
                    if (etDueDate != null) {
                        etDueDate.setVisibility(View.VISIBLE);
                        // Trigger DatePickerDialog when custom due date field is clicked
                        etDueDate.setOnClickListener(v -> showDatePickerDialog());
                    }
                } else {
                    if (etDueDate != null) {
                        etDueDate.setVisibility(View.GONE);
                        etDueDate.setText(""); // clear selected date
                    }
                }
            });
        }

        // Action: Click "+ Add Item" -> Show dynamic popup dialog
        if (tvAddItem != null) {
            tvAddItem.setOnClickListener(v -> showAddItemDialog());
        }

        // Action: Update invoice totals in real-time when tax rate changes
        if (etTaxRate != null) {
            etTaxRate.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    calculateInvoiceTotal();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Action: Handle Next step to forward user input data onto template selector screen
        if (btnGenerateInvoice != null) {
            btnGenerateInvoice.setOnClickListener(v -> {
                String invoiceNum = etInvoiceNumber != null ? etInvoiceNumber.getText().toString().trim() : "";
                String clientName = etClientName != null ? etClientName.getText().toString().trim() : "";
                String clientEmail = etClientEmail != null ? etClientEmail.getText().toString().trim() : "";
                String taxRateStr = etTaxRate != null ? etTaxRate.getText().toString().trim() : "";

                if (invoiceNum.isEmpty()) {
                    Toast.makeText(activity_create_invoice.this, "Please enter an invoice number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (clientName.isEmpty()) {
                    Toast.makeText(activity_create_invoice.this, "Please enter client name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (clientEmail.isEmpty()) {
                    Toast.makeText(activity_create_invoice.this, "Please enter client email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Determine due date value based on choice
                String finalDueDate;
                if (rgDueDateOption != null && rgDueDateOption.getCheckedRadioButtonId() == R.id.rbDueImmediately) {
                    finalDueDate = etInvoiceDate != null ? etInvoiceDate.getText().toString().trim() : today;
                } else {
                    finalDueDate = etDueDate != null ? etDueDate.getText().toString().trim() : "";
                    if (finalDueDate.isEmpty()) {
                        Toast.makeText(activity_create_invoice.this, "Please select a custom due date", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if (invoiceItems.isEmpty()) {
                    Toast.makeText(activity_create_invoice.this, "Please add at least one line item", Toast.LENGTH_SHORT).show();
                    return;
                }

                double taxRate = 0;
                try {
                    taxRate = taxRateStr.isEmpty() ? 0 : Double.parseDouble(taxRateStr);
                } catch (NumberFormatException ignored) {}

                double total = getInvoiceSubtotal() * (1 + taxRate / 100);

                // Create full Invoice model
                Invoice invoice = new Invoice(
                        invoiceNum,
                        clientName,
                        clientEmail,
                        finalDueDate, // set determined due date
                        taxRate,
                        total,
                        "Pending", // default status
                        "classic", // default template
                        System.currentTimeMillis(),
                        invoiceItems
                );

                // Populate business profile snapshot details from local SharedPreferences
                android.content.SharedPreferences prefs = getSharedPreferences("InvoixPrefs", MODE_PRIVATE);
                invoice.setBusinessName(prefs.getString("businessName", ""));
                invoice.setBusinessAddress(prefs.getString("businessAddress", ""));
                invoice.setBusinessPhone(prefs.getString("businessPhone", ""));
                invoice.setBusinessTaxId(prefs.getString("businessTaxId", ""));
                invoice.setBusinessLogoBase64(prefs.getString("businessLogoBase64", ""));

                // Intent routing setup downstream to template selector page
                Intent intent = new Intent(activity_create_invoice.this, activity_template_selector.class);
                intent.putExtra("INVOICE", invoice);
                startActivity(intent);
            });
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year1, monthOfYear, dayOfMonth);
                    SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    if (etDueDate != null) {
                        etDueDate.setText(format.format(selectedCal.getTime()));
                    }
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showAddItemDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("Item name or description");
        layout.addView(etName);

        final EditText etPrice = new EditText(this);
        etPrice.setHint("Price ($)");
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etPrice);

        new AlertDialog.Builder(this)
                .setTitle("Add Invoice Item")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String itemName = etName.getText().toString().trim();
                    String itemPriceStr = etPrice.getText().toString().trim();

                    if (itemName.isEmpty()) {
                        Toast.makeText(activity_create_invoice.this, "Item name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price = 0;
                    try {
                        price = itemPriceStr.isEmpty() ? 0 : Double.parseDouble(itemPriceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(activity_create_invoice.this, "Invalid price amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    InvoiceItem item = new InvoiceItem(itemName, price);
                    invoiceItems.add(item);
                    addDynamicItemView(item);
                    calculateInvoiceTotal();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addDynamicItemView(InvoiceItem item) {
        View itemView = getLayoutInflater().inflate(R.layout.item_invoice, llItemsContainer, false);
        
        TextView tvItemName = itemView.findViewById(R.id.tvInvoiceNum); // Reusing standard view IDs
        TextView tvItemPrice = itemView.findViewById(R.id.tvTotalAmount);
        TextView tvClient = itemView.findViewById(R.id.tvClientName);
        TextView tvDue = itemView.findViewById(R.id.tvDueDate);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        TextView tvAction = itemView.findViewById(R.id.tvToggleStatusAction);

        // Hide unnecessary components for sub-item card view inside create page
        tvClient.setVisibility(View.GONE);
        tvDue.setVisibility(View.GONE);
        tvStatus.setVisibility(View.GONE);
        tvAction.setText("Remove");
        tvAction.setTextColor(Color.RED);

        String currencySymbol = LocalDatabaseHelper.getCurrencySymbol(this);

        tvItemName.setText(item.getName());
        tvItemPrice.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, item.getPrice()));

        tvAction.setOnClickListener(v -> {
            invoiceItems.remove(item);
            llItemsContainer.removeView(itemView);
            calculateInvoiceTotal();
        });

        llItemsContainer.addView(itemView);
    }

    private double getInvoiceSubtotal() {
        double subtotal = 0;
        for (InvoiceItem item : invoiceItems) {
            subtotal += item.getPrice();
        }
        return subtotal;
    }

    private void calculateInvoiceTotal() {
        double subtotal = getInvoiceSubtotal();
        String taxStr = etTaxRate != null ? etTaxRate.getText().toString().trim() : "";
        double taxRate = 0;
        try {
            taxRate = taxStr.isEmpty() ? 0 : Double.parseDouble(taxStr);
        } catch (NumberFormatException ignored) {}

        double total = subtotal * (1 + taxRate / 100);
        if (tvInvoiceTotal != null) {
            String currencySymbol = LocalDatabaseHelper.getCurrencySymbol(this);
            tvInvoiceTotal.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, total));
        }
    }
}