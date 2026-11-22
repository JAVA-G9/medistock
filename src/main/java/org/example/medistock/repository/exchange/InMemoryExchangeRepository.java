package org.example.medistock.repository.exchange;

import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory implementation of ExchangeRepository for Evaluation 1.
 * Folder: repository/exchange/
 * Owned by Member 3.
 */
public class InMemoryExchangeRepository implements ExchangeRepository {

    private final Map<String, ExchangeListing> listings = new ConcurrentHashMap<>();
    private final Map<String, ExchangeRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, StockTransfer> transfers = new ConcurrentHashMap<>();

    // --- Listing Operations ---
    @Override
    public void saveListing(ExchangeListing listing) {
        if (listing != null && listing.getListingId() != null) {
            listings.put(listing.getListingId(), listing);
        }
    }

    @Override
    public void updateListing(ExchangeListing listing) {
        if (listing != null && listing.getListingId() != null) {
            listings.put(listing.getListingId(), listing);
        }
    }

    @Override
    public Optional<ExchangeListing> findListingById(String listingId) {
        return Optional.ofNullable(listings.get(listingId));
    }

    @Override
    public List<ExchangeListing> findAllListings() {
        return new ArrayList<>(listings.values());
    }

    @Override
    public List<ExchangeListing> findActiveListings() {
        return listings.values().stream()
                .filter(l -> l.getStatus() == ExchangeListing.ListingStatus.AVAILABLE && l.getAvailableQuantity() > 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeListing> findListingsByPharmacy(String pharmacyId) {
        return listings.values().stream()
                .filter(l -> l.getSellerPharmacyId().equalsIgnoreCase(pharmacyId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeListing> searchListings(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findActiveListings();
        }
        String lower = query.trim().toLowerCase();
        return listings.values().stream()
                .filter(l -> l.getStatus() == ExchangeListing.ListingStatus.AVAILABLE)
                .filter(l -> (l.getMedicineName() != null && l.getMedicineName().toLowerCase().contains(lower))
                        || (l.getBatchNumber() != null && l.getBatchNumber().toLowerCase().contains(lower))
                        || (l.getSellerPharmacyName() != null && l.getSellerPharmacyName().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    // --- Request Operations ---
    @Override
    public void saveRequest(ExchangeRequest request) {
        if (request != null && request.getRequestId() != null) {
            requests.put(request.getRequestId(), request);
        }
    }

    @Override
    public void updateRequest(ExchangeRequest request) {
        if (request != null && request.getRequestId() != null) {
            requests.put(request.getRequestId(), request);
        }
    }

    @Override
    public Optional<ExchangeRequest> findRequestById(String requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    @Override
    public List<ExchangeRequest> findAllRequests() {
        return new ArrayList<>(requests.values());
    }

    @Override
    public List<ExchangeRequest> findRequestsByRequester(String requesterPharmacyId) {
        return requests.values().stream()
                .filter(r -> r.getRequesterPharmacyId().equalsIgnoreCase(requesterPharmacyId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeRequest> findRequestsForSeller(String sellerPharmacyId) {
        return requests.values().stream()
                .filter(r -> r.getSellerPharmacyId().equalsIgnoreCase(sellerPharmacyId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExchangeRequest> findRequestsByListingId(String listingId) {
        return requests.values().stream()
                .filter(r -> r.getListingId().equalsIgnoreCase(listingId))
                .collect(Collectors.toList());
    }

    // --- Transfer Operations ---
    @Override
    public void saveTransfer(StockTransfer transfer) {
        if (transfer != null && transfer.getTransferId() != null) {
            transfers.put(transfer.getTransferId(), transfer);
        }
    }

    @Override
    public void updateTransfer(StockTransfer transfer) {
        if (transfer != null && transfer.getTransferId() != null) {
            transfers.put(transfer.getTransferId(), transfer);
        }
    }

    @Override
    public Optional<StockTransfer> findTransferById(String transferId) {
        return Optional.ofNullable(transfers.get(transferId));
    }

    @Override
    public List<StockTransfer> findAllTransfers() {
        return new ArrayList<>(transfers.values());
    }

    @Override
    public List<StockTransfer> findTransfersByPharmacy(String pharmacyId) {
        return transfers.values().stream()
                .filter(t -> t.getSourcePharmacyId().equalsIgnoreCase(pharmacyId)
                        || t.getDestinationPharmacyId().equalsIgnoreCase(pharmacyId))
                .collect(Collectors.toList());
    }
}
