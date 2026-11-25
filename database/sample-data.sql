-- Sample data for testing and demonstration
USE medistock;

-- Member 1: Pharmacies and Users
INSERT IGNORE INTO pharmacy (pharmacy_id, name, license_number, address, city, phone, email, total_inventory_items, near_expiry_count, active_listing_count)
VALUES
('PHARM-001', 'Apex Care Pharmacy', 'DL-2024-APEX01', '124 Healthcare Boulevard, Suite A', 'Metro City', '+1 (555) 234-5678', 'contact@apexcare.com', 142, 18, 5),
('PHARM-002', 'City Health Drugstore', 'DL-2024-CITY02', '45 Commercial Square, Downtown', 'Riverdale', '+1 (555) 345-6789', 'orders@cityhealth.com', 98, 12, 3),
('PHARM-003', 'GreenCross LifeSciences', 'DL-2024-GCR03', '78 Botanical Way, West End', 'Green Valley', '+1 (555) 456-7890', 'support@greencrosspharma.com', 210, 24, 8);

INSERT IGNORE INTO user (user_id, pharmacy_id, username, password, full_name, email, role, active)
VALUES
('USER-001', 'PHARM-001', 'admin', 'admin123', 'Dr. Marcus Vance', 'marcus.vance@apexcare.com', 'Chief Pharmacist', TRUE),
('USER-002', 'PHARM-002', 'citypharm', 'city123', 'Sarah Jenkins, PharmD', 'sarah.j@cityhealth.com', 'Inventory Director', TRUE),
('USER-003', 'PHARM-003', 'greencross', 'green123', 'Rajesh Patel', 'rajesh.p@greencrosspharma.com', 'Lead Pharmacist', TRUE);

INSERT IGNORE INTO medicine (medicine_id, medicine_name, generic_name, manufacturer_name, category, strength, dosage_form, description)
VALUES
(1, 'Paracetamol', 'Acetaminophen', 'GSK Healthcare', 'Pain Relief', '650mg', 'Tablet', 'Antipyretic and analgesic for fever and pain'),
(2, 'Amoxicillin', 'Amoxicillin Trihydrate', 'Cipla Ltd', 'Antibiotic', '500mg', 'Capsule', 'Broad-spectrum penicillin antibiotic'),
(3, 'Azithromycin', 'Azithromycin', 'Pfizer', 'Antibiotic', '500mg', 'Tablet', 'Macrolide antibiotic for respiratory tract infections'),
(4, 'Ciprofloxacin', 'Ciprofloxacin HCl', 'Bayer', 'Antibiotic', '500mg', 'Tablet', 'Fluoroquinolone antibiotic'),
(5, 'Pantoprazole', 'Pantoprazole Sodium', 'Alkem Labs', 'Antacid', '40mg', 'Tablet', 'Proton pump inhibitor for acid reflux'),
(6, 'Metformin', 'Metformin HCl', 'Sun Pharma', 'Antidiabetic', '500mg', 'Tablet', 'Biguanide antihyperglycemic'),
(7, 'Atorvastatin', 'Atorvastatin Calcium', 'Ranbaxy', 'Cardiovascular', '10mg', 'Tablet', 'Statin for cholesterol management'),
(8, 'Cetirizine', 'Cetirizine HCl', 'Dr. Reddys', 'Antihistamine', '10mg', 'Tablet', 'Second-generation antihistamine');

INSERT IGNORE INTO medicine_batch (medicine_id, medicine_name, batch_number, manufacturing_date, expiry_date, quantity_received, quantity_available, purchase_price, selling_price, pharmacy_id, status, received_date)
VALUES
(1, 'Paracetamol 650mg', 'BAT-PCM-091',
 DATE_SUB(CURDATE(), INTERVAL 11 MONTH),
 DATE_ADD(CURDATE(), INTERVAL 10 DAY),
 120, 120, 18.50, 25.00,
 'PHARM-001', 'AVAILABLE', CURDATE()),

(2, 'Amoxicillin 500mg', 'BAT-AMX-204',
 DATE_SUB(CURDATE(), INTERVAL 18 MONTH),
 DATE_ADD(CURDATE(), INTERVAL 24 DAY),
 85, 85, 42.00, 55.00,
 'PHARM-001', 'AVAILABLE', CURDATE()),

(3, 'Azithromycin 500mg', 'BAT-AZI-118',
 DATE_SUB(CURDATE(), INTERVAL 24 MONTH),
 DATE_SUB(CURDATE(), INTERVAL 5 DAY),
 40, 40, 65.00, 80.00,
 'PHARM-001', 'EXPIRED', CURDATE()),

(4, 'Ciprofloxacin 500mg', 'BAT-CIP-302',
 DATE_SUB(CURDATE(), INTERVAL 15 MONTH),
 DATE_ADD(CURDATE(), INTERVAL 14 DAY),
 60, 60, 38.00, 50.00,
 'PHARM-001', 'AVAILABLE', CURDATE()),

(5, 'Pantoprazole 40mg', 'BAT-PAN-841',
 DATE_SUB(CURDATE(), INTERVAL 10 MONTH),
 DATE_ADD(CURDATE(), INTERVAL 45 DAY),
 200, 200, 28.00, 38.00,
 'PHARM-001', 'AVAILABLE', CURDATE()),

(6, 'Metformin 500mg SR', 'BAT-MET-554',
 DATE_SUB(CURDATE(), INTERVAL 12 MONTH),
 DATE_ADD(CURDATE(), INTERVAL 55 DAY),
 150, 150, 15.00, 22.00,
 'PHARM-001', 'AVAILABLE', CURDATE()),

(7, 'Atorvastatin 10mg', 'BAT-ATO-712',
 DATE_SUB(CURDATE(), INTERVAL 6 MONTH),
 DATE_ADD(CURDATE(), INTERVAL 8 MONTH),
 180, 180, 52.00, 68.00,
 'PHARM-001', 'AVAILABLE', CURDATE()),

(8, 'Cetirizine 10mg', 'BAT-CET-409',
 DATE_SUB(CURDATE(), INTERVAL 4 MONTH),
 DATE_ADD(CURDATE(), INTERVAL 12 MONTH),
 300, 300, 8.50, 15.00,
 'PHARM-001', 'AVAILABLE', CURDATE());
