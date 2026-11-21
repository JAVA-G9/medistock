package com.medistock.service;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.repository.PharmacyRepository;
import com.medistock.repository.memory.InMemoryPharmacyRepository;
import com.medistock.util.SessionContext;

import java.util.List;
import java.util.Optional;

/**
 * Service for Pharmacy and User authentication, registration, and profile management.
 * Owner: Member 1.
 */
public class PharmacyService {

    private static final PharmacyService INSTANCE = new PharmacyService();

    private final PharmacyRepository pharmacyRepository;

    public PharmacyService() {
        this(new InMemoryPharmacyRepository());
    }

    public PharmacyService(PharmacyRepository pharmacyRepository) {
        this.pharmacyRepository = pharmacyRepository;
    }

    public static PharmacyService getInstance() {
        return INSTANCE;
    }

    /**
     * Authenticate user with username and password.
     * Sets active SessionContext on success.
     */
    public User login(String username, String password) throws AuthenticationException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Username and password are required.");
        }

        Optional<User> userOpt = pharmacyRepository.findUserByUsername(username.trim());
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("No account found with username: " + username);
        }

        User user = userOpt.get();
        if (!user.authenticate(password)) {
            throw new AuthenticationException("Invalid password. Please check your credentials.");
        }

        Optional<Pharmacy> pharmacyOpt = pharmacyRepository.findById(user.getPharmacyId());
        if (pharmacyOpt.isEmpty()) {
            throw new AuthenticationException("Associated pharmacy not found for user: " + username);
        }

        SessionContext.setSession(user, pharmacyOpt.get());
        return user;
    }

    /**
     * Register a new pharmacy and its primary pharmacist user account.
     */
    public User register(Pharmacy pharmacy, User user) throws ValidationException {
        if (pharmacy == null || user == null) {
            throw new ValidationException("Pharmacy and User details must be provided.");
        }

        if (pharmacy.getName() == null || pharmacy.getName().isBlank()) {
            throw new ValidationException("Pharmacy name is required.");
        }
        if (pharmacy.getLicenseNumber() == null || pharmacy.getLicenseNumber().isBlank()) {
            throw new ValidationException("Drug license number is required.");
        }
        if (pharmacyRepository.findByLicenseNumber(pharmacy.getLicenseNumber()).isPresent()) {
            throw new ValidationException("A pharmacy with license number " + pharmacy.getLicenseNumber() + " is already registered.");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new ValidationException("Username is required.");
        }
        if (user.getPassword() == null || user.getPassword().length() < 4) {
            throw new ValidationException("Password must be at least 4 characters long.");
        }
        if (pharmacyRepository.findUserByUsername(user.getUsername()).isPresent()) {
            throw new ValidationException("Username '" + user.getUsername() + "' is already taken.");
        }

        Pharmacy savedPharmacy = pharmacyRepository.save(pharmacy);
        user.setPharmacyId(savedPharmacy.getId());
        User savedUser = pharmacyRepository.saveUser(user);

        SessionContext.setSession(savedUser, savedPharmacy);
        return savedUser;
    }

    public void updatePharmacy(Pharmacy pharmacy) {
        pharmacyRepository.update(pharmacy);
        if (SessionContext.getCurrentPharmacy() != null &&
                SessionContext.getCurrentPharmacy().getId().equals(pharmacy.getId())) {
            SessionContext.setSession(SessionContext.getCurrentUser(), pharmacy);
        }
    }

    public void updateUser(User user) {
        pharmacyRepository.updateUser(user);
        if (SessionContext.getCurrentUser() != null &&
                SessionContext.getCurrentUser().getId().equals(user.getId())) {
            SessionContext.setSession(user, SessionContext.getCurrentPharmacy());
        }
    }

    public List<Pharmacy> getAllPharmacies() {
        return pharmacyRepository.findAll();
    }

    public Optional<Pharmacy> getPharmacyById(String id) {
        return pharmacyRepository.findById(id);
    }

    public PharmacyRepository getRepository() {
        return pharmacyRepository;
    }

    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
