package org.example.medistock.controller.inventory;
// Use the actual model package from your project.

import java.time.LocalDate;

public class showBean {

    private String medicineName;
    private String batchNumber;
    private int quantityReceived;
    private int quantityAvailable;
    private double purchasePrice;
    private double sellingPrice;
    private LocalDate expiryDate;

    public showBean(String medicineName, String batchNumber,
                    int quantityReceived, int quantityAvailable,
                    double purchasePrice, double sellingPrice,
                    LocalDate expiryDate) {

        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.quantityReceived = quantityReceived;
        this.quantityAvailable = quantityAvailable;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.expiryDate = expiryDate;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}