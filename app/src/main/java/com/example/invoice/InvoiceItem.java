package com.example.invoice;

import java.io.Serializable;

public class InvoiceItem implements Serializable {
    private String name;
    private double price;

    public InvoiceItem() {
        // Required for Firestore deserialization
    }

    public InvoiceItem(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
