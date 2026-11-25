package org.example.medistock.repository.exchange;

import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;
import org.example.medistock.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dual-tier implementation of ExchangeRepository:
 * 1. In-memory HashMap collections protected by synchronized methods for instant UI response (Unit 2 / Unit 3).
 * 2. JDBC MySQL/TiDB Cloud persistence using PreparedStatement and parameterized SQL (Unit 4 / Labs 10–14).
 * Owned by Member 3 (Stock Exchange & Requests).
 */
public class InMemoryExchangeRepository implements ExchangeRepository {

    // Standard Java Collections Framework (Unit 2 / Lab 1)
    private final Map<String, ExchangeListing> listings = new HashMap<>();
    private final Map<String, ExchangeRequest> requests = new HashMap<>();
    private final Map<String, StockTransfer> transfers = new HashMap<>();

    public InMemoryExchangeRepository() {
        // Initialize MySQL tables in background thread without blocking UI startup (Unit 4)
        new Thread(this::initDatabase, "ExchangeDB-Init").start();
    }

    /**
     * Creates database tables for Member 3's domain if they do not already exist (Unit 4 / Lab 10).
     */
    private synchronized void initDatabase() {
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                try (Statement stmt = con.createStatement()) {
                    // 1. exchange_listing table
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS exchange_listing (
                            listing_id VARCHAR(50) PRIMARY KEY,
                            seller_pharmacy_id VARCHAR(50) NOT NULL,
                            seller_pharmacy_name VARCHAR(255) NOT NULL,
                            medicine_name VARCHAR(255) NOT NULL,
                            batch_number VARCHAR(100) NOT NULL,
                            expiry_date DATE NOT NULL,
                            total_quantity INT NOT NULL,
                            reserved_quantity INT NOT NULL DEFAULT 0,
                            unit_price DECIMAL(10,2) NOT NULL,
                            notes TEXT,
                            status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

                    // 2. exchange_request table
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS exchange_request (
                            request_id VARCHAR(50) PRIMARY KEY,
                            listing_id VARCHAR(50) NOT NULL,
                            requester_pharmacy_id VARCHAR(50) NOT NULL,
                            requester_pharmacy_name VARCHAR(255) NOT NULL,
                            seller_pharmacy_id VARCHAR(50) NOT NULL,
                            medicine_name VARCHAR(255) NOT NULL,
                            requested_quantity INT NOT NULL,
                            offered_price DECIMAL(10,2) NOT NULL,
                            request_message TEXT,
                            status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

                    // 3. stock_transfer table
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS stock_transfer (
                            transfer_id VARCHAR(50) PRIMARY KEY,
                            request_id VARCHAR(50) NOT NULL,
                            listing_id VARCHAR(50) NOT NULL,
                            source_pharmacy_id VARCHAR(50) NOT NULL,
                            source_pharmacy_name VARCHAR(255) NOT NULL,
                            destination_pharmacy_id VARCHAR(50) NOT NULL,
                            destination_pharmacy_name VARCHAR(255) NOT NULL,
                            medicine_name VARCHAR(255) NOT NULL,
                            batch_number VARCHAR(100) NOT NULL,
                            quantity INT NOT NULL,
                            total_price DECIMAL(10,2) NOT NULL,
                            status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
                            tracking_number VARCHAR(100),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);
                    System.out.println("[ExchangeRepository] MySQL tables (exchange_listing, exchange_request, stock_transfer) verified/initialized.");
                }
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRepository] DB initialization note: " + e.getMessage());
        }
    }

    // --- Listing Operations ---
    @Override
    public synchronized void saveListing(ExchangeListing listing) {
        if (listing != null && listing.getListingId() != null) {
            listings.put(listing.getListingId(), listing);
            new Thread(() -> persistListingToDb(listing), "DbListingSave-Worker").start();
        }
    }

    @Override
    public synchronized void updateListing(ExchangeListing listing) {
        if (listing != null && listing.getListingId() != null) {
            listings.put(listing.getListingId(), listing);
            new Thread(() -> persistListingToDb(listing), "DbListingUpdate-Worker").start();
        }
    }

    private void persistListingToDb(ExchangeListing l) {
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = """
                    INSERT INTO exchange_listing (listing_id, seller_pharmacy_id, seller_pharmacy_name, medicine_name, batch_number, expiry_date, total_quantity, reserved_quantity, unit_price, notes, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        total_quantity = VALUES(total_quantity),
                        reserved_quantity = VALUES(reserved_quantity),
                        status = VALUES(status)
                """;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, l.getListingId());
                    ps.setString(2, l.getSellerPharmacyId());
                    ps.setString(3, l.getSellerPharmacyName());
                    ps.setString(4, l.getMedicineName());
                    ps.setString(5, l.getBatchNumber());
                    ps.setDate(6, java.sql.Date.valueOf(l.getExpiryDate()));
                    ps.setInt(7, l.getTotalQuantity());
                    ps.setInt(8, l.getReservedQuantity());
                    ps.setDouble(9, l.getUnitPrice());
                    ps.setString(10, l.getNotes());
                    ps.setString(11, l.getStatus().name());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRepository] DB saveListing notice: " + e.getMessage());
        }
    }

    @Override
    public synchronized ExchangeListing findListingById(String listingId) {
        return listings.get(listingId);
    }

    @Override
    public synchronized List<ExchangeListing> findAllListings() {
        return new ArrayList<>(listings.values());
    }

    @Override
    public synchronized List<ExchangeListing> findActiveListings() {
        List<ExchangeListing> active = new ArrayList<>();
        for (ExchangeListing l : listings.values()) {
            if (l.getStatus() == ExchangeListing.ListingStatus.AVAILABLE && l.getAvailableQuantity() > 0) {
                active.add(l);
            }
        }
        return active;
    }

    @Override
    public synchronized List<ExchangeListing> findListingsByPharmacy(String pharmacyId) {
        List<ExchangeListing> result = new ArrayList<>();
        for (ExchangeListing l : listings.values()) {
            if (l.getSellerPharmacyId().equalsIgnoreCase(pharmacyId)) {
                result.add(l);
            }
        }
        return result;
    }

    @Override
    public synchronized List<ExchangeListing> searchListings(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findActiveListings();
        }
        String lower = query.trim().toLowerCase();
        List<ExchangeListing> matches = new ArrayList<>();
        for (ExchangeListing l : listings.values()) {
            if (l.getStatus() == ExchangeListing.ListingStatus.AVAILABLE) {
                boolean matchesName = l.getMedicineName() != null && l.getMedicineName().toLowerCase().contains(lower);
                boolean matchesBatch = l.getBatchNumber() != null && l.getBatchNumber().toLowerCase().contains(lower);
                boolean matchesSeller = l.getSellerPharmacyName() != null && l.getSellerPharmacyName().toLowerCase().contains(lower);
                if (matchesName || matchesBatch || matchesSeller) {
                    matches.add(l);
                }
            }
        }
        return matches;
    }

    // --- Request Operations ---
    @Override
    public synchronized void saveRequest(ExchangeRequest request) {
        if (request != null && request.getRequestId() != null) {
            requests.put(request.getRequestId(), request);
            new Thread(() -> persistRequestToDb(request), "DbRequestSave-Worker").start();
        }
    }

    @Override
    public synchronized void updateRequest(ExchangeRequest request) {
        if (request != null && request.getRequestId() != null) {
            requests.put(request.getRequestId(), request);
            new Thread(() -> persistRequestToDb(request), "DbRequestUpdate-Worker").start();
        }
    }

    private void persistRequestToDb(ExchangeRequest r) {
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = """
                    INSERT INTO exchange_request (request_id, listing_id, requester_pharmacy_id, requester_pharmacy_name, seller_pharmacy_id, medicine_name, requested_quantity, offered_price, request_message, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE status = VALUES(status)
                """;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, r.getRequestId());
                    ps.setString(2, r.getListingId());
                    ps.setString(3, r.getRequesterPharmacyId());
                    ps.setString(4, r.getRequesterPharmacyName());
                    ps.setString(5, r.getSellerPharmacyId());
                    ps.setString(6, r.getMedicineName());
                    ps.setInt(7, r.getRequestedQuantity());
                    ps.setDouble(8, r.getOfferedPrice());
                    ps.setString(9, r.getRequestMessage());
                    ps.setString(10, r.getStatus().name());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRepository] DB saveRequest notice: " + e.getMessage());
        }
    }

    @Override
    public synchronized ExchangeRequest findRequestById(String requestId) {
        return requests.get(requestId);
    }

    @Override
    public synchronized List<ExchangeRequest> findAllRequests() {
        return new ArrayList<>(requests.values());
    }

    @Override
    public synchronized List<ExchangeRequest> findRequestsByRequester(String requesterPharmacyId) {
        List<ExchangeRequest> result = new ArrayList<>();
        for (ExchangeRequest r : requests.values()) {
            if (r.getRequesterPharmacyId().equalsIgnoreCase(requesterPharmacyId)) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public synchronized List<ExchangeRequest> findRequestsForSeller(String sellerPharmacyId) {
        List<ExchangeRequest> result = new ArrayList<>();
        for (ExchangeRequest r : requests.values()) {
            if (r.getSellerPharmacyId().equalsIgnoreCase(sellerPharmacyId)) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public synchronized List<ExchangeRequest> findRequestsByListingId(String listingId) {
        List<ExchangeRequest> result = new ArrayList<>();
        for (ExchangeRequest r : requests.values()) {
            if (r.getListingId().equalsIgnoreCase(listingId)) {
                result.add(r);
            }
        }
        return result;
    }

    // --- Transfer Operations ---
    @Override
    public synchronized void saveTransfer(StockTransfer transfer) {
        if (transfer != null && transfer.getTransferId() != null) {
            transfers.put(transfer.getTransferId(), transfer);
            new Thread(() -> persistTransferToDb(transfer), "DbTransferSave-Worker").start();
        }
    }

    @Override
    public synchronized void updateTransfer(StockTransfer transfer) {
        if (transfer != null && transfer.getTransferId() != null) {
            transfers.put(transfer.getTransferId(), transfer);
            new Thread(() -> persistTransferToDb(transfer), "DbTransferUpdate-Worker").start();
        }
    }

    private void persistTransferToDb(StockTransfer t) {
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = """
                    INSERT INTO stock_transfer (transfer_id, request_id, listing_id, source_pharmacy_id, source_pharmacy_name, destination_pharmacy_id, destination_pharmacy_name, medicine_name, batch_number, quantity, total_price, status, tracking_number)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE status = VALUES(status)
                """;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, t.getTransferId());
                    ps.setString(2, t.getRequestId());
                    ps.setString(3, t.getListingId());
                    ps.setString(4, t.getSourcePharmacyId());
                    ps.setString(5, t.getSourcePharmacyName());
                    ps.setString(6, t.getDestinationPharmacyId());
                    ps.setString(7, t.getDestinationPharmacyName());
                    ps.setString(8, t.getMedicineName());
                    ps.setString(9, t.getBatchNumber());
                    ps.setInt(10, t.getQuantity());
                    ps.setDouble(11, t.getTotalPrice());
                    ps.setString(12, t.getStatus().name());
                    ps.setString(13, t.getTrackingNumber());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRepository] DB saveTransfer notice: " + e.getMessage());
        }
    }

    @Override
    public synchronized StockTransfer findTransferById(String transferId) {
        return transfers.get(transferId);
    }

    @Override
    public synchronized List<StockTransfer> findAllTransfers() {
        return new ArrayList<>(transfers.values());
    }

    @Override
    public synchronized List<StockTransfer> findTransfersByPharmacy(String pharmacyId) {
        List<StockTransfer> result = new ArrayList<>();
        for (StockTransfer t : transfers.values()) {
            if (t.getSourcePharmacyId().equalsIgnoreCase(pharmacyId)
                    || t.getDestinationPharmacyId().equalsIgnoreCase(pharmacyId)) {
                result.add(t);
            }
        }
        return result;
    }
}
