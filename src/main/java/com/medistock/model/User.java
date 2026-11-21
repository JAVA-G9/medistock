package com.medistock.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model representing a user / pharmacist account affiliated with a Pharmacy.
 * Owner: Member 1.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String pharmacyId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String role; // e.g., "Chief Pharmacist", "Inventory Manager", "Staff Pharmacist"
    private boolean active;
    private LocalDateTime createdAt;

    public User() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(String id, String pharmacyId, String username, String password, String fullName, String email, String role) {
        this.id = id;
        this.pharmacyId = pharmacyId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public boolean authenticate(String plainPassword) {
        return this.active && this.password != null && this.password.equals(plainPassword);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(String pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (@" + username + " - " + role + ")";
    }
}
