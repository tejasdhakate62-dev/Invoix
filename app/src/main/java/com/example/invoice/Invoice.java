package com.example.invoice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Invoice implements Serializable {
    private String id;
    private String invoiceNumber;
    private String clientName;
    private String clientEmail;
    private String dueDate;
    private double taxRate;
    private double totalAmount;
    private String status; // "Paid" or "Pending"
    private String template; // "classic", "minimal", "compact"
    private long createdAt;
    private List<InvoiceItem> items;

    // Expanded Premium audit fields
    private String clientPhone;
    private double discountRate;
    private String notes;
    private String terms;
    private boolean hasGst;

    // Business Profile Snapshot fields
    private String businessName;
    private String businessAddress;
    private String businessPhone;
    private String businessTaxId;
    private String businessLogoBase64;

    public Invoice() {
        // Required for Firestore deserialization
        this.items = new ArrayList<>();
    }

    public Invoice(String invoiceNumber, String clientName, String clientEmail, String dueDate, double taxRate, double totalAmount, String status, String template, long createdAt, List<InvoiceItem> items) {
        this.invoiceNumber = invoiceNumber;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.dueDate = dueDate;
        this.taxRate = taxRate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.template = template;
        this.createdAt = createdAt;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getBusinessPhone() {
        return businessPhone;
    }

    public void setBusinessPhone(String businessPhone) {
        this.businessPhone = businessPhone;
    }

    public String getBusinessTaxId() {
        return businessTaxId;
    }

    public void setBusinessTaxId(String businessTaxId) {
        this.businessTaxId = businessTaxId;
    }

    public String getBusinessLogoBase64() {
        return businessLogoBase64;
    }

    public void setBusinessLogoBase64(String businessLogoBase64) {
        this.businessLogoBase64 = businessLogoBase64;
    }

    // Getters and Setters for Expanded Premium fields
    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public boolean isHasGst() {
        return hasGst;
    }

    public void setHasGst(boolean hasGst) {
        this.hasGst = hasGst;
    }
}
