package com.example.invoice;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabaseHelper {
    private static final String PREF_NAME = "InvoixLocalDb";
    private static final String KEY_INVOICES = "local_invoices";

    public static String getCurrencySymbol(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("InvoixPrefs", Context.MODE_PRIVATE);
        int pos = prefs.getInt("currencyPosition", 0);
        switch (pos) {
            case 1: return "₹";
            case 2: return "€";
            case 3: return "£";
            case 0:
            default: return "$";
        }
    }

    public static void saveLocalProfile(Context context, String name, String email, String businessName, String taxId, String phone, String address, String logoBase64) {
        saveLocalProfile(context, name, email, "", businessName, taxId, phone, address, logoBase64);
    }

    public static void saveLocalProfile(Context context, String name, String email, String personalPhone, String businessName, String taxId, String phone, String address, String logoBase64) {
        SharedPreferences prefs = context.getSharedPreferences("InvoixPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("businessOwnerName", name)
                .putString("businessOwnerEmail", email)
                .putString("personalPhone", personalPhone)
                .putString("businessName", businessName)
                .putString("businessTaxId", taxId)
                .putString("businessPhone", phone)
                .putString("businessAddress", address)
                .putString("businessLogoBase64", logoBase64)
                .apply();
    }

    public static void saveInvoiceLocally(Context context, Invoice invoice) {
        List<Invoice> invoices = getLocalInvoices(context);
        
        // Generate a local dummy ID if null
        if (invoice.getId() == null || invoice.getId().isEmpty()) {
            invoice.setId("local_" + System.currentTimeMillis());
        }
        
        // Check if invoice already exists, if so update it, otherwise add it
        int index = -1;
        for (int i = 0; i < invoices.size(); i++) {
            if (invoices.get(i).getId().equals(invoice.getId())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            invoices.set(index, invoice);
        } else {
            invoices.add(0, invoice); // add to top (recent first)
        }

        saveInvoicesList(context, invoices);
    }

    public static List<Invoice> getLocalInvoices(Context context) {
        List<Invoice> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_INVOICES, null);
        if (jsonStr == null) return list;

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Invoice inv = new Invoice();
                inv.setId(obj.getString("id"));
                inv.setInvoiceNumber(obj.getString("invoiceNumber"));
                inv.setClientName(obj.getString("clientName"));
                inv.setClientEmail(obj.getString("clientEmail"));
                inv.setDueDate(obj.getString("dueDate"));
                inv.setTaxRate(obj.getDouble("taxRate"));
                inv.setTotalAmount(obj.getDouble("totalAmount"));
                inv.setStatus(obj.getString("status"));
                inv.setTemplate(obj.getString("template"));
                inv.setCreatedAt(obj.getLong("createdAt"));

                JSONArray itemsArr = obj.getJSONArray("items");
                List<InvoiceItem> items = new ArrayList<>();
                for (int j = 0; j < itemsArr.length(); j++) {
                    JSONObject itemObj = itemsArr.getJSONObject(j);
                    items.add(new InvoiceItem(itemObj.getString("name"), itemObj.getDouble("price")));
                }
                inv.setItems(items);
                list.add(inv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void saveInvoicesList(Context context, List<Invoice> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (Invoice inv : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", inv.getId());
                obj.put("invoiceNumber", inv.getInvoiceNumber());
                obj.put("clientName", inv.getClientName());
                obj.put("clientEmail", inv.getClientEmail());
                obj.put("dueDate", inv.getDueDate());
                obj.put("taxRate", inv.getTaxRate());
                obj.put("totalAmount", inv.getTotalAmount());
                obj.put("status", inv.getStatus());
                obj.put("template", inv.getTemplate());
                obj.put("createdAt", inv.getCreatedAt());

                JSONArray itemsArr = new JSONArray();
                for (InvoiceItem item : inv.getItems()) {
                    JSONObject itemObj = new JSONObject();
                    itemObj.put("name", item.getName());
                    itemObj.put("price", item.getPrice());
                    itemsArr.put(itemObj);
                }
                obj.put("items", itemsArr);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_INVOICES, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateLocalInvoiceStatus(Context context, String invoiceId, String status) {
        List<Invoice> invoices = getLocalInvoices(context);
        for (Invoice inv : invoices) {
            if (inv.getId().equals(invoiceId)) {
                inv.setStatus(status);
                break;
            }
        }
        saveInvoicesList(context, invoices);
    }
}
