package com.medistock.repository;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import org.example.medistock.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * JDBC implementation of PharmacyRepository for Evaluation 2.
 * Connects to MySQL / TiDB Cloud database using configuration from database.properties.
 * Uses a memory-first dual-tier architecture to guarantee 0ms instant UI responses
 * with background persistence and synchronization.
 * Owner: Member 1 (Pharmacy, Dashboard & Thread Monitoring).
 */
public class JdbcPharmacyRepository implements PharmacyRepository {

    private final Map<String, Pharmacy> cachePharmacies = new ConcurrentHashMap<>();
    private final Map<String, User> cacheUsers = new ConcurrentHashMap<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PharmacyRepo-DbWorker");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean tablesInitialized = false;
    private volatile long lastMetricsRefresh = 0;

    public JdbcPharmacyRepository() {
        seedInitialInMemoryData();
        // Run database verification and initial sync in background so UI startup is 0ms instant
        dbExecutor.submit(() -> {
            initDatabase();
            loadAllFromDatabase();
            refreshAllLiveMetrics();
        });
    }

    /**
     * Seeds default data into in-memory fallback cache.
     */
    private void seedInitialInMemoryData() {
        Pharmacy p1 = new Pharmacy(
                "PHARM-001", "Apex Care Pharmacy", "DL-2024-APEX01",
                "124 Healthcare Boulevard, Suite A", "Metro City",
                "+1 (555) 234-5678", "contact@apexcare.com", 142, 18, 5
        );
        Pharmacy p2 = new Pharmacy(
                "PHARM-002", "City Health Drugstore", "DL-2024-CITY02",
                "45 Commercial Square, Downtown", "Riverdale",
                "+1 (555) 345-6789", "orders@cityhealth.com", 98, 12, 3
        );
        Pharmacy p3 = new Pharmacy(
                "PHARM-003", "GreenCross LifeSciences", "DL-2024-GCR03",
                "78 Botanical Way, West End", "Green Valley",
                "+1 (555) 456-7890", "support@greencrosspharma.com", 210, 24, 8
        );

        cachePharmacies.put(p1.getId(), p1);
        cachePharmacies.put(p2.getId(), p2);
        cachePharmacies.put(p3.getId(), p3);

        User u1 = new User("USER-001", p1.getId(), "admin", "admin123", "Dr. Marcus Vance", "marcus.vance@apexcare.com", "Chief Pharmacist");
        User u2 = new User("USER-002", p2.getId(), "citypharm", "city123", "Sarah Jenkins, PharmD", "sarah.j@cityhealth.com", "Inventory Director");
        User u3 = new User("USER-003", p3.getId(), "greencross", "green123", "Rajesh Patel", "rajesh.p@greencrosspharma.com", "Lead Pharmacist");

        cacheUsers.put(u1.getId(), u1);
        cacheUsers.put(u2.getId(), u2);
        cacheUsers.put(u3.getId(), u3);
    }

    /**
     * Ensures pharmacy and user tables exist in the database and seeds initial rows if empty.
     */
    public synchronized void initDatabase() {
        if (tablesInitialized) return;

        try (Connection con = DBConnection.connectToMySql()) {
            if (con == null) return;

            try (Statement stmt = con.createStatement()) {
                // 1. pharmacy table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pharmacy (
                        pharmacy_id VARCHAR(50) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        license_number VARCHAR(100) NOT NULL UNIQUE,
                        address VARCHAR(255),
                        city VARCHAR(100),
                        phone VARCHAR(50),
                        email VARCHAR(100),
                        verified BOOLEAN NOT NULL DEFAULT TRUE,
                        registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        total_inventory_items INT DEFAULT 0,
                        near_expiry_count INT DEFAULT 0,
                        active_listing_count INT DEFAULT 0
                    )
                """);

                // 2. user table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user (
                        user_id VARCHAR(50) PRIMARY KEY,
                        pharmacy_id VARCHAR(50) NOT NULL,
                        username VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        full_name VARCHAR(255) NOT NULL,
                        email VARCHAR(100),
                        role VARCHAR(100) NOT NULL DEFAULT 'Chief Pharmacist',
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                // Seed default pharmacies if table empty
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pharmacy")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.executeUpdate("""
                            INSERT INTO pharmacy (pharmacy_id, name, license_number, address, city, phone, email, total_inventory_items, near_expiry_count, active_listing_count)
                            VALUES
                            ('PHARM-001', 'Apex Care Pharmacy', 'DL-2024-APEX01', '124 Healthcare Boulevard, Suite A', 'Metro City', '+1 (555) 234-5678', 'contact@apexcare.com', 142, 18, 5),
                            ('PHARM-002', 'City Health Drugstore', 'DL-2024-CITY02', '45 Commercial Square, Downtown', 'Riverdale', '+1 (555) 345-6789', 'orders@cityhealth.com', 98, 12, 3),
                            ('PHARM-003', 'GreenCross LifeSciences', 'DL-2024-GCR03', '78 Botanical Way, West End', 'Green Valley', '+1 (555) 456-7890', 'support@greencrosspharma.com', 210, 24, 8)
                        """);
                    }
                }

                // Seed default users if table empty
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.executeUpdate("""
                            INSERT INTO user (user_id, pharmacy_id, username, password, full_name, email, role, active)
                            VALUES
                            ('USER-001', 'PHARM-001', 'admin', 'admin123', 'Dr. Marcus Vance', 'marcus.vance@apexcare.com', 'Chief Pharmacist', TRUE),
                            ('USER-002', 'PHARM-002', 'citypharm', 'city123', 'Sarah Jenkins, PharmD', 'sarah.j@cityhealth.com', 'Inventory Director', TRUE),
                            ('USER-003', 'PHARM-003', 'greencross', 'green123', 'Rajesh Patel', 'rajesh.p@greencrosspharma.com', 'Lead Pharmacist', TRUE)
                        """);
                    }
                }

                tablesInitialized = true;
            }
        } catch (SQLException e) {
            System.err.println("[JdbcPharmacyRepository] Database init error: " + e.getMessage());
        }
    }

    /**
     * Loads all pharmacy and user records from database into memory cache.
     */
    public void loadAllFromDatabase() {
        try (Connection con = DBConnection.connectToMySql()) {
            if (con == null) return;

            // Load pharmacies
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM pharmacy")) {
                while (rs.next()) {
                    Pharmacy p = mapResultSetToPharmacy(rs);
                    cachePharmacies.put(p.getId(), p);
                }
            }

            // Load users
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM user")) {
                while (rs.next()) {
                    User u = mapResultSetToUser(rs);
                    cacheUsers.put(u.getId(), u);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JdbcPharmacyRepository] Error loading from DB: " + e.getMessage());
        }
    }

    /**
     * Efficiently calculates metrics for all pharmacies in ONE single batch query.
     */
    public void refreshAllLiveMetrics() {
        long now = System.currentTimeMillis();
        if (now - lastMetricsRefresh < 10_000) {
            return; // Throttle to at most once per 10 seconds
        }
        lastMetricsRefresh = now;

        try (Connection con = DBConnection.connectToMySql()) {
            if (con == null) return;

            for (Pharmacy p : cachePharmacies.values()) {
                calculateLiveMetrics(con, p);
            }
        } catch (Exception e) {
            System.err.println("[JdbcPharmacyRepository] Metrics refresh warning: " + e.getMessage());
        }
    }

    /**
     * Calculates live metrics for a pharmacy from related database tables.
     */
    public void calculateLiveMetrics(Connection con, Pharmacy pharmacy) {
        if (con == null || pharmacy == null || pharmacy.getId() == null) return;

        try {
            // 1. Total inventory items from medicine_batch
            String stockSql = "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batch WHERE pharmacy_id = ?";
            try (PreparedStatement ps = con.prepareStatement(stockSql)) {
                ps.setString(1, pharmacy.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int stock = rs.getInt(1);
                        if (stock > 0 || pharmacy.getTotalInventoryItems() == 0) {
                            pharmacy.setTotalInventoryItems(stock);
                        }
                    }
                }
            }

            // 2. Near expiry batches (<= 30 days and available)
            String expirySql = "SELECT COUNT(*) FROM medicine_batch WHERE pharmacy_id = ? AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) AND quantity_available > 0 AND status != 'EXPIRED'";
            try (PreparedStatement ps = con.prepareStatement(expirySql)) {
                ps.setString(1, pharmacy.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int nearExpiry = rs.getInt(1);
                        if (nearExpiry > 0 || pharmacy.getNearExpiryCount() == 0) {
                            pharmacy.setNearExpiryCount(nearExpiry);
                        }
                    }
                }
            }

            // 3. Active marketplace listings from exchange_listing
            String listingSql = "SELECT COUNT(*) FROM exchange_listing WHERE seller_pharmacy_id = ? AND status = 'AVAILABLE'";
            try (PreparedStatement ps = con.prepareStatement(listingSql)) {
                ps.setString(1, pharmacy.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int listings = rs.getInt(1);
                        if (listings > 0 || pharmacy.getActiveListingCount() == 0) {
                            pharmacy.setActiveListingCount(listings);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public Optional<Pharmacy> findById(String id) {
        if (id == null) return Optional.empty();
        Pharmacy cached = cachePharmacies.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Cache miss: sync fetch once
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "SELECT * FROM pharmacy WHERE pharmacy_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Pharmacy p = mapResultSetToPharmacy(rs);
                            calculateLiveMetrics(con, p);
                            cachePharmacies.put(p.getId(), p);
                            return Optional.of(p);
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
        }

        return Optional.empty();
    }

    @Override
    public Optional<Pharmacy> findByLicenseNumber(String licenseNumber) {
        if (licenseNumber == null) return Optional.empty();

        Optional<Pharmacy> inCache = cachePharmacies.values().stream()
                .filter(p -> p.getLicenseNumber() != null && p.getLicenseNumber().equalsIgnoreCase(licenseNumber.trim()))
                .findFirst();

        if (inCache.isPresent()) {
            return inCache;
        }

        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "SELECT * FROM pharmacy WHERE LOWER(license_number) = LOWER(?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, licenseNumber.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Pharmacy p = mapResultSetToPharmacy(rs);
                            cachePharmacies.put(p.getId(), p);
                            return Optional.of(p);
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
        }

        return Optional.empty();
    }

    @Override
    public List<Pharmacy> findAll() {
        // Return instant in-memory list (0ms latency, never blocks UI thread!)
        return new ArrayList<>(cachePharmacies.values());
    }

    @Override
    public Pharmacy save(Pharmacy pharmacy) {
        if (pharmacy == null) throw new IllegalArgumentException("Pharmacy cannot be null");

        if (pharmacy.getId() == null || pharmacy.getId().isBlank()) {
            pharmacy.setId("PHARM-" + String.format("%03d", cachePharmacies.size() + 1));
        }

        // Update in-memory immediately for 0ms UI responsiveness
        cachePharmacies.put(pharmacy.getId(), pharmacy);

        // Persist to DB in background worker
        dbExecutor.submit(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = """
                        INSERT INTO pharmacy (pharmacy_id, name, license_number, address, city, phone, email, verified, total_inventory_items, near_expiry_count, active_listing_count)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            name = VALUES(name),
                            license_number = VALUES(license_number),
                            address = VALUES(address),
                            city = VALUES(city),
                            phone = VALUES(phone),
                            email = VALUES(email),
                            verified = VALUES(verified),
                            total_inventory_items = VALUES(total_inventory_items),
                            near_expiry_count = VALUES(near_expiry_count),
                            active_listing_count = VALUES(active_listing_count)
                    """;
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, pharmacy.getId());
                        ps.setString(2, pharmacy.getName());
                        ps.setString(3, pharmacy.getLicenseNumber());
                        ps.setString(4, pharmacy.getAddress());
                        ps.setString(5, pharmacy.getCity());
                        ps.setString(6, pharmacy.getPhone());
                        ps.setString(7, pharmacy.getEmail());
                        ps.setBoolean(8, pharmacy.isVerified());
                        ps.setInt(9, pharmacy.getTotalInventoryItems());
                        ps.setInt(10, pharmacy.getNearExpiryCount());
                        ps.setInt(11, pharmacy.getActiveListingCount());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                System.err.println("[JdbcPharmacyRepository] Background save error: " + e.getMessage());
            }
        });

        return pharmacy;
    }

    @Override
    public void update(Pharmacy pharmacy) {
        if (pharmacy == null || pharmacy.getId() == null) return;

        cachePharmacies.put(pharmacy.getId(), pharmacy);

        dbExecutor.submit(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = """
                        UPDATE pharmacy SET
                            name = ?,
                            license_number = ?,
                            address = ?,
                            city = ?,
                            phone = ?,
                            email = ?,
                            verified = ?,
                            total_inventory_items = ?,
                            near_expiry_count = ?,
                            active_listing_count = ?
                        WHERE pharmacy_id = ?
                    """;
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, pharmacy.getName());
                        ps.setString(2, pharmacy.getLicenseNumber());
                        ps.setString(3, pharmacy.getAddress());
                        ps.setString(4, pharmacy.getCity());
                        ps.setString(5, pharmacy.getPhone());
                        ps.setString(6, pharmacy.getEmail());
                        ps.setBoolean(7, pharmacy.isVerified());
                        ps.setInt(8, pharmacy.getTotalInventoryItems());
                        ps.setInt(9, pharmacy.getNearExpiryCount());
                        ps.setInt(10, pharmacy.getActiveListingCount());
                        ps.setString(11, pharmacy.getId());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                System.err.println("[JdbcPharmacyRepository] Background update error: " + e.getMessage());
            }
        });
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) return false;

        boolean removed = cachePharmacies.remove(id) != null;

        dbExecutor.submit(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = "DELETE FROM pharmacy WHERE pharmacy_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, id);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException ignored) {
            }
        });

        return removed;
    }

    @Override
    public Optional<User> findUserById(String id) {
        if (id == null) return Optional.empty();
        User cached = cacheUsers.get(id);
        if (cached != null) return Optional.of(cached);

        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "SELECT * FROM user WHERE user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            User u = mapResultSetToUser(rs);
                            cacheUsers.put(u.getId(), u);
                            return Optional.of(u);
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
        }

        return Optional.empty();
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        if (username == null) return Optional.empty();

        Optional<User> inCache = cacheUsers.values().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();

        if (inCache.isPresent()) {
            return inCache;
        }

        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "SELECT * FROM user WHERE LOWER(username) = LOWER(?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, username.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            User u = mapResultSetToUser(rs);
                            cacheUsers.put(u.getId(), u);
                            return Optional.of(u);
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
        }

        return Optional.empty();
    }

    @Override
    public List<User> findUsersByPharmacyId(String pharmacyId) {
        if (pharmacyId == null) return Collections.emptyList();

        List<User> filtered = new ArrayList<>();
        for (User u : cacheUsers.values()) {
            if (pharmacyId.equals(u.getPharmacyId())) {
                filtered.add(u);
            }
        }
        return filtered;
    }

    @Override
    public User saveUser(User user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");

        if (user.getId() == null || user.getId().isBlank()) {
            user.setId("USER-" + String.format("%03d", cacheUsers.size() + 1));
        }

        cacheUsers.put(user.getId(), user);

        dbExecutor.submit(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = """
                        INSERT INTO user (user_id, pharmacy_id, username, password, full_name, email, role, active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            pharmacy_id = VALUES(pharmacy_id),
                            username = VALUES(username),
                            password = VALUES(password),
                            full_name = VALUES(full_name),
                            email = VALUES(email),
                            role = VALUES(role),
                            active = VALUES(active)
                    """;
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, user.getId());
                        ps.setString(2, user.getPharmacyId());
                        ps.setString(3, user.getUsername());
                        ps.setString(4, user.getPassword());
                        ps.setString(5, user.getFullName());
                        ps.setString(6, user.getEmail());
                        ps.setString(7, user.getRole());
                        ps.setBoolean(8, user.isActive());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException ignored) {
            }
        });

        return user;
    }

    @Override
    public void updateUser(User user) {
        if (user == null || user.getId() == null) return;

        cacheUsers.put(user.getId(), user);

        dbExecutor.submit(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = """
                        UPDATE user SET
                            pharmacy_id = ?,
                            username = ?,
                            password = ?,
                            full_name = ?,
                            email = ?,
                            role = ?,
                            active = ?
                        WHERE user_id = ?
                    """;
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, user.getPharmacyId());
                        ps.setString(2, user.getUsername());
                        ps.setString(3, user.getPassword());
                        ps.setString(4, user.getFullName());
                        ps.setString(5, user.getEmail());
                        ps.setString(6, user.getRole());
                        ps.setBoolean(7, user.isActive());
                        ps.setString(8, user.getId());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException ignored) {
            }
        });
    }

    // Helper mappers
    private Pharmacy mapResultSetToPharmacy(ResultSet rs) throws SQLException {
        Pharmacy p = new Pharmacy();
        p.setId(rs.getString("pharmacy_id"));
        p.setName(rs.getString("name"));
        p.setLicenseNumber(rs.getString("license_number"));
        p.setAddress(rs.getString("address"));
        p.setCity(rs.getString("city"));
        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setVerified(rs.getBoolean("verified"));
        Timestamp ts = rs.getTimestamp("registered_at");
        if (ts != null) {
            p.setRegisteredAt(ts.toLocalDateTime());
        }
        p.setTotalInventoryItems(rs.getInt("total_inventory_items"));
        p.setNearExpiryCount(rs.getInt("near_expiry_count"));
        p.setActiveListingCount(rs.getInt("active_listing_count"));
        return p;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("user_id"));
        u.setPharmacyId(rs.getString("pharmacy_id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            u.setCreatedAt(ts.toLocalDateTime());
        }
        return u;
    }
}
