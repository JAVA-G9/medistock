package org.example.medistock.repository.exchange;

import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe in-memory implementation of ExchangeRepository.
 * Uses standard HashMap collections protected by synchronized methods
 * adhering to the Chitkara Java Course Plan (Unit 2: Collections & Unit 3: Synchronization).
 * Folder: repository/exchange/
 * Owned by Member 3.
 */
public class InMemoryExchangeRepository implements ExchangeRepository {

    // Standard Java Collections Framework (Unit 2 / Lab 1)
    private final Map<String, ExchangeListing> listings = new HashMap<>();
    private final Map<String, ExchangeRequest> requests = new HashMap<>();
    private final Map<String, StockTransfer> transfers = new HashMap<>();

    // --- Listing Operations ---
    @Override
    public synchronized void saveListing(ExchangeListing listing) {
        if (listing != null && listing.getListingId() != null) {
            listings.put(listing.getListingId(), listing);
        }
    }

    @Override
    public synchronized void updateListing(ExchangeListing listing) {
        if (listing != null && listing.getListingId() != null) {
            listings.put(listing.getListingId(), listing);
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
        }
    }

    @Override
    public synchronized void updateRequest(ExchangeRequest request) {
        if (request != null && request.getRequestId() != null) {
            requests.put(request.getRequestId(), request);
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
        }
    }

    @Override
    public synchronized void updateTransfer(StockTransfer transfer) {
        if (transfer != null && transfer.getTransferId() != null) {
            transfers.put(transfer.getTransferId(), transfer);
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
