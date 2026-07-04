package com.example.invoice;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class activity_dashboard extends AppCompatActivity {

    private TextView tvBusinessProfileName, tvTotalPaidValue, tvPendingDuesValue, tvViewAllInvoices;
    private LinearLayout llEmptyState, llInvoicesListContainer;
    private Button btnCreateNewInvoice;

    private String currentFilter = "ALL";
    private TextView tabFilterAll, tabFilterPaid, tabFilterPending;
    private View viewSegmentedThumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Handle Edge-to-Edge window spacing layout bar offsets safely via root view ID "main"
        View rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize UI Elements
        tvBusinessProfileName = findViewById(R.id.tvBusinessProfileName);
        tvTotalPaidValue = findViewById(R.id.tvTotalPaidValue);
        tvPendingDuesValue = findViewById(R.id.tvPendingDuesValue);
        tvViewAllInvoices = findViewById(R.id.tvViewAllInvoices);
        llEmptyState = findViewById(R.id.llEmptyState);
        llInvoicesListContainer = findViewById(R.id.llInvoicesListContainer);
        btnCreateNewInvoice = findViewById(R.id.btnCreateNewInvoice);

        // Initialize Filter Tabs & Sliding Thumb
        tabFilterAll = findViewById(R.id.tabFilterAll);
        tabFilterPaid = findViewById(R.id.tabFilterPaid);
        tabFilterPending = findViewById(R.id.tabFilterPending);
        viewSegmentedThumb = findViewById(R.id.viewSegmentedThumb);
        View flSegmentedControlContainer = findViewById(R.id.flSegmentedControlContainer);

        if (flSegmentedControlContainer != null && viewSegmentedThumb != null) {
            flSegmentedControlContainer.post(() -> {
                int padding = flSegmentedControlContainer.getPaddingLeft() + flSegmentedControlContainer.getPaddingRight();
                int containerWidth = flSegmentedControlContainer.getWidth() - padding;
                int thumbWidth = containerWidth / 3;
                
                android.view.ViewGroup.LayoutParams params = viewSegmentedThumb.getLayoutParams();
                params.width = thumbWidth;
                viewSegmentedThumb.setLayoutParams(params);
                
                updateFilterTabStyles(false); // Initial load without glide animation
            });
        }

        if (flSegmentedControlContainer != null) {
            flSegmentedControlContainer.setOnTouchListener(new View.OnTouchListener() {
                private float startX = 0;
                private float startY = 0;
                private long startTouchTime = 0;
                private boolean isScrolling = false;
                private int lastActiveIndex = -1;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (viewSegmentedThumb == null) return false;

                    int padding = flSegmentedControlContainer.getPaddingLeft() + flSegmentedControlContainer.getPaddingRight();
                    int containerWidth = flSegmentedControlContainer.getWidth() - padding;
                    if (containerWidth <= 0) return false;
                    int thumbWidth = containerWidth / 3;
                    if (thumbWidth <= 0) return false;

                    int[] location = new int[2];
                    flSegmentedControlContainer.getLocationOnScreen(location);
                    float parentRelativeX = event.getRawX() - location[0];

                    float minX = 0;
                    float maxX = containerWidth - thumbWidth;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            startY = event.getRawY();
                            startTouchTime = System.currentTimeMillis();
                            isScrolling = false;
                            lastActiveIndex = -1;
                            // Squeeze water droplet slightly on touch start
                            viewSegmentedThumb.animate().scaleY(1.15f).scaleX(0.9f).setDuration(120).start();
                            // Center thumb on touch
                            float targetXDown = parentRelativeX - (thumbWidth / 2f);
                            targetXDown = Math.max(minX, Math.min(maxX, targetXDown));
                            viewSegmentedThumb.setTranslationX(targetXDown);
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float deltaY = Math.abs(event.getRawY() - startY);
                            float deltaX = Math.abs(event.getRawX() - startX);

                            if (isScrolling) {
                                return false; // Let parent ScrollView handle it
                            }

                            if (deltaY > deltaX && deltaY > 15) {
                                isScrolling = true;
                                lastActiveIndex = -1;
                                // Snap scale back to normal
                                viewSegmentedThumb.animate().scaleY(1.0f).scaleX(1.0f).setDuration(100).start();
                                // Snap back to correct active tab position
                                updateFilterTabStyles(true);
                                return false; // Pass scroll down to ScrollView
                            }

                            float targetX = parentRelativeX - (thumbWidth / 2f);
                            targetX = Math.max(minX, Math.min(maxX, targetX));
                            viewSegmentedThumb.setTranslationX(targetX);
                            
                            // Highlight the closest text option as we hover/drag over it
                            int tempIndex = Math.round(targetX / thumbWidth);
                            tempIndex = Math.max(0, Math.min(2, tempIndex));
                            highlightHoveredTab(tempIndex);

                            // Play wobble feedback animation when crossing category boundary (matching tag-follower wobble!)
                            if (tempIndex != lastActiveIndex) {
                                lastActiveIndex = tempIndex;
                                viewSegmentedThumb.animate()
                                        .scaleX(0.9f)
                                        .scaleY(1.15f)
                                        .setDuration(120)
                                        .withEndAction(() -> {
                                            viewSegmentedThumb.animate()
                                                    .scaleX(1.0f)
                                                    .scaleY(1.0f)
                                                    .setDuration(180)
                                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                                                    .start();
                                        })
                                        .start();
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // Snap scale back to normal
                            viewSegmentedThumb.animate().scaleY(1.0f).scaleX(1.0f).setDuration(150).start();
                            lastActiveIndex = -1;

                            if (isScrolling) {
                                isScrolling = false;
                                return false;
                            }

                            long duration = System.currentTimeMillis() - startTouchTime;
                            float dX = Math.abs(event.getRawX() - startX);
                            float dY = Math.abs(event.getRawY() - startY);

                            int targetIndex;
                            if (duration < 250 && dX < 15 && dY < 15) {
                                // Tap event! Calculate segment index based on tap X coordinate
                                targetIndex = (int) (parentRelativeX / thumbWidth);
                                targetIndex = Math.max(0, Math.min(2, targetIndex));
                                
                                // Play macOS bounce animation on the selected text view
                                View tappedTab = null;
                                if (targetIndex == 0) tappedTab = tabFilterAll;
                                else if (targetIndex == 1) tappedTab = tabFilterPaid;
                                else if (targetIndex == 2) tappedTab = tabFilterPending;
                                animateTabClick(tappedTab);
                            } else {
                                // Drag event! Get the index closest to where we released our finger
                                targetIndex = Math.round(viewSegmentedThumb.getTranslationX() / thumbWidth);
                                targetIndex = Math.max(0, Math.min(2, targetIndex));
                            }

                            String newFilter = "ALL";
                            if (targetIndex == 1) newFilter = "PAID";
                            else if (targetIndex == 2) newFilter = "PENDING";

                            if (!currentFilter.equals(newFilter)) {
                                currentFilter = newFilter;
                                loadInvoices();
                            }
                            updateFilterTabStyles(true); // Snap to the selected tab with smooth horizontal glide
                            break;
                    }
                    return true;
                }
            });
        }

        // Initialize Bottom Navigation Views
        View llBottomHome = findViewById(R.id.llBottomHome);
        View llBottomCreate = findViewById(R.id.llBottomCreate);
        View llBottomSettings = findViewById(R.id.llBottomSettings);

        if (llBottomHome != null) {
            llBottomHome.setOnClickListener(v -> {
                animateTabClick(v);
                refreshDashboardData();
            });
        }
        if (llBottomCreate != null) {
            llBottomCreate.setOnClickListener(v -> {
                animateTabClick(v);
                v.postDelayed(() -> {
                    Intent intent = new Intent(activity_dashboard.this, activity_create_invoice.class);
                    startActivity(intent);
                }, 340);
            });
        }
        if (llBottomSettings != null) {
            llBottomSettings.setOnClickListener(v -> {
                animateTabClick(v);
                v.postDelayed(() -> {
                    Intent intent = new Intent(activity_dashboard.this, activity_settings.class);
                    startActivity(intent);
                }, 340);
            });
        }

        // Action: Click + Create New Invoice -> Link sequence directly to Create Invoice Form
        if (btnCreateNewInvoice != null) {
            btnCreateNewInvoice.setOnClickListener(v -> {
                animateTabClick(v);
                v.postDelayed(() -> {
                    Intent intent = new Intent(activity_dashboard.this, activity_create_invoice.class);
                    startActivity(intent);
                }, 340);
            });
        }

        // Action: Click Avatar Profile Placeholder -> Launch Profile Editor Screen
        View tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        if (tvProfileAvatar != null) {
            tvProfileAvatar.setOnClickListener(v -> {
                animateTabClick(v);
                v.postDelayed(() -> {
                    Intent intent = new Intent(activity_dashboard.this, activity_profile.class);
                    startActivity(intent);
                }, 340);
            });
        }

        // Action: Click View All -> Filter to All Invoices
        if (tvViewAllInvoices != null) {
            tvViewAllInvoices.setOnClickListener(v -> {
                animateTabClick(v);
                currentFilter = "ALL";
                updateFilterTabStyles(true);
                loadInvoices();
                Toast.makeText(activity_dashboard.this, "Showing all invoices", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load data on screen resume to capture updates dynamically
        refreshDashboardData();
    }

    private void refreshDashboardData() {
        if (!FirebaseHelper.getInstance().isUserLoggedIn()) {
            // Safety fallback: Redirect to login if user session expired
            Intent intent = new Intent(this, activity_login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        loadBusinessProfile();
        loadInvoices();
    }

    private void loadBusinessProfile() {
        // Load local cached business profile name first for instant UI response
        String localBizName = getSharedPreferences("InvoixPrefs", MODE_PRIVATE).getString("businessName", "Your Business Name");
        if (tvBusinessProfileName != null) {
            tvBusinessProfileName.setText(localBizName);
        }

        FirebaseHelper.getInstance().getUserProfile(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                String businessName = doc.getString("businessName");
                if (businessName != null && !businessName.isEmpty()) {
                    if (tvBusinessProfileName != null) {
                        tvBusinessProfileName.setText(businessName);
                    }
                    // Cache locally for offline/instant resume
                    getSharedPreferences("InvoixPrefs", MODE_PRIVATE).edit().putString("businessName", businessName).apply();
                }
            }
        });
    }

    private void loadInvoices() {
        FirebaseHelper.getInstance().getInvoices(task -> {
            java.util.List<Invoice> displayList = new java.util.ArrayList<>();
            java.util.List<Invoice> localList = LocalDatabaseHelper.getLocalInvoices(this);
            String currencySymbol = LocalDatabaseHelper.getCurrencySymbol(this);

            if (task.isSuccessful() && task.getResult() != null) {
                // 1. Add Firebase invoices
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Invoice invoice = doc.toObject(Invoice.class);
                    invoice.setId(doc.getId());
                    displayList.add(invoice);
                }

                // 2. Add local invoices that aren't on the server yet (compare by ID or Invoice number)
                for (Invoice localInv : localList) {
                    boolean existsOnServer = false;
                    for (Invoice serverInv : displayList) {
                        if (serverInv.getId().equals(localInv.getId()) || serverInv.getInvoiceNumber().equals(localInv.getInvoiceNumber())) {
                            existsOnServer = true;
                            break;
                        }
                    }
                    if (!existsOnServer) {
                        displayList.add(localInv);
                    }
                }
                // 3. Save the merged list locally to update the offline cache!
                LocalDatabaseHelper.saveInvoicesList(this, displayList);
            } else {
                // Firebase load failed (offline / rules error) - Fall back to local list
                displayList.addAll(localList);
            }

            llInvoicesListContainer.removeAllViews();
            
            double totalPaid = 0;
            double totalPending = 0;
            boolean hasFilteredInvoices = false;

            for (final Invoice invoice : displayList) {
                String status = invoice.getStatus() != null ? invoice.getStatus() : "Pending";
                
                // 1. Accumulate overall metrics stats
                if (status.startsWith("Paid")) {
                    totalPaid += invoice.getTotalAmount();
                } else {
                    totalPending += invoice.getTotalAmount();
                }

                // 2. Apply active filter tab check
                if (currentFilter.equals("PAID") && !status.startsWith("Paid")) {
                    continue;
                }
                if (currentFilter.equals("PENDING") && status.startsWith("Paid")) {
                    continue;
                }

                hasFilteredInvoices = true;

                // Populate dynamic View Card
                View invoiceCard = getLayoutInflater().inflate(R.layout.item_invoice, llInvoicesListContainer, false);
                
                TextView tvInvoiceNum = invoiceCard.findViewById(R.id.tvInvoiceNum);
                TextView tvClientName = invoiceCard.findViewById(R.id.tvClientName);
                TextView tvDueDate = invoiceCard.findViewById(R.id.tvDueDate);
                TextView tvTotalAmount = invoiceCard.findViewById(R.id.tvTotalAmount);
                TextView tvStatus = invoiceCard.findViewById(R.id.tvStatus);
                TextView tvToggleStatusAction = invoiceCard.findViewById(R.id.tvToggleStatusAction);

                tvInvoiceNum.setText(invoice.getInvoiceNumber());
                tvClientName.setText(invoice.getClientName());
                tvDueDate.setText("Due: " + invoice.getDueDate());
                tvTotalAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, invoice.getTotalAmount()));
                
                tvStatus.setText(status);

                // Check if system is in night mode to choose optimal high-contrast paid/pending colors
                boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;

                if (status.startsWith("Paid")) {
                    tvStatus.setTextColor(Color.parseColor(isNight ? "#00FF87" : "#007A5E")); // Neon Green (dark) vs Emerald Green (light)
                    tvStatus.setBackgroundColor(Color.parseColor(isNight ? "#2000FF87" : "#15007A5E")); // Translucent opacity
                    tvToggleStatusAction.setVisibility(View.GONE);
                } else {
                    tvStatus.setTextColor(Color.parseColor(isNight ? "#FF3366" : "#C62828")); // Neon Pink (dark) vs Crimson Red (light)
                    tvStatus.setBackgroundColor(Color.parseColor(isNight ? "#20FF3366" : "#15C62828")); // Translucent opacity
                    tvToggleStatusAction.setVisibility(View.VISIBLE);
                    tvToggleStatusAction.setText("Mark as Paid");
                    tvToggleStatusAction.setTextColor(Color.parseColor(isNight ? "#00FF87" : "#007A5E"));
                }

                // Click to View Invoice
                invoiceCard.setOnClickListener(v -> {
                    Intent intent = new Intent(activity_dashboard.this, activity_view_invoice.class);
                    intent.putExtra("INVOICE", invoice);
                    startActivity(intent);
                });

                // Click to Toggle Status
                tvToggleStatusAction.setOnClickListener(v -> {
                    if (status.startsWith("Paid")) {
                        updateInvoiceStatusOnServerOrLocal(invoice, "Pending");
                    } else {
                        showPaymentModeDialog(invoice);
                    }
                });

                llInvoicesListContainer.addView(invoiceCard);
            }

            // Update Metrics Card Values
            if (tvTotalPaidValue != null) {
                tvTotalPaidValue.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, totalPaid));
            }
            if (tvPendingDuesValue != null) {
                tvPendingDuesValue.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, totalPending));
            }

            // Toggle empty state visibility based on filtered matches
            if (hasFilteredInvoices) {
                llEmptyState.setVisibility(View.GONE);
                llInvoicesListContainer.setVisibility(View.VISIBLE);
            } else {
                llEmptyState.setVisibility(View.VISIBLE);
                llInvoicesListContainer.setVisibility(View.GONE);
            }
        });
    }

    private void updateInvoiceStatusOnServerOrLocal(Invoice invoice, String newStatus) {
        if (invoice.getId() != null && invoice.getId().startsWith("local_")) {
            // Toggle local invoice status
            LocalDatabaseHelper.updateLocalInvoiceStatus(activity_dashboard.this, invoice.getId(), newStatus);
            Toast.makeText(activity_dashboard.this, "Local status updated!", Toast.LENGTH_SHORT).show();
            refreshDashboardData();
        } else {
            // Toggle server invoice status
            FirebaseHelper.getInstance().updateInvoiceStatus(invoice.getId(), newStatus, updateTask -> {
                if (updateTask.isSuccessful()) {
                    invoice.setStatus(newStatus);
                    LocalDatabaseHelper.saveInvoiceLocally(activity_dashboard.this, invoice);
                    Toast.makeText(activity_dashboard.this, "Invoice status updated!", Toast.LENGTH_SHORT).show();
                    refreshDashboardData();
                } else {
                    String errMsg = updateTask.getException() != null ? updateTask.getException().getMessage() : "Sync failed.";
                    Toast.makeText(activity_dashboard.this, "Saved locally (Server update failed: " + errMsg + ")", Toast.LENGTH_LONG).show();
                    
                    // Fallback to local
                    invoice.setStatus(newStatus);
                    LocalDatabaseHelper.saveInvoiceLocally(activity_dashboard.this, invoice);
                    refreshDashboardData();
                }
            });
        }
    }

    private void showPaymentModeDialog(Invoice invoice) {
        String[] options = {"Cash", "Online"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Payment Method")
                .setItems(options, (dialog, which) -> {
                    String selectedMode = options[which];
                    String newStatus = "Paid (" + selectedMode + ")";
                    updateInvoiceStatusOnServerOrLocal(invoice, newStatus);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFilterTabStyles(boolean animate) {
        if (tabFilterAll == null || tabFilterPaid == null || tabFilterPending == null || viewSegmentedThumb == null) return;

        boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        // Reset sizes to default 12sp
        tabFilterAll.setTextSize(12);
        tabFilterPaid.setTextSize(12);
        tabFilterPending.setTextSize(12);

        // Reset colors
        int inactiveColor = Color.parseColor(isNight ? "#A099C0" : "#757575");
        int activeColor = Color.parseColor(isNight ? "#FFFFFF" : "#4F46E5"); // White in night, Indigo in light mode

        tabFilterAll.setTextColor(inactiveColor);
        tabFilterAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabFilterPaid.setTextColor(inactiveColor);
        tabFilterPaid.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabFilterPending.setTextColor(inactiveColor);
        tabFilterPending.setTypeface(null, android.graphics.Typeface.NORMAL);

        int thumbWidth = viewSegmentedThumb.getWidth();
        float targetX = 0;

        if (currentFilter.equals("ALL")) {
            tabFilterAll.setTextColor(activeColor);
            tabFilterAll.setTextSize(14); // Text Zoom effect!
            tabFilterAll.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFilterAll.animate().scaleX(1.08f).scaleY(1.08f).setDuration(180).start();
            tabFilterPaid.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();
            tabFilterPending.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();
            targetX = 0;
        } else if (currentFilter.equals("PAID")) {
            tabFilterPaid.setTextColor(activeColor);
            tabFilterPaid.setTextSize(14); // Text Zoom effect!
            tabFilterPaid.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFilterPaid.animate().scaleX(1.08f).scaleY(1.08f).setDuration(180).start();
            tabFilterAll.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();
            tabFilterPending.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();
            targetX = thumbWidth;
        } else if (currentFilter.equals("PENDING")) {
            tabFilterPending.setTextColor(activeColor);
            tabFilterPending.setTextSize(14); // Text Zoom effect!
            tabFilterPending.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFilterPending.animate().scaleX(1.08f).scaleY(1.08f).setDuration(180).start();
            tabFilterAll.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();
            tabFilterPaid.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start();
            targetX = thumbWidth * 2;
        }

        if (animate) {
            // Wobbly liquid elastic drop bounce effect (matching GSAP ease)!
            viewSegmentedThumb.setScaleX(1.25f);
            viewSegmentedThumb.setScaleY(0.75f);

            viewSegmentedThumb.animate()
                    .translationX(targetX)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(340) // slightly longer duration to accommodate the wobble settle
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.8f)) // Elastic overshoot!
                    .start();
        } else {
            viewSegmentedThumb.setTranslationX(targetX);
            viewSegmentedThumb.setScaleX(1.0f);
            viewSegmentedThumb.setScaleY(1.0f);
        }
    }

    private void highlightHoveredTab(int index) {
        if (tabFilterAll == null || tabFilterPaid == null || tabFilterPending == null) return;

        boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int inactiveColor = Color.parseColor(isNight ? "#A099C0" : "#757575");
        int activeColor = Color.parseColor(isNight ? "#FFFFFF" : "#4F46E5");

        // Reset all
        tabFilterAll.setTextColor(inactiveColor);
        tabFilterAll.setTextSize(12);
        tabFilterAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabFilterAll.setScaleX(1.0f);
        tabFilterAll.setScaleY(1.0f);

        tabFilterPaid.setTextColor(inactiveColor);
        tabFilterPaid.setTextSize(12);
        tabFilterPaid.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabFilterPaid.setScaleX(1.0f);
        tabFilterPaid.setScaleY(1.0f);

        tabFilterPending.setTextColor(inactiveColor);
        tabFilterPending.setTextSize(12);
        tabFilterPending.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabFilterPending.setScaleX(1.0f);
        tabFilterPending.setScaleY(1.0f);

        if (index == 0) {
            tabFilterAll.setTextColor(activeColor);
            tabFilterAll.setTextSize(14);
            tabFilterAll.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFilterAll.setScaleX(1.08f);
            tabFilterAll.setScaleY(1.08f);
        } else if (index == 1) {
            tabFilterPaid.setTextColor(activeColor);
            tabFilterPaid.setTextSize(14);
            tabFilterPaid.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFilterPaid.setScaleX(1.08f);
            tabFilterPaid.setScaleY(1.08f);
        } else if (index == 2) {
            tabFilterPending.setTextColor(activeColor);
            tabFilterPending.setTextSize(14);
            tabFilterPending.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFilterPending.setScaleX(1.08f);
            tabFilterPending.setScaleY(1.08f);
        }
    }

    private void animateTabClick(View view) {
        if (view == null) return;
        view.animate()
                .translationY(-15f)
                .setDuration(120)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .translationY(0f)
                            .setDuration(220)
                            .setInterpolator(new android.view.animation.BounceInterpolator())
                            .start();
                })
                .start();
    }
}