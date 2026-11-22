package org.example.medistock.repository.exchange;

import org.example.medistock.model.ExchangeListing;
import org.example.medistock.model.ExchangeRequest;
import org.example.medistock.model.StockTransfer;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Stock Exchange operations.
 * Folder: repository/exchange/
 * Owned by Member 3 (Stock Exchange & Requests).
 * Evaluation 1: In-memory implementation (InMemoryExchangeRepository).
 * Evaluation 2: JDBC and MySQL transaction implementation.
 */
public interface ExchangeRepository {

    // Listing CRUD & Queries
    void saveListing(ExchangeListing listing);
    void updateListing(ExchangeListing listing);
    Optional<ExchangeListing> findListingById(String listingId);
    List<ExchangeListing> findAllListings();
    List<ExchangeListing> findActiveListings();
    List<ExchangeListing> findListingsByPharmacy(String pharmacyId);
    List<ExchangeListing> searchListings(String query);

    // Request CRUD & Queries
    void saveRequest(ExchangeRequest request);
    void updateRequest(ExchangeRequest request);
    Optional<ExchangeRequest> findRequestById(String requestId);
    List<ExchangeRequest> findAllRequests();
    List<ExchangeRequest> findRequestsByRequester(String requesterPharmacyId);
    List<ExchangeRequest> findRequestsForSeller(String sellerPharmacyId);
    List<ExchangeRequest> findRequestsByListingId(String listingId);

    // Transfer CRUD & Queries
    void saveTransfer(StockTransfer transfer);
    void updateTransfer(StockTransfer transfer);
    Optional<StockTransfer> findTransferById(String transferId);
    List<StockTransfer> findAllTransfers();
    List<StockTransfer> findTransfersByPharmacy(String pharmacyId);
}
