package com.medistock.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model representing a licensed pharmacy participating in the MediStock network.
 * Owner: Member 1.
 */
public class Pharmacy implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String licenseNumber;
    private String address;
    private String city;
    private String phone;
    private String email;
    private boolean verified;
    private LocalDateTime registeredAt;

    // Aggregated metrics for prototype display
    private int totalInventoryItems;
    private int nearExpiryCount;
    private int activeListingCount;

    public Pharmacy() {
        this.registeredAt = LocalDateTime.now();
        this.verified = true;
    }

    public Pharmacy(String id, String name, String licenseNumber, String address, String city, String phone, String email) {
        this.id = id;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.verified = true;
        this.registeredAt = LocalDateTime.now();
        this.totalInventoryItems = 0;
        this.nearExpiryCount = 0;
        this.activeListingCount = 0;
    }

    public Pharmacy(String id, String name, String licenseNumber, String address, String city, String phone, String email,
                    int totalInventoryItems, int nearExpiryCount, int activeListingCount) {
        this(id, name, licenseNumber, address, city, phone, email);
        this.totalInventoryItems = totalInventoryItems;
        this.nearExpiryCount = nearExpiryCount;
        this.activeListingCount = activeListingCount;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public int getTotalInventoryItems() {
        return totalInventoryItems;
    }

    public void setTotalInventoryItems(int totalInventoryItems) {
        this.totalInventoryItems = totalInventoryItems;
    }

    public int getNearExpiryCount() {
        return nearExpiryCount;
    }

    public void setNearExpiryCount(int nearExpiryCount) {
        this.nearExpiryCount = nearExpiryCount;
    }

    public int getActiveListingCount() {
        return activeListingCount;
    }

    public void setActiveListingCount(int activeListingCount) {
        this.activeListingCount = activeListingCount;
    }

    @Override
    public String toString() {
        return name + " (" + licenseNumber + " - " + city + ")";
    }
}
