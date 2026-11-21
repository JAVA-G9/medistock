package com.medistock.repository;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Pharmacy and User persistence.
 * Owner: Member 1.
 * Evaluation 1: In-memory implementation.
 * Evaluation 2: JDBC implementation.
 */
public interface PharmacyRepository {

    // Pharmacy operations
    Optional<Pharmacy> findById(String id);

    Optional<Pharmacy> findByLicenseNumber(String licenseNumber);

    List<Pharmacy> findAll();

    Pharmacy save(Pharmacy pharmacy);

    void update(Pharmacy pharmacy);

    boolean deleteById(String id);

    // User operations
    Optional<User> findUserById(String id);

    Optional<User> findUserByUsername(String username);

    List<User> findUsersByPharmacyId(String pharmacyId);

    User saveUser(User user);

    void updateUser(User user);
}
