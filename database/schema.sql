-- =====================================================================
-- MediStock - Pharmacy Inventory & Expiry Management Database Schema
-- TiDB Cloud / MySQL 8.0 Compatible
-- =====================================================================

CREATE DATABASE IF NOT EXISTS medistock;
USE medistock;

-- 1. Medicine Catalog (Member 2)
CREATE TABLE IF NOT EXISTS medicine (
    medicine_id INT AUTO_INCREMENT PRIMARY KEY,
    medicine_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    manufacturer_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    strength VARCHAR(50),
    dosage_form VARCHAR(50),
    description TEXT
);

-- 2. Medicine Batches with Expiry Tracking (Member 4 & Member 2)
CREATE TABLE IF NOT EXISTS medicine_batch (
    batch_id INT AUTO_INCREMENT PRIMARY KEY,
    medicine_id INT,
    medicine_name VARCHAR(255) NOT NULL,
    batch_number VARCHAR(100) NOT NULL UNIQUE,
    manufacturing_date DATE NOT NULL,
    expiry_date DATE NOT NULL,

    quantity_received INT NOT NULL DEFAULT 0,
    quantity_available INT NOT NULL DEFAULT 0,
    purchase_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    received_date DATE,

    pharmacy_id VARCHAR(50) NOT NULL DEFAULT 'PHARM-001',
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    INDEX idx_expiry (expiry_date),
    INDEX idx_pharmacy (pharmacy_id)
);

-- 3. Centralized Notifications & Expiry Alerts (Member 4)
CREATE TABLE IF NOT EXISTS notification (
    notification_id VARCHAR(100) PRIMARY KEY,
    pharmacy_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pharmacy_read (pharmacy_id, is_read)
);
